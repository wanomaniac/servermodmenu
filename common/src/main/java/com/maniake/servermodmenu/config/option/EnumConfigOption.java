package com.maniake.servermodmenu.config.option;

import com.mojang.serialization.Codec;
import com.maniake.servermodmenu.utils.TranslationUtil;
import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;
//import net.minecraft.client.option.SimpleOption;
//import net.minecraft.screen.ScreenTexts;
//import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.Locale;

public class EnumConfigOption<E extends Enum<E>> implements OptionConvertable {
	private final String key, translationKey;
	private final Class<E> enumClass;
	private final E defaultValue;

	public EnumConfigOption(String key, E defaultValue) {
		ConfigOptionStorage.setEnum(key, defaultValue);
		this.key = key;
		this.translationKey = TranslationUtil.translationKeyOf("option", key);
		this.enumClass = defaultValue.getDeclaringClass();
		this.defaultValue = defaultValue;
	}

	public String getKey() {
		return key;
	}

	public E getValue() {
		return ConfigOptionStorage.getEnum(key, enumClass);
	}

	public void setValue(E value) {
		ConfigOptionStorage.setEnum(key, value);
	}

	public void cycleValue() {
		ConfigOptionStorage.cycleEnum(key, enumClass);
	}

	public void cycleValue(int amount) {
		ConfigOptionStorage.cycleEnum(key, enumClass, amount);
	}

	public E getDefaultValue() {
		return defaultValue;
	}

	private static <E extends Enum<E>> Component getValueText(EnumConfigOption<E> option, E value) {
		return Component.translatable(option.translationKey + "." + value.name().toLowerCase(Locale.ROOT));
	}

	public Component getButtonText() {
        return Component.translatable("options.generic_value", new Object[]{Component.translatable(translationKey), getValueText(this, getValue())});
	}

	@Override
	public OptionInstance<E> asOption() {
		return new OptionInstance<>(translationKey, OptionInstance.noTooltip(),
				(text, value) -> getValueText(this, value),
				new OptionInstance.Enum<>(Arrays.asList(enumClass.getEnumConstants()),
						Codec.STRING.xmap(
								string -> Arrays.stream(enumClass.getEnumConstants()).filter(e -> e.name().toLowerCase().equals(string)).findAny().orElse(null),
								newValue -> newValue.name().toLowerCase()
						)),
				getValue(), value -> ConfigOptionStorage.setEnum(key, value));
	}
}
