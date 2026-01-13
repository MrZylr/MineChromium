package com.zylr.pipbrowser.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.zylr.pipbrowser.Controls;
import com.zylr.pipbrowser.PIPBrowser;
import com.zylr.pipbrowser.widgets.BrowserWidget;
import com.zylr.pipbrowser.widgets.IWidget;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {

    @Inject(method = "renderBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderBackground(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", shift = At.Shift.AFTER))
    private void renderBrowserWidgetBetweenBackgroundAndBg(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        // Render browser widgets AFTER the transparent background
        // but BEFORE the inventory background (renderBg)
        try {
            if (PIPBrowser.getInstance() != null && PIPBrowser.getInstance().mainHud != null) {
                for (IWidget widget : PIPBrowser.getInstance().mainHud.widgets) {
                    if (widget instanceof BrowserWidget browser) {
                        browser.render(guiGraphics);
                    }
                }
            }
        } catch (Exception e) {
            // Silently catch to avoid crashing the inventory screen
        }
    }

    @Inject(method = "keyPressed", at = @At(value = "HEAD"))
    private void sendUpKeyPress(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        if (Controls.UP_KEY.matches(keyEvent)) {
            if (PIPBrowser.getInstance().mainHud.getBrowserWidget() != null) {
                PIPBrowser.getInstance().mainHud.getBrowserWidget().keyPressed(GLFW.GLFW_KEY_UP, 0, 0);
            }
        }
        if (Controls.DOWN_KEY.matches(keyEvent)) {
            if (PIPBrowser.getInstance().mainHud.getBrowserWidget() != null) {
                PIPBrowser.getInstance().mainHud.getBrowserWidget().keyPressed(GLFW.GLFW_KEY_DOWN, 0, 0);
            }
        }
    }
}
