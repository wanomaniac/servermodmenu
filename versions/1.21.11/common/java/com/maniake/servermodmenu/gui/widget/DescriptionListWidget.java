package com.maniake.servermodmenu.gui.widget;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.maniake.servermodmenu.config.ModMenuConfig;
import com.maniake.servermodmenu.db.SMod;
import com.maniake.servermodmenu.gui.ModsScreen;
import com.maniake.servermodmenu.gui.widget.entries.ModListEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.CreditsAndAttributionScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

import java.util.*;

public class DescriptionListWidget extends AbstractSelectionList<DescriptionListWidget.DescriptionEntry> {
	private static final Component HAS_UPDATE_TEXT = Component.translatable("modmenu.hasUpdate");
	private static final Component EXPERIMENTAL_TEXT = Component.translatable("modmenu.experimental").withStyle(ChatFormatting.GOLD);
	private static final Component MODRINTH_TEXT = Component.translatable("modmenu.modrinth");
	private static final Component CHILD_HAS_UPDATE_TEXT = Component.translatable("modmenu.childHasUpdate");
	private static final Component LINKS_TEXT = Component.translatable("modmenu.links");
	private static final Component SOURCE_TEXT = Component.translatable("modmenu.source").withStyle(ChatFormatting.BLUE).withStyle(ChatFormatting.UNDERLINE);
	private static final Component LICENSE_TEXT = Component.translatable("modmenu.license");
	private static final Component VIEW_CREDITS_TEXT = Component.translatable("modmenu.viewCredits").withStyle(ChatFormatting.BLUE).withStyle(ChatFormatting.UNDERLINE);
	private static final Component CREDITS_TEXT = Component.translatable("modmenu.credits");

	private final ModsScreen parent;
	private final Font textRenderer;
	private ModListEntry lastSelected = null;

	public DescriptionListWidget(Minecraft client, int width,
                                 int height,
                                 int y,
                                 int itemHeight, ModsScreen parent) {
		super(client, width, height, y, itemHeight);
		this.parent = parent;
		this.textRenderer = client.font;
	}

	@Override
	public DescriptionEntry getSelected() {
		return null;
	}

	@Override
	public int getRowWidth() {
		return this.width - 10;
	}

//	@Override
//	protected int getScrollBarX() {
//		return this.width - 6 + this.getX();
//	}

