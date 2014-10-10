package com.toet.TinyVoxel.Shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.toet.TinyVoxel.Config;
import com.toet.TinyVoxel.Debug.LogHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Kajos on 3-1-14.
 */
public class ShaderManager {
    private static ShaderManager instance;

    public static ShaderManager get() {
        if (instance == null)
            instance = new ShaderManager();

        return instance;
    }

    public static void refresh() {
        instance = null;
    }

    private Map<String, ShaderProgram> programs = new HashMap<String, ShaderProgram>();

    private ShaderProgram init(String frag, String vert) {
        LogHandler.log("Init frag: " + frag);
        LogHandler.log("Init vert: " + vert);

        FileHandle vFile = Gdx.files.internal(vert);
        FileHandle fFile = Gdx.files.internal(frag);
        if (!vFile.exists() || !fFile.exists()) {
            LogHandler.throwEx("Shaders file not found!: " + frag + ", " + vert);
        }

        ShaderProgram sh = new ShaderProgram(vFile, fFile);
        if (sh.isCompiled() == false) {
            LogHandler.throwEx("Frag: " + frag + " Vert: " + vert + "--" + sh.getLog());
        }

        if (Config.ENABLE_ERROR_HANDLER) {
            LogHandler.log("Frag: " + frag + " Vert: " + vert);
            LogHandler.log(sh.getLog());
        }

        //ErrorHandler.log(sh.getLog());
        sh.pedantic = false;
        String name = frag + "|" + vert;
        programs.remove(name);
        programs.put(name, sh);
        return sh;
    }

    public void reloadAll() {
        String[] keys =  programs.keySet().toArray(new String[0]);

        for (int i = 0; i < keys.length; i++) {
            String val[] = keys[i].split("\\|");
            init(val[0], val[1]);
        }
    }

    public ShaderProgram getShader(String frag, String vert) {
        ShaderProgram sh = programs.get(frag + "|" + vert);
        if (sh != null)
            return sh;
        else
            return init(frag, vert);
    }

    public static void disposeAll() {
        if (instance == null)
            return;

        for (ShaderProgram sh : instance.programs.values()) {
            sh.dispose();
        }
    }
}
