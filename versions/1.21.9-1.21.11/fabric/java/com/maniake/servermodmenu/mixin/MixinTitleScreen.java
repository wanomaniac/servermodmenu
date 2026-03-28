package com.maniake.servermodmenu.mixin;

import com.maniake.servermodmenu.Constants;
import com.maniake.servermodmenu.config.ModMenuConfig;
import com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(TitleScreen.class)
public class MixinTitleScreen extends Screen {
    @Shadow
    private long fadeInStart;
    @Shadow
    private boolean fading;

    protected MixinTitleScreen(Component title) {
        super(title);
    }

    @ModifyArg(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/realmsclient/gui/screens/RealmsNotificationsScreen;init(Lnet/minecraft/client/Minecraft;II)V"
            ),
            index = 2
    )
	private int adjustRealmsHeight(int height) {

		if (ModMenuConfig.MODIFY_TITLE_SCREEN.getValue() && ModMenuConfig.MODS_BUTTON_STYLE.getValue() == ModMenuConfig.TitleMenuButtonStyle.CLASSIC) {
			return height - 51;
		} else if (ModMenuConfig.MODS_BUTTON_STYLE.getValue() == ModMenuConfig.TitleMenuButtonStyle.REPLACE_REALMS || ModMenuConfig.MODS_BUTTON_STYLE.getValue() == ModMenuConfig.TitleMenuButtonStyle.SHRINK) {
			return -99999;
		} else {
			return height;
		}
	}

//	@ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V", ordinal = 0))
//	private String onRender(String string) {
//		if (ModMenuConfig.MODIFY_TITLE_SCREEN.getValue() && ModMenuConfig.MOD_COUNT_LOCATION.getValue().isOnTitleScreen()) {
//			String count = Constants.getDisplayedModCount();
//			String specificKey = "modmenu.mods." + count;
//			String replacementKey = I18n.exists(specificKey) ? specificKey : "modmenu.mods.n";
//			if (ModMenuConfig.EASTER_EGGS.getValue() && I18n.exists(specificKey + ".secret")) {
//				replacementKey = specificKey + ".secret";
//			}
//
//			return string.replace(I18n.get(I18n.get("menu.modded")), I18n.get(replacementKey, count));
//		} else {
//			return string;
//		}
//	}

    @Inject(
            method = "render",
            at = @At("TAIL")
    )
    private void    renderExtraText(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float delta,
            CallbackInfo ci
    ) {
        if (!ModMenuConfig.MODIFY_TITLE_SCREEN.getValue()
                || !ModMenuConfig.MOD_COUNT_LOCATION.getValue().isOnTitleScreen()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        MutableComponent count = Constants.getDisplayedModCount();

        float f = 1.0F;
        if (this.fading) {
            float g = (float) (Util.getMillis() - this.fadeInStart) / 2000.0F;
            if (g > 1.0F) {
                this.fading = false;
            } else {
                g = Mth.clamp(g, 0.0F, 1.0F);
                f = Mth.clampedMap(g, 0.5F, 1.0F, 0.0F, 1.0F);
            }
        }

            // Draw ABOVE version text
        int maxWidth = this.width - 4;

        List<FormattedCharSequence> lines =
                font.split(count, maxWidth);

        int y = this.height - 10 - (lines.size() * font.lineHeight);

        for (FormattedCharSequence line : lines) {
            guiGraphics.drawString(
                    font,
                    line,
                    2,
                    y,
                    ARGB.color(f, -1),
                    true
            );
            y += font.lineHeight;
        }
    }
}