	@Override
	public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
		SMod mod = parent.getSelectedEntry().getSMod();
		if (mod == null) return;
        narrationElementOutput.add(NarratedElementType.TITLE, mod.getMeta().name + " " + mod.version);
	}

	private void rebuildUI() {
		ModListEntry selectedEntry = parent.getSelectedEntry();
		if (selectedEntry == null) {
			return;
		}
		if (selectedEntry != lastSelected) {
            lastSelected = selectedEntry;
            clearEntries();
            setScrollAmount(-Double.MAX_VALUE);
            if (lastSelected != null) {
                DescriptionEntry emptyEntry = new DescriptionEntry(FormattedCharSequence.EMPTY);
                int wrapWidth = getRowWidth() - 5;
                String description;
                if (lastSelected.useSMOD()) {
                    SMod smod = lastSelected.getSMod();
                    if (!lastSelected.renderSvnNO) {

                        description = smod.meta.baseDesc;
                        if (!description.isEmpty()) {
                            for (FormattedCharSequence line : textRenderer.split(Component.literal(description.replaceAll("\n", "\n\n")), wrapWidth)) {
                                addEntry(new DescriptionEntry(line));
                            }
                        }

                        Map<String, String> links = smod.meta.links.getLinks();
                        String sourceLink = smod.meta.links.getLink("sources");


                        if ((!links.isEmpty() || sourceLink != null) && !ModMenuConfig.HIDE_MOD_LINKS.getValue()) {
                            if (!Objects.equals(links.get("modmenu.links"), "{}")) {
                                addEntry(emptyEntry);

                                for (FormattedCharSequence line : textRenderer.split(LINKS_TEXT, wrapWidth)) {
                                    addEntry(new DescriptionEntry(line));
                                }

                                if (sourceLink != null) {
                                    int indent = 8;
                                    for (FormattedCharSequence line : textRenderer.split(SOURCE_TEXT, wrapWidth - 16)) {
                                        addEntry(new LinkEntry(line, sourceLink, indent));
                                        indent = 16;
                                    }
                                }


                                links.forEach((key, value) -> {
                                    int indent = 8;
                                    Gson gson = new Gson();
                                    JsonObject jsonObject = gson.fromJson(value, JsonObject.class);

                                    for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                                        for (FormattedCharSequence line : textRenderer.split(Component.translatable(entry.getKey()).withStyle(ChatFormatting.BLUE).withStyle(ChatFormatting.UNDERLINE), wrapWidth - 16)) {
                                            addEntry(new LinkEntry(line, entry.getValue().getAsString(), indent));
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
                                    addEntry(emptyEntry);

                                    for (FormattedCharSequence line : textRenderer.split(VIEW_CREDITS_TEXT, wrapWidth)) {
                                        addEntry(new MojangCreditsEntry(line));
                                    }
                                } else if (!"java".equals(smod.getId())) {
                                    String[] mergedArray = new String[smod.meta.authors.length + smod.meta.contributers.length];
                                    System.arraycopy(smod.meta.authors, 0, mergedArray, 0, smod.meta.authors.length);
                                    System.arraycopy(smod.meta.contributers, 0, mergedArray, smod.meta.authors.length, smod.meta.contributers.length);

                                    List<String> credits = Arrays.asList(mergedArray);
                                    //credits.
                                    if (!credits.isEmpty()) {
                                        addEntry(emptyEntry);

                                        for (FormattedCharSequence line : textRenderer.split(CREDITS_TEXT, wrapWidth)) {
                                            addEntry(new DescriptionEntry(line));
                                        }

                                        for (String credit : credits) {
                                            int indent = 8;
                                            for (FormattedCharSequence line : textRenderer.split(Component.literal(credit), wrapWidth - 16)) {
                                                addEntry(new DescriptionEntry(line, indent));
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
                }
            }
        }
	}

	@Override
	public void renderListItems(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
		if(parent.getSelectedEntry() != lastSelected){
			rebuildUI();
		}
		this.enableScissor(drawContext);
		super.renderListItems(drawContext, mouseX, mouseY, delta);
		drawContext.disableScissor();
	}


    protected class DescriptionEntry extends AbstractSelectionList.Entry<DescriptionEntry> {
		protected FormattedCharSequence text;
		protected int indent;
		public boolean updateTextEntry = false;

		public DescriptionEntry(FormattedCharSequence text, int indent) {
			this.text = text;
			this.indent = indent;
		}

		public DescriptionEntry(FormattedCharSequence text) {
			this(text, 0);
		}

		public DescriptionEntry setUpdateTextEntry() {
			this.updateTextEntry = true;
			return this;
		}

		@Override
		public void renderContent(GuiGraphics guiGraphics, int i, int i1, boolean b, float v) {
			guiGraphics.drawString(textRenderer, text, getX() + indent, getY(), 0xFFAAAAAA);
		}

//		@Override
//		public List<? extends Element> children() {
//			return Collections.emptyList();
//		}
//
//		@Override
//		public List<? extends Selectable> selectableChildren() {
//			return Collections.emptyList();
//		}
    }

	protected class MojangCreditsEntry extends DescriptionEntry {
		public MojangCreditsEntry(FormattedCharSequence text) {
			super(text);
		}

		@Override
		public boolean mouseClicked(MouseButtonEvent event, boolean something) {
			if (isMouseOver(event.x(), event.y())) {
				minecraft.setScreen(new MinecraftCredits());
			}
			return super.mouseClicked(event, something);
		}

		class MinecraftCredits extends CreditsAndAttributionScreen {
			public MinecraftCredits() {
				super(parent);
			}
		}
	}

	protected class LinkEntry extends DescriptionEntry {
		private final String link;

		public LinkEntry(FormattedCharSequence text, String link, int indent) {
			super(text, indent);
			this.link = link;
		}

		public LinkEntry(FormattedCharSequence text, String link) {
			this(text, link, 0);
		}

		@Override
		public boolean mouseClicked(MouseButtonEvent event, boolean something) {
			if (isMouseOver(event.x(), event.y())) {
				Minecraft.getInstance().setScreen(new ConfirmLinkScreen((open) -> {
					if (open) {
						Util.getPlatform().openUri(link);
					}
                    Minecraft.getInstance().setScreen(parent);
					parent.modList.setSelected(lastSelected);
				}, link, false));
			}
			return super.mouseClicked(event, something);
		}
	}

}
