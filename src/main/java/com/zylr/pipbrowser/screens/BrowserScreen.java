package com.zylr.pipbrowser.screens;

import com.cinemamod.mcef.MCEFBrowser;
import com.zylr.pipbrowser.PIPBrowser;
import com.zylr.pipbrowser.widgets.BrowserWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;

public class BrowserScreen extends Screen {
    private static final int TAB_HEIGHT = 20;
    private static final int NAV_BAR_HEIGHT = 20;
    // Insets for the fullscreen browser texture. Left/right/bottom should be 20px
    // while the top needs 40px (to sit below the tabs + nav bar).
    public static final int BROWSER_DRAW_LEFT = 20;
    public static final int BROWSER_DRAW_RIGHT = 20;
    public static final int BROWSER_DRAW_TOP = TAB_HEIGHT + NAV_BAR_HEIGHT; // 40
    public static final int BROWSER_DRAW_BOTTOM = 20;
    private static final int TAB_WIDTH = 150;

    private MCEFBrowser browser;
    private BrowserWidget myBrowser;
    private Button minimiseButton;
    private EditBox urlTextField;
    private Button externalBrowserButton;

    // Tab management - static to persist across screen opens/closes
    private static java.util.List<BrowserTab> persistedTabs = null;
    private static int persistedActiveTabIndex = 0;

    // Instance-level references
    private java.util.List<BrowserTab> tabs;
    private int activeTabIndex;

    // Tab scrolling
    private int tabScrollOffset = 0;
    private Button scrollLeftButton;
    private Button scrollRightButton;

    private double oldX, oldY;

    // Inner class to represent a browser tab
    private static class BrowserTab {
        BrowserWidget browserWidget;
        String title;
        Button tabButton;
        Button closeButton;
        int framesSinceCreation = 0;
        boolean hasBeenResized = false;

        BrowserTab(BrowserWidget widget, String title) {
            this.browserWidget = widget;
            this.title = title;
        }
    }

    public BrowserScreen() {
        this(null);
    }

    public BrowserScreen(String initialUrl) {
        super(Component.empty().append("Browser"));

        // Restore persisted tabs if they exist
        if (persistedTabs != null && !persistedTabs.isEmpty()) {
            this.tabs = persistedTabs;
            this.activeTabIndex = persistedActiveTabIndex;

            // Reset resize flags for all tabs so they get resized to new screen dimensions
            for (BrowserTab tab : tabs) {
                tab.framesSinceCreation = 0;
                tab.hasBeenResized = false;
            }

            // If an initialUrl was provided, create a new tab with it
            if (initialUrl != null && !initialUrl.isEmpty()) {
                BrowserWidget newBrowser = new BrowserWidget(0, 0, 267, 150, initialUrl);
                BrowserTab newTab = new BrowserTab(newBrowser, "Loading...");
                tabs.add(newTab);
                this.activeTabIndex = tabs.size() - 1;
            }
        } else {
            // No persisted tabs - create new tab list
            this.tabs = new java.util.ArrayList<>();

            // Get existing browser from HUD or create new one
            BrowserWidget existingBrowser = PIPBrowser.getInstance().mainHud.getBrowserWidget();
            if (existingBrowser == null) {
                String url = initialUrl != null ? initialUrl : "https://www.google.com";
                existingBrowser = new BrowserWidget(0, 0, 267, 150, url);
                PIPBrowser.getInstance().mainHud.setBrowserWidget(existingBrowser);
                PIPBrowser.getInstance().mainHud.fillHudList();
            }

            // Initialize first tab with existing browser
            BrowserTab firstTab = new BrowserTab(existingBrowser, "Tab 1");
            tabs.add(firstTab);
            this.activeTabIndex = 0;
        }
    }

