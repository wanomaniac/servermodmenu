package com.maniake.servermodmenu;

import com.maniake.servermodmenu.event.EventRegisterNeoForge;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.lifecycle.ClientStartedEvent;

import java.util.Arrays;
import java.util.stream.Stream;

public class ClientEvents {
    public static void onClientStartedInitalize(ClientStartedEvent event) {
        Commons.clientInitalize(event.getClient());
        Commons.clientStartedInitalize(event.getClient());
    }

    public static void onScreenAfterInit(ScreenEvent.Init.Post event) {
        EventRegisterNeoForge.ModMenuEventHandler.afterScreenInit(Minecraft.getInstance(), event.getScreen(), event.getScreen().width, event.getScreen().height);
    }

    public static void onClientEndTick(ClientTickEvent.Post event) {
        EventRegisterNeoForge.ModMenuEventHandler.onClientEndTick(Minecraft.getInstance());
    }
}