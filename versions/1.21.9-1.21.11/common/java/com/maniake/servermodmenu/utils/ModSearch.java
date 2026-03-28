package com.maniake.servermodmenu.utils;

import com.maniake.servermodmenu.Constants;
import com.maniake.servermodmenu.config.ModMenuConfig;
import com.maniake.servermodmenu.db.SMod;
import com.maniake.servermodmenu.gui.ModsScreen;
import com.maniake.servermodmenu.interfaces.IMod;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.resources.language.I18n;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class ModSearch {

	public static boolean validSearchQuery(String query) {
		return query != null && !query.isEmpty();
	}

	public static List<IMod> search(ModsScreen screen, String query, List<IMod> candidates) {
		if (!validSearchQuery(query)) {
			return candidates;
		}



		return candidates.stream()
				.map(modContainer -> new Pair<>(modContainer, passesFilters(screen, modContainer, query.toLowerCase(Locale.ROOT))))
				.filter(pair -> pair.getSecond() > 0)
				.sorted((a, b) -> b.getSecond() - a.getSecond())
				.map(Pair::getFirst)
				.collect(Collectors.toList());
	}

	public static List<SMod> searchS(ModsScreen screen, String query, List<SMod> candidates) {
		if (!validSearchQuery(query)) {
			return candidates;
		}
		return candidates.stream()
			.map(modContainer -> new Pair<>(modContainer, passesFilters(screen, modContainer, query.toLowerCase(Locale.ROOT))))
			.filter(pair -> pair.getSecond() > 0)
			.sorted((a, b) -> b.getSecond() - a.getSecond())
			.map(Pair::getFirst)
			.collect(Collectors.toList());
	}

	private static int passesFilters(ModsScreen screen, SMod mod, String query) {
		String modId = mod.getId();
		String modName = mod.meta.name;
		String modTranslatedName = mod.meta.name;
		String modDescription = mod.meta.baseDesc;
		String modTranslatedDescription = mod.meta.baseDesc;
		String modServer = mod.server;
		//String modSummary = mod.getSummary();

		String library = I18n.get("modmenu.searchTerms.library");
		String patchwork = I18n.get("modmenu.searchTerms.patchwork");
		String modpack = I18n.get("modmenu.searchTerms.modpack");
		String deprecated = I18n.get("modmenu.searchTerms.deprecated");
		String clientside = I18n.get("modmenu.searchTerms.clientside");
		String configurable = I18n.get("modmenu.searchTerms.configurable");
		String hasUpdate = I18n.get("modmenu.searchTerms.hasUpdate");

		// Some basic search, could do with something more advanced but this will do for now
		if (modName.toLowerCase(Locale.ROOT).contains(query) // Search default mod name
				|| modTranslatedName.toLowerCase(Locale.ROOT).contains(query) // Search localized mod name
				|| modId.toLowerCase(Locale.ROOT).contains(query) // Search mod ID
		) {
			if(!Objects.equals(Constants.SMODS.get(modServer).get(mod.getId()).server, modServer))
				return 0; // if the server
			return query.length() >= 3 ? 2 : 1;
		}

		if (modDescription.toLowerCase(Locale.ROOT).contains(query) // Search default mod description
				|| modTranslatedDescription.toLowerCase(Locale.ROOT).contains(query) // Search localized mod description
				|| authorMatches(mod, query) // Search via author

		) {
			return 1;
		}

		// Allow parent to pass filter if a child passes

		return 0;
	}

	private static int passesFilters(ModsScreen screen, IMod mod, String query) {
		String modId = mod.getId();
		String modName = mod.getName();
		String modTranslatedName = mod.getTranslatedName();
		String modDescription = mod.getDescription();
		String modTranslatedDescription = mod.getTranslatedDescription();
		String modSummary = mod.getSummary();

		String library = I18n.get("modmenu.searchTerms.library");
		String patchwork = I18n.get("modmenu.searchTerms.patchwork");
		String modpack = I18n.get("modmenu.searchTerms.modpack");
		String deprecated = I18n.get("modmenu.searchTerms.deprecated");
		String clientside = I18n.get("modmenu.searchTerms.clientside");
		String configurable = I18n.get("modmenu.searchTerms.configurable");
		String hasUpdate = I18n.get("modmenu.searchTerms.hasUpdate");

		// Libraries are currently hidden, ignore them entirely
		if (mod.isHidden() || !ModMenuConfig.SHOW_LIBRARIES.getValue() && mod.getBadges().contains(IMod.Badge.LIBRARY)) {
			return 0;
		}

		// Some basic search, could do with something more advanced but this will do for now
		if (modName.toLowerCase(Locale.ROOT).contains(query) // Search default mod name
			|| modTranslatedName.toLowerCase(Locale.ROOT).contains(query) // Search localized mod name
			|| modId.toLowerCase(Locale.ROOT).contains(query) // Search mod ID
		) {
			return query.length() >= 3 ? 2 : 1;
		}

		if (modDescription.toLowerCase(Locale.ROOT).contains(query) // Search default mod description
			|| modTranslatedDescription.toLowerCase(Locale.ROOT).contains(query) // Search localized mod description
			|| modSummary.toLowerCase(Locale.ROOT).contains(query) // Search mod summary
			|| authorMatches(mod, query) // Search via author
			|| library.contains(query) && mod.getBadges().contains(IMod.Badge.LIBRARY) // Search for lib mods
			|| patchwork.contains(query) && mod.getBadges().contains(IMod.Badge.PATCHWORK_FORGE) // Search for patchwork mods
			|| modpack.contains(query) && mod.getBadges().contains(IMod.Badge.MODPACK) // Search for modpack mods
			|| deprecated.contains(query) && mod.getBadges().contains(IMod.Badge.DEPRECATED) // Search for deprecated mods
			|| clientside.contains(query) && mod.getBadges().contains(IMod.Badge.CLIENT) // Search for clientside mods
			|| configurable.contains(query) && screen.getModHasConfigScreen().get(modId) // Search for mods that can be configured
//			|| hasUpdate.contains(query) && mod.getModrinthData() != null // Search for mods that have updates
		) {
			return 1;
		}

		// Allow parent to pass filter if a child passes
//		if (Constants.PARENT_MAP.keySet().contains(mod)) {
//			for (IMod child : ModMenu.PARENT_MAP.get(mod)) {
//				int result = passesFilters(screen, child, query);
//
//				if (result > 0) {
//					return result;
//				}
//			}
//		}

		return 0;
	}

	private static boolean authorMatches(IMod mod, String query) {
		return mod.getAuthors().stream()
				.map(s -> s.toLowerCase(Locale.ROOT))
				.anyMatch(s -> s.contains(query.toLowerCase(Locale.ROOT)));
	}

	private static boolean authorMatches(SMod mod, String query) {
		return Arrays.stream(mod.meta.authors)
			.map(s -> s.toLowerCase(Locale.ROOT))
			.anyMatch(s -> s.contains(query.toLowerCase(Locale.ROOT)));
	}

}
