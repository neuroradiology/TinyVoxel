/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.toet.TinyVoxel;

import com.badlogic.gdx.Gdx;

/**
 *
 * @author Kajos
 */
public class Time {
    public static boolean emptyDelta = true;
    public static float time = 0f;
    public static long ticks = 0;

    public static float getTime() {
        return time;
    }

    public static void tick() {
        ticks++;
        time += getDelta();
        time %= 1000f;
    }

    public static float getDelta() {
        if (emptyDelta)
            return 0f;

        return Gdx.graphics.getRawDeltaTime();
    }

    public static long getTicks() {
        return ticks;
    }
}
