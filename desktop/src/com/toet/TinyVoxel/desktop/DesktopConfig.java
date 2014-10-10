package com.toet.TinyVoxel.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.BufferUtils;
import com.toet.TinyVoxel.Config;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Created by Kajos on 8/30/2014.
 */
public class DesktopConfig extends Config {
    private static IntBuffer paletteBuffer = BufferUtils.newIntBuffer(Config.TINY_GRID_TOTAL * Config.GRID_TOTAL);
    private static IntBuffer tinyPaletteBuffer = BufferUtils.newIntBuffer(Config.TINY_GRID_TOTAL);

    @Override
    public void putPalette(int color) {
        paletteBuffer.put(color);
    }

    @Override
    public void putSinglePalette(int color) {
        tinyPaletteBuffer.put(color);
    }

    @Override
    public void uploadPalette(int width, int height) {
        paletteBuffer.rewind();
        Gdx.graphics.getGL20().glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 1);
        Gdx.graphics.getGL20().glTexImage2D(GL20.GL_TEXTURE_2D, 0, GL20.GL_RGBA, width, height, 0,
                GL20.GL_RGBA,
                GL20.GL_UNSIGNED_BYTE, paletteBuffer);
    }

    @Override
    public void uploadSinglePalette(int paletteId) {
        tinyPaletteBuffer.rewind();
        Gdx.graphics.getGL20().glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 1);
        Gdx.graphics.getGL20().glTexSubImage2D(GL20.GL_TEXTURE_2D, 0, 0, paletteId, Config.TINY_GRID_TOTAL, 1,
                GL20.GL_RGBA,
                GL20.GL_UNSIGNED_BYTE, tinyPaletteBuffer);
    }

    @Override
    public void rewindPalette() {
        paletteBuffer.rewind();
    }

    @Override
    public void rewindSinglePalette() {
        tinyPaletteBuffer.rewind();
    }

    @Override
    public int getLOD() {
        return 60000;
    }

    @Override
    public boolean getPostFBOShader() {
        return false;
    }

    @Override
    public boolean getTransparentTools() {
        return true;
    }
}
