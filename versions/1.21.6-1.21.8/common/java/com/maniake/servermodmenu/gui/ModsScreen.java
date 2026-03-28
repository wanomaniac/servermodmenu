package com.maniake.servermodmenu.gui;

import com.google.common.base.Joiner;
import com.maniake.servermodmenu.Constants;
import com.maniake.servermodmenu.config.ModMenuConfig;
import com.maniake.servermodmenu.config.ModMenuConfigManager;
import com.maniake.servermodmenu.db.SMod;
import com.maniake.servermodmenu.gui.widget.DescriptionListWidget;
import com.maniake.servermodmenu.gui.widget.ModListWidget;
import com.maniake.servermodmenu.gui.widget.entries.ModListEntry;
import com.maniake.servermodmenu.utils.DrawingUtil;
import com.maniake.servermodmenu.utils.Networking;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.CommonColors;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.stream.Collectors;


public class ModsScreen extends Screen {
	private static final ResourceLocation FILTERS_BUTTON_LOCATION = ResourceLocation.parse(Constants.MOD_ID+":"+"textures/gui/filters_button.png");
	private static final ResourceLocation DOWNLOSD_BUTTON_LOCATION = ResourceLocation.parse(Constants.MOD_ID+":"+"textures/gui/download_button.png");
	private static final ResourceLocation RELOADS_BUTTON_LOCATION = ResourceLocation.parse(Constants.MOD_ID+":"+"textures/gui/reload_servers.png");
	private static final Component OptModT = Component.translatable("modmenu.isOpt");
	private static final Component ReqModT = Component.translatable("modmenu.isReq");
	private static final Component TOGGLE_FILTER_OPTIONS = Component.translatable("modmenu.toggleFilterOptions");
	private static final Component RELOAD_ALLSERV_T = Component.translatable("modmenu.reloadAllServers");
	private static final Component DOWNLOADALLSERV_T = Component.translatable("modmenu.downloadsAll");
	private static final Component CONFIGURE = Component.translatable("modmenu.configure");
	private static final Logger LOGGER = LoggerFactory.getLogger("Mod Menu | ModsScreen");
	private EditBox searchBox;
	private DescriptionListWidget descriptionListWidget;
	private final Screen previousScreen;
	public ModListWidget modList;
	private ModListEntry selected;
	private double scrollPercent = 0;
	private boolean init = false;
	private boolean filterOptionsShown = false;
	private int paneY;
	private static final int RIGHT_PANE_Y = 48;
	private int paneWidth;
	private int rightPaneX;
	private int searchBoxX;
	private int filtersX;
	private int filtersWidth;
	private int searchRowWidth;
	public final Set<String> showModChildren = new HashSet<>();
	public SMod[] ModsA = {};
	public final Map<String, Boolean> modHasConfigScreen = new HashMap<>();
	public final Map<String, Throwable> modScreenErrors = new HashMap<>();
	private Minecraft client = Minecraft.getInstance();
	private ServerList serverList;
	public AtomicInteger amountofvmods = new AtomicInteger();
	Button websiteButton;
    Button issuesButton;
    Button downloadAllSButton;
    Button downloadButton;
    Button showHiddenServers;
    Button sortingButton;
    Button filtersButton;
    Button reloadSButton;
    Component sortingText = ModMenuConfig.SSORTING.getButtonText();
    Component showHBT = ModMenuConfig.SHOWHIDDENSERVERS.getButtonText();

