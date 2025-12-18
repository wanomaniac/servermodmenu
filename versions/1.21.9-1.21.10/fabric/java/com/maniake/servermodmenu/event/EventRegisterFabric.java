package com.maniake.servermodmenu.event;

import com.maniake.servermodmenu.Constants;
import com.maniake.servermodmenu.config.ModMenuConfig;
import com.maniake.servermodmenu.gui.ModsScreen;
import com.maniake.servermodmenu.gui.widget.ModMenuButtonWidget;
import com.maniake.servermodmenu.interfaces.IEventRegister;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.awt.*;
import java.util.List;

public class EventRegisterFabric implements IEventRegister {
    @Override
    public void Register(){
        ModMenuEventHandler.register();
    }

    static class ModMenuEventHandler {
	private static KeyMapping MENU_KEY_BIND;

	public static void register() {
		MENU_KEY_BIND = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.servermodmenu.open_menu",
				InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
				KeyMapping.Category.MISC
		));
		ClientTickEvents.END_CLIENT_TICK.register(ModMenuEventHandler::onClientEndTick);
		ScreenEvents.AFTER_INIT.register(ModMenuEventHandler::afterScreenInit);
	}

	public static void afterScreenInit(Minecraft client, Screen screen, int scaledWidth, int scaledHeight) {
		if (screen instanceof TitleScreen) {
			afterTitleScreenInit(screen);
		}
	}

	private static void afterTitleScreenInit(Screen screen) {
		final List<AbstractWidget> buttons = Screens.getButtons(screen);
		if (ModMenuConfig.MODIFY_TITLE_SCREEN.getValue()) {
			int modsButtonIndex = -1;
			final int spacing = 24;
			int buttonsY = screen.height / 4 + 48;
			for (int i = 0; i < buttons.size(); i++) {
				AbstractWidget widget = buttons.get(i);
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
							buttons.set(i, new ModMenuButtonWidget(button.getX(), button.getY(), button.getWidth(), button.getHeight(), Constants.createModsButtonText(true), screen));
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
					buttons.add(modsButtonIndex, new ModMenuButtonWidget(screen.width / 2 - 100, buttonsY + spacing, 200, 20, Constants.createModsButtonText(true), screen));
				} else if (ModMenuConfig.MODS_BUTTON_STYLE.getValue() == ModMenuConfig.TitleMenuButtonStyle.SHRINK) {
					buttons.add(modsButtonIndex, new ModMenuButtonWidget(screen.width / 2 + 2, buttonsY, 98, 20, Constants.createModsButtonText(true), screen));
				}
			}
		}
	}

	private static void onClientEndTick(Minecraft client) {
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
