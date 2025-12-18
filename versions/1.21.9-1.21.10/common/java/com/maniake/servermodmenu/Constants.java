package com.maniake.servermodmenu;

import com.google.gson.*;
import com.maniake.servermodmenu.config.ModMenuConfig;
import com.maniake.servermodmenu.db.ModAdapter;
import com.maniake.servermodmenu.db.SMod;
import com.maniake.servermodmenu.gui.EntryButton;
import com.maniake.servermodmenu.interfaces.IExternalModMgr;
import com.maniake.servermodmenu.interfaces.IMod;
import com.maniake.servermodmenu.interfaces.IModMetadata;
import com.maniake.servermodmenu.interfaces.IPlatformHelper;
import com.maniake.servermodmenu.services.ServiceKey;
import com.maniake.servermodmenu.services.ServicesManager;
import com.maniake.servermodmenu.utils.Networking;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.util.*;

public class Constants {
    public static final IPlatformHelper Platform =
            ServicesManager.get(ServiceKey.of(IPlatformHelper.class));
    public static final IModMetadata ModMetadata =
            ServicesManager.get(ServiceKey.of(IModMetadata.class));
	public static final String MOD_ID = "servermodmenu";
	public static final String MOD_NAME = "Server Mod Menu";
	public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);
    public static final Gson GSON = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setPrettyPrinting().create();
    private static int cachedDisplayedModCount = -1;
    public static final Map<String, Map<String, SMod>> SMODS = new HashMap<>(); // Server Name - Mod Map
    public static Map<String, SMod> SMODSA = new HashMap<>();
    public static final Map<String, IMod> MODS = new HashMap<>();
    public static final List<String> idsDLD = new ArrayList<>();
    public static final IExternalModMgr ExternalModManager = ServicesManager.get(ServiceKey.of(IExternalModMgr.class));
    public static final Map<String, EntryButton> buttonEntries = new HashMap<>();
    public static final Networking NETWORKING = new Networking();
    private static String prevLoc;
    public static boolean isAllDFB = false;
    public static IModMetadata modVersion = ServicesManager.get(ServiceKey.of(IModMetadata.class));

    public static int smmServerCount = 0;
    public static Map<String, Integer> smmModCount = new HashMap<>();
    public static Map<String, Integer> smmInstalledModCount = new HashMap<>();
    public static Map<String, Integer> smmOutofDateModCount = new HashMap<>();
    public static Map<String, Integer> smmUninstalledModCount =new HashMap<>() ;

    public static void clearModCountCache() {
        cachedDisplayedModCount = -1;
    }

    private static String plural(int n) {
        return (n > 1 || n == 0) ? "s" : "";
    }


    public static MutableComponent getDisplayedModCount() {
        MutableComponent root = Component.empty();

        int total = smmModCount.values().stream().mapToInt(Integer::intValue).sum();
        int installed = smmInstalledModCount.values().stream().mapToInt(Integer::intValue).sum();
        int uninstalled = smmUninstalledModCount.values().stream().mapToInt(Integer::intValue).sum();
        int outOfDate = smmOutofDateModCount.values().stream().mapToInt(Integer::intValue).sum();

        // Line 1 Servers in total
        root.append(
                Component.literal(smmServerCount + " ")
                        .append(I18n.get("modmenu.count.smm"))
                        .append(plural(smmServerCount))
                        .withStyle(ChatFormatting.YELLOW).withStyle(ChatFormatting.ITALIC)
        ).append("\n");

        // Line 2 Installed mods
        ChatFormatting installedColor =
                installed == total ? ChatFormatting.GREEN : ChatFormatting.WHITE;

        root.append(
                Component.literal(installed + " ")
                        .append(I18n.get("modmenu.count.installed"))
                        .append(plural(installed))
                        .withStyle(installedColor)
        ).append("\n");

        // Line 3 Uninstalled mods
        ChatFormatting uninstalledColor =
                uninstalled == total ? ChatFormatting.RED : ChatFormatting.WHITE;

        root.append(
                Component.literal(uninstalled + " ")
                        .append(I18n.get("modmenu.count.uninstalled"))
                        .append(plural(uninstalled))
                        .withStyle(uninstalledColor)
        ).append("\n");


        boolean flashOn = (Util.getMillis() / 1500) % 2 == 0;
        ChatFormatting flashColor = flashOn
                ? ChatFormatting.RED
                : ChatFormatting.DARK_RED;

        root.append(
                Component.literal(outOfDate + " ")
                        .append(I18n.get("modmenu.count.outofdate"))
                        .append(plural(outOfDate))
                        .withStyle(outOfDate > 0 ? flashColor : ChatFormatting.RESET)
        );


        root.append("\n");

        // Total
        root.append(
                Component.literal(total + " ")
                        .append(I18n.get("modmenu.count.total"))
                        .append(plural(total))
                        .withStyle(ChatFormatting.WHITE)
        );

        return root;
    }

    public static void sendmodstoserver(String ip, Minecraft client){
        ServerAddress address = ServerAddress.parseString(ip);

        Optional<ServerAddress> optAddress = Networking.AllowedAddressResolver.DEFAULT.resolve(address);

        if(optAddress.isPresent()) {
            final ServerAddress fetchedAddress = optAddress.get();
            NETWORKING.connect(fetchedAddress.getHost(), fetchedAddress.getPort());
        } else {
            NETWORKING.connect(ip, 0);
        }
        Gson gson = new Gson();
        JsonObject obj = new JsonObject();

        obj.addProperty("playerN", client.getUser().getName());
        JsonArray arr = new JsonArray();
        for(SMod mod : ExternalModManager.getAllMods()){
            String entry = mod.getId() + "$" + mod.getVersion();
            arr.add(entry);
        }

        obj.add("data", arr);
        obj.addProperty("protocal", SharedConstants.getProtocolVersion());
        obj.addProperty("version", SharedConstants.getCurrentVersion().id());
        obj.addProperty("loader", Platform.getPlatformName());
        obj.addProperty("local", client.getLanguageManager().getSelected());
        String data = gson.toJson(obj);

        if(optAddress.isPresent()) {
            final ServerAddress fetchedAddress = optAddress.get();
            NETWORKING.requestNResponse(fetchedAddress.getHost(), "addpmods|" + data);
        } else {
            NETWORKING.send(ip, "addpmods|" + data);
        }

    }


    public static void sendmodstolist(ServerList list, Minecraft client){
        list.load();
        for (int i = 0; i < list.size(); i++) {
            ServerData serverInfo = list.get(i);


            ServerAddress address = ServerAddress.parseString(serverInfo.ip);

        Optional<ServerAddress> optAddress = Networking.AllowedAddressResolver.DEFAULT.resolve(address);

        if(optAddress.isPresent()) {
            final ServerAddress fetchedAddress = optAddress.get();
            NETWORKING.connect(fetchedAddress.getHost(), fetchedAddress.getPort());
        } else {
            NETWORKING.connect(serverInfo.ip, 0);
        }
        Gson gson = new Gson();
        JsonObject obj = new JsonObject();

        obj.addProperty("playerN", client.getUser().getName());
        JsonArray arr = new JsonArray();
        for(SMod mod : ExternalModManager.getAllMods()){
            String entry = mod.getId() + "$" + mod.getVersion();
            arr.add(entry);
        }

        obj.add("data", arr);
        obj.addProperty("protocal", SharedConstants.getProtocolVersion());
        obj.addProperty("version", SharedConstants.getCurrentVersion().id());
        obj.addProperty("loader", Platform.getPlatformName());
        obj.addProperty("local", client.getLanguageManager().getSelected());
        String data = gson.toJson(obj);

        if(optAddress.isPresent()) {
            final ServerAddress fetchedAddress = optAddress.get();
            NETWORKING.requestNResponse(fetchedAddress.getHost(), "addpmods|" + data);
        } else {
            NETWORKING.send(serverInfo.ip, "addpmods|" + data);
            }
        }
    }

    public static List<String> incompatibleServers = new ArrayList<>();

    public static boolean ConfirmIfCompatibleServer(String ip) {
//        Optional<ServerAddress> optAddress = Networking.AllowedAddressResolver.DEFAULT.resolve(new ServerAddress(ip, 0));
//        if (optAddress.isPresent()) {
//            final ServerAddress fetchedAddress = optAddress.get();
//            NETWORKING.connect(fetchedAddress.getHost(), fetchedAddress.getPort());
//        } else {
//            NETWORKING.connect(ip, 27752);
//        }
        String versions = NETWORKING.requestNResponse(ip, "getprotversion");
        try {
            String[] parts = versions.split("\\|", 3);
            String protocalVersion = parts[0];
            String gameVersion = parts[1];
            String loader = parts[2];
            int protVersionNumber = Integer.parseInt(protocalVersion);
            if (SharedConstants.getProtocolVersion() != protVersionNumber || !SharedConstants.getCurrentVersion().id().equals(gameVersion) || !loader.equals(Platform.getPlatformName())) {
                incompatibleServers.add(ip);
                return false;
            } else {
                incompatibleServers.remove(ip);
                return true;
            }
        } catch (RuntimeException e) {
            incompatibleServers.add(ip);
            return false;
        }
    }

    public static void LoadServerListConnections(ServerList list, Networking network){
        final SMod[][] ModsA = {{}};
        SMODS.clear();
        SMODSA.clear();
        Constants.smmInstalledModCount.clear();
        Constants.smmUninstalledModCount.clear();
        Constants.smmOutofDateModCount.clear();
        Constants.smmServerCount = 0;
        Constants.smmModCount.clear();

        list.load();
        for (int i = 0; i < list.size(); i++) {
            ServerData serverInfo = list.get(i);

            NETWORKING.connect(serverInfo.ip, 0);

            if(!ConfirmIfCompatibleServer(serverInfo.ip)) continue;

            if(!network.isSocketValid(serverInfo.ip)) continue;
            try {
                GsonBuilder gsonBuilder = new GsonBuilder();
                gsonBuilder.registerTypeAdapter(SMod.class, new ModAdapter());
                Gson gson = gsonBuilder.create();
                String str = NETWORKING.requestNResponse(serverInfo.ip, "getall|" + Minecraft.getInstance().getUser().getName());
                ModsA[0] = gson.fromJson(str, SMod[].class);
                boolean sinit = false;
                if (ModsA[0].length < 1) {
                    SMODS.computeIfAbsent(serverInfo.ip, k -> new HashMap<>());
                    smmModCount.put(serverInfo.ip, 0);
                } else {
                    smmModCount.put(serverInfo.ip, ModsA[0].length);
                }

                for (SMod smod : ModsA[0]) {
                    if (!sinit) {
                        SMODS.computeIfAbsent(serverInfo.ip, k -> new HashMap<>());
                        SMODSA = new HashMap<>();
                        sinit = true;
                    }

                    if(Constants.ExternalModManager.DoesModExist(smod.id)){
                        smmInstalledModCount.put(serverInfo.ip, smmInstalledModCount.getOrDefault(serverInfo.ip, 0) + 1);
                        if(!Networking.isModAlreadyPresent(smod.id, smod.version)){
                            smmOutofDateModCount.put(serverInfo.ip, smmOutofDateModCount.getOrDefault(serverInfo.ip, 0) + 1);
                        }
                    } else {
                        smmUninstalledModCount.put(serverInfo.ip, smmUninstalledModCount.getOrDefault(serverInfo.ip, 0) + 1);
                    }

                    smod.server = serverInfo.ip;
                    SMODS.get(serverInfo.ip).put(smod.getId(), smod);
                }
            } catch (Exception e) {
                continue;
            }


        }
    }

    private static final List<String> loadingServers = new ArrayList<>();

    public static void LoadServer(String ip, Networking network){
        if(loadingServers.contains(ip)) return;

        loadingServers.add(ip);
        final SMod[][] ModsA = {{}};
        ServerAddress parsedAd = ServerAddress.parseString(ip);
        SMODS.remove(ip);
        SMODSA.remove(ip);

        NETWORKING.connect(ip, 0);

        if(!ConfirmIfCompatibleServer(ip)) return;

        if(!network.isSocketValid(ip)){
            loadingServers.remove(ip);
            return;
        }
        try {
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(SMod.class, new ModAdapter());
            Gson gson = gsonBuilder.create();
            String str = NETWORKING.requestNResponse(ip, "getall|" + Minecraft.getInstance().getUser().getName());
            ModsA[0] = gson.fromJson(str, SMod[].class);
            boolean sinit = false;
            if (ModsA[0].length < 1) {
                SMODS.computeIfAbsent(ip, k -> new HashMap<>());
                Constants.smmModCount.put(ip, 0);
            } else {
                Constants.smmModCount.put(ip, ModsA[0].length);
            }
            for (SMod smod : ModsA[0]) {
                if (!sinit) {
                    SMODS.computeIfAbsent(ip, k -> new HashMap<>());
                    SMODSA = new HashMap<>();
                    sinit = true;
                }

                if(Constants.ExternalModManager.DoesModExist(smod.id)){
                    smmInstalledModCount.put(ip, smmInstalledModCount.getOrDefault(ip, 0) + 1);
                    if(!Networking.isModAlreadyPresent(smod.id, smod.version)){
                        smmOutofDateModCount.put(ip, smmOutofDateModCount.getOrDefault(ip, 0) + 1);
                    }
                } else {
                    smmUninstalledModCount.put(ip, smmUninstalledModCount.getOrDefault(ip, 0) + 1);
                }


                smod.server = ip;
                SMODS.get(ip).put(smod.getId(), smod);
            }
            loadingServers.remove(ip);
        } catch (Exception e) {
            loadingServers.remove(ip);
            return;
        }
    }

    public static Component createModsButtonText(boolean title) {
        var titleStyle = ModMenuConfig.MODS_BUTTON_STYLE.getValue();
        var gameMenuStyle = ModMenuConfig.GAME_MENU_BUTTON_STYLE.getValue();
        var isIcon = title ? titleStyle == ModMenuConfig.TitleMenuButtonStyle.ICON : gameMenuStyle == ModMenuConfig.GameMenuButtonStyle.ICON;
        var isShort = title ? titleStyle == ModMenuConfig.TitleMenuButtonStyle.SHRINK : gameMenuStyle == ModMenuConfig.GameMenuButtonStyle.REPLACE_BUGS;
        MutableComponent modsText = Component.translatable("servermodmenu.title");
        if (ModMenuConfig.MOD_COUNT_LOCATION.getValue().isOnModsButton() && !isIcon) {
            MutableComponent count = getDisplayedModCount();
            if (isShort) {
                modsText.append(Component.literal(" ")).append(Component.translatable("modmenu.loaded.short", count));
            } else {
                String specificKey = "modmenu.loaded." + count;
                String key = I18n.exists(specificKey) ? specificKey : "modmenu.loaded";
                if (ModMenuConfig.EASTER_EGGS.getValue() && I18n.exists(specificKey + ".secret")) {
                    key = specificKey + ".secret";
                }
                modsText.append(Component.literal(" ")).append(Component.translatable(key, count));
            }
        }
        return modsText;
    }

}