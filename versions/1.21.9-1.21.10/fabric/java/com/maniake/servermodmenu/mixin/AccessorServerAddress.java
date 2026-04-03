package com.maniake.servermodmenu.mixin;

import com.google.common.net.HostAndPort;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ServerAddress.class)
public interface AccessorServerAddress {
    @Accessor
    HostAndPort getHostAndPort();
}

