package com.maniake.servermodmenu.gui.widget.entries;

import com.maniake.servermodmenu.Constants;
import com.maniake.servermodmenu.config.ModMenuConfig;
import com.maniake.servermodmenu.config.ModMenuConfigManager;
import com.maniake.servermodmenu.db.SMod;
import com.maniake.servermodmenu.gui.EntryButton;
import com.maniake.servermodmenu.gui.widget.ModListWidget;
import com.maniake.servermodmenu.utils.DrawingUtil;
import com.maniake.servermodmenu.utils.Networking;
import com.maniake.servermodmenu.utils.TexturesManager;
import com.maniake.servermodmenu.interfaces.IMod;
//import com.skellybuilds.servermodmenu.util.mod.ModBadgeRenderer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
//import net.minecraft.client.font.TextRenderer;
//import net.minecraft.client.gl.RenderPipelines;
//import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractTextAreaWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.CommonColors;
import org.lwjgl.glfw.GLFW;
//import net.minecraft.client.sound.PositionedSoundInstance;
//import net.minecraft.client.texture.NativeImageBackedTexture;
//import net.minecraft.sound.SoundEvents;
//import net.minecraft.text.StringVisitable;
//import net.minecraft.text.Text;
//import net.minecraft.util.math.ColorHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

//import static com.skellybuilds.servermodmenu.ModMenu.MainNetwork;

public class ModListEntry extends ObjectSelectionList.Entry<ModListEntry> {
    public static final ResourceLocation UNKNOWN_ICON = ResourceLocation.parse("minecraft:textures/misc/unknown_pack.png");
    public static final ResourceLocation DOWNLOAD_ICON = ResourceLocation.parse("servermodmenu:textures/gui/download_button.png");
    public static final ResourceLocation RELOAD_ICON = ResourceLocation.parse("servermodmenu:textures/gui/reload_servers.png");
    public static final ResourceLocation HIDE_ICON = ResourceLocation.parse("servermodmenu:textures/gui/hide_button.png");
    //private static final Identifier MOD_CONFIGURATION_ICON = new Identifier("servermodmenu", "textures/gui/mod_configuration.png");
    private static final ResourceLocation ERROR_ICON = ResourceLocation.parse("minecraft:textures/gui/world_selection.png");
    protected final Minecraft client;
    public IMod mod;
    protected final ModListWidget list;
    protected ResourceLocation iconLocation;
    protected static final int FULL_ICON_SIZE = 32;
    protected static final int COMPACT_ICON_SIZE = 19;
    protected long sinceLastClick;
    private boolean useSMod;
    public SMod smod;
    public String serverName;
    public boolean isFirst = false;
    public int index = 0;
    public Thread serverStat;
    //	public Networking.SocketStatusLoop serverStatR;
    public boolean renderSvnNO = false;
    private int ButtonX;
    private int ButtonY;
    private int ButtonSX = 20;
    private int ButtonSY = 20;
    public boolean netER = false;
    //	private boolean isPrevHB = false;
//	private boolean disableB = false;
//	private boolean isDone = false;
    public boolean moreY = false;
    public boolean downloading;

    public List<EntryButton> buttons;

    public List<EntryButton> getActiveButtons() {
        return Collections.singletonList(Constants.buttonEntries
                .getOrDefault(serverName, (EntryButton) Collections.emptyList()));
    }