	public ModsScreen(Screen previousScreen) {
		super(Component.translatable("servermodmenu.title"));
		this.previousScreen = previousScreen;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (modList.isMouseOver(mouseX, mouseY)) {
			return this.modList.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
		}

		if (descriptionListWidget.isMouseOver(mouseX, mouseY)) {
			return this.descriptionListWidget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
		}

		return false;
	}


	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && selected == null) { // left click
			int textWidth = font.width(Component.translatable("modmenu.adddamnservers"));
			int x = (this.width - textWidth) / 2;
			int y = this.height / 2;

			boolean clicked =
                    mouseX >= x &&
                            mouseX <= x + textWidth &&
                        mouseY >= y &&
                            mouseY <= y + font.lineHeight;

			if (clicked) {
				new Thread(() -> {
					serverList = new ServerList(client);
					serverList.load();
					Constants.NETWORKING.shutdown();
					Constants.LoadServerListConnections(serverList, Constants.NETWORKING);
					modList.reloadFilters();
				}).start();
				return true;
			}
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

//	private boolean NThreadsFinished(){
//		final boolean[] isAllDone = {true};
//		MainNetwork.networkThreads.forEach((d, a) -> {
//			if(!isAllDone[0]) return;
//			if(a.getState() == Thread.State.RUNNABLE) {
//				isAllDone[0] = false;
//			}
//		});
//
//		return isAllDone[0];
//	}

	public void switchToConfirm(){
			this.client.execute(() -> {
				 this.client.setScreen(new ConfirmationScreen(this, this::resCB, this::backCB, Component.literal("All of your mods have finished downloading! Do you wish to close the game?")
					.withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GREEN)));
			});

	}

	public void switchToConfirmCS(String CSText){
		this.client.execute(() -> {
			this.client.setScreen(new ConfirmationScreen(this, this::resCB, this::backCB, Component.literal(CSText)
                    .withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GREEN)));
		});

	}

