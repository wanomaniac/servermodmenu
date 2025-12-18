package com.maniake.servermodmenu;

import com.maniake.servermodmenu.config.ModMenuConfigManager;
import com.maniake.servermodmenu.interfaces.IModMetadata;
import com.maniake.servermodmenu.interfaces.IEventRegister;
import com.maniake.servermodmenu.services.ServiceKey;
import com.maniake.servermodmenu.services.ServicesManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerList;
public class Commons {
    static boolean event = false;

    public static void clientStartedInitalize(Minecraft client) {
        new Thread(() -> {
            ServerList serverList = new ServerList(client);
            Constants.sendmodstolist(serverList, client);
        }).start();
    }

    public static void clientInitalize(Minecraft client) {
        Constants.LOG.info("Running on platform {}", Constants.Platform.getPlatformName());
        IEventRegister iEventRegister = ServicesManager.get(ServiceKey.of(IEventRegister.class));
        ServerList serverList = new ServerList(client);
            new Thread(() -> {
                Constants.LoadServerListConnections(serverList, Constants.NETWORKING);
            }).start();

        ModMenuConfigManager.initializeConfig();
        iEventRegister.Register();
    }
}