    public void testF(EntryButton button){
        button.setActive(false);

        (new Thread(() -> {
            List<SMod> tList = new ArrayList<>(Constants.SMODS.get(serverName).values());

            list.children().forEach((entryM) -> {
                entryM.downloading = true;
            });

            while(!tList.isEmpty()) {
//				ModMenu.LOGGER.info("processing chunk");
                this.processChunk(tList);

                try {
                    Thread.sleep(1250L);
                } catch (InterruptedException var3) {
                    Thread.currentThread().interrupt();
                }
            }

            if(netER){
                button.visible = true;
                button.active = true;
                button.SOUNDMANAGER.play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1.0F));
                return;
            } else {
                while(!Constants.NETWORKING.isDthreadsDone()){

                }
                if (!Constants.isAllDFB) {
                    List<Boolean> isAllt = new ArrayList<>();
                    Constants.buttonEntries.forEach((name, mButton) -> {
                        if (!mButton.visible)
                            isAllt.add(true);
                    });

                    if (isAllt.size() == Constants.buttonEntries.size())
                        Constants.isAllDFB = true;
                }
                button.setVisible(false);
                button.SOUNDMANAGER.play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.0F));
                list.getParent().switchToConfirm();
            }
        })).start();


        if(!Constants.isAllDFB && !netER){
            List<Boolean> isAllt = new ArrayList<>();
            Constants.buttonEntries.forEach((name, mButton) -> {
                if(!mButton.visible)
                    isAllt.add(true);
            });

            if(isAllt.size() == Constants.buttonEntries.size())
                Constants.isAllDFB = true;
        }



    }


    public void setHiddenS(EntryButton button){
        AtomicBoolean isExisting = new AtomicBoolean(false);
        ModMenuConfig.HIDDEN_SERVERS.getValue().forEach((name) -> {
            if(Objects.equals(name, serverName)){
                isExisting.set(true);
            }
        });
        if(isExisting.get()){
            ModMenuConfig.HIDDEN_SERVERS.getValue().remove(serverName);
        } else {
            ModMenuConfig.HIDDEN_SERVERS.getValue().add(serverName);
        }
        ModMenuConfigManager.save();
        list.reloadFilters();
        list.getParent().calcServersSize();
        list.setSelected(null);
        button.SOUNDMANAGER.play(SimpleSoundInstance.forUI(SoundEvents.PORTAL_TRIGGER, 1.0F));
    }

    public boolean downloadA(EntryButton button){

        button.setActive(false);
        Thread t0 = new Thread(() -> {
            //List<SMod> tList = this.list.smods;
            List<SMod> tList = new ArrayList<>(Constants.SMODS.get(serverName).values());

            while(!tList.isEmpty()) {
                Constants.LOG.info("processing chunk");
                this.processChunk(tList);


                try {
                    Thread.sleep(1250L);
                } catch (InterruptedException var3) {
                    Thread.currentThread().interrupt();
                }
            }


            if(netER){
                button.visible = true;
                button.active = true;
                button.SOUNDMANAGER.play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1.0F));
            } else {
                if (!Constants.isAllDFB) {
                    List<Boolean> isAllt = new ArrayList<>();
                    Constants.buttonEntries.forEach((name, mButton) -> {
                        if (!mButton.visible)
                            isAllt.add(true);
                    });

                    if (isAllt.size() == Constants.buttonEntries.size())
                        Constants.isAllDFB = true;
                }
                button.setVisible(false);
                button.SOUNDMANAGER.play(SimpleSoundInstance.forUI(SoundEvents.ARROW_HIT, 1.0F));
            }
        });

        t0.start();
        boolean a = false;
        while(true){
            if(t0.getState() != Thread.State.RUNNABLE){
                a = true;
                break;
            }
        }
        return a;

    }

    private void reloadServer(EntryButton button){
//		MainNetwork.reloadServer(serverName);
        new Thread(() -> {
            Constants.NETWORKING.disconnect(serverName);
//            Constants.ConnectAndDetectPort(serverName, Constants.NETWORKING);
            Constants.LoadServer(serverName, Constants.NETWORKING);
            button.active = true;
            this.list.reloadFilters();
        }).start();
    }

    EntryButton testB; // Download Button
    EntryButton reloadB = new EntryButton(RELOAD_ICON, this::reloadServer); //
    EntryButton hideB = new EntryButton(HIDE_ICON, this::setHiddenS);
    public ModListEntry(SMod smod, ModListWidget list){
        useSMod = true;
        this.smod = smod;
        this.list = list;
        this.client = Minecraft.getInstance();

    }

    public ModListEntry(SMod smod, ModListWidget list, String svn){
        useSMod = true;
        this.smod = smod;
        this.list = list;
        this.client = Minecraft.getInstance();
        this.serverName = svn;
    }

    public ModListEntry(SMod smod, ModListWidget list, String svn, boolean isF, boolean renSvn){
        useSMod = true;
        this.smod = smod;
        this.list = list;
        this.client = Minecraft.getInstance();
        this.serverName = svn;
        this.isFirst = isF;
//		if(isF){
//			if(ModMenu.socketLoops.get(svn) != null){
//				serverStatR = ModMenu.socketLoops.get(svn);
//				serverStat = new Thread(serverStatR);
//				serverStat.start();
//			} else {
//				serverStatR = new Networking.SocketStatusLoop(svn);
//				ModMenu.socketLoops.put(svn, serverStatR);
//				serverStat = new Thread(serverStatR);
//				serverStat.start();
//			}
//		}
        this.renderSvnNO = renSvn;
        if(isF) {
            if (Constants.buttonEntries.get(svn) != null) testB = Constants.buttonEntries.get(svn);
            else {
                testB = new EntryButton(DOWNLOAD_ICON, this::testF, ButtonX, ButtonY, ButtonSX, ButtonSY);
                Constants.buttonEntries.put(svn, testB);
            }
        }
    }

    public ModListEntry(SMod smod, ModListWidget list, String svn, boolean isF, boolean renSvn, boolean moreY){
        useSMod = true;
        this.smod = smod;
        this.list = list;
        this.client = Minecraft.getInstance();
        this.serverName = svn;
        this.moreY = moreY;
        this.isFirst = isF;
//		if(isF){
//			if(ModMenu.socketLoops.get(svn) != null){
//				serverStatR = ModMenu.socketLoops.get(svn);
//				serverStat = new Thread(serverStatR);
//				serverStat.start();
//			} else {
//				serverStatR = new Networking.SocketStatusLoop(svn);
//				ModMenu.socketLoops.put(svn, serverStatR);
//				serverStat = new Thread(ModMenu.socketLoops.get(svn));
//				serverStat.start();
//			}
//		}
        this.renderSvnNO = renSvn;
        if(isF) {
            if (Constants.buttonEntries.get(svn) != null) testB = Constants.buttonEntries.get(svn);
            else {
                testB = new EntryButton(DOWNLOAD_ICON, this::testF, ButtonX, ButtonY, ButtonSX, ButtonSY);
                Constants.buttonEntries.put(svn, testB);
            }
        }
    }

    public ModListEntry(IMod mod, ModListWidget list) {
        this.mod = mod;
        this.list = list;
        this.client = Minecraft.getInstance();
    }

    @Override
    public Component getNarration() {
        if(useSMOD() && smod.meta == null){
            return Component.literal("NO NAME");
        }
        if(useSMOD())return Component.literal(smod.meta.name);
        else return Component.literal(mod.getTranslatedName());
    }


    public void renderContent(GuiGraphics guiGraphics, int i, int i1, boolean b, float v) {
        int iconSize = ModMenuConfig.COMPACT_LIST.getValue() ? COMPACT_ICON_SIZE : FULL_ICON_SIZE;
        int rowWidth = this.getContentWidth();

        int x = getX();
        int y = list.getRowTop(index) + 12;

        if (isFirst) {
            Component svName = Component.literal(serverName);
            FormattedText trimmedName = svName;
            int maxNameWidth = rowWidth - iconSize - 3;
            if (client.font.width(svName) > maxNameWidth) {
                FormattedText ellipsis = FormattedText.of("...");
                trimmedName = FormattedText.composite(client.font.substrByWidth(svName, maxNameWidth - client.font.width(ellipsis)), ellipsis);
            }
            testB.ButtonX = getX() + client.font.width(svName) + 15;
            reloadB.ButtonX = getX() + client.font.width(svName) + 35;
            hideB.ButtonX = getX() + client.font.width(svName) + 55;
            int wa = getY();
//            if (moreY) {
//                wa = wa + 12;
//            }
            testB.ButtonY = wa - 5;
            reloadB.ButtonY = wa - 5;
            hideB.ButtonY = wa - 5;
            guiGraphics.drawString(client.font, Language.getInstance().getVisualOrder(trimmedName), x - 5, wa, 0xFFFFFFFF, false);
            if (!renderSvnNO) {
                testB.render(guiGraphics, i, i1);
            }
            reloadB.render(guiGraphics, i, i1);
            hideB.render(guiGraphics, i, i1);

            if (Constants.NETWORKING.isSocketValid(serverName)) {
                guiGraphics.fill(client.font.width(trimmedName) + 8, wa - 11, client.font.width(trimmedName) + 6, wa - 12, 0xFF00FF00);
            } else {
//                guiGraphics.fill(client.font.width(trimmedName) + 8, wa - 11, client.font.width(trimmedName) + 6, wa - 12, 0xFF808080);
                new Thread(() -> {
                    Constants.LoadServer(serverName, Constants.NETWORKING);
                    list.reloadFilters();
                }).start();
                return;
            }

//            y = (getY() + 4);
//            if (moreY) {
//                y = (getY() + 8);
//            }
        } // else y = (getY() + 6);
//        if (moreY && !isFirst) {
//            y = (getY() + 3);
//        }
        if (renderSvnNO) {
            return;
        }
//        y = (getX()+getXOffset());

//        rowWidth -= getXOffset();
        String modId = smod.getId();
        if ("java".equals(modId)) { // maybe modmenu settings
            DrawingUtil.drawRandomVersionBackgroundS(smod, guiGraphics, x, y, iconSize, iconSize);
        }
        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                this.getIconTexture(),
                x,
                y,
                0.0F,
                0.0F,
                iconSize,
                iconSize,
                iconSize,
                iconSize,
                CommonColors.WHITE
        );
        Component name = Component.literal(smod.meta.name);
        FormattedText trimmedName = name;
        int maxNameWidth = rowWidth - iconSize - 3;
        if (client.font.width(name) > maxNameWidth) {
            FormattedText ellipsis = FormattedText.of("...");
            trimmedName = FormattedText.composite(client.font.substrByWidth(name, maxNameWidth - client.font.width(ellipsis)), ellipsis);
        }
        guiGraphics.drawString(client.font, Language.getInstance().getVisualOrder(trimmedName), x + iconSize + 3, y + 1, 0xFFFFFFFF, false);
