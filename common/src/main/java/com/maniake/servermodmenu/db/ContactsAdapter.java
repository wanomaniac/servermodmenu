package com.maniake.servermodmenu.db;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.lang.reflect.Type;

public class ContactsAdapter implements JsonDeserializer<SMod.Contact> {
	@Override
	public SMod.Contact deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
		SMod.Contact ctn = new SMod.Contact();
		JsonObject jsonObject = json.getAsJsonObject();

		if (jsonObject.get("homepage") != null) {
			ctn.setHomepage(jsonObject.get("homepage").toString());
		}
		if (jsonObject.get("issues") != null) {
			ctn.setHomepage(jsonObject.get("issues").toString());
		}
		if (jsonObject.get("sources") != null) {
			ctn.setHomepage(jsonObject.get("sources").toString());
		}

		return ctn;
	}
}
