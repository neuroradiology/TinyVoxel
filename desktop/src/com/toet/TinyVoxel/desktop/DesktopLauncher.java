package com.toet.TinyVoxel.desktop;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.toet.TinyVoxel.Config;
import com.toet.TinyVoxel.Game;
import com.toet.TinyVoxel.GameControllers.KeyBoardController;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
                config.title = "Tiny Voxel";
                config.width = 1280;
                config.height = 720;
                config.useGL30 = false;
//                config.vSyncEnabled = true;
                config.vSyncEnabled = false;
                config.foregroundFPS = 0; // Setting to 0 disables foreground fps throttling
                config.backgroundFPS = 0; // Setting to 0 disables background fps throttling
                config.stencil = 0;
                //cfg.fullscreen = true;
                config.addIcon("textures/icon.png", Files.FileType.Internal);

        Config.set(new DesktopConfig());
		new LwjglApplication(new Game(new KeyBoardController()), config);
	}
}
