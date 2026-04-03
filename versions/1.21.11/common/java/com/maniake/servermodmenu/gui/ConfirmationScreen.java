package com.maniake.servermodmenu.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class ConfirmationScreen extends Screen {
	private final Consumer<ConfirmationScreen> YesCB;
	private final Consumer<ConfirmationScreen> NoCB;
	public Screen prevS;
	public net.minecraft.network.chat.Component mainT;
    int buttonWidth = 75;
    int spacing = 6;
    int totalButtonsWidth = buttonWidth * 2 + spacing;
    int centerX = this.width / 2;
    int buttonsLeft = centerX - totalButtonsWidth / 2;
    int buttonRight = buttonsLeft + totalButtonsWidth;

    int y = 138;

    public ConfirmationScreen(Screen previousScreen, Consumer<ConfirmationScreen> ycb, Consumer<ConfirmationScreen> ncb, Component text) {
		super(Component.translatable("servermodmenu.conf.title"));
		YesCB = ycb;
		NoCB = ncb;
		prevS = previousScreen;
		mainT = text;

    }



    @Override
    protected void init() {
        this.addRenderableWidget(
                Button.builder(Component.translatable("servermodmenu.conf.y"), b -> YesCB.accept(this))
                        .pos(buttonsLeft + buttonWidth + spacing, y)
                        .size(buttonWidth, 20)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(Component.translatable("servermodmenu.conf.n"), b -> NoCB.accept(this))
                        .pos(buttonRight , y)
                        .size(buttonWidth, 20)
                        .build()
        );
    }

	@Override
	public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
		drawContext.drawCenteredString(this.font, this.mainT, this.width / 2, this.height / 2, 0xFFFFFFFF);
		super.render(drawContext, mouseX, mouseY, delta);
	}
}
