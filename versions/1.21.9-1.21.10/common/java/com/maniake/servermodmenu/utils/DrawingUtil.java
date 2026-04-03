package com.maniake.servermodmenu.utils;

import com.maniake.servermodmenu.interfaces.IMod;
import com.maniake.servermodmenu.config.ModMenuConfig;
import com.maniake.servermodmenu.db.SMod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth; // someone on mojang prolly was on meth abbreviating a 4 letter word
import java.util.List;
import java.util.Random;

public class DrawingUtil {
	private static final Minecraft CLIENT = Minecraft.getInstance();

    public static void drawTooltip(GuiGraphics DrawContext, Component text, int x, int y) {
        DrawContext.renderTooltip(CLIENT.font, List.of(text).stream().map(Component::getVisualOrderText).map(ClientTooltipComponent::create).toList(), x, y, DefaultTooltipPositioner.INSTANCE, null);
    }
	public static void drawRandomVersionBackground(IMod mod, GuiGraphics DrawContext, int x, int y, int width, int height) {
		int seed = mod.getName().hashCode() + mod.getVersion().hashCode();
		Random random = new Random(seed);
		int color = 0xFF000000 | Mth.hsvToRgb(random.nextFloat(1f), random.nextFloat(0.7f, 0.8f), 0.9f);
		if (!ModMenuConfig.RANDOM_JAVA_COLORS.getValue()) {
			color = 0xFFDD5656;
		}
//		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		DrawContext.fill(x, y, x + width, y + height, color);
	}

	public static void drawRandomVersionBackgroundS(SMod mod, GuiGraphics DrawContext, int x, int y, int width, int height) {
		int seed = mod.meta.name.hashCode() + mod.getVersion().hashCode();
		Random random = new Random(seed);
		int color = 0xFF000000 | Mth.hsvToRgb(random.nextFloat(1f), random.nextFloat(0.7f, 0.8f), 0.9f);
		if (!ModMenuConfig.RANDOM_JAVA_COLORS.getValue()) {
			color = 0xFFDD5656;
		}
//		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		DrawContext.fill(x, y, x + width, y + height, color);
	}

	public static void drawWrappedString(GuiGraphics DrawContext, String string, int x, int y, int wrapWidth, int lines, int color) {
		while (string != null && string.endsWith("\n")) {
			string = string.substring(0, string.length() - 1);
		}
		List<FormattedText> strings = CLIENT.font.getSplitter().splitLines(string, wrapWidth, Style.EMPTY);
		for (int i = 0; i < strings.size(); i++) {
			if (i >= lines) {
				break;
			}
            FormattedText renderable = strings.get(i);
			if (i == lines - 1 && strings.size() > lines) {
				renderable = FormattedText.composite(strings.get(i), FormattedText.of("..."));
			}
            FormattedCharSequence line = Language.getInstance().getVisualOrder(renderable);
            float x1 = x;
			if (Language.getInstance().isDefaultRightToLeft()) {
				float width = CLIENT.font.getSplitter().stringWidth(line);
				x1 += (wrapWidth - width);
			}
			DrawContext.drawString(CLIENT.font, line, (int)x1, y + i * CLIENT.font.lineHeight, color, true);
		}
	}

	public static void drawBadge(GuiGraphics DrawContext, int x, int y, int tagWidth, MutableComponent text, int outlineColor, int fillColor, int textColor) {
		DrawContext.fill(x + 1, y - 1, x + tagWidth, y, outlineColor);
		DrawContext.fill(x, y, x + 1, y + CLIENT.font.lineHeight, outlineColor);
		DrawContext.fill(x + 1, y + 1 + CLIENT.font.lineHeight - 1, x + tagWidth, y + CLIENT.font.lineHeight + 1, outlineColor);
		DrawContext.fill( x + tagWidth, y, x + tagWidth + 1, y + CLIENT.font.lineHeight, outlineColor);
		DrawContext.fill( x + 1, y, x + tagWidth, y + CLIENT.font.lineHeight, fillColor);
		DrawContext.drawString(CLIENT.font, text, (int) (x + 1 + (tagWidth - CLIENT.font.width(text)) / (float) 2), y + 1, textColor, false);
	}
}
