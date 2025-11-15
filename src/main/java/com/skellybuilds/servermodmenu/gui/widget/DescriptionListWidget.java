package com.skellybuilds.servermodmenu.gui.widget;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import com.skellybuilds.servermodmenu.config.ModMenuConfig;
import com.skellybuilds.servermodmenu.db.SMod;
import com.skellybuilds.servermodmenu.gui.ModsScreen;
import com.skellybuilds.servermodmenu.gui.widget.entries.ModListEntry;
import com.skellybuilds.servermodmenu.util.VersionUtil;
import com.skellybuilds.servermodmenu.util.mod.Mod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.screen.option.CreditsAndAttributionScreen;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.render.*;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.util.*;

public class DescriptionListWidget extends EntryListWidget<DescriptionListWidget.DescriptionEntry> {

	private static final Text HAS_UPDATE_TEXT = Text.translatable("modmenu.hasUpdate");
	private static final Text EXPERIMENTAL_TEXT = Text.translatable("modmenu.experimental").formatted(Formatting.GOLD);
	private static final Text MODRINTH_TEXT = Text.translatable("modmenu.modrinth");
	private static final Text CHILD_HAS_UPDATE_TEXT = Text.translatable("modmenu.childHasUpdate");
	private static final Text LINKS_TEXT = Text.translatable("modmenu.links");
	private static final Text SOURCE_TEXT = Text.translatable("modmenu.source").formatted(Formatting.BLUE).formatted(Formatting.UNDERLINE);
	private static final Text LICENSE_TEXT = Text.translatable("modmenu.license");
	private static final Text VIEW_CREDITS_TEXT = Text.translatable("modmenu.viewCredits").formatted(Formatting.BLUE).formatted(Formatting.UNDERLINE);
	private static final Text CREDITS_TEXT = Text.translatable("modmenu.credits");

	private final ModsScreen parent;
	private final TextRenderer textRenderer;
	private ModListEntry lastSelected = null;

	public DescriptionListWidget(MinecraftClient client, int width,
								 int height,
								 int y,
								 int itemHeight, ModsScreen parent) {
		super(client, width, height, y, itemHeight);
		this.parent = parent;
		this.textRenderer = client.textRenderer;
	}

	@Override
	public DescriptionEntry getSelectedOrNull() {
		return null;
	}

	@Override
	public int getRowWidth() {
		return this.width - 10;
	}

	@Override
	protected int getScrollbarX() {
		return this.width - 6 + this.getX();
	}

	@Override
	public void appendClickableNarrations(NarrationMessageBuilder builder) {
		Mod mod = parent.getSelectedEntry().getMod();
		if (mod == null) return;
		builder.put(NarrationPart.TITLE, mod.getTranslatedName() + " " + mod.getPrefixedVersion());
	}

