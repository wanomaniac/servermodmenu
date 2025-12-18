package com.maniake.servermodmenu.interfaces;

import com.maniake.servermodmenu.db.SMod;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class ExternalModManagerForge implements IExternalModMgr {
    public void DeleteExistingMod(String id) {
        Optional<ModContainer> modContainerOptional = (Optional<ModContainer>) ModList.get().getModContainerById(id);
        if (modContainerOptional.isPresent()) {
            ModContainer modContainer = modContainerOptional.get();
            Path modJarPath = modContainer.getModInfo().getOwningFile().getFile().getFilePath();
            try {
                Files.delete(modJarPath);
            } catch (IOException e) {
                // file should be guranteed existing, but shouldn't stop download process if exceptions occur.
            }
        }
    }
    public boolean DoesModExist(String id){
        Optional<ModContainer> modContainerOptional = (Optional<ModContainer>) ModList.get().getModContainerById(id);
        return modContainerOptional.isPresent();
    }
    public String GetModVersion(String id){
        Optional<ModContainer> modContainerOptional = (Optional<ModContainer>) ModList.get().getModContainerById(id);
        if(modContainerOptional.isEmpty()){
            return "";
        }
        return modContainerOptional.get().getModInfo().getVersion().toString();
    }
    public List<SMod> getAllMods(){
        List<SMod> mods = new java.util.ArrayList<>();
        for (ModContainer mod : ModList.get().getLoadedMods()) {
            SMod imod = new SMod();
            imod.version = mod.getModInfo().getVersion().toString();
            imod.id = mod.getModId();
            mods.add(imod);
        }
        return mods;
    }
}
