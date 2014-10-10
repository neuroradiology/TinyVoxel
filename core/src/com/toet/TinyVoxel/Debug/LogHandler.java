/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.toet.TinyVoxel.Debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.toet.TinyVoxel.Config;

/**
 *
 * @author Kajos
 */
public class LogHandler {

    public static void exitOnGLError() {
        if (!Config.get().ENABLE_ERROR_HANDLER) return;

        int errorValue = Gdx.graphics.getGL20().glGetError();

        if (errorValue != GL20.GL_NO_ERROR) {
            //if (Display.isCreated()) Display.destroy();
            throwEx("OpenGL" + errorValue);
        }
    }

    public static void exit(String name, ShaderProgram shader) {
        log("ERROR - " + name);
        log("ERROR - " + shader.toString());
        log("ERROR - " + shader.getLog());
        throwEx(name);
    }

    public static void throwEx(String errorMessage) {
        if (Config.get().ENABLE_ERROR_HANDLER) throw new Error("ERROR - " + errorMessage);
    }

    public static void log(String log) {
        Gdx.app.log("TinyVoxel", log);
    }
}