	private void rebuildUI() {

		ModListEntry selectedEntry = parent.getSelectedEntry();
		if (selectedEntry == null) {
			return;
		}
		if (selectedEntry != lastSelected) {
			lastSelected = selectedEntry;
			clearEntries();
			setScrollY(-Double.MAX_VALUE);
			if (lastSelected != null) {
				DescriptionEntry emptyEntry = new DescriptionEntry(OrderedText.EMPTY);
				int wrapWidth = getRowWidth() - 5;
				String description;

				if(lastSelected.useSMOD()) {
					SMod smod = lastSelected.getSMod();
					if(!lastSelected.renderSvnNO) {

						description = smod.meta.baseDesc;
						if (!description.isEmpty()) {
							for (OrderedText line : textRenderer.wrapLines(Text.literal(description.replaceAll("\n", "\n\n")), wrapWidth)) {
								children().add(new DescriptionEntry(line));
							}
						}

						Map<String, String> links = smod.meta.links.getLinks();
						String sourceLink = smod.meta.links.getLink("sources");


						if ((!links.isEmpty() || sourceLink != null) && !ModMenuConfig.HIDE_MOD_LINKS.getValue()) {
							if (!Objects.equals(links.get("modmenu.links"), "{}")) {
								children().add(emptyEntry);

								for (OrderedText line : textRenderer.wrapLines(LINKS_TEXT, wrapWidth)) {
									children().add(new DescriptionEntry(line));
								}

								if (sourceLink != null) {
									int indent = 8;
									for (OrderedText line : textRenderer.wrapLines(SOURCE_TEXT, wrapWidth - 16)) {
										children().add(new LinkEntry(line, sourceLink, indent));
										indent = 16;
									}
								}


								links.forEach((key, value) -> {
									int indent = 8;
									Gson gson = new Gson();
									JsonObject jsonObject = gson.fromJson(value, JsonObject.class);

										for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
											for (OrderedText line : textRenderer.wrapLines(Text.translatable(entry.getKey()).formatted(Formatting.BLUE).formatted(Formatting.UNDERLINE), wrapWidth - 16)) {
												children().add(new LinkEntry(line, entry.getValue().getAsString(), indent));
												indent = 16;
											}
										}
								});
							}
//						// later
//						Set<String> licenses = mod.getLicense();
//						if (!ModMenuConfig.HIDE_MOD_LICENSE.getValue() && !licenses.isEmpty()) {
//							children().add(emptyEntry);
//
//							for (OrderedText line : textRenderer.wrapLines(LICENSE_TEXT, wrapWidth)) {
//								children().add(new DescriptionEntry(line));
//							}
//
//							for (String license : licenses) {
//								int indent = 8;
//								for (OrderedText line : textRenderer.wrapLines(Text.literal(license), wrapWidth - 16)) {
//									children().add(new DescriptionEntry(line, indent));
//									indent = 16;
//								}
//							}
//						}

							if (!ModMenuConfig.HIDE_MOD_CREDITS.getValue()) {
								if ("minecraft".equals(smod.getId())) {
									children().add(emptyEntry);

									for (OrderedText line : textRenderer.wrapLines(VIEW_CREDITS_TEXT, wrapWidth)) {
										children().add(new MojangCreditsEntry(line));
									}
								} else if (!"java".equals(smod.getId())) {
									String[] mergedArray = new String[smod.meta.authors.length + smod.meta.contributers.length];
									System.arraycopy(smod.meta.authors, 0, mergedArray, 0, smod.meta.authors.length);
									System.arraycopy(smod.meta.contributers, 0, mergedArray, smod.meta.authors.length, smod.meta.contributers.length);

									List<String> credits = Arrays.asList(mergedArray);
									//credits.
									if (!credits.isEmpty()) {
										children().add(emptyEntry);

										for (OrderedText line : textRenderer.wrapLines(CREDITS_TEXT, wrapWidth)) {
											children().add(new DescriptionEntry(line));
										}

										for (String credit : credits) {
											int indent = 8;
											for (OrderedText line : textRenderer.wrapLines(Text.literal(credit), wrapWidth - 16)) {
												children().add(new DescriptionEntry(line, indent));
												indent = 16;
											}
										}
									}
								}
							}
						}
					} else {
						return;
					}
				} else {
					Mod mod = lastSelected.getMod();
					description = mod.getTranslatedDescription();
					if (!description.isEmpty()) {
						for (OrderedText line : textRenderer.wrapLines(Text.literal(description.replaceAll("\n", "\n\n")), wrapWidth)) {
							children().add(new DescriptionEntry(line));
						}
					}


//				if (ModMenuConfig.UPDATE_CHECKER.getValue() && !ModMenuConfig.DISABLE_UPDATE_CHECKER.getValue().contains(mod.getId())) {
//					if (mod.getModrinthData() != null) {
//						children().add(emptyEntry);
//
//						int index = 0;
//						for (OrderedText line : textRenderer.wrapLines(HAS_UPDATE_TEXT, wrapWidth - 11)) {
//							DescriptionEntry entry = new DescriptionEntry(line);
//							if (index == 0) entry.setUpdateTextEntry();
//
//							children().add(entry);
//							index += 1;
//						}
//
//						for (OrderedText line : textRenderer.wrapLines(EXPERIMENTAL_TEXT, wrapWidth - 16)) {
//							children().add(new DescriptionEntry(line, 8));
//						}
//
//						Text updateText = Text.translatable("modmenu.updateText", VersionUtil.stripPrefix(mod.getModrinthData().versionNumber()), MODRINTH_TEXT)
//							.formatted(Formatting.BLUE)
//							.formatted(Formatting.UNDERLINE);
//
//						String versionLink = "https://modrinth.com/project/%s/version/%s".formatted(mod.getModrinthData().projectId(), mod.getModrinthData().versionId());
//
//						for (OrderedText line : textRenderer.wrapLines(updateText, wrapWidth - 16)) {
//							children().add(new LinkEntry(line, versionLink, 8));
//						}
//					}
//					if (mod.getChildHasUpdate()) {
//						children().add(emptyEntry);
//
//						int index = 0;
//						for (OrderedText line : textRenderer.wrapLines(CHILD_HAS_UPDATE_TEXT, wrapWidth - 11)) {
//							DescriptionEntry entry = new DescriptionEntry(line);
//							if (index == 0) entry.setUpdateTextEntry();
//
//							children().add(entry);
//							index += 1;
//						}
//					}
//				}

					Map<String, String> links = mod.getLinks();
					String sourceLink = mod.getSource();
					if ((!links.isEmpty() || sourceLink != null) && !ModMenuConfig.HIDE_MOD_LINKS.getValue()) {
						children().add(emptyEntry);

						for (OrderedText line : textRenderer.wrapLines(LINKS_TEXT, wrapWidth)) {
							children().add(new DescriptionEntry(line));
						}

						if (sourceLink != null) {
							int indent = 8;
							for (OrderedText line : textRenderer.wrapLines(SOURCE_TEXT, wrapWidth - 16)) {

								children().add(new LinkEntry(line, sourceLink, indent));
								indent = 16;
							}
						}

						links.forEach((key, value) -> {
							int indent = 8;
							for (OrderedText line : textRenderer.wrapLines(Text.translatable(key).formatted(Formatting.BLUE).formatted(Formatting.UNDERLINE), wrapWidth - 16)) {
								children().add(new LinkEntry(line, value, indent));
								indent = 16;
							}
						});


					Set<String> licenses = mod.getLicense();
					if (!ModMenuConfig.HIDE_MOD_LICENSE.getValue() && !licenses.isEmpty()) {
						children().add(emptyEntry);

						for (OrderedText line : textRenderer.wrapLines(LICENSE_TEXT, wrapWidth)) {
							children().add(new DescriptionEntry(line));
						}

						for (String license : licenses) {
							int indent = 8;
							for (OrderedText line : textRenderer.wrapLines(Text.literal(license), wrapWidth - 16)) {
								children().add(new DescriptionEntry(line, indent));
								indent = 16;
							}
						}
					}
				}

				if (!ModMenuConfig.HIDE_MOD_CREDITS.getValue()) {
					if ("minecraft".equals(mod.getId())) {
						children().add(emptyEntry);

						for (OrderedText line : textRenderer.wrapLines(VIEW_CREDITS_TEXT, wrapWidth)) {
							children().add(new MojangCreditsEntry(line));
						}
					} else if (!"java".equals(mod.getId())) {
						List<String> credits = mod.getCredits();
						if (!credits.isEmpty()) {
							children().add(emptyEntry);

							for (OrderedText line : textRenderer.wrapLines(CREDITS_TEXT, wrapWidth)) {
								children().add(new DescriptionEntry(line));
							}

							for (String credit : credits) {
								int indent = 8;
								for (OrderedText line : textRenderer.wrapLines(Text.literal(credit), wrapWidth - 16)) {
									children().add(new DescriptionEntry(line, indent));
									indent = 16;
								}
							}
						}
					}
				}
				}
			}
		}

	}

	@Override
	public void renderList(DrawContext drawContext, int mouseX, int mouseY, float delta) {
		if(parent.getSelectedEntry() != lastSelected){
			rebuildUI();
		}
		this.enableScissor(drawContext);
		super.renderList(drawContext, mouseX, mouseY, delta);
		drawContext.disableScissor();
	}

	protected class DescriptionEntry extends ElementListWidget.Entry<DescriptionEntry> {
		protected OrderedText text;
		protected int indent;
		public boolean updateTextEntry = false;

		public DescriptionEntry(OrderedText text, int indent) {
			this.text = text;
			this.indent = indent;
		}

		public DescriptionEntry(OrderedText text) {
			this(text, 0);
		}

		public DescriptionEntry setUpdateTextEntry() {
			this.updateTextEntry = true;
			return this;
		}

		@Override
		public void render(DrawContext DrawContext, int index, int y, int x, int itemWidth, int itemHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
//			if (updateTextEntry) {
//				UpdateAvailableBadge.renderBadge(DrawContext, x + indent, y);
//				x += 11;
//			}
			DrawContext.drawTextWithShadow(textRenderer, text, x + indent, y, 0xFFAAAAAA);
		}

		@Override
		public List<? extends Element> children() {
			return Collections.emptyList();
		}

		@Override
		public List<? extends Selectable> selectableChildren() {
			return Collections.emptyList();
		}
	}

	protected class MojangCreditsEntry extends DescriptionEntry {
		public MojangCreditsEntry(OrderedText text) {
			super(text);
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (isMouseOver(mouseX, mouseY)) {
				client.setScreen(new MinecraftCredits());
			}
			return super.mouseClicked(mouseX, mouseY, button);
		}

		class MinecraftCredits extends CreditsAndAttributionScreen {
			public MinecraftCredits() {
				super(parent);
			}
		}
	}

	protected class LinkEntry extends DescriptionEntry {
		private final String link;

		public LinkEntry(OrderedText text, String link, int indent) {
			super(text, indent);
			this.link = link;
		}

		public LinkEntry(OrderedText text, String link) {
			this(text, link, 0);
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (isMouseOver(mouseX, mouseY)) {
				client.setScreen(new ConfirmLinkScreen((open) -> {
					if (open) {
						Util.getOperatingSystem().open(link);
					}
					client.setScreen(parent);
					parent.modList.setSelected(lastSelected);
				}, link, false));
			}
			return super.mouseClicked(mouseX, mouseY, button);
		}
	}

}
