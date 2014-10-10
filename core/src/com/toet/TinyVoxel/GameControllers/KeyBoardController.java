package com.toet.TinyVoxel.GameControllers;

import com.badlogic.gdx.Input;

/**
 * Created by Kajos on 27-1-14.
 */
public class KeyBoardController extends CharacterController {

    @Override
    public boolean touchDown (int screenX, int screenY, int pointer, int button) {
        switch(button) {
            case Input.Buttons.LEFT: setAction(ACTION.Drag, 1f);
                break;
            case Input.Buttons.MIDDLE:
            case Input.Buttons.RIGHT: setAction(ACTION.Action, 1f);
                break;
        }
        return false;
    }

    @Override
    public boolean touchUp (int screenX, int screenY, int pointer, int button) {
        switch(button) {
            case Input.Buttons.LEFT: setAction(ACTION.Drag, 0f);
                break;
            case Input.Buttons.MIDDLE:
            case Input.Buttons.RIGHT: setAction(ACTION.Action, 0f);
                break;
        }
        return false;
    }

    @Override
    public boolean scrolled (int amount) {
        return false;
    }

    @Override
    public boolean keyDown (int keycode) {
        switch(keycode) {
            case Input.Keys.UP:
            case Input.Keys.W: setAction(ACTION.Forward, 1f);
                break;
            case Input.Keys.DOWN:
            case Input.Keys.S: setAction(ACTION.Forward, -1f);
                break;
            case Input.Keys.LEFT:
            case Input.Keys.A: setAction(ACTION.Left, -1f);
                break;
            case Input.Keys.RIGHT:
            case Input.Keys.D: setAction(ACTION.Left, 1f);
                break;
            case Input.Keys.SHIFT_RIGHT:
            case Input.Keys.SHIFT_LEFT: setAction(ACTION.Shift, 1f);
                break;
            case Input.Keys.SPACE:
            case Input.Keys.CONTROL_RIGHT:
            case Input.Keys.CONTROL_LEFT: setAction(ACTION.Jump, 1f);
                break;
            case Input.Keys.Q: setAction(ACTION.Action, 1f);
                break;
            //case Input.Keys.ESCAPE: setAction(ACTION.DropMouse, 1f);
        }
        return false;
    }

    @Override
    public boolean keyUp (int keycode) {
        switch(keycode) {
            case Input.Keys.UP:
            case Input.Keys.W: setAction(ACTION.Forward, 0f);
                break;
            case Input.Keys.DOWN:
            case Input.Keys.S: setAction(ACTION.Forward, 0f);
                break;
            case Input.Keys.LEFT:
            case Input.Keys.A: setAction(ACTION.Left, 0f);
                break;
            case Input.Keys.RIGHT:
            case Input.Keys.D: setAction(ACTION.Left, 0f);
                break;
            case Input.Keys.SHIFT_RIGHT:
            case Input.Keys.SHIFT_LEFT: setAction(ACTION.Shift, 0f);
                break;
            case Input.Keys.SPACE:
            case Input.Keys.CONTROL_RIGHT:
            case Input.Keys.CONTROL_LEFT: setAction(ACTION.Jump, 0f);
                break;
            case Input.Keys.Q: setAction(ACTION.Action, 0f);
                break;
            //case Input.Keys.ESCAPE: setAction(ACTION.DropMouse, 0f);
        }
        return false;
    }

    @Override
    public boolean keyTyped (char character) {
        return false;
    }
}
