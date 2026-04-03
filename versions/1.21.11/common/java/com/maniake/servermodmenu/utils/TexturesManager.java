package com.maniake.servermodmenu.utils;

import com.maniake.servermodmenu.Constants;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

public class TexturesManager {
	public static Identifier lFromBase64(String base64, String texturePath) {
		// Decode the Base64 string into a byte array
		byte[] imageBytes = Base64.getDecoder().decode(base64);

		try {
			// Create a NativeImage from the byte array
            DynamicTexture texture;

			NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(imageBytes));

			texture = new DynamicTexture(() -> Identifier.parse(Constants.MOD_ID+":"+texturePath).toString(), nativeImage);

			// Create an Identifier for the texture
			Identifier textureIdentifier = Identifier.parse(Constants.MOD_ID+":"+texturePath);

			// Register the texture with Minecraft's texture manager
			Minecraft.getInstance().getTextureManager().register(textureIdentifier, texture);
			return textureIdentifier;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
