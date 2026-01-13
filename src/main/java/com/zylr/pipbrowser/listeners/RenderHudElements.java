package com.zylr.pipbrowser.listeners;

import com.zylr.pipbrowser.PIPBrowser;
import com.zylr.pipbrowser.hud.MainHud;
import com.zylr.pipbrowser.screens.ColorPickerScreen;
import com.zylr.pipbrowser.widgets.BrowserWidget;
import com.zylr.pipbrowser.widgets.IWidget;
import com.zylr.pipbrowser.widgets.Widget;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.lwjgl.glfw.GLFW;

/**
 * Renders HUD widgets and forwards input to the browser widget while an
 * inventory (AbstractContainerScreen) is open so the browser becomes
 * draggable (left-drag), resizable (right-drag) and clickable (short left-press)
 * without opening the HUD edit screen. Middle click opens the color picker.
 */
public class RenderHudElements {
    // Track previous GLFW button states so we can detect presses/releases
    private boolean prevLeft = false;
    private boolean prevRight = false;
    private boolean prevMiddle = false;

    // Keep a small field to reference the deltaTracker to avoid unused-param warnings
    private int lastDeltaTrackerHash = 0;

    // Drag / resize state
    private IWidget draggingWidget = null; // active drag (moving)
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;

    // Track left-press for click vs drag
    private IWidget leftPressedWidget = null;
    private double leftPressPointerX = 0;
    private double leftPressPointerY = 0;
    private static final double DRAG_THRESHOLD = 3.0; // pixels before starting a drag

    private IWidget resizingWidget = null;
    private int resizeStartWidth = 0;
    private int resizeStartHeight = 0;
    private double resizeStartMouseX = 0;
    private double resizeStartMouseY = 0;

