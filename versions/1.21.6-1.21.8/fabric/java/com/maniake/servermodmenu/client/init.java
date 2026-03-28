package com.maniake.servermodmenu.client;

import com.maniake.servermodmenu.Commons;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;

public class init implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Commons.clientInitalize(Minecraft.getInstance());
        ClientLifecycleEvents.CLIENT_STARTED.register(Commons::clientStartedInitalize);
    }
}
