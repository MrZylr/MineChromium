package com.zylr.pipbrowser.mixin;

import com.zylr.pipbrowser.PIPBrowser;
import com.zylr.pipbrowser.widgets.BrowserWidget;
import com.zylr.pipbrowser.widgets.IWidget;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

/**
 * Mixin for IngredientGridWithNavigation to add browser widget to JEI's exclusion areas.
 * This way JEI's own layout algorithm automatically positions the ingredient grid
 * to avoid overlapping the browser, including dynamically as the browser is dragged.
 */
@Mixin(targets = "mezz.jei.gui.overlay.IngredientGridWithNavigation", remap = false, priority = 999)
public abstract class IngredientGridWithNavigationMixin {

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
        LOGGER.info("IngredientGridWithNavigationMixin loaded");
    }

    /**
     * Inject into updateBounds right after the guiExclusionAreas field is assigned.
     */
    @Inject(method = "updateBounds", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lmezz/jei/gui/overlay/IngredientGridWithNavigation;guiExclusionAreas:Ljava/util/Set;", shift = At.Shift.AFTER), remap = false)
    private void updateBrowserExclusionAreasAfterFieldAssignment(CallbackInfo ci) {
        try {
            updateExclusionAreasWithBrowser();
        } catch (Exception e) {
            LOGGER.debug("Error updating browser in updateBounds", e);
        }
    }

    /**
     * Inject into draw to update exclusion areas every frame.
     * This tracks browser movement in real-time as it's dragged.
     */
    @Inject(method = "draw", at = @At("HEAD"), remap = false)
    private void updateBrowserPositionDuringDraw(CallbackInfo ci) {
        try {
            BrowserWidget browser = getBrowserWidget();
            if (browser != null && browser.isVisible() && browser.isInitialized()) {
                float browserX = browser.getLeftSide();
                float browserY = browser.getTop();
                float browserW = browser.getWidgetWidth();
                float browserH = browser.getWidgetHeight();

                // Check if browser position has changed
                if (browserX != lastBrowserX || browserY != lastBrowserY ||
                    browserW != lastBrowserW || browserH != lastBrowserH) {

                    lastBrowserX = browserX;
                    lastBrowserY = browserY;
                    lastBrowserW = browserW;
                    lastBrowserH = browserH;

                    // Update exclusion areas since browser moved
                    updateExclusionAreasWithBrowser();
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Error updating browser position during draw", e);
        }
    }

    /**
     * Inject into drawTooltips (called every frame) to force layout recalculation
     * when the browser position changes. This ensures the overlay repositions dynamically.
     */
    @Inject(method = "drawTooltips", at = @At("HEAD"), remap = false)
    private void forceRelayoutOnBrowserMove(CallbackInfo ci) {
        try {
            BrowserWidget browser = getBrowserWidget();
            if (browser != null && browser.isVisible() && browser.isInitialized()) {
                float browserX = browser.getLeftSide();
                float browserY = browser.getTop();
                float browserW = browser.getWidgetWidth();
                float browserH = browser.getWidgetHeight();

                // Check if browser position has changed
                if (browserX != lastBrowserX || browserY != lastBrowserY ||
                    browserW != lastBrowserW || browserH != lastBrowserH) {

                    lastBrowserX = browserX;
                    lastBrowserY = browserY;
                    lastBrowserW = browserW;
                    lastBrowserH = browserH;

                    // Update exclusion areas (from IngredientGridWithNavigation mixin)
                    updateExclusionAreasWithBrowser();

                    // CRITICAL: Force parent overlay to recalculate layout by calling onScreenPropertiesChanged
                    // This is needed because JEI only repositions when layout is recalculated
                    try {
                        java.lang.reflect.Field parentOverlayField = null;
                        // Try to find reference to parent IngredientListOverlay
                        for (java.lang.reflect.Field field : this.getClass().getDeclaredFields()) {
                            if (field.getType().getName().contains("IngredientListOverlay")) {
                                parentOverlayField = field;
                                break;
                            }
                        }

                        if (parentOverlayField == null) {
                            // Try accessing through constructor - the parent likely holds a reference
                            // Instead, trigger update on the ScreenPropertiesCache via reflection
                            java.lang.reflect.Method updateLayoutMethod = this.getClass().getDeclaredMethod("updateLayout", boolean.class);
                            updateLayoutMethod.setAccessible(true);
                            updateLayoutMethod.invoke(this, false);
                        }
                    } catch (Exception e) {
                        LOGGER.debug("Could not trigger parent relayout: {}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Error checking browser movement", e);
        }
    }

    @Unique
    private void updateExclusionAreasWithBrowser() throws Exception {
        BrowserWidget browser = getBrowserWidget();
        if (browser != null && browser.isVisible() && browser.isInitialized()) {
            java.lang.reflect.Field guiExclusionAreasField = this.getClass().getDeclaredField("guiExclusionAreas");
            guiExclusionAreasField.setAccessible(true);
            Object currentExclusionAreas = guiExclusionAreasField.get(this);

            if (!(currentExclusionAreas instanceof Set)) {
                return;
            }

            @SuppressWarnings("unchecked")
            Set<Object> updatedExclusionAreas = new HashSet<>((Set<Object>) currentExclusionAreas);

            int browserW = browser.getWidgetWidth();
            int browserH = browser.getWidgetHeight();
            updatedExclusionAreas.removeIf(area -> {
                try {
                    java.lang.reflect.Field widthField = area.getClass().getDeclaredField("width");
                    java.lang.reflect.Field heightField = area.getClass().getDeclaredField("height");
                    widthField.setAccessible(true);
                    heightField.setAccessible(true);
                    int w = widthField.getInt(area);
                    int h = heightField.getInt(area);
                    return (w == browserW && h == browserH);
                } catch (Exception e) {
                    return false;
                }
            });

            Object browserBounds = createImmutableRect2i(
                browser.getLeftSide(),
                browser.getTop(),
                browser.getWidgetWidth(),
                browser.getWidgetHeight()
            );

            if (browserBounds != null) {
                updatedExclusionAreas.add(browserBounds);
                guiExclusionAreasField.set(this, updatedExclusionAreas);
            }
        }
    }

    @Unique
    private Object createImmutableRect2i(float x, float y, float width, float height) {
        try {
            Class<?> immutableRect2iClass = Class.forName("mezz.jei.common.util.ImmutableRect2i");
            java.lang.reflect.Constructor<?> constructor = immutableRect2iClass.getConstructor(int.class, int.class, int.class, int.class);
            return constructor.newInstance((int) x, (int) y, (int) width, (int) height);
        } catch (Exception e) {
            LOGGER.error("Could not create ImmutableRect2i: {}", e.getMessage());
            return null;
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