    public void onRenderHud(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();

        // reference deltaTracker to avoid unused-parameter warnings
        int currentDeltaHash = (deltaTracker == null) ? 0 : deltaTracker.hashCode();
        if (currentDeltaHash != lastDeltaTrackerHash) {
            // store the latest hash so the field is used and the parameter is not unused
            lastDeltaTrackerHash = currentDeltaHash;
        }

        // Render all widgets (but skip browser widgets if inventory is open - mixin handles those)
        boolean isInventoryScreen = mc.screen instanceof AbstractContainerScreen;
        for (IWidget widget : PIPBrowser.getInstance().mainHud.widgets) {
            if (widget != null) {
                // Skip browser widgets when inventory is open - they're rendered by the mixin
                if (widget instanceof BrowserWidget && isInventoryScreen) {
                    continue;
                }
                if (widget instanceof BrowserWidget browser) {
                    browser.render(guiGraphics);
                } else {
                    widget.render(guiGraphics);
                }
            }
        }

        // If the current screen is an inventory-like screen, forward input to the widgets
        if (mc.screen instanceof AbstractContainerScreen) {
            long window = mc.getWindow().handle();

            boolean left = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            boolean right = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
            boolean middle = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;

            // GUI coordinates used by widgets (account for HUD scale)
            double pointerX = (mc.mouseHandler.xpos() / mc.getWindow().getGuiScale());
            double pointerY = (mc.mouseHandler.ypos() / mc.getWindow().getGuiScale());

            // ----- LEFT = drag (click vs drag) -----
            if (left && !prevLeft) {
                // left button just pressed -> remember potential press target
                for (IWidget w : PIPBrowser.getInstance().mainHud.widgets) {
                    if (w != null && w.isVisible() && w.isHovered()) {
                        leftPressedWidget = w;
                        leftPressPointerX = pointerX;
                        leftPressPointerY = pointerY;
                        break;
                    }
                }
            }

            // If left is held and a leftPressedWidget exists but no active draggingWidget,
            // check movement threshold to start a drag.
            if (left && leftPressedWidget != null && draggingWidget == null) {
                double dx = Math.abs(pointerX - leftPressPointerX);
                double dy = Math.abs(pointerY - leftPressPointerY);
                if (dx >= DRAG_THRESHOLD || dy >= DRAG_THRESHOLD) {
                    // start dragging
                    draggingWidget = leftPressedWidget;
                    dragOffsetX = draggingWidget.getAnchorX() - pointerX;
                    dragOffsetY = draggingWidget.getAnchorY() - pointerY;
                }
            }

            // While dragging, update widget position
            if (left && draggingWidget != null) {
                double newX = pointerX + dragOffsetX;
                double newY = pointerY + dragOffsetY;

                draggingWidget.setRelativeX((int) newX);
                draggingWidget.setRelativeY((int) newY);
            }

            // On left release, if we were dragging persist position; else (click) forward to browser
            if (!left && prevLeft) {
                if (draggingWidget != null) {
                    try {
                        if (draggingWidget.getConfig() != null) {
                            draggingWidget.getConfig().setProperty("x", draggingWidget.getRelativeAnchorX() + "");
                            draggingWidget.getConfig().setProperty("y", draggingWidget.getRelativeAnchorY() + "");
                            draggingWidget.saveConfig();
                        }
                    } catch (Exception e) {
                        PIPBrowser.LOGGER.warn("Failed to save widget position: {}", e.getMessage());
                    }
                    draggingWidget = null;
                    leftPressedWidget = null;
                } else if (leftPressedWidget != null) {
                    leftPressedWidget = null;
                }
            }

            // ----- RIGHT = resize -----
            // Detect new right-press -> begin resizing if over a widget
            if (right && !prevRight) {
                for (IWidget w : PIPBrowser.getInstance().mainHud.widgets) {
                    if (w != null && w.isVisible() && w.isHovered()) {
                        resizingWidget = w;
                        resizeStartWidth = w.getWidgetWidth();
                        resizeStartHeight = w.getWidgetHeight();
                        resizeStartMouseX = pointerX;
                        resizeStartMouseY = pointerY;
                        break;
                    }
                }
            }

            // While right held and resizingWidget != null, update size based on mouse delta
            if (right && resizingWidget != null) {
                int newWidth = Math.max(20, resizeStartWidth + (int) ((pointerX - resizeStartMouseX) * 1));
                int newHeight = Math.max(20, resizeStartHeight + (int) ((pointerY - resizeStartMouseY) * 1));

                // Prevent resizing larger than the remaining space from widget anchor to the window edge
                int windowWidthWidgets = (int) (mc.getWindow().getGuiScaledWidth());
                int windowHeightWidgets = (int) (mc.getWindow().getGuiScaledHeight());
                int anchorX = resizingWidget.getAnchorX();
                int anchorY = resizingWidget.getAnchorY();
                // Clamp anchors within window bounds before calculating available space
                int clampedAnchorX = Math.max(0, Math.min(anchorX, windowWidthWidgets));
                int clampedAnchorY = Math.max(0, Math.min(anchorY, windowHeightWidgets));
                int maxWidth = Math.max(20, windowWidthWidgets - clampedAnchorX);
                int maxHeight = Math.max(20, windowHeightWidgets - clampedAnchorY);
                newWidth = Math.min(newWidth, maxWidth);
                newHeight = Math.min(newHeight, maxHeight);

                if (resizingWidget instanceof Widget w) {
                    w.setWidgetWidth(newWidth);
                    w.setWidgetHeight(newHeight);
                    w.setWidthPercent(newWidth/(double)mc.getWindow().getGuiScaledWidth());
                    w.setHeightPercent(newHeight/(double)mc.getWindow().getGuiScaledHeight());
                }
            }

            // On right release, persist size
            if (!right && prevRight && resizingWidget != null) {
                try {
                    if (resizingWidget.getConfig() != null) {
                        resizingWidget.getConfig().setProperty("width", Double.toString(resizingWidget.getWidthPercent()));
                        resizingWidget.getConfig().setProperty("height", Double.toString(resizingWidget.getHeightPercent()));
                        resizingWidget.saveConfig();
                    }
                } catch (Exception e) {
                    PIPBrowser.LOGGER.warn("Failed to save widget size: {}", e.getMessage());
                }
                resizingWidget = null;
            }

            // ----- MIDDLE = open ColorPickerScreen -----
            if (middle && !prevMiddle) {
                for (IWidget w : PIPBrowser.getInstance().mainHud.widgets) {
                    if (w != null && w.isVisible() && w.isHovered()) {
                        if (w instanceof Widget widget) {
                            // Open the ColorPickerScreen, pass current MC screen as parent so it returns
                            mc.setScreen(new ColorPickerScreen(widget, mc.screen));
                        }
                        break;
                    }
                }
            }

            // Update previous states
            prevLeft = left;
            prevRight = right;
            prevMiddle = middle;
        }
    }
}
