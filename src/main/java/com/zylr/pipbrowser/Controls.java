package com.zylr.pipbrowser;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.KeyEvent;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

public class Controls {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(ResourceLocation.fromNamespaceAndPath(PIPBrowser.MODID, "browserbutton"));

    public static final KeyMapping BROWSER_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                    "key.pipbrowser.browser",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_HOME,
                    CATEGORY
            )
    );
    public static final KeyMapping UP_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                    "key.pipbrowser.upbutton",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_UP,
                    CATEGORY
            )
    );
    public static final KeyMapping DOWN_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                    "key.pipbrowser.downbutton",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_DOWN,
                    CATEGORY
            )
    );

    public static void register() {
        // KeyBinding is already registered in the static initializer above
        PIPBrowser.LOGGER.info("Registered keybinding: key.pipbrowser.browser -> HOME");
    }

    // Compatibility helper: check if a KeyMapping matches a key event
    public static boolean matchesKey(KeyMapping km, int keyCode, int scanCode) {
        // In 1.21.10, KeyMapping.matches() takes a KeyEvent parameter
        KeyEvent keyEvent = new KeyEvent(keyCode, scanCode, 1);
        return km.matches(keyEvent);
    }
}
