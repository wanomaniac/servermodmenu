package com.skellybuilds.servermodmenu.gui;

import com.skellybuilds.servermodmenu.config.ModMenuConfig;
import com.skellybuilds.servermodmenu.config.ModMenuConfigManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

import java.util.function.Consumer;

public class ConfirmationScreen extends Screen {
	private final Consumer<ConfirmationScreen> YesCB;
	private final Consumer<ConfirmationScreen> NoCB;
	public Screen prevS;
	public Text mainT;

	public ConfirmationScreen(Screen previousScreen, Consumer<ConfirmationScreen> ycb, Consumer<ConfirmationScreen> ncb, Text text) {
		super(Text.translatable("servermodmenu.conf.title"));
		YesCB = ycb;
		NoCB = ncb;
		prevS = previousScreen;
		mainT = text;
	}

	@Override
	protected void init() {
		// Yes button
		this.addDrawableChild(
			ButtonWidget.builder(Text.translatable("servermodmenu.conf.y"), button -> YesCB.accept(this))
				.position(8, 138)
				.size(75, 20)
				.build()
		);

		// No button
		this.addDrawableChild(
			ButtonWidget.builder(Text.translatable("servermodmenu.conf.n"), button -> NoCB.accept(this))
				.position(346, 138)
				.size(75, 20)
				.build()
		);
	}

	@Override
	public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
		// Draw main text

		drawContext.drawCenteredTextWithShadow(this.textRenderer, this.mainT, this.width / 2, 45, Colors.WHITE);
		super.render(drawContext, mouseX, mouseY, delta);
	}
}
