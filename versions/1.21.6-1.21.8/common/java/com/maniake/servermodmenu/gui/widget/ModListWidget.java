package com.maniake.servermodmenu.gui.widget;

import com.maniake.servermodmenu.Constants;
import com.maniake.servermodmenu.config.ModMenuConfig;
import com.maniake.servermodmenu.db.SMod;
import com.maniake.servermodmenu.gui.ModsScreen;
import com.maniake.servermodmenu.gui.widget.entries.IndependentEntry;
import com.maniake.servermodmenu.gui.widget.entries.ModListEntry;
import com.maniake.servermodmenu.interfaces.IMod;
//import com.maniake.servermodmenu.util.mod.Mod;
import com.maniake.servermodmenu.utils.ModSearch;
//import com.maniake.servermodmenu.util.mod.fabric.FabricIconHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import org.lwjgl.glfw.GLFW;
import net.minecraft.util.Mth;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ModListWidget extends ObjectSelectionList<ModListEntry> implements AutoCloseable {
	public static final boolean DEBUG = Boolean.getBoolean("modmenu.debug");
	private final ModsScreen parent;
	public List<IMod> mods = null;
	public List<SMod> smods = null;
	public boolean useSMod = false;
	private final Set<IMod> addedMods = new HashSet<>();
	// <Server Name, ServerMod> - for the highlighted text string to top
	private final Map<String, SMod> SaddedMods = new HashMap<>();
	private String selectedModId = null;
	private boolean scrolling;
//	private final FabricIconHandler iconHandler = new FabricIconHandler();
	private boolean isInit = false;
	private double scrollAm;
	private int origT;

	public ModListWidget(Minecraft client, int width, int height, int y, int entryHeight, String searchTerm, ModListWidget list, ModsScreen parent) {
		super(client, width, height, y, entryHeight);

		this.parent = parent;
		if(list != null) {
			if (list.useSMod) {
			this.useSMod = true;
			this.smods = list.smods;
			}
				else this.mods = list.mods;


		}
//		this.filter(searchTerm, false);
//		setScrollAmount(parent.getScrollPercent() * Math.max(0, this.getMaxPosition() - (this.getBottom() - this.getTop() - 4)));
	}


	@Override
	public boolean isFocused() {
		return parent.getFocused() == this;
	}

	public void select(String id, String server) {
		children().forEach((entryM) -> {
			if(Objects.equals(entryM.smod.getId(), id)){
				if(entryM.useSMOD()){
					if(Objects.equals(entryM.serverName, server)){
						this.setSelected(entryM);
						if(entryM.renderSvnNO) return;
						this.minecraft.getNarrator().saySystemNow(Component.translatable("narrator.select", entryM.smod.meta.name));
					}
				} else {
					if(Objects.equals(entryM.serverName, server)){
					this.setSelected(entryM);
					this.minecraft.getNarrator().saySystemNow(Component.translatable("narrator.select", entryM.mod.getTranslatedName()));
				}

				}
			}
		});

	}

	@Override
	public void setSelected(ModListEntry entry) {
		if(entry == null)return;
		super.setSelected(entry);
		if(entry.useSMOD()){
			selectedModId = entry.getSMod().getId();
			parent.updateSelectedEntry(getSelected());
		} else {
			selectedModId = entry.getMod().getId();
			parent.updateSelectedEntry(getSelected());
		}
	}

//	@Override
	protected boolean isSelectedEntry(int index) {
		ModListEntry selected = getSelected();
		//assert selected != null; why the fuck am i asserting?
		if(selected != null) {
			if (selected.useSMOD()) {
				if(!Objects.equals(selected.getSMod().server, children().get(index).getSMod().server))return false;
				return selected.getSMod().getId().equals(children().get(index).getSMod().getId());
			} else {
				if(!Objects.equals(selected.serverName, children().get(index).serverName)) return false;
				return selected.getMod().getId().equals(children().get(index).getMod().getId());
			}
		} else return false;

//		return false;
		}

	@Override
	public int addEntry(ModListEntry entry) {
		if(entry.useSMOD()){
			if (SaddedMods.get(entry.serverName) == entry.smod) {
				return 0;
			}
			SaddedMods.put(entry.serverName, entry.smod);
			int i = super.addEntry(entry);
			if (entry.getSMod().getId().equals(selectedModId)) {
				setSelected(entry);
			}
			return i;
		} else {
			if (addedMods.contains(entry.mod)) {
				return 0;
			}
			addedMods.add(entry.mod);
			int i = super.addEntry(entry);
			if (entry.getMod().getId().equals(selectedModId)) {
				setSelected(entry);
			}
			return i;
		}
	}

	@Override
	protected boolean removeEntry(ModListEntry entry) {
		if(entry.useSMOD()){
			SaddedMods.remove(entry.serverName, entry.smod);
		} else {
			addedMods.remove(entry.mod);
		}
        super.removeEntry(entry);

        return true;
    }

	protected void removeAllEntries(){
		children().forEach(super::removeEntry);
	}

//	@Override
//	protected ModListEntry remove(int index) {
//		if(getEntry(index).useSMOD()) addedMods.remove(getEntry(index).smod);
//		else addedMods.remove(getEntry(index).mod);
//
//		return super.remove(index);
//	}

	public void reloadFilters() {
		isInit = false;
		filter(parent.getSearchInput(), true, false);
	}


	public void filter(String searchTerm, boolean refresh) {
		isInit = false;
		filter(searchTerm, refresh, true);
	}


	private void filter(String searchTerm, boolean refresh, boolean search) {
		this.clearEntries();
		SaddedMods.clear();
		addedMods.clear();
		this.removeAllEntries();

//		if (useSMod) {


			Map<String, SMod> modsMA;

			if(Constants.SMODSA.isEmpty()) {
				modsMA = new HashMap<>();

                Constants.SMODS.forEach((svn, mod) -> {
					for (Map.Entry<String, SMod> entry : mod.entrySet()) {
						String id = entry.getKey();
						SMod mods2 = entry.getValue();
						modsMA.put(id, mods2);
					}
				});
			} else modsMA = Constants.SMODSA;

			AtomicBoolean addMoreY = new AtomicBoolean(false);
            Constants.SMODS.forEach((serverName, modsM) -> {

				Collection<SMod> mods = new HashSet<>(modsM.values());
                if(!mods.isEmpty()) {
                    this.smods = null;

//				if (this.smods == null || refresh) {
                    this.smods = new ArrayList<>();
                    this.smods.addAll(mods);

                    this.smods.sort(ModMenuConfig.SSORTING.getValue().getComparator());
//				}

                    List<SMod> matched = ModSearch.searchS(parent, searchTerm, this.smods);

                    AtomicBoolean isHidden = new AtomicBoolean(false);
                    boolean isF = true;
                    for (SMod mod : matched) {
                        ModMenuConfig.HIDDEN_SERVERS.getValue().forEach((name) -> {
                            if (Objects.equals(name, serverName)) {
                                if (!ModMenuConfig.SHOWHIDDENSERVERS.getValue()) {
                                    isHidden.set(true);
                                }
                            }
                        });


                        if (matched.isEmpty()) {
                            if (isHidden.get() && ModMenuConfig.SHOWHIDDENSERVERS.getValue() || !isHidden.get()) {
                                this.addEntry(new IndependentEntry(new SMod("d", "d", null, false, false), this, serverName, true, true, addMoreY.get()));
                            }
                        }


                        if (isHidden.get() && ModMenuConfig.SHOWHIDDENSERVERS.getValue() || !isHidden.get()) {
                            this.addEntry(new IndependentEntry(mod, this, serverName, isF, false, addMoreY.get()));

                        }

                        if (isHidden.get() && ModMenuConfig.SHOWHIDDENSERVERS.getValue() || !isHidden.get()) {
                            isF = false;
                        }
                    }

                    addMoreY.set(true);


                    if (children().size() > 1) {
                        setSelected(children().getFirst());
                    } else {
                        setSelected(null);
                    }
                }
			});

//			if(children().size() > 2){
//				if(addMoreY.get()){
//					int wa2 = children().stream().filter((ra) -> ra.isFirst).toList().size()-1;
//
//					this.top = this.top - 6 * wa2;
//				}
//			}

//			if (getScrollAmount() > Math.max(0, this.getMaxPosition() - (this.bottom - this.top - 4))) {
//				setScrollAmount(Math.max(0, this.getMaxPosition() - (this.bottom - this.top - 4)));
//			}
			parent.calcServersSize();

			isInit = true;

//		}
	}


	@Override
	public void renderListItems(GuiGraphics DrawContext, int mouseX, int mouseY, float delta) {
		int entryCount = this.children().size();

if(isInit) {
	for (int index = 0; index < entryCount; ++index) {

		if(this.children().isEmpty() || index > this.children().size()){
			return; // isInit and entry count can be unreliable when reloading servers.
			// This will reduce the likely chances of crashing on reload render
		}

		ModListEntry entry = this.children().get(index);
        entry.index = index;
		int entryTop = this.getRowTop(index) + 12;
		int entryHeight = this.itemHeight - 4;
		int rowWidth = this.getRowWidth();
		int entryLeft = this.getRowLeft();
		if (this.isSelectedEntry(index) && !entry.renderSvnNO) {
			int entryContentLeft = entryLeft + entry.getXOffset() - 2;
			int entryContentWidth = rowWidth - entry.getXOffset() + 4;
			this.drawSelectionHighlight(
				DrawContext,
				entryContentLeft,
				entryTop,
				entryContentWidth,
				entryHeight,
				this.isFocused() ? CommonColors.WHITE : CommonColors.GRAY, CommonColors.BLACK
			);
		}

		entryLeft = this.getRowLeft();
		entry.render(DrawContext, index, entryTop, entryLeft, rowWidth, entryHeight, mouseX, mouseY, this.isMouseOver(mouseX, mouseY) && Objects.equals(this.getEntryAtPos(mouseX, mouseY), entry), delta);
	}
}
	}


//	public void ensureVisible(ModListEntry entry) {
//		super.(entry);
//	}


	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (getSelected() != null) {
            return getSelected().keyPressed(keyCode, scanCode, modifiers);
        }
		return false;
	}

	public final ModListEntry getEntryAtPos(double x, double y) {
		int int_5 = Mth.floor(y - (double) this.getY()) + (int) this.scrollAmount() - 4;
		int index = int_5 / this.contentHeight();
		return x < (double) this.scrollBarX() && x >= (double) getRowLeft() && x <= (double) (getRowLeft() + getRowWidth()) && index >= 0 && int_5 >= 0 && index < this.children().size() ? this.children().get(index) : null;
	}

