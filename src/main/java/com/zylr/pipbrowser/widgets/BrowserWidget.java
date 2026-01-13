package com.zylr.pipbrowser.widgets;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import com.zylr.pipbrowser.PIPBrowser;
import com.zylr.pipbrowser.screens.BrowserScreen;
import com.zylr.pipbrowser.properties.MainProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;

import java.awt.*;
import java.util.Properties;

public class BrowserWidget extends Widget {
    // Use BrowserScreen.BROWSER_DRAW_OFFSET as the single source of truth for where
    // the fullscreen browser texture should start (below tabs + nav bar).
    public MCEFBrowser browser;
    private boolean initialized = false;
    private String currentUrl;
    private String initialUrl;
    private float opacity = 0.8f; // 80% opacity (20% transparent)
    // Browser audio volume (0.0 - 1.0)
    public float browserVolume = 1.0f;

    public BrowserWidget(int x, int y, int width, int height, String initialUrl) {
        this.anchorX = x;
        this.anchorY = y;
        this.widgetWidth = width;
        this.widgetHeight = height;
        this.isParent = true;
        this.visible = true;
        this.type = WidgetType.BROWSER;
        this.setupConfig();
        this.initialUrl = initialUrl;
        // Load browser volume from main config if present
        try {
            Properties mainConfig = MainProperties.getConfig();
            if (mainConfig != null) {
                String volStr = mainConfig.getProperty("browserVolume", Float.toString(this.browserVolume));
                this.browserVolume = Float.parseFloat(volStr);
            }
        } catch (Throwable t) {
            // ignore and keep default
        }
        initializeBrowser();

        this.relativeAnchorx = Double.parseDouble(config.getProperty("x", "0"));
        this.relativeAnchorY = Double.parseDouble(config.getProperty("y", "0"));
        this.anchorX = this.getAnchorXFromRelative(this.relativeAnchorx);
        this.anchorY = this.getAnchorYFromRelative(this.relativeAnchorY);
        this.setWidthPercent(Double.parseDouble(config.getProperty("width", "0")));
        this.setHeightPercent(Double.parseDouble(config.getProperty("height", "0")));
        this.backgroundColor = Integer.parseInt(config.getProperty("backgroundColor", "-1"));
    }

    private void initializeBrowser() {
        try {
            if (MCEF.isInitialized()) {
                PIPBrowser.LOGGER.info("MCEF browser initializing");
                // Create browser with transparency enabled
                browser = MCEF.createBrowser(this.initialUrl, true);

                // Set initial size to 1920x1080 for consistent HUD display
                // The texture will be scaled to fit the widget dimensions (267x150)
                try {
                    browser.resize(1920, 1080);
                    PIPBrowser.LOGGER.info("Initialized browser at 1920x1080");
                } catch (Exception resizeError) {
                    PIPBrowser.LOGGER.warn("Could not resize browser immediately: {}", resizeError.getMessage());
                    // Browser will be resized later by BrowserScreen
                }

                initialized = true;
            } else {
                PIPBrowser.LOGGER.warn("MCEF not initialized");
            }
        } catch (Exception e) {
            PIPBrowser.LOGGER.error("Failed to initialize MCEF browser: {}", e.getMessage(), e);
        }
    }

    public void loadURL(String url) {
        if (browser != null && initialized) {
            this.currentUrl = url;
            browser.loadURL(url);
        }
    }

    public void navigateToGoogle() {
        loadURL("https://www.google.com");
    }

    public void navigateToYouTube() {
        loadURL("https://www.youtube.com");
    }

