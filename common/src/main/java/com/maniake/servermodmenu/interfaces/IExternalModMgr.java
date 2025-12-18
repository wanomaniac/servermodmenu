package com.maniake.servermodmenu.interfaces;

import com.maniake.servermodmenu.db.SMod;

import java.util.List;

public interface IExternalModMgr {
    void DeleteExistingMod(String id);
    boolean DoesModExist(String id);
    String GetModVersion(String id);
    List<SMod> getAllMods();
}
