package com.maniake.servermodmenu.mixin;

import com.maniake.servermodmenu.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public abstract class MixinConnectScreen extends Screen {
	@Unique
	private boolean hasConnected = false;
	@Unique
	private boolean connecting = false;
	@Unique
	private boolean networkError = false;
	@Unique private ServerAddress address;
	@Unique private ServerData info;
	@Unique private @Nullable TransferState cookieStorage;
    @Unique private Minecraft client;
	@Unique
	int cancelY; // same as vanilla cancel
	@Unique
	int btnHeight = 20;

	protected MixinConnectScreen() {
		super(Component.empty());
	}

	@Shadow
	protected abstract void connect(final Minecraft minecraft, final ServerAddress serverAddress, final ServerData serverData, @Nullable final TransferState transferState);


	@Shadow
	@Final
	private Screen parent;

	@Unique Button skipButton;
	@Unique Button yesButton;
	@Unique Button retryButton;

	@Inject(
		method = "connect",
		at = @At("HEAD"),
		cancellable = true)
	    private void beforeLogin(Minecraft client, ServerAddress address, ServerData info, TransferState storage, CallbackInfo ci) {
		// Update the text
		this.client = client;
		this.address = address;
		this.info = info;
		this.cookieStorage = storage;
		this.cancelY = this.height / 4 + 120;

		if(!hasConnected) {
			new Thread(() -> {
				String ip = info.ip.split(":")[0];
				connecting = true;

				 skipButton = Button.builder(
					 Component.translatable("modmenu.sendingmods.skip"),
					btn -> onYes()
				).bounds(this.width / 2 - 100, cancelY - btnHeight - 4, 200, btnHeight).build();
					Constants.sendmodstoserver(ip, client);
					if(!Constants.NETWORKING.isSocketValid(ip)){
						connecting = false;
						networkError = true;

						retryButton = Button.builder(
							Component.translatable("servermodmenu.connect.unable.retry"),
							btn -> onRetry()
						).bounds(this.width / 2 - 100, cancelY - btnHeight - 36, 200, btnHeight).build();

						yesButton = Button.builder(
                                Component.translatable("servermodmenu.connect.unable.y"),
							btn -> onYes()
						).bounds(this.width / 2 - 100, cancelY - btnHeight - 4, 200, btnHeight).build();
						return;
					}

					connecting = false;
					hasConnected = true;
					connect(client, address, info, cookieStorage);
			}).start();

			ci.cancel();
		}
	}

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void renderInjected(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if(connecting) {
			super.render(context, mouseX, mouseY, delta);
			if (skipButton != null) {
				skipButton.render(context, mouseX, mouseY, delta);
			}
			context.drawCenteredString(this.font, Component.translatable("modmenu.sendingmods"), this.width / 2, this.height / 2 - 50, -1);
			ci.cancel();
		} else if (networkError) {
			super.render(context, mouseX, mouseY, delta);
			if (yesButton != null) {
				yesButton.render(context, mouseX, mouseY, delta);
			}
			if(retryButton != null){
				retryButton.render(context, mouseX, mouseY, delta);
			}
			context.drawCenteredString(this.font, Component.translatable("servermodmenu.connect.unable"), this.width / 2, this.height / 2 - 50, 0xFFFF0000);
			ci.cancel();
		}
	}

	@Unique
    @Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		boolean handled = false;

		if (skipButton != null) {
			handled |= skipButton.mouseClicked(mouseX, mouseY, button); // returns true if click is inside
		}

		if (yesButton != null) {
			handled |= yesButton.mouseClicked(mouseX, mouseY, button);
		}

		if(retryButton != null){
			handled |= retryButton.mouseClicked(mouseX, mouseY, button);
		}

		// If you handled the click, return true so the screen knows it’s consumed
		return handled || super.mouseClicked(mouseX, mouseY, button);
	}
	private void onYes() {
		hasConnected = true;
		connecting = false;
		networkError = false;
		connect(client, address, info, cookieStorage);
	}

	private void onRetry() {
		Constants.NETWORKING.disconnect(info.ip);
		hasConnected = false;
		connecting = false;
		networkError = false;
		connect(client, address, info, cookieStorage);
	}

}

