package com.maniake.servermodmenu;

import com.maniake.servermodmenu.event.EventRegisterForge;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void onScreenAfterInit(ScreenEvent.Init.Post event) {
        Commons.clientInitalize(Minecraft.getInstance());
        Commons.clientStartedInitalize(Minecraft.getInstance());
        EventRegisterForge.ModMenuEventHandler.afterScreenInit(Minecraft.getInstance(), event.getScreen(), event.getScreen().width, event.getScreen().height);
    }
    @SubscribeEvent
    public static void onClientEndTick(TickEvent.ClientTickEvent.Post event) {
        EventRegisterForge.ModMenuEventHandler.onClientEndTick(Minecraft.getInstance());
    }
}