//	@Override
//	public void tick() {
//		this.searchBox.tick();
//	}

	private void backCB(ConfirmationScreen bla){
		Minecraft.getInstance().setScreen(bla.prevS);
	}
	private void resCB(ConfirmationScreen bla){
		LOGGER.info("Your game didn't crash, you intentionally (or by mistake, you never know) closed the game.");
        Minecraft.getInstance().stop();
	}

	public void calcServersSize(){
		amountofvmods.set(0);
		modList.children().forEach((entry) -> {
			if(entry.isFirst) {
				AtomicBoolean isHidden = new AtomicBoolean(false);
				if (!entry.renderSvnNO && Constants.SMODS.get(entry.serverName).size() > 1) {
					ModMenuConfig.HIDDEN_SERVERS.getValue().forEach((name) -> {
							if (Objects.equals(name, entry.serverName)) {
								isHidden.set(true);
							}
						});

					if(isHidden.get()){
						if(ModMenuConfig.SHOWHIDDENSERVERS.getValue()) amountofvmods.getAndIncrement();
					} else {
						amountofvmods.getAndIncrement();
					}
				}
			}
		});
	}

	private boolean isValidUrl(String url) {
		if (url == null || url.isEmpty()) return false;
		try {
			new java.net.URI(url.replaceAll("\"", ""));
			return true;
		} catch (Exception e) {
			return false;
		}
	}


	@Override
	protected void init() {

		serverList = new ServerList(client);
		serverList.load();

		paneY = ModMenuConfig.CONFIG_MODE.getValue() ? 48 : 48 + 19;
		paneWidth = this.width / 2 - 8;
		rightPaneX = width - paneWidth;

		int filtersButtonSize = (ModMenuConfig.CONFIG_MODE.getValue() ? 0 : 22);
		int searchWidthMax = paneWidth - 32 - filtersButtonSize;
		int searchBoxWidth = ModMenuConfig.CONFIG_MODE.getValue() ? Math.min(200, searchWidthMax) : searchWidthMax;
		searchBoxX = paneWidth / 2 - searchBoxWidth / 2 - filtersButtonSize / 2;
		this.searchBox = new EditBox(this.font, searchBoxX, 22, searchBoxWidth, 20, this.searchBox, Component.translatable("modmenu.search"));
		this.searchBox.setResponder((string_1) -> this.modList.filter(string_1, false));

		this.modList = new ModListWidget(this.client, this.paneWidth,
			this.height - paneY - 36,
			paneY, ModMenuConfig.COMPACT_LIST.getValue() ? 23 : 36, this.searchBox.getValue(), this.modList, this);
		if(Constants.MODS.isEmpty() && !Constants.SMODS.isEmpty()){
			this.modList.useSMod = true;
		}
		this.modList.setX(0);
		modList.reloadFilters();

		// Downloads all from each server. Yep, may take time!
		downloadAllSButton =
			LegacyTexturedButtonWidget.legacyTexturedBuilder(
					Component.empty(), // or a tooltip text if you want
					button -> {

						final SoundManager[] tempmgr = new SoundManager[1];
						boolean change = false;
						button.active = false;
							Thread finalT = new Thread(() -> {
							AtomicBoolean isERRORD = new AtomicBoolean(false);
							AtomicBoolean isSUCONCE = new AtomicBoolean(false);
							modList.children().forEach((child) -> {
								if(child.isFirst){
									EntryButton mButton = Constants.buttonEntries.get(child.serverName);
									mButton.active = false;
								}
								child.downloading = true;
							});

							modList.children().forEach((child) -> {
								if(child.isFirst) {
									EntryButton mButton = Constants.buttonEntries.get(child.serverName);
									if (!change) {
										tempmgr[0] = mButton.SOUNDMANAGER;
									}
									child.downloadA(mButton);
									boolean isDT0 = false;
									boolean isDTFW = false;
									while (true) {
										if(Constants.NETWORKING.isDthreadDone(child.serverName, child.smod.id)){
											isDT0 = true;
										}
										if(isDT0 && !isDTFW){
											try {
												Thread.sleep(2950);
											} catch (InterruptedException e) {
												LOGGER.error("Interrupted: {}", e.toString());
											}
											if(Constants.NETWORKING.isDthreadDone(child.serverName, child.smod.id)){
												if(Objects.equals(Constants.NETWORKING.networkErrors.get(child.serverName + child.smod.id), "ERR")){
													button.active = true;
													button.visible = true;
													mButton.active = true;
													mButton.visible = true;
													tempmgr[0].play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1.0F));
													isERRORD.set(true);
													break;
												} else {
													isDTFW = true;
													isSUCONCE.set(true);
												}
											} else isDT0 = false;
										} else {
											if(!isDTFW) {
												try {
													Thread.sleep(750);
												} catch (InterruptedException e) {
													LOGGER.error("Interrupted: {}", e.toString());
												}
											} else {
												break;
											}
										}
									}
									if(!isERRORD.get() && !isSUCONCE.get()) {
										mButton.visible = false;
									}
								}
							});

							// if no errors & downloaded a server sucessfully
							if(!isERRORD.get() && isSUCONCE.get()) {
								tempmgr[0].play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F));
								button.visible = false;
								switchToConfirm();
							} // a server downlaoded successfully but another one failed!
							else if(isERRORD.get() && isSUCONCE.get()) {
								tempmgr[0].play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_BIG_FALL, 1.0F));
								//button.visible = false;
								switchToConfirmCS("A server's mod successfully were downloaded but another one failed!!! Do you wish to close the game?");
							} // All servers failed to download!!!
							else if(isERRORD.get() && !isSUCONCE.get()){
								tempmgr[0].play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_DEATH, 1.0F));
							}
						});

						finalT.start();




					}
				)
				.position(paneWidth / 2 + searchBoxWidth / 2 - 10 + 41, 22)
				.size(20, 20)
				.uv(0, 0, 20)   // (u, v, vOffset) like your old constructor
				.texture(DOWNLOSD_BUTTON_LOCATION, 32, 64)
				.build();


		downloadAllSButton.setTooltip(Tooltip.create(DOWNLOADALLSERV_T));

		this.descriptionListWidget = new DescriptionListWidget(
			this.client, this.paneWidth,
			this.height - RIGHT_PANE_Y - 96,
			RIGHT_PANE_Y + 60,
			font.lineHeight + 1, this);
		this.descriptionListWidget.setX(rightPaneX);

		downloadButton =
			LegacyTexturedButtonWidget.legacyTexturedBuilder(
					Component.empty(),
					button -> {
						if (selected == null) return;

						if (!Constants.buttonEntries.get(selected.serverName).active) return;
						if (!Constants.buttonEntries.get(selected.serverName).visible) return;

						final String id = selected.getSMod().getId();

						if (Networking.isModAlreadyPresent(id, selected.getSMod().getVersion())) {
							return;
						}

						button.active = false;

						new Thread(() -> {
							// This runs in a background thread
							selected.downloading = true;
                            Constants.NETWORKING.requestNDownload(selected.serverName, id, selected.smod.getVersion());
							while(!Constants.NETWORKING.isDthreadDone(selected.serverName, id)) {

							}
							boolean networkError = "ERR".equals(Constants.NETWORKING.networkErrors.get(selected.serverName));

							// Update button state on the client thread
							Minecraft.getInstance().execute(() -> {
								if (networkError) {
									button.active = true;
									button.visible = true;
								} else {
									Constants.idsDLD.add(selected.getSMod().id);
									selected.smod.isDownloaded = true;
									button.active = true;

									if (!Constants.isAllDFB) {
										boolean allHidden = Constants.buttonEntries.values().stream()
											.allMatch(b -> !b.visible);
										if (allHidden) Constants.isAllDFB = true;
									}

									// Show confirmation screen if no network error
									if (!networkError) switchToConfirm();
								}
							});
						}).start();

					}
				)
				.position(width - 24, RIGHT_PANE_Y)
				.size(20, 20)
				.uv(0, 0, 20)
				.texture(DOWNLOSD_BUTTON_LOCATION, 32, 64)
				.build();

		int urlButtonWidths = paneWidth / 2 - 2;
		int cappedButtonWidth = Math.min(urlButtonWidths, 200);
		websiteButton =
			Button.builder(
					Component.translatable("modmenu.website"),
					button -> {
						if(selected.useSMOD()) {
							final SMod mod = Objects.requireNonNull(selected).getSMod();
							this.client.setScreen(new ConfirmLinkScreen((bool) -> {
								if (bool) {
									Util.getPlatform().openUri(mod.meta.contact.getHomepage().replaceAll("\"", ""));
								}
								this.client.setScreen(this);
							}, mod.meta.contact.getHomepage().replaceAll("\"", ""), false));
						}
					}
				)
				.pos(
					rightPaneX + (urlButtonWidths / 2) - (cappedButtonWidth / 2),
					RIGHT_PANE_Y + 36
				)
				.size(Math.min(urlButtonWidths, 200), 20)
				.build();


		 issuesButton =
			Button.builder(
					Component.translatable("modmenu.issues"),
					button -> {
						if(selected.useSMOD()){
							if(selected.renderSvnNO){
								return;
							}
							final SMod mod = Objects.requireNonNull(selected).getSMod();
							this.client.setScreen(new ConfirmLinkScreen((bool) -> {
								if (bool) {
									Util.getPlatform().openUri(mod.meta.contact.getIssues().replaceAll("\"", ""));
								}
								this.client.setScreen(this);
							}, mod.meta.contact.getIssues(), false));
						}
					}
				)
				.pos(
					rightPaneX + urlButtonWidths + 4 + (urlButtonWidths / 2) - (cappedButtonWidth / 2),
					RIGHT_PANE_Y + 36
				)
				.size(Math.min(urlButtonWidths, 200), 20)
				.build();

		this.addWidget(this.searchBox);
		filtersButton =
			LegacyTexturedButtonWidget.legacyTexturedBuilder(
					TOGGLE_FILTER_OPTIONS,
					button -> filterOptionsShown = !filterOptionsShown
				)
				.position(
					paneWidth / 2 + searchBoxWidth / 2 - 20 / 2 + 2,
					22
				)
				.size(20, 20)
				.uv(0, 0, 20)
				.texture(FILTERS_BUTTON_LOCATION, 32, 64)
				.build();

		filtersButton.setTooltip(Tooltip.create(TOGGLE_FILTER_OPTIONS));

		reloadSButton =
			LegacyTexturedButtonWidget.legacyTexturedBuilder(
					Component.empty(), // or your narration text if you use one
					button -> {
						button.active = false;
						new Thread(() -> {
							serverList = new ServerList(client);
							serverList.load();
							Constants.NETWORKING.shutdown();
							Constants.LoadServerListConnections(serverList, Constants.NETWORKING);
							this.modList.reloadFilters();
							button.active = true;
						}).start();
//						new Thread(() -> {
//							while(true) {
//								if(MainNetwork.isNthreadsDone()){
//									this.modList.reloadFilters();
//									button.active = true;
//									break;
//								}
//							}
//							return;
//						}).start();
					}
				)
				.position(
					paneWidth / 2 + searchBoxWidth / 2 - 20 / 2 + 22,
					22
				)
				.size(20, 20)
				.uv(0, 0, 20)
				.texture(RELOADS_BUTTON_LOCATION, 32, 64)
				.build();


		reloadSButton.setTooltip(Tooltip.create(RELOAD_ALLSERV_T));
		this.addRenderableWidget(filtersButton);
		this.addRenderableWidget(reloadSButton);
		Component showLibrariesText = ModMenuConfig.SHOW_LIBRARIES.getButtonText();
		int showLibrariesWidth = font.width(showLibrariesText) + 4;
		int sortingWidth = font.width(sortingText);

		filtersWidth = showLibrariesWidth + sortingWidth + 2;
		searchRowWidth = searchBoxX + searchBoxWidth + 22;
		updateFiltersX();

		sortingButton = Button.builder(
				sortingText,
				btn -> {
					ModMenuConfig.SSORTING.cycleValue();
					ModMenuConfigManager.save();
					modList.reloadFilters();
				}
			)
			.pos(21, 45)
			.size(50, 20)
			.build();

		this.addRenderableWidget(
			sortingButton
		);
		showHiddenServers = Button.builder(
				showHBT,
				btn -> {
					ModMenuConfig.SHOWHIDDENSERVERS.toggleValue();
					ModMenuConfigManager.save();
					calcServersSize();
					modList.reloadFilters();
				}
			)
			.pos(77, 45)
			.size(font.width(showHBT) + 6, 20)
			.build();

		this.addRenderableWidget(
			showHiddenServers
		);

		this.addWidget(this.modList);
		this.addRenderableWidget(downloadAllSButton);
		if (!ModMenuConfig.HIDE_CONFIG_BUTTONS.getValue()) {
			this.addRenderableWidget(downloadButton);
		}
		this.addRenderableWidget(websiteButton);
		this.addRenderableWidget(issuesButton);