    public void navigateToGitHub() {
        loadURL("https://www.github.com");
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    /**
     * Compute browser texture resolution so that:
     * - at widget 267x150 -> 1280x720
     * - at widget fullscreen (screenW x screenH) -> 1920x1080
     * Uses width-based linear interpolation.
     */
    public static Dimension computeBrowserResolution(int widgetW, int widgetH, int screenW, int screenH) {
        final float minWidgetW = 267f;
        final float minResW = 1280f;
        final float maxResW = 1920f;

        float denom = Math.max(1f, screenW - minWidgetW);
        float t = clamp((widgetW - minWidgetW) / denom, 0f, 1f);

        float aspectRatio = (float) widgetW / (float) widgetH;

        int resW = Math.round(minResW + t * (maxResW - minResW)); // 1280 -> 1920
        int resH = Math.round(resW / aspectRatio);

        return new Dimension(resW, resH);
    }

    @Override
    public void render(GuiGraphics gui) {
        if (!this.isVisible())
            return;
        if (this.browser != null) {
            this.fixAnchors();
            this.anchorX = this.getAnchorXFromRelative(this.getRelativeAnchorX());
            this.anchorY = this.getAnchorYFromRelative(this.getRelativeAnchorY());
            if (widthPercent > 0 && heightPercent > 0) {
                this.widgetWidth = (int) (this.widthPercent * Minecraft.getInstance().getWindow().getGuiScaledWidth());
                this.widgetHeight = (int) (this.heightPercent * Minecraft.getInstance().getWindow().getGuiScaledHeight());
                if (this.widgetWidth > Minecraft.getInstance().getWindow().getGuiScaledWidth())
                    this.widgetWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
                if (this.widgetHeight > Minecraft.getInstance().getWindow().getGuiScaledHeight())
                    this.widgetHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            }
            // Get widget position in widget coordinates
            float left = this.getLeftSide();
            float top = this.getTop();

            // Apply scale to convert widget coordinates to screen coordinates
            // The border thickness and corner radius also scale
            float borderThickness = 2.0f;
            float cornerRadius = 5.0f;

            // Calculate screen positions and sizes
            int screenLeft = (int) (left);
            int screenTop = (int) (top);
            int screenWidth = (int) (this.widgetWidth);
            int screenHeight = (int) (this.widgetHeight);
            int screenBorderThickness = (int) (borderThickness);
            int screenCornerRadius = (int) (cornerRadius);

            Color color = new Color(backgroundColor, true);
            Color browserColor = new Color(255, 255, 255, color.getAlpha());

            if (browser.isTextureReady()) {
                if (Minecraft.getInstance().screen instanceof BrowserScreen) {
                    // Draw the browser content in fullscreen mode using per-side insets
                    int leftInset = BrowserScreen.BROWSER_DRAW_LEFT;
                    int rightInset = BrowserScreen.BROWSER_DRAW_RIGHT;
                    int topInset = BrowserScreen.BROWSER_DRAW_TOP;
                    int bottomInset = BrowserScreen.BROWSER_DRAW_BOTTOM;

                    int renderWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth() - leftInset - rightInset;
                    int renderHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight() - topInset - bottomInset;
                    gui.blit(RenderPipelines.GUI_TEXTURED, browser.getTextureLocation(),
                            leftInset, topInset,
                            0, 0,
                            renderWidth, renderHeight,
                            renderWidth, renderHeight);
                } else {
                    Dimension browserRes = computeBrowserResolution(this.widgetWidth, this.widgetHeight,
                            Minecraft.getInstance().getWindow().getGuiScaledWidth(),
                            Minecraft.getInstance().getWindow().getGuiScaledHeight());
                    int browserWidth = browserRes.width;
                    int browserHeight = browserRes.height;
                    browser.resize(browserWidth, browserHeight); // Scale up for better resolution

                    // Draw the browser content inside the border
                    // Position: border position + border thickness
                    // Size: border size - 2 * border thickness
                    gui.blit(RenderPipelines.GUI_TEXTURED, browser.getTextureLocation(),
                            screenLeft + screenBorderThickness,
                            screenTop + screenBorderThickness,
                            0, 0,
                            screenWidth - screenBorderThickness * 2,
                            screenHeight - screenBorderThickness * 2,
                            screenWidth - screenBorderThickness * 2,
                            screenHeight - screenBorderThickness * 2,
                            browserColor.getRGB());
                }
            }

            // Draw rounded border (not in BrowserScreen)
            if (!(Minecraft.getInstance().screen instanceof BrowserScreen)) {
                // Draw border at the exact same position and size as calculated above
                drawRoundedBorder(gui, screenLeft, screenTop, screenWidth, screenHeight,
                        screenCornerRadius,
                        backgroundColor,
                        screenBorderThickness);
            }
        }

    }

    private void drawRoundedBorder(GuiGraphics gui, int x, int y, int width, int height, int radius, int color, int thickness) {
        int left = x;
        int top = y;
        int right = x + width;
        int bottom = y + height;

        // Ensure minimum thickness
        int thick = Math.max(1, thickness);

        // Ensure radius is reasonable
        int rad = Math.max(0, radius);
        int maxRad = Math.min(width / 2, height / 2);
        if (rad > maxRad) rad = maxRad;

        // Disable rounded corners at small scales (radius less than 4 pixels)
        boolean useRoundedCorners = rad >= 4;

        if (useRoundedCorners) {
            // Draw the four rounded corners first
            // Top-left corner
            drawFilledCorner(gui, left + rad, top + rad, rad, thick, color, 2);
            // Top-right corner
            drawFilledCorner(gui, right - rad, top + rad, rad, thick, color, 1);
            // Bottom-right corner
            drawFilledCorner(gui, right - rad, bottom - rad, rad, thick, color, 0);
            // Bottom-left corner
            drawFilledCorner(gui, left + rad, bottom - rad, rad, thick, color, 3);

            // Draw the four straight edges between the corners (avoiding overlap)
            // Top edge - leave space for corners
            gui.fill(left + rad + 1, top, right - rad, top + thick, color);
            // Bottom edge - leave space for corners
            gui.fill(left + rad + 1, bottom - thick, right - rad, bottom, color);
            // Left edge - leave space for corners
            gui.fill(left, top + rad + 1, left + thick, bottom - rad, color);
            // Right edge - leave space for corners
            gui.fill(right - thick, top + rad + 1, right, bottom - rad, color);
        } else {
            // Draw simple square border at small scales
            // Top edge
            gui.fill(left, top, right, top + thick, color);
            // Bottom edge
            gui.fill(left, bottom - thick, right, bottom, color);
            // Left edge
            gui.fill(left, top, left + thick, bottom, color);
            // Right edge
            gui.fill(right - thick, top, right, bottom, color);
        }
    }

    /**
     * Draws a filled quarter circle corner for the border
     * @param quadrant 0=bottom-right, 1=top-right, 2=top-left, 3=bottom-left
     */
    private void drawFilledCorner(GuiGraphics gui, int cx, int cy, int radius, int thickness, int color, int quadrant) {
        // For small radii, just draw a filled square corner
        if (radius <= 2) {
            int size = radius;
            switch (quadrant) {
                case 0: // bottom-right
                    gui.fill(cx, cy, cx + size, cy + size, color);
                    break;
                case 1: // top-right
                    gui.fill(cx, cy - size + 1, cx + size, cy + 1, color);
                    break;
                case 2: // top-left
                    gui.fill(cx - size + 1, cy - size + 1, cx + 1, cy + 1, color);
                    break;
                case 3: // bottom-left
                    gui.fill(cx - size + 1, cy, cx + 1, cy + size, color);
                    break;
            }
            return;
        }

        // For larger radii, use proper circle math with better boundary handling
        double outerRad = radius + 0.5;
        double innerRad = Math.max(0, radius - thickness + 0.5);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                // Calculate distance from center using the center of the pixel
                double dist = Math.sqrt((dx + 0.5) * (dx + 0.5) + (dy + 0.5) * (dy + 0.5));

                // Check if point is within the border ring
                if (dist >= innerRad && dist <= outerRad) {
                    // Check if point is in the correct quadrant
                    boolean inQuadrant = false;
                    switch (quadrant) {
                        case 0: // bottom-right
                            inQuadrant = dx >= 0 && dy >= 0;
                            break;
                        case 1: // top-right
                            inQuadrant = dx >= 0 && dy <= 0;
                            break;
                        case 2: // top-left
                            inQuadrant = dx <= 0 && dy <= 0;
                            break;
                        case 3: // bottom-left
                            inQuadrant = dx <= 0 && dy >= 0;
                            break;
                    }
                    if (inQuadrant) {
                        gui.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
                    }
                }
            }
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (browser != null && initialized) {
            if (mouseX >= this.getLeftSide() && mouseX <= this.getLeftSide() + this.widgetWidth && mouseY >= this.getTop() && mouseY <= this.getTop() + this.widgetHeight) {
                int browserX = (int)(mouseX - this.getLeftSide());
                int browserY = (int)(mouseY - this.getTop());

                browser.sendMousePress(browserX, browserY, button);
                browser.sendMouseRelease(browserX, browserY, button);
                return true;
            }
        }
        return false;
    }

