package com.maniake.servermodmenu.gui.widget;

import com.maniake.servermodmenu.gui.ModsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;


public class ModMenuButtonWidget extends Button {
	public ModMenuButtonWidget(int x, int y, int width, int height, Component text, Screen screen) {
		super(x, y, width, height, text, button -> Minecraft.getInstance().setScreen(new ModsScreen(screen)), Button.DEFAULT_NARRATION);

	}


	@Override
	protected void renderContents(GuiGraphics guiGraphics, int i, int i1, float v) {
		this.renderDefaultSprite(guiGraphics);
		this.renderDefaultLabel(guiGraphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE));
	}
}
