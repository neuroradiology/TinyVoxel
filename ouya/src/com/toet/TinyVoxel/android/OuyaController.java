package com.toet.TinyVoxel;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.controllers.mappings.Ouya;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import com.toet.TinyVoxel.GameControllers.CharacterController;

/**
 * Created by Kajos on 27-1-14.
 */
public class OuyaController extends CharacterController {
    public Controller controller;
    private final float SPEED = 0.1f;

    public OuyaController() {
    }

    @Override
    public float getX() {
        if (controller == null) {
            return 0f;
        }
        return controller.getAxis(Ouya.AXIS_RIGHT_X) * SPEED;
    }

    @Override
    public float getY() {
        if (controller == null) {
            return 0f;
        }
        return controller.getAxis(Ouya.AXIS_RIGHT_Y) * SPEED;
    }

    @Override
    public boolean getActionPress() {
        if (controller == null) {
            return false;
        }
        return controller.getButton(Ouya.BUTTON_O);
    }

    @Override
    public boolean getDragPress() {
        return false;
    }

    @Override
    public float getForward() {
        if (controller == null) {
            return 0f;
        }
        if (controller.getAxis(Ouya.AXIS_LEFT_Y) == 0f) {
            return 0f;
        }
        return controller.getAxis(Ouya.AXIS_LEFT_Y);
    }

    @Override
    public float getLeft() {
        if (controller == null) {
            return 0f;
        }
        if (controller.getAxis(Ouya.AXIS_LEFT_X) == 0f) {
            return 0f;
        }
        return controller.getAxis(Ouya.AXIS_LEFT_X);
    }

    @Override
    public boolean getShift() {
        if (controller == null) {
            return false;
        }
        return controller.getButton(Ouya.BUTTON_A);
    }

    @Override
    public void init(int id) {
        if (controller == null) {
            for (int i = 0; i < Controllers.getControllers().size; i++) {
                Controller current = Controllers.getControllers().get(i);
                // Dispose any irregular controllers
                if (current.getName().equals(Ouya.ID)) {
                    controller = Controllers.getControllers().get(i);
                    break;
                }
            }
        }
    }

    @Override
    public void render(PerspectiveCamera camera) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void setAsInput() {

    }

    @Override
    public boolean showMainMenu() {
        return false;
    }
}
