package com.zylr.pipbrowser;

import com.zylr.pipbrowser.screens.BrowserScreen;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public void onClientTick(Minecraft client) {
        while (Controls.BROWSER_KEY.consumeClick()) {
            Minecraft.getInstance().setScreen(new BrowserScreen());
        }
        while (Controls.UP_KEY.consumeClick()) {
            if (PIPBrowser.getInstance().mainHud.getBrowserWidget() != null) {
                PIPBrowser.getInstance().mainHud.getBrowserWidget().keyPressed(GLFW.GLFW_KEY_UP, 0, 0);
            }
        }
        while (Controls.DOWN_KEY.consumeClick()) {
            if (PIPBrowser.getInstance().mainHud.getBrowserWidget() != null) {
                PIPBrowser.getInstance().mainHud.getBrowserWidget().keyPressed(GLFW.GLFW_KEY_DOWN, 0, 0);
            }
        }
    }

}
