package com.maniake.servermodmenu.interfaces;
import com.maniake.servermodmenu.db.SMod;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class ExternalModManagerFabric implements IExternalModMgr {
    public void DeleteExistingMod(String id) {
        Optional<ModContainer> modContainerOptional = FabricLoader.getInstance().getModContainer(id);
        if (modContainerOptional.isPresent()) {
            ModContainer modContainer = modContainerOptional.get();
            Path modJarPath = modContainer.getOrigin().getPaths().get(0);
            try {
                Files.delete(modJarPath);
            } catch (IOException e) {
                // file should be guranteed existing, but shouldn't stop download process if exceptions occur.
            }
        }
    }
    public boolean DoesModExist(String id){
        Optional<ModContainer> modContainerOptional = FabricLoader.getInstance().getModContainer(id);
        return modContainerOptional.isPresent();
    }
    public String GetModVersion(String id){
        Optional<ModContainer> modContainerOptional = FabricLoader.getInstance().getModContainer(id);
        if(modContainerOptional.isEmpty()){
            return "";
        }
        return modContainerOptional.get().getMetadata().getVersion().getFriendlyString();
    }
    public List<SMod> getAllMods(){
        List<SMod> mods = new java.util.ArrayList<>();
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            SMod imod = new SMod();
            imod.version = mod.getMetadata().getVersion().getFriendlyString();
            imod.id = mod.getMetadata().getId();
            mods.add(imod);
        }
        return mods;
    }
}