//			if (downloading) {
        Long progressObj = Constants.NETWORKING.getDThreadProgress(serverName, smod.id);

        if (progressObj != null) {
            float progress = ((float) progressObj / 100);

            int nameX = x + iconSize + 3;
            int nameWidth = client.font.width(trimmedName);

            int barX = nameX + nameWidth + 8; // 8px padding after the name
            int barY = y + 1;                 // align vertically with text

            int barWidth = rowWidth - (barX - x) - 10; // remaining space
            if (barWidth < 50) barWidth = 50; // minimum bar width safety

            int barHeight = 10;

            // background
            guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);

            // fill
            int fillWidth = (int) (barWidth * progress);
            guiGraphics.fill(barX, barY, barX + fillWidth, barY + barHeight, 0xFF00AA00);

            // text centered over bar
            String txt = (int) (progress * 100) + "%";
            int textX = barX + barWidth / 2 - client.font.width(txt) / 2;
            int textY = barY - 10;
            guiGraphics.drawString(client.font, txt, textX, textY, 0xFFFFFFFF, true);
        }

        if (Constants.NETWORKING.networkErrors.get(Networking.GetIPData(serverName) + smod.id) != null && !Objects.equals(Constants.NETWORKING.networkErrors.get(Networking.GetIPData(serverName) + smod.id), "OK")) {
            int nameX = x + iconSize + 3;
            int nameWidth = client.font.width(trimmedName);

            int xMarkX = nameX + nameWidth + 6; // just after the name
            int xMarkY = y + 1;
            guiGraphics.drawString(client.font, "§c✖", xMarkX, xMarkY, 0xFFFF5555, false);
        }
