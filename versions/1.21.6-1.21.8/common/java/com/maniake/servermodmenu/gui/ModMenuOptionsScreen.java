package com.maniake.servermodmenu.gui;


import com.maniake.servermodmenu.config.ModMenuConfig;
import com.maniake.servermodmenu.config.ModMenuConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

public class ModMenuOptionsScreen extends OptionsSubScreen {
	public ModMenuOptionsScreen(Screen previous) {
		super(previous, Minecraft.getInstance().options, Component.translatable("modmenu.options"));
	}

    @Override
    protected void addOptions() {
        if(this.list != null) {
            OptionInstance<?>[] options = ModMenuConfig.asOptions();
            for (int i = 0; i < options.length; i += 2) {
                this.list.addBig(options[i]);
            }
        }
    }

    @Override
	public void removed() {
		ModMenuConfigManager.save();
	}
}