    public void mouseMoved(double mouseX, double mouseY) {
        if (browser != null && initialized) {
            if (mouseX >= this.getLeftSide() && mouseX <= this.getLeftSide() + this.widgetWidth && mouseY >= this.getTop() && mouseY <= this.getTop() + this.widgetHeight) {
                int browserX = (int)(mouseX - this.getLeftSide());
                int browserY = (int)(mouseY - this.getTop());
                browser.sendMouseMove(browserX, browserY);
            }
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (browser != null && initialized) {
            if (mouseX >= this.getLeftSide() && mouseX <= this.getLeftSide() + this.widgetWidth && mouseY >= this.getTop() && mouseY <= this.getTop() + this.widgetHeight) {
                int browserX = (int)(mouseX - this.getLeftSide());
                int browserY = (int)(mouseY - this.getTop());
                browser.sendMouseWheel(browserX, browserY, delta, 0);
                return true;
            }
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (browser != null && initialized) {
            browser.sendKeyPress(keyCode, scanCode, modifiers);
            return true;
        }
        return false;
    }

    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (browser != null && initialized) {
            browser.sendKeyRelease(keyCode, scanCode, modifiers);
            return true;
        }
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (browser != null && initialized) {
            browser.sendKeyTyped(chr, modifiers);
            return true;
        }
        return false;
    }

    public boolean isInitialized() {
        return initialized && browser != null;
    }

    public void cleanup() {
        if (browser != null) {
            try {
                browser.close();
            } catch (Exception e) {
                System.err.println("Error closing browser: " + e.getMessage());
            }
            browser = null;
        }
        initialized = false;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
        if (browser != null) {
            browser.resize(width, height);
        }
    }

    public float getOpacity() {
        return opacity;
    }

    public String getCurrentUrl() {
        return currentUrl;
    }

    // Browser navigation methods
    public void goBack() {
        if (browser != null && browser.canGoBack()) {
            browser.goBack();
        }
    }

    public void goForward() {
        if (browser != null && browser.canGoForward()) {
            browser.goForward();
        }
    }

    public void refresh() {
        if (browser != null) {
            browser.reload();
        }
    }

    public void setZoom(float zoomLevel) {
        if (browser != null) {
            browser.setZoomLevel(zoomLevel);
        }
    }

    public void pauseAllMedia() {
        if (browser != null && initialized) {
            String pauseScript =
                    "var videos = document.querySelectorAll('video'); " +
                            "for(var i = 0; i < videos.length; i++) { videos[i].pause(); } " +
                            "var audios = document.querySelectorAll('audio'); " +
                            "for(var i = 0; i < audios.length; i++) { audios[i].pause(); }";

            browser.executeJavaScript(pauseScript, "", 0);
        }
    }
}
