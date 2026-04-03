package com.maniake.servermodmenu;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Constants.MOD_ID)
public class SERVERMODMENU {
    public SERVERMODMENU(IEventBus bus) {
        NeoForge.EVENT_BUS.addListener(ClientEvents::onClientStartedInitalize);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onClientEndTick);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onScreenAfterInit);
    }
}

