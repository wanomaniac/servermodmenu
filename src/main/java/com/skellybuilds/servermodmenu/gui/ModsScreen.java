package com.skellybuilds.servermodmenu.gui;

import com.google.common.base.Joiner;

import com.mojang.blaze3d.systems.RenderSystem;
import com.skellybuilds.servermodmenu.config.ModMenuConfig;
import com.skellybuilds.servermodmenu.config.ModMenuConfigManager;
import com.skellybuilds.servermodmenu.db.SMod;
import com.skellybuilds.servermodmenu.gui.widget.ModListWidget;
import com.skellybuilds.servermodmenu.gui.widget.entries.ModListEntry;
import com.skellybuilds.servermodmenu.ModMenu;
import com.skellybuilds.servermodmenu.gui.widget.DescriptionListWidget;
import com.skellybuilds.servermodmenu.util.DrawingUtil;
import com.skellybuilds.servermodmenu.util.Networking;
import com.skellybuilds.servermodmenu.util.TranslationUtil;
import com.skellybuilds.servermodmenu.util.mod.Mod;
import com.skellybuilds.servermodmenu.util.mod.ModBadgeRenderer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.option.ServerList;
import net.minecraft.client.render.*;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import static com.skellybuilds.servermodmenu.ModMenu.MainNetwork;


public class ModsScreen extends Screen {
	private static final Identifier FILTERS_BUTTON_LOCATION = Identifier.of(ModMenu.MOD_ID, "textures/gui/filters_button.png");
	private static final Identifier DOWNLOSD_BUTTON_LOCATION = Identifier.of(ModMenu.MOD_ID, "textures/gui/download_button.png");
	private static final Identifier RELOADS_BUTTON_LOCATION = Identifier.of(ModMenu.MOD_ID, "textures/gui/reload_servers.png");
	private static final Text OptModT = Text.translatable("modmenu.isOpt");
	private static final Text ReqModT = Text.translatable("modmenu.isReq");
	private static final Text TOGGLE_FILTER_OPTIONS = Text.translatable("modmenu.toggleFilterOptions");
	private static final Text RELOAD_ALLSERV_T = Text.translatable("modmenu.reloadAllServers");
	private static final Text DOWNLOADALLSERV_T = Text.translatable("modmenu.downloadsAll");
	private static final Text CONFIGURE = Text.translatable("modmenu.configure");
	private static final Logger LOGGER = LoggerFactory.getLogger("Mod Menu | ModsScreen");
	private TextFieldWidget searchBox;
	private DescriptionListWidget descriptionListWidget;
	private final Screen previousScreen;
	public ModListWidget modList;
	private ModListEntry selected;
	private ModBadgeRenderer modBadgeRenderer;
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
	private MinecraftClient client = MinecraftClient.getInstance();
	private ServerList serverList;
	public AtomicInteger amountofvmods = new AtomicInteger();
	ButtonWidget websiteButton;
	ButtonWidget issuesButton;
	ButtonWidget downloadAllSButton;
	ButtonWidget downloadButton;
	ButtonWidget showHiddenServers;
	ButtonWidget sortingButton;
	ButtonWidget filtersButton;
	ButtonWidget reloadSButton;

	public ModsScreen(Screen previousScreen) {
		super(Text.translatable("servermodmenu.title"));
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
			int textWidth = textRenderer.getWidth(Text.translatable("modmenu.adddamnservers"));
			int x = (this.width - textWidth) / 2;
			int y = this.height / 2;

			boolean clicked =
				mouseX >= x &&
					mouseX <= x + textWidth &&
					mouseY >= y &&
					mouseY <= y + textRenderer.fontHeight;

			if (clicked) {
				serverList = new ServerList(client);
				serverList.loadFile();
				MainNetwork.shutdown();
				ModMenu.LoadServerListConnections(serverList, MainNetwork);
				close();
				MinecraftClient.getInstance().setScreen(new ModsScreen(this.previousScreen));
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
				 this.client.setScreen(new ConfirmationScreen(this, this::resCB, this::backCB, Text.literal("All of your mods have finished downloading! Do you wish to close the game?")
					.formatted(Formatting.ITALIC).formatted(Formatting.GREEN)));
			});

	}

	public void switchToConfirmCS(String CSText){
		this.client.execute(() -> {
			this.client.setScreen(new ConfirmationScreen(this, this::resCB, this::backCB, Text.literal(CSText)
				.formatted(Formatting.ITALIC).formatted(Formatting.GREEN)));
		});

	}

