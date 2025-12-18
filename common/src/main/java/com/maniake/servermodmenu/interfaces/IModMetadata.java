package com.maniake.servermodmenu.interfaces;

import java.io.File;

public interface IModMetadata {
    String getVersionName();
    String getConfigVersion();
    File getConfigDir();
}