    @Override
    protected void init() {
        super.init();

        // Clear any previous tab button references
        for (BrowserTab tab : tabs) {
            tab.tabButton = null;
            tab.closeButton = null;
        }

        // Get active tab's browser
        if (!tabs.isEmpty()) {
            BrowserTab activeTab = tabs.get(activeTabIndex);
            this.myBrowser = activeTab.browserWidget;
            this.browser = myBrowser.browser;

            if (browser == null) {
                return;
            }

            this.mouseReleased(new MouseButtonEvent(0, 0, new MouseButtonInfo(0, 0)));
            this.oldX = myBrowser.getRelativeAnchorX();
            this.oldY = myBrowser.getRelativeAnchorY();

            resizeBrowser();
        }

        // Create tab buttons (must be done after super.init() which clears widgets)
        createTabButtons();

        // Add navigation buttons
        Button backButton = Button.builder(Component.literal("←"), button -> {
                    if (browser != null) {
                        browser.goBack();
                    }
                    this.setFocused(null);
                })
                .bounds(0, TAB_HEIGHT, 30, 20)
                .build();

        Button forwardButton = Button.builder(Component.literal("→"), button -> {
                    if (browser != null) {
                        browser.goForward();
                    }
                    this.setFocused(null);
                })
                .bounds(35, TAB_HEIGHT, 30, 20)
                .build();

        Button refreshButton = Button.builder(Component.literal("⭮"), button -> {
                    if (this.browser != null) {
                        this.browser.reload();
                    }
                    this.setFocused(null);
                })
                .bounds(70, TAB_HEIGHT, 20, 20)
                .build();

        Button googleButton = Button.builder(Component.literal("⌂"), button -> {
                    if (browser != null) {
                        browser.loadURL("https://www.google.com");
                        if (this.urlTextField != null) {
                            this.urlTextField.setValue("https://www.google.com");
                            this.urlTextField.moveCursorToStart(false);
                        }
                    }
                    this.setFocused(null);
                })
                .bounds(95, TAB_HEIGHT, 20, 20)
                .build();

        // Add URL text field
        int urlFieldWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth() - 170;
        this.urlTextField = new EditBox(this.font, 120, TAB_HEIGHT + 2, urlFieldWidth, 16, Component.literal("URL"));
        this.urlTextField.setMaxLength(2048);

        String initialUrl = "https://www.google.com";
        if (browser != null) {
            String browserUrl = browser.getURL();
            if (browserUrl != null && !browserUrl.isEmpty() && !browserUrl.equals("about:blank")) {
                initialUrl = browserUrl;
            }
        }
        this.urlTextField.setValue(initialUrl);
        this.urlTextField.moveCursorToStart(false);
        this.urlTextField.setEditable(true);
        this.urlTextField.setCanLoseFocus(true);
        this.urlTextField.setResponder(text -> {});

        this.minimiseButton = Button.builder(
                        this.myBrowser.isVisible() ? Component.literal("-") : Component.literal("+"),
                        button -> {
                            if (this.myBrowser != null) {
                                this.myBrowser.setVisible(!this.myBrowser.isVisible());
                                if (this.myBrowser.isVisible()) {
                                    this.minimiseButton.setMessage(Component.literal("-"));
                                } else {
                                    this.minimiseButton.setMessage(Component.literal("+"));
                                }
                            }
                            this.setFocused(null);
                        })
                .bounds(Minecraft.getInstance().getWindow().getGuiScaledWidth() - 45, TAB_HEIGHT, 20, 20)
                .build();

        Button closeButton = Button.builder(Component.literal("✖"), button -> {
                    // Just close the screen, browsers remain open and persisted
                    this.onClose();
                })
                .bounds(Minecraft.getInstance().getWindow().getGuiScaledWidth() - 20, TAB_HEIGHT, 20, 20)
                .build();

        this.externalBrowserButton = Button.builder(Component.literal("🌐"), button -> {
                    PIPBrowser.LOGGER.info("External browser button clicked");
                    if (browser != null) {
                        String currentUrl = browser.getURL();
                        PIPBrowser.LOGGER.debug("Current URL: {}", currentUrl);
                        if (currentUrl != null && !currentUrl.isEmpty() && !currentUrl.equals("about:blank")) {
                            try {
                                PIPBrowser.LOGGER.info("Opening URL in external browser: {}", currentUrl);

                                // Use ProcessBuilder for cross-platform URL opening
                                String os = System.getProperty("os.name").toLowerCase();
                                ProcessBuilder pb;

                                if (os.contains("win")) {
                                    // Windows
                                    pb = new ProcessBuilder("cmd", "/c", "start", "", currentUrl);
                                } else if (os.contains("mac")) {
                                    // macOS
                                    pb = new ProcessBuilder("open", currentUrl);
                                } else {
                                    // Linux/Unix
                                    pb = new ProcessBuilder("xdg-open", currentUrl);
                                }

                                pb.start();
                                PIPBrowser.LOGGER.info("Successfully opened URL in external browser");
                            } catch (Exception e) {
                                PIPBrowser.LOGGER.error("Failed to open URL in external browser: {}", e.getMessage(), e);

                                // Fallback to Desktop API if ProcessBuilder fails
                                try {
                                    if (java.awt.Desktop.isDesktopSupported()) {
                                        java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                                        if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                                            desktop.browse(new java.net.URI(currentUrl));
                                            PIPBrowser.LOGGER.info("Opened URL using Desktop API fallback");
                                        }
                                    }
                                } catch (Exception fallbackException) {
                                    PIPBrowser.LOGGER.error("Desktop API fallback also failed: {}", fallbackException.getMessage(), fallbackException);
                                }
                            }
                        } else {
                            PIPBrowser.LOGGER.debug("No valid URL to open (URL is null, empty, or about:blank)");
                        }
                    } else {
                        PIPBrowser.LOGGER.debug("Browser is null");
                    }
                    this.setFocused(null);
                })
                .bounds(Minecraft.getInstance().getWindow().getGuiScaledWidth() - 45,
                        Minecraft.getInstance().getWindow().getGuiScaledHeight() - 20,
                        45, 20)
                .build();

