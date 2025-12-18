package com.maniake.servermodmenu.config.option;

import net.minecraft.client.OptionInstance;

public interface OptionConvertable {
    OptionInstance<?> asOption();
}
