package com.toet.TinyVoxel.GameControllers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.toet.TinyVoxel.Time;

/**
 * Created by Kajos on 27-1-14.
 */
public abstract class CharacterController extends InputAdapter {
    float prev[] = {0,0,0,0};
    boolean firstTouch[] = {true, true, true, true};

    int x = 0, y = 0;

    public int getX() {
        if (Gdx.input.isCursorCatched())
            return Gdx.graphics.getWidth() / 2;
        return x;
    }

    public int getY() {
        if (Gdx.input.isCursorCatched())
            return Gdx.graphics.getHeight() / 2;
        return y;
    }

    @Override
    public boolean touchDragged (int screenX, int screenY, int pointer) {
        x = screenX;
        y = screenY;
        prevDrag = 0f;
        return true;
    }

    @Override
    public boolean mouseMoved (int screenX, int screenY) {
        x = screenX;
        y = screenY;
        prevDrag = 0f;
        return false;
    }

    public void update() {
        setPoint(x, true, true);
        setPoint(y, false, true);
        setPoint(x, true, false);
        setPoint(y, false, false);

        loop();
        nowActionB = getBoolean(ACTION.Action);

        if (prevDrag < .5f)
            prevDrag += Time.getDelta();
    }

    public void setPoint(int value, boolean XorY, boolean alternative) {
        int id = !alternative ? 0 : 1;
        boolean button = alternative ? getBoolean(ACTION.Action) : getBoolean(ACTION.Drag);

        if (!XorY)
            id += 2;

        float result = 0f;

        float div;
        if (XorY) {
            div = Gdx.graphics.getWidth();
        } else {
            div = Gdx.graphics.getHeight();
        }

        float val = ((float)value / div - 0.5f);
        if (firstTouch[id] && button) {
            prev[id] = val;
            firstTouch[id] = false;
        } else if (button) {
            result = val - prev[id];
            prev[id] = val;
        } else {
            firstTouch[id] = true;
        }
        if (!alternative) {
            if (XorY) {
                setAction(ACTION.X, result);
            } else {
                setAction(ACTION.Y, result);
            }
        } else {
            if (XorY) {
                setAction(ACTION.AlternativeX, result);
            } else {
                setAction(ACTION.AlternativeY, result);
            }
        }
    }

    boolean prevActionB = false;
    boolean nowActionB = false;
    boolean prevDragB = false;
    boolean nowDragB = false;

    private void loop() {
        prevActionB = nowActionB;
        prevDragB = nowDragB;
        nowActionB = getBoolean(ACTION.Action);
        nowDragB = getBoolean(ACTION.Drag);
    }

    public boolean getReleaseActionPress() {
        return prevActionB == true && nowActionB == false;
    }

    public boolean getReleaseDragPress() {
        return prevDragB == true && nowDragB == false;
    }

    float prevDrag = 0f;
    public boolean getLongDragPress() {
        boolean now = getBoolean(ACTION.Drag);

        boolean result = prevDrag > .5f && now == true;
        if (result)
            prevDrag = 0f;

        return result;
    }

    public boolean getBoolean(ACTION action) {
        return actions[action.ordinal()] > 0f;
    }

    public float getFloat(ACTION action) {
        return actions[action.ordinal()];
    }

    public void setAction(ACTION action, float value) {
        actions[action.ordinal()] = value;
    }

    public void init() {

    }

    public enum ACTION {Forward, Left, Shift, X, Y, AlternativeX, AlternativeY, Action, Drag, Jump, DropMouse};
    protected float actions[] = {0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f};
}