//	@Override
//	public void tick() {
//		this.searchBox.tick();
//	}

	private void backCB(ConfirmationScreen bla){
		MinecraftClient.getInstance().setScreen(bla.prevS);
	}
	private void resCB(ConfirmationScreen bla){
		LOGGER.info("Your game didn't crash, you intentionally (or by mistake, you never know) closed the game. The game will be restarted.");
		try {
			// Get the current java executable
			String javaBin = System.getProperty("java.home") + "/bin/java";
			// Get the path of the running jar
			String jarPath = new java.io.File(
				MinecraftClient.class.getProtectionDomain()
					.getCodeSource()
					.getLocation()
					.toURI()
			).getPath();

			// Build command: java -jar yourJar.jar
			ProcessBuilder builder = new ProcessBuilder(javaBin, "-jar", jarPath);
			builder.start(); // Launch new process
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("Failed to restart the game automatically.");
		}
		MinecraftClient.getInstance().scheduleStop();
	}

	public void calcServersSize(){
		amountofvmods.set(0);
		modList.children().forEach((entry) -> {
			if(entry.isFirst) {
				AtomicBoolean isHidden = new AtomicBoolean(false);
				if (!entry.renderSvnNO && ModMenu.SMODS.get(entry.serverName).size() > 1) {
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
		serverList.loadFile();

		paneY = ModMenuConfig.CONFIG_MODE.getValue() ? 48 : 48 + 19;
		paneWidth = this.width / 2 - 8;
		rightPaneX = width - paneWidth;

		int filtersButtonSize = (ModMenuConfig.CONFIG_MODE.getValue() ? 0 : 22);
		int searchWidthMax = paneWidth - 32 - filtersButtonSize;
		int searchBoxWidth = ModMenuConfig.CONFIG_MODE.getValue() ? Math.min(200, searchWidthMax) : searchWidthMax;
		searchBoxX = paneWidth / 2 - searchBoxWidth / 2 - filtersButtonSize / 2;
		this.searchBox = new TextFieldWidget(this.textRenderer, searchBoxX, 22, searchBoxWidth, 20, this.searchBox, Text.translatable("modmenu.search"));
		this.searchBox.setChangedListener((string_1) -> this.modList.filter(string_1, false));

		for (Mod mod : ModMenu.MODS.values()) {
			String id = mod.getId();
			if (!modHasConfigScreen.containsKey(id)) {
				try {
					Screen configScreen = ModMenu.getConfigScreen(id, this);
					modHasConfigScreen.put(id, configScreen != null);
				} catch (java.lang.NoClassDefFoundError e) {
					LOGGER.warn("The '" + id + "' mod config screen is not available because " + e.getLocalizedMessage() + " is missing.");
					modScreenErrors.put(id, e);
					modHasConfigScreen.put(id, false);
				} catch (Throwable e) {
					LOGGER.error("Error from mod '" + id + "'", e);
					modScreenErrors.put(id, e);
					modHasConfigScreen.put(id, false);
				}
			}
		}
		ModMenu.SMODS.forEach((d, w) -> {
			w.forEach((e, mod) -> {
				String id = mod.id;
				if (!modHasConfigScreen.containsKey(id)) {
					try {
						Screen configScreen = ModMenu.getConfigScreen(id, this);
						modHasConfigScreen.put(id, configScreen != null);
					} catch (java.lang.NoClassDefFoundError wa) {
						LOGGER.warn("The '" + id + "' mod config screen is not available because " + wa.getLocalizedMessage() + " is missing.");
						modScreenErrors.put(id, wa);
						modHasConfigScreen.put(id, false);
					} catch (Throwable wa) {
						LOGGER.error("Error from mod '" + id + "'", wa);
						modScreenErrors.put(id, wa);
						modHasConfigScreen.put(id, false);
					}
				}
			});

			});



		this.modList = new ModListWidget(this.client, this.paneWidth,
			this.height - paneY - 36,
			paneY, ModMenuConfig.COMPACT_LIST.getValue() ? 23 : 36, this.searchBox.getText(), this.modList, this);
		if(ModMenu.MODS.isEmpty() && !ModMenu.SMODS.isEmpty()){
			this.modList.useSMod = true;
		}
		this.modList.setX(0);
		modList.reloadFilters();

		// Downloads all from each server. Yep, may take time!
		downloadAllSButton =
			LegacyTexturedButtonWidget.legacyTexturedBuilder(
					Text.empty(), // or a tooltip text if you want
					button -> {

						final SoundManager[] tempmgr = new SoundManager[1];
						boolean change = false;
						button.active = false;
							Thread finalT = new Thread(() -> {
							AtomicBoolean isERRORD = new AtomicBoolean(false);
							AtomicBoolean isSUCONCE = new AtomicBoolean(false);
							modList.children().forEach((child) -> {
								if(child.isFirst){
									EntryButton mButton = ModMenu.buttonEntries.get(child.serverName);
									mButton.active = false;
								}
							});

							modList.children().forEach((child) -> {
								if(child.isFirst) {
									EntryButton mButton = ModMenu.buttonEntries.get(child.serverName);
									if (!change) {
										tempmgr[0] = mButton.SOUNDMANAGER;
									}
									child.downloadA(mButton);
									boolean isDT0 = false;
									boolean isDTFW = false;
									while (true) {
										if(MainNetwork.isDthreadDone(child.serverName, child.smod.id)){
											isDT0 = true;
										}
										if(isDT0 && !isDTFW){
											try {
												Thread.sleep(2950);
											} catch (InterruptedException e) {
												LOGGER.error("Interrupted: {}", e.toString());
											}
											if(MainNetwork.isDthreadDone(child.serverName, child.smod.id)){
												if(Objects.equals(MainNetwork.networkErrors.get(child.serverName + child.smod.id), "ERR")){
													button.active = true;
													button.visible = true;
													mButton.active = true;
													mButton.visible = true;
													tempmgr[0].play(PositionedSoundInstance.master(SoundEvents.ENTITY_VILLAGER_NO, 1.0F));
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
								tempmgr[0].play(PositionedSoundInstance.master(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F));
								button.visible = false;
								switchToConfirm();
							} // a server downlaoded successfully but another one failed!
							else if(isERRORD.get() && isSUCONCE.get()) {
								tempmgr[0].play(PositionedSoundInstance.master(SoundEvents.ENTITY_PLAYER_BIG_FALL, 1.0F));
								//button.visible = false;
								switchToConfirmCS("A server's mod successfully were downloaded but another one failed!!! Do you wish to close the game?");
							} // All servers failed to download!!!
							else if(isERRORD.get() && !isSUCONCE.get()){
								tempmgr[0].play(PositionedSoundInstance.master(SoundEvents.ENTITY_PLAYER_DEATH, 1.0F));
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


		downloadAllSButton.setTooltip(Tooltip.of(DOWNLOADALLSERV_T));

		this.descriptionListWidget = new DescriptionListWidget(
			this.client, this.paneWidth,
			this.height - RIGHT_PANE_Y - 96,
			RIGHT_PANE_Y + 60,
			textRenderer.fontHeight + 1, this);
		this.descriptionListWidget.setX(rightPaneX);

		downloadButton =
			LegacyTexturedButtonWidget.legacyTexturedBuilder(
					Text.empty(),
					button -> {
						if (selected == null) return;

						if (!ModMenu.buttonEntries.get(selected.serverName).active) return;
						if (!ModMenu.buttonEntries.get(selected.serverName).visible) return;

						final String id = selected.getSMod().getId();

						if (Networking.isModAlreadyPresent(id)) {
							return;
						}

						button.active = false;

						new Thread(() -> {
							// This runs in a background thread
							MainNetwork.requestNDownload(selected.serverName, id);
							while(!MainNetwork.isDthreadDone(selected.serverName, id)) {

							}
							boolean networkError = "ERR".equals(MainNetwork.networkErrors.get(selected.serverName));

							// Update button state on the client thread
							MinecraftClient.getInstance().execute(() -> {
								if (networkError) {
									button.active = true;
									button.visible = true;
								} else {
									ModMenu.idsDLD.add(selected.getSMod().id);
									selected.smod.isDownloaded = true;
									button.active = true;

									if (!ModMenu.isAllDFB) {
										boolean allHidden = ModMenu.buttonEntries.values().stream()
											.allMatch(b -> !b.visible);
										if (allHidden) ModMenu.isAllDFB = true;
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
			ButtonWidget.builder(
					Text.translatable("modmenu.website"),
					button -> {
						if(selected.useSMOD()) {
							final SMod mod = Objects.requireNonNull(selected).getSMod();
							this.client.setScreen(new ConfirmLinkScreen((bool) -> {
								if (bool) {
									Util.getOperatingSystem().open(mod.meta.contact.getHomepage().replaceAll("\"", ""));
								}
								this.client.setScreen(this);
							}, mod.meta.contact.getHomepage().replaceAll("\"", ""), false));
						} else {
							final Mod mod = Objects.requireNonNull(selected).getMod();
							this.client.setScreen(new ConfirmLinkScreen((bool) -> {
								if (bool) {
									Util.getOperatingSystem().open(mod.getWebsite());
								}
								this.client.setScreen(this);
							}, mod.getWebsite(), false));
						}
					}
				)
				.position(
					rightPaneX + (urlButtonWidths / 2) - (cappedButtonWidth / 2),
					RIGHT_PANE_Y + 36
				)
				.size(Math.min(urlButtonWidths, 200), 20)
				.build();


		 issuesButton =
			ButtonWidget.builder(
					Text.translatable("modmenu.issues"),
					button -> {
						if(selected.useSMOD()){
							if(selected.renderSvnNO){
								return;
							}
							final SMod mod = Objects.requireNonNull(selected).getSMod();
							this.client.setScreen(new ConfirmLinkScreen((bool) -> {
								if (bool) {
									Util.getOperatingSystem().open(mod.meta.contact.getIssues().replaceAll("\"", ""));
								}
								this.client.setScreen(this);
							}, mod.meta.contact.getIssues(), false));
						} else {
							final Mod mod = Objects.requireNonNull(selected).getMod();
							this.client.setScreen(new ConfirmLinkScreen((bool) -> {
								if (bool) {
									Util.getOperatingSystem().open(mod.getIssueTracker());
								}
								this.client.setScreen(this);
							}, mod.getIssueTracker(), false));
						}
					}
				)
				.position(
					rightPaneX + urlButtonWidths + 4 + (urlButtonWidths / 2) - (cappedButtonWidth / 2),
					RIGHT_PANE_Y + 36
				)
				.size(Math.min(urlButtonWidths, 200), 20)
				.build();



		this.addSelectableChild(this.searchBox);
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

		filtersButton.setTooltip(Tooltip.of(TOGGLE_FILTER_OPTIONS));

		reloadSButton =
			LegacyTexturedButtonWidget.legacyTexturedBuilder(
					Text.empty(), // or your narration text if you use one
					button -> {
						button.active = false;
						serverList = new ServerList(client);
						serverList.loadFile();
						MainNetwork.shutdown();
						ModMenu.LoadServerListConnections(serverList, MainNetwork);
						close();
						MinecraftClient.getInstance().setScreen(new ModsScreen(this.previousScreen));
						button.active = true;
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


		reloadSButton.setTooltip(Tooltip.of(RELOAD_ALLSERV_T));
		this.addDrawableChild(filtersButton);
		this.addDrawableChild(reloadSButton);
		Text showLibrariesText = ModMenuConfig.SHOW_LIBRARIES.getButtonText();
		Text sortingText = ModMenuConfig.SSORTING.getButtonText();
		int showLibrariesWidth = textRenderer.getWidth(showLibrariesText) + 4;
		int sortingWidth = textRenderer.getWidth(sortingText);
		Text showHBT = ModMenuConfig.SHOWHIDDENSERVERS.getButtonText();
		filtersWidth = showLibrariesWidth + sortingWidth + 2;
		searchRowWidth = searchBoxX + searchBoxWidth + 22;
		updateFiltersX();

		sortingButton = ButtonWidget.builder(
				sortingText,
				btn -> {
					ModMenuConfig.SSORTING.cycleValue();
					ModMenuConfigManager.save();
					modList.reloadFilters();
				}
			)
			.position(21, 45)
			.size(50, 20)
			.build();

		this.addDrawableChild(
			sortingButton
		);
		showHiddenServers = ButtonWidget.builder(
				showHBT,
				btn -> {
					ModMenuConfig.SHOWHIDDENSERVERS.toggleValue();
					ModMenuConfigManager.save();
					calcServersSize();
					modList.reloadFilters();
				}
			)
			.position(77, 45)
			.size(textRenderer.getWidth(showHBT) + 6, 20)
			.build();

		this.addDrawableChild(
			showHiddenServers
		);

		this.addSelectableChild(this.modList);
		this.addDrawableChild(downloadAllSButton);
		if (!ModMenuConfig.HIDE_CONFIG_BUTTONS.getValue()) {
			this.addDrawableChild(downloadButton);
		}
		this.addDrawableChild(websiteButton);
		this.addDrawableChild(issuesButton);
		this.addSelectableChild(this.descriptionListWidget);
		this.addDrawableChild(
				ButtonWidget.builder(ScreenTexts.DONE, button -> client.setScreen(previousScreen))
						.position(215, this.height - 28) //this.width / 2 + 4 - 14
						.size(150, 20)
						.narrationSupplier(Supplier::get)
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

	@Override
	public void render(DrawContext DrawContext, int mouseX, int mouseY, float delta) {
		//if(selected == null && ModsA.length > 0)updateSelectedEntry(new ModListEntry(ModsA[0], modList));
		super.render(DrawContext, mouseX, mouseY, delta);

		if(selected == null){

		}

		ModListEntry selectedEntry = selected;
		if (selectedEntry != null && !selectedEntry.renderSvnNO) {
			this.descriptionListWidget.render(DrawContext, mouseX, mouseY, delta);
		}
		if(selectedEntry != null && selectedEntry.useSMOD()) {
			issuesButton.active = isValidUrl(selectedEntry.smod.meta.contact.getIssues());
			websiteButton.active = isValidUrl(selectedEntry.smod.meta.contact.getHomepage());
			downloadButton.active = !Networking.isModAlreadyPresent(selectedEntry.smod.id);


		this.modList.render(DrawContext, mouseX, mouseY, delta);
		this.searchBox.render(DrawContext, mouseX, mouseY, delta);

//		RenderSystem.disableBlend();
		DrawContext.drawCenteredTextWithShadow(this.textRenderer, this.title, this.modList.getWidth() / 2, 8, 0xFFFFFFFF);
//		if (!ModMenuConfig.DISABLE_DRAG_AND_DROP.getValue()) {
//			DrawContext.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("modmenu.dropInfo.line1").formatted(Formatting.GRAY), this.width - this.modList.getWidth() / 2, RIGHT_PANE_Y / 2 - client.textRenderer.fontHeight - 1, 16777215);
//			DrawContext.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("modmenu.dropInfo.line2").formatted(Formatting.GRAY), this.width - this.modList.getWidth() / 2, RIGHT_PANE_Y / 2 + 1, 16777215);
//		}
		if (!ModMenuConfig.CONFIG_MODE.getValue()) {



			Text fullModCount = Text.translatable("servermodmenu.showingMods.n", amountofvmods);
			if (!ModMenuConfig.CONFIG_MODE.getValue() && updateFiltersX()) {
				if (filterOptionsShown) {
					if (!ModMenuConfig.SHOW_LIBRARIES.getValue() || textRenderer.getWidth(fullModCount) <= filtersX - 5) {
						DrawContext.drawText(textRenderer, fullModCount.asOrderedText(), searchBoxX, 52, 0xFFFFFFFF, false);
					} else {
						if (selected == null) {
							DrawContext.drawText(textRenderer, Text.translatable("modmenu.adddamnservers"), searchBoxX, 46, 0xFFFFFFFF, true);
						} else {
							DrawContext.drawText(textRenderer, Text.translatable("servermodmenu.showingMods.n", amountofvmods).asOrderedText(), searchBoxX, 46, 0xFFFFFFFF, false);
							DrawContext.drawText(textRenderer, computeLibraryCountText().asOrderedText(), searchBoxX, 57, 0xFFFFFFFF, false);
						}
					}
				} else {
					if (!ModMenuConfig.SHOW_LIBRARIES.getValue() || textRenderer.getWidth(fullModCount) <= modList.getWidth() - 5) {
						DrawContext.drawText(textRenderer, fullModCount.asOrderedText(), searchBoxX, 52, 0xFFFFFF, false);
					} else {
						if (selected == null) {
							DrawContext.drawText(textRenderer, Text.translatable("modmenu.adddamnservers"), searchBoxX, 46, 0xFF0000, true);
						} else {
							DrawContext.drawText(textRenderer, Text.translatable("servermodmenu.showingMods.n", amountofvmods).asOrderedText(), searchBoxX, 46, 0xFFFFFFFF, false);
							DrawContext.drawText(textRenderer, Text.translatable("servermodmenu.showingMods.n", amountofvmods).asOrderedText(), searchBoxX, 57, 0xFFFFFFFF, false);
						}
					}
				}
			}
		}
		if (selectedEntry != null) {
			if (selectedEntry.useSMOD()) {
				SMod smod = selectedEntry.getSMod();
				if(!selectedEntry.renderSvnNO) {
					int x = rightPaneX;
					if ("java".equals(smod.getId())) {
						DrawingUtil.drawRandomVersionBackgroundS(smod, DrawContext, x, RIGHT_PANE_Y, 32, 32);
					}
					DrawContext.drawTexture(RenderPipelines.GUI_TEXTURED, this.selected.getIconTexture(), x, RIGHT_PANE_Y, 0.0F, 0.0F, 32, 32, 32, 32, 0xFFFFFFFF);
					int lineSpacing = textRenderer.fontHeight + 1;
					int imageOffset = 36;
					Text name = Text.literal(smod.meta.name);
					StringVisitable trimmedName = name;
					int maxNameWidth = this.width - (x + imageOffset);
					if (textRenderer.getWidth(name) > maxNameWidth) {
						StringVisitable ellipsis = StringVisitable.plain("...");
						trimmedName = StringVisitable.concat(textRenderer.trimToWidth(name, maxNameWidth - textRenderer.getWidth(ellipsis)), ellipsis);
					}
					DrawContext.drawText(textRenderer, Language.getInstance().reorder(trimmedName), x + imageOffset, RIGHT_PANE_Y + 1, Colors.WHITE, false);
					if (mouseX > x + imageOffset && mouseY > RIGHT_PANE_Y + 1 && mouseY < RIGHT_PANE_Y + 1 + textRenderer.fontHeight && mouseX < x + imageOffset + textRenderer.getWidth(trimmedName)) {
						DrawContext.drawTooltip(Text.translatable("modmenu.modIdToolTip", smod.getId()), mouseX, mouseY);
					}
					if (init || modBadgeRenderer == null || modBadgeRenderer.getSMod() != smod) {
						modBadgeRenderer = new ModBadgeRenderer(x + imageOffset + this.client.textRenderer.getWidth(trimmedName) + 2, RIGHT_PANE_Y, width - 28, selectedEntry.smod, this);
						init = false;
					}
					if (!ModMenuConfig.HIDE_BADGES.getValue()) {
						if (!selected.useSMOD()) modBadgeRenderer.draw(DrawContext, mouseX, mouseY);
					}

					DrawContext.drawText(textRenderer, smod.getVersion(), x + imageOffset, RIGHT_PANE_Y + 2 + lineSpacing, 0xFFAAAAAA, false);
					if(smod.isOptional){
						DrawContext.drawText(textRenderer, OptModT, x + imageOffset, RIGHT_PANE_Y + 10 + lineSpacing, 0xFFAAAAAA, false);
					} else {
						DrawContext.drawText(textRenderer, ReqModT, x + imageOffset, RIGHT_PANE_Y + 10 + lineSpacing, 0xFFAAAAAA, false);
					}


					String authors;
					List<String> names = Arrays.asList(smod.meta.authors);

					if (!names.isEmpty()) {
						if (names.size() > 1) {
							authors = Joiner.on(", ").join(names);
						} else {
							authors = names.get(0);
						}
						DrawingUtil.drawWrappedString(DrawContext, I18n.translate("modmenu.authorPrefix", authors), x + imageOffset, RIGHT_PANE_Y + 2 + lineSpacing * 2, paneWidth - imageOffset - 4, 1, 0xFFAAAAAA);
					}
				}
			} else {
				Mod mod = selectedEntry.getMod();
				int x = rightPaneX;
				if ("java".equals(mod.getId())) {
					DrawingUtil.drawRandomVersionBackground(mod, DrawContext, x, RIGHT_PANE_Y, 32, 32);
				}
				DrawContext.drawTexture(RenderPipelines.GUI_TEXTURED, this.selected.getIconTexture(), x, RIGHT_PANE_Y, 0.0F, 0.0F, 32, 32, 32, 32, 0xFFFFFFFF);
				int lineSpacing = textRenderer.fontHeight + 1;
				int imageOffset = 36;
				Text name = Text.literal(mod.getTranslatedName());
				StringVisitable trimmedName = name;
				int maxNameWidth = this.width - (x + imageOffset);
				if (textRenderer.getWidth(name) > maxNameWidth) {
					StringVisitable ellipsis = StringVisitable.plain("...");
					trimmedName = StringVisitable.concat(textRenderer.trimToWidth(name, maxNameWidth - textRenderer.getWidth(ellipsis)), ellipsis);
				}
				DrawContext.drawText(textRenderer, Language.getInstance().reorder(trimmedName), x + imageOffset, RIGHT_PANE_Y + 1, 0xFFFFFFFF, false);
				if (mouseX > x + imageOffset && mouseY > RIGHT_PANE_Y + 1 && mouseY < RIGHT_PANE_Y + 1 + textRenderer.fontHeight && mouseX < x + imageOffset + textRenderer.getWidth(trimmedName)) {
					DrawContext.drawTooltip(Text.translatable("modmenu.modIdToolTip", mod.getId()), mouseX, mouseY);
				}
				if (init || modBadgeRenderer == null || modBadgeRenderer.getMod() != mod) {
					modBadgeRenderer = new ModBadgeRenderer(x + imageOffset + this.client.textRenderer.getWidth(trimmedName) + 2, RIGHT_PANE_Y, width - 28, selectedEntry.mod, this);
					init = false;
				}
				if (!ModMenuConfig.HIDE_BADGES.getValue()) {
					modBadgeRenderer.draw(DrawContext, mouseX, mouseY);
				}
				if (mod.isReal()) {
					DrawContext.drawText(textRenderer, mod.getPrefixedVersion(), x + imageOffset, RIGHT_PANE_Y + 2 + lineSpacing, 0xFFAAAAAA, false);
				}
				String authors;
				List<String> names = mod.getAuthors();

				if (!names.isEmpty()) {
					if (names.size() > 1) {
						authors = Joiner.on(", ").join(names);
					} else {
						authors = names.get(0);
					}
					DrawingUtil.drawWrappedString(DrawContext, I18n.translate("modmenu.authorPrefix", authors), x + imageOffset, RIGHT_PANE_Y + 2 + lineSpacing * 2, paneWidth - imageOffset - 4, 1, 0xFFAAAAAA);
				}
			}
		}

//			super.render(DrawContext, mouseX, mouseY, delta);
		} else {
			showHiddenServers.visible = false;
			sortingButton.visible = false;
			websiteButton.visible = false;
			issuesButton.visible = false;
			filtersButton.visible = false;
			reloadSButton.visible = false;
			downloadAllSButton.visible = false;
			downloadButton.visible = false;

			Text txt = Text.translatable("modmenu.adddamnservers");
			int textWidth = textRenderer.getWidth(txt);
			int x = (this.width - textWidth) / 2;
			int y = this.height / 2;

			DrawContext.drawText(textRenderer, txt, x, y, 0xFFFF0000, true);
			boolean hovering =
				mouseX >= x &&
					mouseX <= x + textWidth &&
					mouseY >= y &&
					mouseY <= y + textRenderer.fontHeight;
			if (hovering) {
				DrawContext.drawTooltip(Text.translatable("modmenu.adddamnservers.tooltip"), mouseX, mouseY);
			}

		}
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

	private Text computeLibraryCountText() {
		if (ModMenuConfig.SHOW_LIBRARIES.getValue()) {
			int[] rootLibs = formatModCount(ModMenu.ROOT_MODS.values().stream().filter(mod -> !mod.isHidden() && mod.getBadges().contains(Mod.Badge.LIBRARY)).map(Mod::getId).collect(Collectors.toSet()));

			if(rootLibs.length < 1){
				return Text.translatable("modmenu.adddamnservers");
			}
			return TranslationUtil.translateNumeric("modmenu.showingLibraries", rootLibs);
		} else {
			return Text.literal(null);
		}
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
	public void close() {
		this.modList.close();
		this.client.setScreen(this.previousScreen);
	}

	public ModListEntry getSelectedEntry() {
		return selected;
	}

	public void updateSelectedEntry(ModListEntry entry) {
		if (entry != null) {
			this.selected = entry;
		}

	}

	public double getScrollPercent() {
		return scrollPercent;
	}

	public void updateScrollPercent(double scrollPercent) {
		this.scrollPercent = scrollPercent;
	}

	public String getSearchInput() {
		return searchBox.getText();
	}

	private boolean updateFiltersX() {
		if ((filtersWidth + textRenderer.getWidth(Text.translatable("servermodmenu.showingMods.n", ModMenu.SMODS.values().size())) + 20) >= searchRowWidth && ((filtersWidth + textRenderer.getWidth(Text.translatable("servermodmenu.showingMods.n", ModMenu.SMODS.values().size())) + 20) >= searchRowWidth || (filtersWidth + textRenderer.getWidth(computeLibraryCountText()) + 20) >= searchRowWidth)) {
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