//		this.addRenderableWidget(this.descriptionListWidget);
		this.addRenderableWidget(
				Button.builder(Component.translatable("gui.done"), button -> client.setScreen(previousScreen))
						.pos((this.width - 150) / 2, this.height - 28)
						.size(150, 20)
                        .createNarration(Supplier::get)
						.build());
		this.searchBox.setFocused(true);

		init = true;
	}

	@Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(keyCode, scanCode, modifiers) || this.searchBox.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int keyCode) {
        return this.searchBox.charTyped(chr, keyCode);
    }
    private boolean wasHidden = false;

	@Override
	public void render(GuiGraphics DrawContext, int mouseX, int mouseY, float delta) {
        sortingText = ModMenuConfig.SSORTING.getButtonText();
        showHBT = ModMenuConfig.SHOWHIDDENSERVERS.getButtonText();
        sortingButton.setMessage(sortingText);
        showHiddenServers.setMessage(showHBT);

        if(selected == null){
            if(!modList.children().isEmpty()){
                updateSelectedEntry(modList.children().getFirst());
            }
        }

        ModListEntry selectedEntry = selected;
        if (selectedEntry != null && !selectedEntry.renderSvnNO) {
            this.descriptionListWidget.render(DrawContext, mouseX, mouseY, delta);
        }
        if (selectedEntry != null && selectedEntry.useSMOD()) {
            if(wasHidden){
                showHiddenServers.visible = true;
                sortingButton.visible = true;
                websiteButton.visible = true;
                issuesButton.visible = true;
                filtersButton.visible = true;
                reloadSButton.visible = true;
                downloadAllSButton.visible = true;
                downloadButton.visible = true;
                wasHidden = false;
            }
            issuesButton.active = isValidUrl(selectedEntry.smod.meta.contact.getIssues());
            websiteButton.active = isValidUrl(selectedEntry.smod.meta.contact.getHomepage());
            downloadButton.active = !Networking.isModAlreadyPresent(selectedEntry.smod.id, selectedEntry.smod.version);

            this.modList.render(DrawContext, mouseX, mouseY, delta);
            this.searchBox.render(DrawContext, mouseX, mouseY, delta);

//		RenderSystem.disableBlend();
            DrawContext.drawCenteredString(this.font, this.title, this.modList.getWidth() / 2, 8, 0xFFFFFFFF);
//		if (!ModMenuConfig.DISABLE_DRAG_AND_DROP.getValue()) {
//			DrawContext.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("modmenu.dropInfo.line1").formatted(Formatting.GRAY), this.width - this.modList.getWidth() / 2, RIGHT_PANE_Y / 2 - client.textRenderer.fontHeight - 1, 16777215);
//			DrawContext.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("modmenu.dropInfo.line2").formatted(Formatting.GRAY), this.width - this.modList.getWidth() / 2, RIGHT_PANE_Y / 2 + 1, 16777215);
//		}
            if (!ModMenuConfig.CONFIG_MODE.getValue()) {
                Component fullModCount = Component.translatable("servermodmenu.showingMods.n", amountofvmods);
                if (!ModMenuConfig.CONFIG_MODE.getValue() && updateFiltersX()) {
                    if (filterOptionsShown) {
                        if (!ModMenuConfig.SHOW_LIBRARIES.getValue() || font.width(fullModCount) <= filtersX - 5) {
                            DrawContext.drawString(font, fullModCount.getVisualOrderText(), searchBoxX, 52, 0xFFFFFFFF, false);
                        } else {
                            if (selected == null) {
                                DrawContext.drawString(font, Component.translatable("modmenu.adddamnservers"), searchBoxX, 46, 0xFFFFFFFF, true);
                            } else {
                                DrawContext.drawString(font, Component.translatable("servermodmenu.showingMods.n", amountofvmods).getVisualOrderText(), searchBoxX, 46, 0xFFFFFFFF, false);
                                DrawContext.drawString(font, computeLibraryCountText().getVisualOrderText(), searchBoxX, 57, 0xFFFFFFFF, false);
                            }
                        }
                    } else {
                        if (!ModMenuConfig.SHOW_LIBRARIES.getValue() || font.width(fullModCount) <= modList.getWidth() - 5) {
                            DrawContext.drawString(font, fullModCount.getVisualOrderText(), searchBoxX, 52, 0xFFFFFF, false);
                        } else {
                            if (selected == null) {
                                DrawContext.drawString(font, Component.translatable("modmenu.adddamnservers"), searchBoxX, 46, 0xFF0000, true);
                            } else {
                                DrawContext.drawString(font, Component.translatable("servermodmenu.showingMods.n", amountofvmods).getVisualOrderText(), searchBoxX, 46, 0xFFFFFFFF, false);
                                DrawContext.drawString(font, Component.translatable("servermodmenu.showingMods.n", amountofvmods).getVisualOrderText(), searchBoxX, 57, 0xFFFFFFFF, false);
                            }
                        }
                    }
                }
            }
            if (selectedEntry != null) {
                if (selectedEntry.useSMOD()) {
                    SMod smod = selectedEntry.getSMod();
                    if (!selectedEntry.renderSvnNO) {
                        int x = rightPaneX;
                        if ("java".equals(smod.getId())) {
                            DrawingUtil.drawRandomVersionBackgroundS(smod, DrawContext, x, RIGHT_PANE_Y, 32, 32);
                        }
                        DrawContext.blit(RenderPipelines.GUI_TEXTURED, this.selected.getIconTexture(), x, RIGHT_PANE_Y, 0.0F, 0.0F, 32, 32, 32, 32, 0xFFFFFFFF);
                        int lineSpacing = font.lineHeight + 1;
                        int imageOffset = 36;
                        Component name = Component.literal(smod.meta.name);
                        FormattedText trimmedName = name;
                        int maxNameWidth = this.width - (x + imageOffset);
                        if (font.width(name) > maxNameWidth) {
                            FormattedText ellipsis = FormattedText.of("...");
                            trimmedName = FormattedText.composite(font.substrByWidth(name, maxNameWidth - font.width(ellipsis)), ellipsis);
                        }
                        DrawContext.drawString(font, Language.getInstance().getVisualOrder(trimmedName), x + imageOffset, RIGHT_PANE_Y + 1, CommonColors.WHITE, false);
                        if (mouseX > x + imageOffset && mouseY > RIGHT_PANE_Y + 1 && mouseY < RIGHT_PANE_Y + 1 + font.lineHeight && mouseX < x + imageOffset + font.width(trimmedName)) {
                            DrawingUtil.drawTooltip(DrawContext, Component.translatable("modmenu.modIdToolTip", smod.getId()), mouseX, mouseY);
                        }
                        DrawContext.drawString(font, smod.getVersion(), x + imageOffset, RIGHT_PANE_Y + 2 + lineSpacing, 0xFFAAAAAA, false);
                        if (smod.isOptional) {
                            DrawContext.drawString(font, OptModT, x + imageOffset, RIGHT_PANE_Y + 10 + lineSpacing, 0xFFAAAAAA, false);
                        } else {
                            DrawContext.drawString(font, ReqModT, x + imageOffset, RIGHT_PANE_Y + 10 + lineSpacing, 0xFFAAAAAA, false);
                        }
                    }
                }

//			super.render(DrawContext, mouseX, mouseY, delta);
            }
        }  else {
            showHiddenServers.visible = false;
            sortingButton.visible = false;
            websiteButton.visible = false;
            issuesButton.visible = false;
            filtersButton.visible = false;
            reloadSButton.visible = false;
            downloadAllSButton.visible = false;
            downloadButton.visible = false;
            wasHidden = true;

            Component txt = Component.translatable("modmenu.adddamnservers");
            int textWidth = font.width(txt);
            int x = (this.width - textWidth) / 2;
            int y = this.height / 2;

            DrawContext.drawString(font, txt, x, y, 0xFFFF0000, true);
            boolean hovering =
                    mouseX >= x &&
                            mouseX <= x + textWidth &&
                            mouseY >= y &&
                            mouseY <= y + font.lineHeight;
            if (hovering) {
                DrawingUtil.drawTooltip(DrawContext, Component.translatable("modmenu.adddamnservers.tooltip"), mouseX, mouseY);
            }
        }

        super.render(DrawContext, mouseX, mouseY, delta);
    }

