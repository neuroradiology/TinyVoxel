/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.toet.TinyVoxel;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.IntBuffer;

/**
 *
 * @author Kajos
 */
public class IOSConfig extends Config {
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
        return 10000;
    }

    @Override
    public int getOffsetDetail() {
        return 0;
    }

    @Override
    public boolean getPostFBOShader() {
        return false;
    }

    @Override
    public boolean getTransparentTools() {
        return false;
    }
}
