package com.maniake.servermodmenu.db;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.lang.reflect.Type;
import java.util.Map;

public class LinksAdapter implements JsonDeserializer<SMod.Links> {
	@Override
	public SMod.Links deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
		SMod.Links tav = new SMod.Links();
		JsonObject jsonObject = json.getAsJsonObject();

		for (Map.Entry<String, JsonElement> entry : jsonObject.asMap().entrySet()) {
			String e = entry.getKey();
			JsonElement f = entry.getValue();
			tav.setLink(e, f.toString());
		}

		return tav;
	}
}
