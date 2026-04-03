package com.maniake.servermodmenu.event;

import com.maniake.servermodmenu.gui.ModsScreen;
import com.maniake.servermodmenu.interfaces.IEventRegister;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;

public class EventRegisterForge implements IEventRegister {
    @Override
    public void Register(){

    }

    public static final KeyMapping MENU_KEY_BIND = new KeyMapping(
            "key.servermodmenu.open_menu",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            KeyMapping.Category.MISC
    );

    public static class ModMenuEventHandler {
	public static void afterScreenInit(Minecraft client, Screen screen, int scaledWidth, int scaledHeight) {
		if (screen instanceof TitleScreen) {
			afterTitleScreenInit(screen);
		}
	}

	private static void afterTitleScreenInit(Screen screen) {

	}

	public static void onClientEndTick(Minecraft client) {
		while (MENU_KEY_BIND.consumeClick()) {
			client.setScreen(new ModsScreen(client.screen));
		}
	}

	public static boolean buttonHasText(AbstractButton widget, String translationKey) {
		if (widget instanceof Button button) {
			Component text = button.getMessage();
			ComponentContents textContent = text.getContents();
			return textContent instanceof TranslatableContents && ((TranslatableContents) textContent).getKey().equals(translationKey);
		}
		return false;
	}

	public static void shiftButtons(AbstractWidget widget, boolean shiftUp, int spacing) {
		if (shiftUp) {
			widget.setY(widget.getY() - spacing / 2);
		} else if (!(widget instanceof AbstractButton button && button.getMessage().equals(Component.translatable("title.credits")))) {
			widget.setY(widget.getY() + spacing / 2);
		}
	}
}
}