//	private Text computeModCountText(boolean includeLibs) {
//		int davin = ModMenu.SMODS.values().size();
//
//		//int[] rootMods = formatModCount(davin.stream().map((mmod) -> mmod.).collect(Collectors.toSet()));
//
//		if(davin < 1){
//			return Text.translatable("modmenu.adddamnservers");
//		}
//
//		if (includeLibs && ModMenuConfig.SHOW_LIBRARIES.getValue()) {
//			//int[] rootLibs = formatModCount(ModMenu.ROOT_MODS.values().stream().filter(mod -> !mod.isHidden() && mod.getBadges().contains(Mod.Badge.LIBRARY)).map(Mod::getId).collect(Collectors.toSet()));
//			return TranslationUtil.
//		} else {
//			return TranslationUtil.translateNumeric("modmenu.showingMods", rootMods);
//		}
//	}

	private Component computeLibraryCountText() {
        return Component.literal(null);
	}

	private int[] formatModCount(Set<String> set) {
		int visible = modList.getDisplayedCountFor(set);
		int total = set.size();
		if (visible == total) {
			return new int[]{total};
		}
		return new int[]{visible, total};
	}

	@Override
	public void onClose() {
		this.modList.close();
		this.client.setScreen(this.previousScreen);
	}

	public ModListEntry getSelectedEntry() {
		return selected;
	}

	public void updateSelectedEntry(ModListEntry entry) {
        // null is allowed if entry has to be invalid.
        this.selected = entry;
	}

	public double getScrollPercent() {
		return scrollPercent;
	}

	public void updateScrollPercent(double scrollPercent) {
		this.scrollPercent = scrollPercent;
	}

	public String getSearchInput() {
		return searchBox.getValue();
	}

	private boolean updateFiltersX() {
		if ((filtersWidth + font.width(Component.translatable("servermodmenu.showingMods.n", Constants.SMODS.values().size())) + 20) >= searchRowWidth && ((filtersWidth + font.width(Component.translatable("servermodmenu.showingMods.n", Constants.SMODS.values().size())) + 20) >= searchRowWidth || (filtersWidth + font.width(computeLibraryCountText()) + 20) >= searchRowWidth)) {
			filtersX = paneWidth / 2 - filtersWidth / 2;
			return !filterOptionsShown;
		} else {
			filtersX = searchRowWidth - filtersWidth + 1;
			return true;
		}
	}

	private static boolean isFabricMod(Path mod) {
		try (JarFile jarFile = new JarFile(mod.toFile())) {
			return jarFile.getEntry("fabric.mod.json") != null;
		} catch (IOException e) {
			return false;
		}
	}

	public Map<String, Boolean> getModHasConfigScreen() {
		return modHasConfigScreen;
	}
}