//	@Override
//	protected int getScrollbarPositionX() {
//		return this.width - 6;
//	}

	@Override
	public int getRowWidth() {
		return this.width - (Math.max(0, (this.children().size() * this.contentHeight() + this.itemHeight + 4) - (this.getBottom() - this.getY() - 4)) > 0 ? 18 : 12);
	}

	@Override
	public int getRowLeft() {
		return getX() + 6;
	}

	public int getWidth() {
		return width;
	}

//	public int getTop() {
//		return this.getTop();
//	}

	public ModsScreen getParent() {
		return parent;
	}

//	@Override
//	protected int getMaxPosition() {
//		return super.getMaxPosition() + 4;
//	}

	public int getDisplayedCountFor(Set<String> set) {
		int count = 0;
		for (ModListEntry c : children()) {
			if(c.useSMOD()){
				if (set.contains(c.getSMod().getId())) {
					count++;
				}
			} else {
			 	if (set.contains(c.getMod().getId())) {
					count++;
				}
			}
		}
		return count;
	}

	protected void drawSelectionHighlight(GuiGraphics context, int x, int y, int width, int height, int borderColor, int fillColor) {
		context.fill(x, y - 2, x + width, y + height + 2, borderColor);
		context.fill(x + 1, y - 1, x + width - 1, y + height + 1, fillColor);
	}


	@Override
	public void close() {

	}
}