//			}
//			if(enableDownloads){
//				DrawContext.drawTexture(DOWNLOAD_ICON, x + iconSize - 12, y+1, 0.0F, 0.0F, DLICON_SIZE, DLICON_SIZE, 16, 16);
//			}
        //var updateBadgeXOffset = 0;

//			if(isFirst && !renderSvnNO && !isDone) {
//				//ButtonSY = 19;
//				if (isMouseOverButton(mouseX, mouseY) && !disableB) {
//					isPrevHB = true;
//
//					DrawContext.drawTexture(DOWNLOAD_ICON, ButtonX, ButtonY, 0, 21, ButtonSX, ButtonSY, 32, 64);
//					// disabled - DrawContext.drawTexture(DOWNLOAD_ICON, ButtonX, ButtonY, 0, 42,ButtonSX, ButtonSY, 32, 64);
//				} else {
//					if(disableB) DrawContext.drawTexture(DOWNLOAD_ICON, ButtonX, ButtonY, 0, 42,ButtonSX, ButtonSY, 32, 64);
//					else DrawContext.drawTexture(DOWNLOAD_ICON, ButtonX, ButtonY, 0, 0,ButtonSX, ButtonSY, 32, 64);
//				}
//			}

        final int textureSize = ModMenuConfig.COMPACT_LIST.getValue() ? (int) (256 / (FULL_ICON_SIZE / (double) COMPACT_ICON_SIZE)) : 256;
        if (this.client.options.touchscreen().get() || b) {
//				if(enableDownloads){
//					DrawContext.fill(x, y, x + iconSize, y + iconSize, -1601138544);
//				} else DrawContext.fill(x, y, x + iconSize, y + iconSize, 0xFF0000);
//
//				boolean hoveringIcon = mouseX - x < iconSize;
//				int v = hoveringIcon ? iconSize : 0;
//
//				if (hoveringIcon && !enableDownloads) {
//					if(smod.meta.authors.length > 0){
//					this.list.getParent().setTooltip(this.client.textRenderer.wrapLines(Text.translatable("modmenu.download.error", modId, modId).copy().append("\n\n").append(smod.meta.authors[0]).formatted(Formatting.RED), 175));
//					DrawContext.drawTexture(ERROR_ICON, x, y, 0.0F, (float) v, iconSize, iconSize, textureSize, textureSize);
//					}
//				} else {
//					DrawContext.drawTexture(DOWNLOAD_ICON, x, y, 0.0F, (float) v, iconSize, iconSize, textureSize, textureSize);
//				}

//				if (this.list.getParent().modScreenErrors.containsKey(modId)) {
//					DrawContext.drawTexture(ERROR_ICON, x, y, 96.0F, (float) v, iconSize, iconSize, textureSize, textureSize);
//					if (hoveringIcon) {
//						Throwable e = this.list.getParent().modScreenErrors.get(modId);
//						this.list.getParent().setTooltip(this.client.textRenderer.wrapLines(Text.translatable("modmenu.configure.error", modId, modId).copy().append("\n\n").append(e.toString()).formatted(Formatting.RED), 175));
//					}
//				} else {

            //}
        }
    }

