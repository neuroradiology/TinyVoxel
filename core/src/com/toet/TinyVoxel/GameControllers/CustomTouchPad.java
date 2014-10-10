/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.toet.TinyVoxel.GameControllers;


import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Pools;

public class CustomTouchPad extends Widget {
    private TouchpadStyle style;
    boolean touched;
    private float deadzoneRadius;
    private final Circle knobBounds = new Circle(0, 0, 0);
    private final Circle touchBounds = new Circle(0, 0, 0);
    private final Circle deadzoneBounds = new Circle(0, 0, 0);
    private final Vector2 knobPosition = new Vector2();
    private final Vector2 knobPercent = new Vector2();

    public CustomTouchPad (float deadzoneRadius, Skin skin) {
        this(deadzoneRadius, skin.get(TouchpadStyle.class));
    }

    public CustomTouchPad (float deadzoneRadius, Skin skin, String styleName) {
        this(deadzoneRadius, skin.get(styleName, TouchpadStyle.class));
    }

    public CustomTouchPad (float deadzoneRadius, TouchpadStyle style) {
        if (deadzoneRadius < 0) throw new IllegalArgumentException("deadzoneRadius must be > 0");
        this.deadzoneRadius = deadzoneRadius;

        knobPosition.set(getWidth() / 2f, getHeight() / 2f);

        setStyle(style);
        setSize(getPrefWidth(), getPrefHeight());
    }

    public void setTouched(boolean value) {
        touched = value;
    }

    @Override
    public void setPosition(float x, float y) {
        super.setPosition(x, y);
        layout();
        System.out.println(knobBounds);
    }

    void calculatePositionAndValue (float x, float y, boolean isTouchUp) {
        float oldPositionX = knobPosition.x;
        float oldPositionY = knobPosition.y;
        float oldPercentX = knobPercent.x;
        float oldPercentY = knobPercent.y;
        float centerX = knobBounds.x;
        float centerY = knobBounds.y;
        knobPosition.set(centerX, centerY);
        knobPercent.set(0f, 0f);
        if (!isTouchUp) {
            if (!deadzoneBounds.contains(x, y)) {
                knobPercent.set((x - centerX) / knobBounds.radius, (y - centerY) / knobBounds.radius);
                float length = knobPercent.len();
                if (length > 1) knobPercent.scl(1 / length);
                if (knobBounds.contains(x, y)) {
                    knobPosition.set(x, y);
                } else {
                    knobPosition.set(knobPercent).nor().scl(knobBounds.radius).add(knobBounds.x, knobBounds.y);
                }
            }
        }
        if (oldPercentX != knobPercent.x || oldPercentY != knobPercent.y) {
            ChangeListener.ChangeEvent changeEvent = Pools.obtain(ChangeListener.ChangeEvent.class);
            if (fire(changeEvent)) {
                knobPercent.set(oldPercentX, oldPercentY);
                knobPosition.set(oldPositionX, oldPositionY);
            }
            Pools.free(changeEvent);
        }
    }

    public void setStyle (TouchpadStyle style) {
        if (style == null) throw new IllegalArgumentException("style cannot be null");
        this.style = style;
        invalidateHierarchy();
    }

    /** Returns the touchpad's style. Modifying the returned style may not have an effect until {@link #setStyle(TouchpadStyle)} is
     * called. */
    public TouchpadStyle getStyle () {
        return style;
    }

    @Override
    public Actor hit (float x, float y, boolean touchable) {
        return touchBounds.contains(x, y) ? this : null;
    }

    @Override
    public void layout () {
        // Recalc pad and deadzone bounds
        float halfWidth = getWidth() / 2;
        float halfHeight = getHeight() / 2;
        float radius = Math.min(halfWidth, halfHeight);
        touchBounds.set(halfWidth, halfHeight, radius);
        if (style.knob != null) radius -= Math.max(style.knob.getMinWidth(), style.knob.getMinHeight()) / 2;
        knobBounds.set(halfWidth, halfHeight, radius);
        deadzoneBounds.set(halfWidth, halfHeight, deadzoneRadius);
        // Recalc pad values and knob position
        knobPosition.set(halfWidth, halfHeight);
        knobPercent.set(0, 0);
    }

    @Override
    public float getPrefWidth () {
        return style.background != null ? style.background.getMinWidth() : 0;
    }

    @Override
    public float getPrefHeight () {
        return style.background != null ? style.background.getMinHeight() : 0;
    }

    public boolean isTouched () {
        return touched;
    }

    public void setDeadzone (float deadzoneRadius) {
        if (deadzoneRadius < 0) throw new IllegalArgumentException("deadzoneRadius must be > 0");
        this.deadzoneRadius = deadzoneRadius;
        invalidate();
    }

    public float getKnobX () {
        return knobPosition.x;
    }

    public float getKnobY () {
        return knobPosition.y;
    }

    public float getKnobPercentX () {
        return knobPercent.x;
    }

    public float getKnobPercentY () {
        return knobPercent.y;
    }

    public static class TouchpadStyle {
        /** Stretched in both directions. Optional. */
        public Drawable background;

        /** Optional. */
        public Drawable knob;

        public TouchpadStyle () {
        }

        public TouchpadStyle (Drawable background, Drawable knob) {
            this.background = background;
            this.knob = knob;
        }

        public TouchpadStyle (TouchpadStyle style) {
            this.background = style.background;
            this.knob = style.knob;
        }
    }
}