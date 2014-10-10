package com.toet.TinyVoxel.Util;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GLTexture;

/**
 * Created by Kajos on 8/30/2014.
 */
public class NonBackedTexture extends GLTexture {
    int width, height;

    public NonBackedTexture() {
        super(GL20.GL_TEXTURE_2D, createGLHandle());
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getDepth() {
        return 0;
    }

    @Override
    public boolean isManaged() {
        return false;
    }

    @Override
    protected void reload() {

    }
}