private static final int CHUNK_SIZE = 3; // Number of items to process per chunk
private static final int SLEEP_TIME_MS = 1250; // Delay between chunks in milliseconds

private boolean processChunk(List<SMod> list) {
    int chunkSize = Math.min(CHUNK_SIZE, list.size());

    List<SMod> chunk = new ArrayList<>(list.subList(0, chunkSize));
    list.subList(0, chunkSize).clear(); // Remove processed items from the list

    for (SMod item : chunk) {
        Constants.NETWORKING.requestNDownload(this.serverName, item.getId(), smod.version);
//        while(!Constants.NETWORKING.isDthreadDone(this.serverName, item.getId())){
//
//        }
//        if(Objects.equals(Constants.NETWORKING.networkErrors.get(serverName + smod.id), "ERR")){
//            netER = true;
//        } else {
//            netER = false;
//        }
//        if(!netER) {
//            Constants.idsDLD.add(item.getId());
//        }
    }

    return true;
}

private boolean isMouseOverButton(int mouseX, int mouseY) {
    return mouseX >= ButtonX && mouseX < ButtonX + ButtonSX && mouseY >= ButtonY && mouseY < ButtonY + ButtonSY;
}

@Override
public boolean mouseClicked (MouseButtonEvent button, boolean something){
    list.select(smod.getId(), serverName);
    if(this.useSMOD()){
        if(this.isFirst && !renderSvnNO) {
            testB.handleOnClickEvent((int)button.x(),(int)button.y());
            reloadB.handleOnClickEvent((int)button.x(),(int)button.y());
            hideB.handleOnClickEvent((int)button.x(),(int)button.y());
        } else if(this.isFirst){
            reloadB.handleOnClickEvent((int)button.x(),(int)button.y());
            hideB.handleOnClickEvent((int)button.x(),(int)button.y());
        }

// uNUSED VARIABLE
//				if (ModMenuConfig.QUICK_CONFIGURE.getValue() && this.list.getParent().getModHasConfigScreen().get(this.smod.getId())) {
//					int iconSize = ModMenuConfig.COMPACT_LIST.getValue() ? COMPACT_ICON_SIZE : FULL_ICON_SIZE;
////					if (mouseX - list.getRowLeft() <= iconSize) {
////						Thread downloadT = new Thread(() -> {
////							isBusy = true;
////							Networking.HTTPS.downloadMod(smod.getId(), smod.getVersion(), "./mods");
////							isBusy = false;
////						});
////						downloadT.start();
////					} else if (Util.getMeasuringTimeMs() - this.sinceLastClick < 250) {
////						Thread downloadT = new Thread(() -> {
////							isBusy = true;
////							Networking.HTTPS.downloadMod(smod.getId(), smod.getVersion(), "./mods");
////							isBusy = false;
////						});
////						downloadT.start();
////					}
//				}
    } else {
        if (ModMenuConfig.QUICK_CONFIGURE.getValue() && this.list.getParent().getModHasConfigScreen().get(this.mod.getId())) {
            int iconSize = ModMenuConfig.COMPACT_LIST.getValue() ? COMPACT_ICON_SIZE : FULL_ICON_SIZE;
//					if (mouseX - list.getRowLeft() <= iconSize) {
//						Thread downloadT = new Thread(() -> {
//							isBusy = true;
//							Networking.HTTPS.downloadMod(smod.getId(), smod.getVersion(), "./mods");
//							isBusy = false;
//						});
//						downloadT.start();
//					} else if (Util.getMeasuringTimeMs() - this.sinceLastClick < 250) {
//						Thread downloadT = new Thread(() -> {
//							isBusy = true;
//							Networking.HTTPS.downloadMod(smod.getId(), smod.getVersion(), "./mods");
//							isBusy = false;
//						});
//						downloadT.start();
//					}
        }
    }
    this.sinceLastClick = Util.getEpochMillis();
    return true;
}


//public void openConfig() {
//    Minecraft.getInstance().setScreen(ModMenu.getConfigScreen(mod.getId(), list.getParent()));
//}

public IMod getMod() {
    return mod;
}

public boolean useSMOD() {
    return useSMod;
}

public SMod getSMod() {
    return smod;
}

public ResourceLocation getIconTexture() {
    if (this.iconLocation == null) {
        if(useSMod) {
            ResourceLocation modIcon = null;
            if (smod.meta != null && smod.meta.icon != null) {
                modIcon = TexturesManager.lFromBase64(smod.meta.icon, "/textures/logo"+smod.id);
            } else {
                this.iconLocation = UNKNOWN_ICON;
            }
            if(modIcon == null){
                this.iconLocation =  UNKNOWN_ICON; // icon data
            } else {
                this.iconLocation = modIcon;
            }

        }
    }
    return iconLocation;
}

public int getXOffset() {
    return 0;
}
}
