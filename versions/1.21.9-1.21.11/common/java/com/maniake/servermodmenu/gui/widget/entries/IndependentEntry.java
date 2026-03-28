package com.maniake.servermodmenu.gui.widget.entries;

import com.maniake.servermodmenu.db.SMod;
import com.maniake.servermodmenu.gui.widget.ModListWidget;
import com.maniake.servermodmenu.interfaces.IMod;


public class IndependentEntry extends ModListEntry {

	public IndependentEntry(IMod mod, ModListWidget list) {
		super(mod, list);
	}

	public IndependentEntry(SMod mod, ModListWidget list) {
		super(mod, list);
	}

	public IndependentEntry(SMod mod, ModListWidget list, String name) {
		super(mod, list, name);
	}

	public IndependentEntry(SMod mod, ModListWidget list, String name, boolean isF, boolean ren) {
		super(mod, list, name, isF, ren);
	}
	public IndependentEntry(SMod mod, ModListWidget list, String name, boolean isF, boolean ren, boolean moreY) {
		super(mod, list, name, isF, ren, moreY);
	}
}
