package com.zylr.pipbrowser;

import com.zylr.pipbrowser.listeners.ClientPlayerNetworkListener;
import com.zylr.pipbrowser.listeners.InputListener;
import com.zylr.pipbrowser.listeners.LinkScreenListener;
import com.zylr.pipbrowser.listeners.RenderHudElements;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;

public class PIPBrowserClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PIPBrowser.LOGGER.info("HELLO FROM CLIENT SETUP");
        PIPBrowser.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());

        // Register event listeners
        KeyBindings keyBindings = new KeyBindings();
        ClientTickEvents.END_CLIENT_TICK.register(keyBindings::onClientTick);

        RenderHudElements renderHudElements = new RenderHudElements();
        HudRenderCallback.EVENT.register(renderHudElements::onRenderHud);

        // Register network listener
        ClientPlayerNetworkListener.register();

        // Register link screen listener
        LinkScreenListener.register();

        // Register key bindings
        Controls.register();
    }
}
