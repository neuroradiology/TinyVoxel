package com.toet.TinyVoxel.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtGL20;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.google.gwt.typedarrays.client.Float32ArrayNative;
import com.google.gwt.typedarrays.client.Uint32ArrayNative;
import com.google.gwt.typedarrays.client.Uint8ArrayNative;
import com.google.gwt.typedarrays.shared.ArrayBufferView;
import com.google.gwt.typedarrays.shared.Float32Array;
import com.google.gwt.typedarrays.shared.Uint32Array;
import com.google.gwt.typedarrays.shared.Uint8Array;
import com.google.gwt.webgl.client.WebGLRenderingContext;
import com.google.gwt.webgl.client.WebGLUniformLocation;
import com.toet.TinyVoxel.Config;

import java.nio.ByteBufferWrapper;
import java.nio.FloatBuffer;
import java.nio.HasArrayBufferView;
import java.nio.IntBuffer;

/**
 * Created by Kajos on 8/30/2014.
 */
public class GwtConfig extends Config {

    static Uint8Array paletteBuffer = Uint8ArrayNative.create(Config.TINY_GRID_TOTAL * Config.GRID_TOTAL * 4);
    static Uint8Array tinyPaletteBuffer = Uint8ArrayNative.create(Config.TINY_GRID_TOTAL * 4);

    int paletteCounter = 0;
    int singlePaletteCounter = 0;

    @Override
    public void putPalette(int color) {
        paletteBuffer.set(paletteCounter++, color & 0xff);
        paletteBuffer.set(paletteCounter++, (color >> 8) & 0xff);
        paletteBuffer.set(paletteCounter++, (color >> 16) & 0xff);
        paletteBuffer.set(paletteCounter++, (color >> 24)  & 0xff);
    }

    @Override
    public void putSinglePalette(int color) {
        tinyPaletteBuffer.set(singlePaletteCounter++, color & 0xff);
        tinyPaletteBuffer.set(singlePaletteCounter++, (color >> 8) & 0xff);
        tinyPaletteBuffer.set(singlePaletteCounter++, (color >> 16) & 0xff);
        tinyPaletteBuffer.set(singlePaletteCounter++, (color >> 24) & 0xff);

    }

    @Override
    public void uploadPalette(int width, int height) {
        Gdx.graphics.getGL20().glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 1);
        WebGLRenderingContext.getContext(((GwtApplication) Gdx.app).getCanvasElement()).texImage2D(GL20.GL_TEXTURE_2D, 0, GL20.GL_RGBA, width, height, 0,
                GL20.GL_RGBA,
                GL20.GL_UNSIGNED_BYTE, paletteBuffer);
    }

    @Override
    public void uploadSinglePalette(int paletteId) {
        Gdx.graphics.getGL20().glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 1);
        WebGLRenderingContext.getContext(((GwtApplication) Gdx.app).getCanvasElement()).texSubImage2D(GL20.GL_TEXTURE_2D, 0, 0, paletteId, Config.TINY_GRID_TOTAL, 1,
                GL20.GL_RGBA,
                GL20.GL_UNSIGNED_BYTE, tinyPaletteBuffer);
    }

    @Override
    public void rewindPalette() {
        paletteCounter = 0;
    }

    @Override
    public void rewindSinglePalette() {
        singlePaletteCounter = 0;
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
