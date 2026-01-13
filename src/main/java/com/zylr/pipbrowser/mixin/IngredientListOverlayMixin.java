package com.zylr.pipbrowser.mixin;

import com.zylr.pipbrowser.PIPBrowser;
import com.zylr.pipbrowser.widgets.BrowserWidget;
import com.zylr.pipbrowser.widgets.IWidget;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for IngredientListOverlay to force layout recalculation when browser moves.
 * This ensures JEI repositions dynamically as the browser is dragged.
 */
@Mixin(targets = "mezz.jei.gui.overlay.IngredientListOverlay", remap = false, priority = 999)
public abstract class IngredientListOverlayMixin {

    @Unique
    private static final Logger LOGGER = LogManager.getLogger();

    @Unique
    private float lastBrowserX = -1;
    @Unique
    private float lastBrowserY = -1;
    @Unique
    private float lastBrowserW = -1;
    @Unique
    private float lastBrowserH = -1;

    static {
        LOGGER.info("IngredientListOverlayMixin loaded");
    }

    /**
     * Inject into drawScreen to detect browser movement and force relayout.
     * This is called every frame, so we can track browser position changes in real-time.
     */
    @Inject(method = "drawScreen", at = @At("HEAD"), remap = false)
    private void forceRelayoutOnBrowserMove(CallbackInfo ci) {
        try {
            BrowserWidget browser = getBrowserWidget();
            if (browser != null && browser.isVisible() && browser.isInitialized()) {
                float browserX = browser.getLeftSide();
                float browserY = browser.getTop();
                float browserW = browser.getWidgetWidth();
                float browserH = browser.getWidgetHeight();

                // Check if browser position or size has changed
                if (browserX != lastBrowserX || browserY != lastBrowserY ||
                    browserW != lastBrowserW || browserH != lastBrowserH) {

                    lastBrowserX = browserX;
                    lastBrowserY = browserY;
                    lastBrowserW = browserW;
                    lastBrowserH = browserH;

                    // Force JEI to recalculate layout by calling onScreenPropertiesChanged()
                    try {
                        java.lang.reflect.Method onScreenPropertiesChangedMethod = this.getClass()
                            .getDeclaredMethod("onScreenPropertiesChanged");
                        onScreenPropertiesChangedMethod.setAccessible(true);
                        onScreenPropertiesChangedMethod.invoke(this);

                        LOGGER.debug("Forced layout recalculation for browser at [{},{}]", browserX, browserY);
                    } catch (Exception e) {
                        LOGGER.debug("Could not force relayout: {}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Error checking browser movement in drawScreen", e);
        }
    }

    @Unique
    private BrowserWidget getBrowserWidget() {
        if (PIPBrowser.getInstance() == null || PIPBrowser.getInstance().mainHud == null) {
            return null;
        }
        for (IWidget widget : PIPBrowser.getInstance().mainHud.widgets) {
            if (widget instanceof BrowserWidget) {
                return (BrowserWidget) widget;
            }
        }
        return null;
    }
}

