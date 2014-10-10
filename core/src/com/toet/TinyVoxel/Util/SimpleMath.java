package com.toet.TinyVoxel.Util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.toet.TinyVoxel.Config;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Created by Kajos on 7/10/2014.
 */
public class SimpleMath {
    public static float squared(float value) {
        return value * value;
    }

    public static boolean sphereAabbOverlap ( BoundingBox aabb, Vector3 sphere, float radius) {

        float squaredDistance = 0;

        // process X
        if (sphere.x < aabb.min.x) {
            float diff = sphere.x - aabb.min.x;
            squaredDistance += diff * diff;
        }

        else if (sphere.x > aabb.max.x) {
            float diff = sphere.x - aabb.max.x;
            squaredDistance += diff * diff;
        }

        // process Y
        if (sphere.y < aabb.min.y) {
            float diff = sphere.y - aabb.min.y;
            squaredDistance += diff * diff;
        }

        else if (sphere.y > aabb.max.y) {
            float diff = sphere.y - aabb.max.y;
            squaredDistance += diff * diff;
        }

        // process Z
        if (sphere.z < aabb.min.z) {
            float diff = sphere.z - aabb.min.z;
            squaredDistance += diff * diff;
        }

        else if (sphere.z > aabb.max.z) {
            float diff = sphere.z - aabb.max.z;
            squaredDistance += diff * diff;
        }

        return squaredDistance <= radius * radius;
    }

    public static IntBuffer getFrameBufferPixmap (IntBuffer buffer, int x, int y, int w, int h) {
        buffer.rewind();
        Gdx.gl.glPixelStorei(GL20.GL_PACK_ALIGNMENT, 1);
        Gdx.gl.glReadPixels(x, y, w, h, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, buffer);
        buffer.rewind();

        return buffer;
    }

    private static int shades[][][] = new int[Config.TINY_GRID_SIZE][Config.TINY_GRID_SIZE][Config.TINY_GRID_SIZE];
    public static void init() {
        for (int x = 0; x < Config.TINY_GRID_SIZE; x++)
            for (int y = 0; y < Config.TINY_GRID_SIZE; y++)
                for (int z = 0; z < Config.TINY_GRID_SIZE; z++) {
                    shades[x][y][z] = shadeFunc(x,y,z);
                }
    }

    private static Vector3 pos = new Vector3();
    private static int shadeFunc(int tx, int ty, int tz) {
        pos.set(tx - Config.TINY_GRID_SIZE / 2, ty - Config.TINY_GRID_SIZE / 2, tz - Config.TINY_GRID_SIZE / 2);
        return (int)((Math.sin((double)pos.len() / (double)Config.TINY_GRID_SIZE * 6.6f) * .125 + .875) * 254.0);
    }

    public static int shadeLookUp(int tx, int ty, int tz) {
        return shades[tx][ty][tz];
    }

    public static int setShadow(int shadow, int color) {
        color = color & 0x00ffffff;
        color |= shadow << 24;
        return color;
    }

    private static Vector3 col = new Vector3();
    public static Vector3 IntToRGB888( int color )
    {
        float r = (float)((color >> 0) & 0xff);
        float g = (float)((color >> 8) & 0xff);
        float b = (float)((color >> 16) & 0xff);

        col.set(r, g, b);
        return col;
    }

    public static int RGB888ToInt( Vector3 col )
    {
        col.x = Math.max(0f, Math.min(col.x, 255f));
        col.y = Math.max(0f, Math.min(col.y, 255f));
        col.z = Math.max(0f, Math.min(col.z, 255f));

        return RGB888ToInt((int)col.x, (int)col.y, (int)col.z);
    }

    public static int RGB888ToInt( int r, int g, int b )
    {
        int result = (r & 0xff) << 0;
        result |= (g & 0xff) << 8;
        result |= (b & 0xff) << 16;
        result |= 0xff << 24;
        return result;
    }
}