        this.addRenderableWidget(backButton);
        this.addRenderableWidget(forwardButton);
        this.addRenderableWidget(refreshButton);
        this.addRenderableWidget(googleButton);
        this.addRenderableWidget(this.urlTextField);
        this.addRenderableWidget(minimiseButton);
        this.addRenderableWidget(closeButton);
        this.addRenderableWidget(this.externalBrowserButton);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);

        // Resize all browsers to match the new window size
        for (BrowserTab tab : tabs) {
            if (tab.browserWidget != null && tab.browserWidget.browser != null) {
                try {
                    int newWidth = scaleX(width);
                    int newHeight = scaleY(height);
                    if (newWidth > 100 && newHeight > 100) {
                        tab.browserWidget.browser.resize(newWidth, newHeight);
                        // Update widget dimensions
                        tab.browserWidget.setWidgetWidth(width);
                        tab.browserWidget.setWidgetHeight(height);
                    }
                } catch (Exception e) {
                    PIPBrowser.LOGGER.warn("Could not resize browser: {}", e.getMessage());
                }
            }
        }
    }

    @Override
    public void onClose() {
        // Release mouse capture and focus before closing
        if (browser != null) {
            browser.setFocus(false);
            browser.sendMouseRelease(-1, -1, -1); // Release any held mouse buttons
            browser.setFocus(false);
        }

        // Persist tabs and active tab index for next time screen opens
        persistedTabs = this.tabs;
        persistedActiveTabIndex = this.activeTabIndex;

        // Resize all browsers back to HUD size when closing BrowserScreen
        // Widget dimensions: 267x150 (logical GUI coordinates)
        // Browser resolution: 1920x1080 (texture size, will be scaled to fit widget)
        for (BrowserTab tab : tabs) {
            if (tab.browserWidget != null && tab.browserWidget.browser != null) {
                try {
                    tab.browserWidget.setWidgetWidth(267);
                    tab.browserWidget.setWidgetHeight(150);
                    tab.browserWidget.browser.resize(1920, 1080);
                    tab.browserWidget.setAnchorX(tab.browserWidget.getAnchorXFromRelative(oldX));
                    tab.browserWidget.setAnchorY(tab.browserWidget.getAnchorYFromRelative(oldY));
                } catch (Exception e) {
                    PIPBrowser.LOGGER.warn("Could not resize browser on close: {}", e.getMessage());
                }
            }
        }

        // Save the active tab's browser back to the HUD
        if (this.myBrowser != null) {
            PIPBrowser.getInstance().mainHud.setBrowserWidget(this.myBrowser);
            PIPBrowser.getInstance().mainHud.fillHudList();
        }

        // Force Minecraft to recapture the mouse
        Minecraft mc = Minecraft.getInstance();
        mc.mouseHandler.releaseMouse();
        mc.mouseHandler.grabMouse();

        super.onClose();

        // Release all keyboard state to prevent stuck keys - do this AFTER super.onClose()
        // to ensure it's the last thing that happens
        if (minecraft != null && minecraft.options != null) {
            // Reset all movement keys by setting them to not pressed and resetting press count
            minecraft.options.keyUp.setDown(false);
            minecraft.options.keyDown.setDown(false);
            minecraft.options.keyLeft.setDown(false);
            minecraft.options.keyRight.setDown(false);
            minecraft.options.keyJump.setDown(false);
            minecraft.options.keyShift.setDown(false);
            minecraft.options.keySprint.setDown(false);

            // Also reset the click count to prevent stuck keys
            while (minecraft.options.keyUp.consumeClick()) {}
            while (minecraft.options.keyDown.consumeClick()) {}
            while (minecraft.options.keyLeft.consumeClick()) {}
            while (minecraft.options.keyRight.consumeClick()) {}
            while (minecraft.options.keyJump.consumeClick()) {}
            while (minecraft.options.keyShift.consumeClick()) {}
            while (minecraft.options.keySprint.consumeClick()) {}
        }
    }

    private void createTabButtons() {
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int scrollButtonWidth = 20;
        int newTabButtonWidth = 30;

        // Calculate available width for tabs (screen width - scroll buttons - new tab button)
        int availableWidth = screenWidth - (scrollButtonWidth * 2) - newTabButtonWidth;
        int maxVisibleTabs = availableWidth / TAB_WIDTH;

        // Ensure scroll offset is valid
        if (tabScrollOffset < 0) {
            tabScrollOffset = 0;
        }
        if (tabScrollOffset > Math.max(0, tabs.size() - maxVisibleTabs)) {
            tabScrollOffset = Math.max(0, tabs.size() - maxVisibleTabs);
        }

        // Add left scroll button
        boolean canScrollLeft = tabScrollOffset > 0;
        scrollLeftButton = Button.builder(Component.literal("◀"), button -> {
                    if (tabScrollOffset > 0) {
                        tabScrollOffset--;
                        this.rebuildWidgets();
                    }
                })
                .bounds(0, 0, scrollButtonWidth, TAB_HEIGHT)
                .build();
        if (!canScrollLeft) {
            scrollLeftButton.active = false;
        }
        this.addRenderableWidget(scrollLeftButton);

        int tabX = scrollButtonWidth;
        int visibleTabCount = 0;

        // Create buttons for visible tabs only
        for (int i = tabScrollOffset; i < tabs.size() && visibleTabCount < maxVisibleTabs; i++) {
            final int tabIndex = i;
            BrowserTab tab = tabs.get(i);

            // Tab button
            tab.tabButton = Button.builder(Component.literal(tab.title), button -> {
                        switchToTab(tabIndex);
                    })
                    .bounds(tabX, 0, TAB_WIDTH - 20, TAB_HEIGHT)
                    .build();
            this.addRenderableWidget(tab.tabButton);

            // Close tab button (always show, even for last tab)
            tab.closeButton = Button.builder(Component.literal("✖"), button -> {
                        closeTab(tabIndex);
                    })
                    .bounds(tabX + TAB_WIDTH - 20, 0, 20, TAB_HEIGHT)
                    .build();
            this.addRenderableWidget(tab.closeButton);

            tabX += TAB_WIDTH;
            visibleTabCount++;
        }

        // Add right scroll button
        boolean canScrollRight = (tabScrollOffset + maxVisibleTabs) < tabs.size();
        scrollRightButton = Button.builder(Component.literal("▶"), button -> {
                    if ((tabScrollOffset + maxVisibleTabs) < tabs.size()) {
                        tabScrollOffset++;
                        this.rebuildWidgets();
                    }
                })
                .bounds(screenWidth - scrollButtonWidth - newTabButtonWidth, 0, scrollButtonWidth, TAB_HEIGHT)
                .build();
        if (!canScrollRight) {
            scrollRightButton.active = false;
        }
        this.addRenderableWidget(scrollRightButton);

        // Add "New Tab" button
        Button newTabButton = Button.builder(Component.literal("+"), button -> {
                    createNewTab();
                })
                .bounds(screenWidth - newTabButtonWidth, 0, newTabButtonWidth, TAB_HEIGHT)
                .build();
        this.addRenderableWidget(newTabButton);
    }

    private void switchToTab(int tabIndex) {
        if (tabIndex >= 0 && tabIndex < tabs.size() && tabIndex != activeTabIndex) {
            activeTabIndex = tabIndex;

            // Update browser references to the new active tab BEFORE rebuilding widgets
            BrowserTab newActiveTab = tabs.get(tabIndex);
            this.myBrowser = newActiveTab.browserWidget;
            this.browser = (this.myBrowser != null) ? this.myBrowser.browser : null;

            // Ensure the active tab is visible by adjusting scroll offset
            int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int scrollButtonWidth = 20;
            int newTabButtonWidth = 30;
            int availableWidth = screenWidth - (scrollButtonWidth * 2) - newTabButtonWidth;
            int maxVisibleTabs = availableWidth / TAB_WIDTH;

            // If active tab is before the visible range, scroll to it
            if (tabIndex < tabScrollOffset) {
                tabScrollOffset = tabIndex;
            }
            // If active tab is after the visible range, scroll to show it
            else if (tabIndex >= tabScrollOffset + maxVisibleTabs) {
                tabScrollOffset = tabIndex - maxVisibleTabs + 1;
            }

            // Use rebuildWidgets to properly clear and rebuild the screen
            this.rebuildWidgets();
        }
    }

    private void createNewTab() {
        BrowserWidget newBrowser = new BrowserWidget(0, 0, 267, 150, "https://www.google.com");
        BrowserTab newTab = new BrowserTab(newBrowser, "Tab " + (tabs.size() + 1));
        tabs.add(newTab);
        activeTabIndex = tabs.size() - 1;

        // Update browser references to the new active tab BEFORE rebuilding widgets
        this.myBrowser = newTab.browserWidget;
        this.browser = (this.myBrowser != null) ? this.myBrowser.browser : null;

        // Use rebuildWidgets to properly clear and rebuild the screen
        this.rebuildWidgets();
    }

    private void closeTab(int tabIndex) {
        if (tabIndex < 0 || tabIndex >= tabs.size()) {
            return; // Invalid index
        }

        // Capture the browser instance to close *after* we update UI state to avoid side-effects
        BrowserTab tab = tabs.get(tabIndex);
        com.cinemamod.mcef.MCEFBrowser browserToClose = null;
        if (tab.browserWidget != null && tab.browserWidget.browser != null) {
            browserToClose = tab.browserWidget.browser;
            // Detach the browser reference from the widget immediately so no other code
            // will try to use it while we update UI state.
            tab.browserWidget.browser = null;
        }

        // Remove tab from list
        tabs.remove(tabIndex);

        // If this was the last tab, close the entire browser screen
        if (tabs.isEmpty()) {
            PIPBrowser.LOGGER.info("Closing last tab - closing browser screen");
            // Clear persisted tabs so nothing restores on next open
            persistedTabs = null;
            persistedActiveTabIndex = 0;

            // Clear the HUD browser widget
            PIPBrowser.getInstance().mainHud.setBrowserWidget(null);
            PIPBrowser.getInstance().mainHud.fillHudList();

            // Close the browser instance before closing the screen
            if (browserToClose != null) {
                try {
                    browserToClose.setFocus(false);
                    browserToClose.sendMouseRelease(-1, -1, -1);
                    browserToClose.sendKeyPress(256, 1, 0);
                    browserToClose.sendKeyRelease(256, 1, 0);
                    browserToClose.close();
                } catch (Throwable t) {
                    PIPBrowser.LOGGER.warn("Failed to close browser instance: {}", t.getMessage());
                }
            }

            // Close the screen
            minecraft.setScreen(null);
            return;
        } else {
            // Adjust active tab index if needed
            if (activeTabIndex == tabIndex) {
                // If we closed the active tab, switch to the previous tab (or next if it was the first)
                activeTabIndex = Math.max(0, tabIndex - 1);
                // Update references immediately so the UI reflects the change
                if (!tabs.isEmpty() && activeTabIndex >= 0 && activeTabIndex < tabs.size()) {
                    BrowserTab newActive = tabs.get(activeTabIndex);
                    this.myBrowser = newActive.browserWidget;
                    this.browser = (this.myBrowser != null) ? this.myBrowser.browser : null;
                } else {
                    this.myBrowser = null;
                    this.browser = null;
                }
            } else if (activeTabIndex > tabIndex) {
                // If we closed a tab before the active one, adjust the active index
                activeTabIndex--;
            }
        }

        // Adjust scroll offset if needed
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int scrollButtonWidth = 20;
        int newTabButtonWidth = 30;
        int availableWidth = screenWidth - (scrollButtonWidth * 2) - newTabButtonWidth;
        int maxVisibleTabs = availableWidth / TAB_WIDTH;

        if (tabScrollOffset > 0 && tabScrollOffset >= tabs.size() - maxVisibleTabs + 1) {
            tabScrollOffset = Math.max(0, tabs.size() - maxVisibleTabs);
        }

        // Use rebuildWidgets to properly clear and rebuild the screen
        this.rebuildWidgets();

        // Now close the MCEF browser instance off the render path to avoid triggering reloads
        // (we detached the widget reference above to prevent UI code from touching it).
        if (browserToClose != null) {
            try {
                // Try to gracefully release focus and input before closing the instance
                try {
                    browserToClose.setFocus(false);
                } catch (Throwable ignore) {}
                try {
                    browserToClose.sendMouseRelease(-1, -1, -1);
                } catch (Throwable ignore) {}
                try {
                    browserToClose.sendKeyPress(256, 1, 0);
                    browserToClose.sendKeyRelease(256, 1, 0);
                } catch (Throwable ignore) {}

                browserToClose.close();
            } catch (Throwable t) {
                PIPBrowser.LOGGER.warn("Failed to close browser instance cleanly: {}", t.getMessage());
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        // Handle delayed browser resize for all tabs (avoid OpenGL conflicts during creation)
        for (BrowserTab tab : tabs) {
            if (!tab.hasBeenResized && tab.browserWidget != null && tab.browserWidget.browser != null) {
                tab.framesSinceCreation++;
                // Wait 5 frames before resizing to ensure browser is fully initialized
                if (tab.framesSinceCreation > 5) {
                    if (width > 100 && height > 100) {
                        tab.browserWidget.browser.resize(scaleX(width), scaleY(height));
                    }
                    tab.hasBeenResized = true;
                }
            }
        }

        // Update URL text field if browser URL has changed and text field is not focused
        if (this.urlTextField != null && browser != null && !this.urlTextField.isFocused()) {
            String browserUrl = browser.getURL();
            if (browserUrl != null && !browserUrl.isEmpty() && !browserUrl.equals("about:blank")) {
                String currentTextFieldValue = this.urlTextField.getValue();
                if (!browserUrl.equals(currentTextFieldValue)) {
                    this.urlTextField.setValue(browserUrl);
                    // Set cursor to beginning so long URLs show the domain first
                    this.urlTextField.moveCursorToStart(false);
                }
            }
        }

        // Update active tab name based on current URL
        updateActiveTabName();

        // Then render widgets and buttons on top using the default Screen rendering path
        super.render(guiGraphics, i, j, f);

        // Render browser first
        if (myBrowser != null) {

            this.myBrowser.render(guiGraphics);
        }

        // Clear focus state before rendering (but not if URL text field is focused)
        if (getFocused() instanceof Button) {
            setFocused(null);
        }
    }

    private int mouseX(double x) {
        // Convert GUI X to browser pixel X (subtract left inset) and clamp inside browser texture
        double guiScale = minecraft.getWindow().getGuiScale();
        int raw = (int) ((x - BROWSER_DRAW_LEFT) * guiScale);
        int max = Math.max(0, scaleX(minecraft.getWindow().getGuiScaledWidth()) - 1);
        return Math.max(0, Math.min(raw, max));
    }

    private int mouseY(double y) {
        // Convert GUI Y to browser pixel Y (subtract top inset) and clamp inside browser texture
        double guiScale = minecraft.getWindow().getGuiScale();
        int raw = (int) ((y - BROWSER_DRAW_TOP) * guiScale);
        int max = Math.max(0, scaleY(minecraft.getWindow().getGuiScaledHeight()) - 1);
        return Math.max(0, Math.min(raw, max));
    }

    private int scaleX(double x) {
        // Browser width in pixels = (GUI width - left inset - right inset) * guiScale
        return (int) ((x - (BROWSER_DRAW_LEFT + BROWSER_DRAW_RIGHT)) * minecraft.getWindow().getGuiScale());
    }

    private int scaleY(double y) {
        // Browser height in pixels = (GUI height - top inset - bottom inset) * guiScale
        return (int) ((y - (BROWSER_DRAW_TOP + BROWSER_DRAW_BOTTOM)) * minecraft.getWindow().getGuiScale());
    }

    private boolean isInBrowserArea(double x, double y) {
        int guiWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int guiHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int left = BROWSER_DRAW_LEFT;
        int right = guiWidth - BROWSER_DRAW_RIGHT;
        int top = BROWSER_DRAW_TOP;
        int bottom = guiHeight - BROWSER_DRAW_BOTTOM;

        // Check if in browser area
        boolean inBrowserArea = x >= left && x < right && y >= top && y < bottom;

        // Exclude the external browser button area (bottom-right corner)
        int buttonLeft = guiWidth - 45;
        int buttonTop = guiHeight - 20;
        int buttonRight = guiWidth;
        int buttonBottom = guiHeight;
        boolean inButtonArea = x >= buttonLeft && x < buttonRight && y >= buttonTop && y < buttonBottom;

        return inBrowserArea && !inButtonArea;
    }

    private void resizeBrowser() {
        if (myBrowser != null) {
            myBrowser.setWidgetWidth(Minecraft.getInstance().getWindow().getGuiScaledWidth());
            myBrowser.setWidgetHeight(Minecraft.getInstance().getWindow().getGuiScaledHeight());

        }
    }

    private String extractDomainFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "New Tab";
        }

        try {
            // Remove protocol
            String domain = url;
            if (domain.startsWith("https://")) {
                domain = domain.substring(8); // Remove "https://"
            } else if (domain.startsWith("http://")) {
                domain = domain.substring(7); // Remove "http://"
            }

            // Remove www. prefix if present
            if (domain.startsWith("www.")) {
                domain = domain.substring(4); // Remove "www."
            }

            // Remove path and query
            int slashIdx = domain.indexOf('/');
            if (slashIdx >= 0) {
                domain = domain.substring(0, slashIdx);
            }

            // Remove port if present
            int colonIdx = domain.indexOf(':');
            if (colonIdx >= 0) {
                domain = domain.substring(0, colonIdx);
            }

            return domain;
        } catch (Exception e) {
            return "New Tab";
        }
    }

    private void updateActiveTabName() {
        if (tabs.isEmpty() || activeTabIndex < 0 || activeTabIndex >= tabs.size()) {
            return;
        }

        BrowserTab activeTab = tabs.get(activeTabIndex);
        if (activeTab == null) {
            return;
        }

        // Update tab title based on current URL, or default to "Tab X"
        String newTitle;
        if (browser != null) {
            String url = browser.getURL();
            if (url != null && !url.isEmpty() && !url.equals("about:blank")) {
                newTitle = extractDomainFromUrl(url);
            } else {
                newTitle = "Tab " + (activeTabIndex + 1);
            }
        } else {
            newTitle = "Tab " + (activeTabIndex + 1);
        }

        // Only update if the title has changed
        if (!newTitle.equals(activeTab.title)) {
            activeTab.title = newTitle;
            if (activeTab.tabButton != null) {
                activeTab.tabButton.setMessage(Component.literal(newTitle));
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        // Check if click is on URL text field and manually set focus before calling super
        if (this.urlTextField != null) {
            int textFieldX = 120;
            int textFieldY = TAB_HEIGHT + 2;
            int textFieldWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth() - 170;
            int textFieldHeight = 16;

            if (event.x() >= textFieldX && event.x() <= textFieldX + textFieldWidth &&
                event.y() >= textFieldY && event.y() <= textFieldY + textFieldHeight) {
                this.setFocused(this.urlTextField);
                this.urlTextField.setFocused(true);
                this.urlTextField.mouseClicked(event, isDoubleClick);
                return true;
            }
        }

        // Let super handle widget clicks first (including text field and buttons)
        boolean handled = super.mouseClicked(event, isDoubleClick);

        // Otherwise send to browser if inside browser area
        if (!handled && isInBrowserArea(event.x(), event.y()) && browser != null) {

            browser.sendMousePress(mouseX(event.x()), mouseY(event.y()), event.button());
            browser.setFocus(true);
        }

        return handled || !isInBrowserArea(event.x(), event.y());
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean handled = super.mouseReleased(event);

        if (!handled && isInBrowserArea(event.x(), event.y()) && browser != null) {
            browser.sendMouseRelease(mouseX(event.x()), mouseY(event.y()), event.button());
            browser.setFocus(true);
        }

        return handled || !isInBrowserArea(event.x(), event.y());
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (isInBrowserArea(mouseX, mouseY) && browser != null) {
            browser.sendMouseMove(mouseX(mouseX), mouseY(mouseY));
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.urlTextField != null && this.urlTextField.isFocused()) {
            return this.urlTextField.mouseDragged(event, dragX, dragY);
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isInBrowserArea(mouseX, mouseY) && browser != null) {
            browser.sendMouseWheel(mouseX(mouseX), mouseY(mouseY), scrollY, 0);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, mouseX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // Allow Escape key to pass through to close the screen
        if (event.key() == 256) { // 256 is the key code for Escape
            return super.keyPressed(event);
        }

        if (this.urlTextField != null && this.urlTextField.isFocused()) {
            if (event.key() == 257) { // Enter key
                String input = this.urlTextField.getValue().trim();
                if (!input.isEmpty() && browser != null) {
                    String url = processUrlOrSearch(input);
                    browser.loadURL(url);
                    this.setFocused(null);
                }
                return true;
            }
            boolean handled = super.keyPressed(event);
            if (handled) return true;
        }

        if (browser != null) {
            browser.sendKeyPress(event.key(), event.scancode(), event.modifiers());
            browser.setFocus(true);
        }

        // Return true to consume the event and prevent it from reaching Minecraft's game logic
        // This prevents movement keys from being "stuck" when the screen closes
        return true;
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (this.urlTextField != null && this.urlTextField.isFocused()) {
            return super.keyReleased(event);
        }

        if (browser != null) {
            browser.sendKeyRelease(event.key(), event.scancode(), event.modifiers());
            browser.setFocus(true);
        }

        // Return true to consume the event and prevent it from reaching Minecraft's game logic
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.urlTextField != null && this.urlTextField.isFocused()) {
            return super.charTyped(event);
        }

        if (event.codepoint() == (char)0) return false;
        browser.sendKeyTyped((char)event.codepoint(), event.modifiers());
        browser.setFocus(true);
        return super.charTyped(event);
    }

    private String processUrlOrSearch(String input) {
        // If it already has a protocol, use it as-is
        if (input.startsWith("http://") || input.startsWith("https://")) {
            return input;
        }

        // Check if it looks like a domain (contains a dot and no spaces)
        boolean looksLikeDomain = input.contains(".") && !input.contains(" ");

        // Check if it looks like localhost or an IP address
        boolean isLocalhost = input.startsWith("localhost") || input.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+.*");

        if (looksLikeDomain || isLocalhost) {
            // Treat as a URL, add https://
            return "https://" + input;
        } else {
            // Treat as a search query, encode it for Google
            try {
                String encodedQuery = java.net.URLEncoder.encode(input, "UTF-8");
                return "https://www.google.com/search?q=" + encodedQuery;
            } catch (Exception e) {
                // Fallback if encoding fails
                return "https://www.google.com/search?q=" + input.replace(" ", "+");
            }
        }
    }
}


