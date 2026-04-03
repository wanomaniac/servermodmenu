package com.maniake.servermodmenu.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class EntryButton {
	private final Identifier Texture;
	private final Consumer<EntryButton> callback;
	public int ButtonX = 0;
	public int ButtonY = 0;
	protected int ButtonSX = 20;
	protected int ButtonSY = 20;
	public boolean active = true;
	public boolean visible = true;
	private boolean pauseRender = false;
    private boolean focused = false;
    private final Logger LOGGER = LoggerFactory.getLogger("Server Mod Menu");
	public SoundManager SOUNDMANAGER = Minecraft.getInstance().getSoundManager();;
	public EntryButton(Identifier text, Consumer<EntryButton> cb){
		this.Texture = text;
		this.callback = cb;
	}

	public EntryButton(Identifier text, Consumer<EntryButton> cb, int X, int Y, int XS, int XY){
		this.Texture = text;
		this.callback = cb;
		this.ButtonX = X;
		this.ButtonY = Y;
		this.ButtonSX = XS;
		this.ButtonSY = XY;
	}


    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    public void render(GuiGraphics dc, int mouseX, int mouseY){
		if (pauseRender || !visible) {
			return;
		}

		if (active) {
			if (!hoverHandler(mouseX, mouseY) && !focused) {
				dc.blit(RenderPipelines.GUI_TEXTURED,
					Texture,
					ButtonX, ButtonY,
					0, 0,
					ButtonSX, ButtonSY,
					32, 64
				);
			} else {
				dc.blit(
					RenderPipelines.GUI_TEXTURED,
					Texture,
					ButtonX, ButtonY,
					0, 21,
					ButtonSX, ButtonSY,
					32, 64
				);
			}
		} else {
			dc.blit(
				RenderPipelines.GUI_TEXTURED,
				Texture,
				ButtonX, ButtonY,
				0, 42,
				ButtonSX, ButtonSY,
				32, 64
			);
		}


	}

	private boolean hoverHandler(int mouseX, int mouseY){
		return mouseX >= ButtonX && mouseX < ButtonX + ButtonSX
			&& mouseY >= ButtonY && mouseY < ButtonY + ButtonSY;
	}

	public void handleOnClickEvent(int mouseX, int mouseY){
		if(hoverHandler(mouseX, mouseY) && active) {
			SOUNDMANAGER.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			callback.accept(this);
		}
	}

    public boolean keyPressed(int keyCode) {
        if (!active || !visible || !focused) return false;

        if (keyCode == GLFW.GLFW_KEY_ENTER ||
                keyCode == GLFW.GLFW_KEY_KP_ENTER ||
                keyCode == GLFW.GLFW_KEY_SPACE) {

            SOUNDMANAGER.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            callback.accept(this);
            return true;
        }
        return false;
    }

    public void setActive(boolean a){
		this.active = a;
	}
	public void setVisible(boolean v){
		this.visible = v;
	}
	public void setX(int X){
		this.ButtonX =X;
	}
	public void setY(int Y){
		this.ButtonY = Y;
	}
	public void setSX(int X){
		this.ButtonSX =X;
	}
	public void setSY(int Y){
		this.ButtonSY = Y;
	}
}
