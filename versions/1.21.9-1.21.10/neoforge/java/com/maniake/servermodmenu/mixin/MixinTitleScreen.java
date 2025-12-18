package com.maniake.servermodmenu.mixin;

import com.maniake.servermodmenu.Constants;
import com.maniake.servermodmenu.config.ModMenuConfig;
import com.maniake.servermodmenu.gui.widget.ModMenuButtonWidget;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
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

import static com.maniake.servermodmenu.event.EventRegisterNeoForge.ModMenuEventHandler.buttonHasText;
import static com.maniake.servermodmenu.event.EventRegisterNeoForge.ModMenuEventHandler.shiftButtons;

@Mixin(TitleScreen.class)
public class MixinTitleScreen extends Screen {
    @Shadow
    private long fadeInStart;
    @Shadow
    private boolean fading;

    protected MixinTitleScreen(Component title) {
        super(title);
    }

    @Inject(
            method = "init", at = @At("RETURN")
    )
    private void afterInit(CallbackInfo ci) {
        if (ModMenuConfig.MODIFY_TITLE_SCREEN.getValue()) {
            int modsButtonIndex = -1;
            final int spacing = 24;
            int buttonsY = height / 4 + 48;
            for (int i = 0; i < renderables.size(); i++) {
                AbstractWidget widget = (AbstractWidget) renderables.get(i);
                if (widget instanceof Button button) {
                    if (ModMenuConfig.MODS_BUTTON_STYLE.getValue() == ModMenuConfig.TitleMenuButtonStyle.CLASSIC) {
                        if (button.visible) {
                            shiftButtons(button, modsButtonIndex == -1, spacing);
                            if (modsButtonIndex == -1) {
                                buttonsY = button.getY();
                            }
                        }
                    }
                    if (buttonHasText(button, "menu.online")) {
                        if (ModMenuConfig.MODS_BUTTON_STYLE.getValue() == ModMenuConfig.TitleMenuButtonStyle.REPLACE_REALMS) {
                            renderables.set(i, new ModMenuButtonWidget(button.getX(), button.getY(), button.getWidth(), button.getHeight(), Constants.createModsButtonText(true), this));
                        } else {
                            if (ModMenuConfig.MODS_BUTTON_STYLE.getValue() == ModMenuConfig.TitleMenuButtonStyle.SHRINK) {
                                button.setWidth(98);
                            }
                            modsButtonIndex = i + 1;
                            if (button.visible) {
                                buttonsY = button.getY();
                            }
                        }
                    }
                }

            }
            if (modsButtonIndex != -1) {
                if (ModMenuConfig.MODS_BUTTON_STYLE.getValue() == ModMenuConfig.TitleMenuButtonStyle.CLASSIC) {
                    addRenderableWidget(new ModMenuButtonWidget(this.width / 2 - 100, buttonsY + spacing, 200, 20, Constants.createModsButtonText(true), this));
                } else if (ModMenuConfig.MODS_BUTTON_STYLE.getValue() == ModMenuConfig.TitleMenuButtonStyle.SHRINK) {
                    addRenderableWidget(new ModMenuButtonWidget(this.width / 2 + 2, buttonsY, 98, 20, Constants.createModsButtonText(true), this));
                }
            }
        }
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

        int y = this.height - 20 - (lines.size() * font.lineHeight);

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
