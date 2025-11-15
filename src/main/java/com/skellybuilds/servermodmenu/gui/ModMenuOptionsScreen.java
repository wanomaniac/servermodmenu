package com.skellybuilds.servermodmenu.gui;

import com.skellybuilds.servermodmenu.config.ModMenuConfig;
import com.skellybuilds.servermodmenu.config.ModMenuConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.OptionListWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class ModMenuOptionsScreen extends GameOptionsScreen {
	public ModMenuOptionsScreen(Screen previous) {
		super(previous, MinecraftClient.getInstance().options, Text.translatable("modmenu.options"));
	}

	@Override
	protected void addOptions() {
		if (this.body != null) {
			this.body.addAll(ModMenuConfig.asOptions());
		}
	}

	@Override
	public void removed() {
		ModMenuConfigManager.save();
	}
}
