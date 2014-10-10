package com.toet.TinyVoxel.Screens;

import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.toet.TinyVoxel.GameControllers.CharacterController;

/**
 * Created by Kajos on 9/7/2014.
 */
public class Menu extends GUI {
    private static Menu INSTANCE;

    public static Menu get() {
        if (INSTANCE == null)
            INSTANCE = new Menu();

        return INSTANCE;
    }

    public Menu() {
        super();

        Image image = new Image(ninePatch);
        image.setWidth(500);
        image.setHeight(500);
        image.setPosition(250, 250);
        image.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                return true;
            }
            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
            }
        });
        stage.addActor(image);

        addImage("textures/logo.png", 200, 700, 200,100);
    }

    boolean enabled = false;
    public void enable(InputMultiplexer inputMultiplexer, CharacterController controller) {
        enabled = true;
        inputMultiplexer.clear();
        addToMultiplexer(inputMultiplexer);
        inputMultiplexer.addProcessor(controller);
    }

    public void disable(InputMultiplexer inputMultiplexer, CharacterController controller) {
        enabled = false;
        inputMultiplexer.clear();
        GUI.get().addToMultiplexer(inputMultiplexer);
        inputMultiplexer.addProcessor(controller);
    }

    public boolean render() {
        if (enabled) {
            stage.act();
            stage.draw();
            return true;
        }
        return false;
    }

}
