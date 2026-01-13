package com.zylr.pipbrowser.listeners;

import com.zylr.pipbrowser.PIPBrowser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

public class ClientPlayerNetworkListener {

    public static void register() {
        ClientPlayConnectionEvents.DISCONNECT.register(ClientPlayerNetworkListener::onDisconnect);
    }

    private static void onDisconnect(ClientPacketListener handler, Minecraft client) {
        if (PIPBrowser.getInstance().mainHud.getBrowserWidget() != null)
            PIPBrowser.getInstance().mainHud.getBrowserWidget().pauseAllMedia();
    }
}
