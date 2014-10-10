package com.toet.TinyVoxel.Importer;

/**
 * Created by Kajos on 8/7/2014.
 */

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.BufferUtils;
import com.toet.TinyVoxel.Config;
import com.toet.TinyVoxel.Debug.LogHandler;
import com.toet.TinyVoxel.Renderer.Bundles.Bundle;
import com.toet.TinyVoxel.Util.Position;
import com.toet.TinyVoxel.Util.SimpleMath;

import java.nio.IntBuffer;
import java.util.LinkedList;
import java.util.Queue;

import static com.badlogic.gdx.graphics.GL20.*;

public class MeshImporter {
    private static int[][][] voxels;
    private static int[][][] voxelsOutside;

    private static void shift(int size, int shift) {
        for (int x = 0; x < size-shift; x++)
            for (int y = 0; y < size-shift; y++)
                for (int z = 0; z < size-shift; z++) {
                    int color = 0xff000000;
                    for (int dx = 0; dx <= shift; dx++)
                        for (int dy = 0; dy <= shift; dy++)
                            for (int dz = 0; dz <= shift; dz++) {
                                int nwCol = voxels[x+dx][y+dy][z+dz];
                                if (nwCol != 0xff000000) {
                                    color = nwCol;
                                }
                            }

                    if (color != 0xff000000) {
                        voxels[x][y][z] = color;
                    }
                }
    }

    private static boolean touches(Position pos, int size) {
        if (pos.get(0) < 0 || pos.get(0) >= size)
            return false;
        if (pos.get(1) < 0 || pos.get(1) >= size)
            return false;
        if (pos.get(2) < 0 || pos.get(2) >= size)
            return false;

        return voxelsOutside[pos.get(0)][pos.get(1)][pos.get(2)] == 0;
    }

    private static void set(Position pos) {
        voxelsOutside[pos.get(0)][pos.get(1)][pos.get(2)] = 1;
    }

    private static void floodFill(int size, int r, int g, int b) {
        voxelsOutside = new int[size][size][size];
        for (int x = 0; x < size; x++)
            for (int y = 0; y < size; y++)
                for (int z = 0; z < size; z++) {
                    voxelsOutside[x][y][z] = voxels[x][y][z] == 0xff000000 ? 0 : 1;
                }

        Queue<Position> q = new LinkedList<Position>();
        q.add(Position.create(0,0,0));
        q.add(Position.create(size-1,0,0));
        q.add(Position.create(0,0,size-1));
        q.add(Position.create(0,size-1,0));
        q.add(Position.create(size-1,0,size-1));
        q.add(Position.create(0,size-1,size-1));
        q.add(Position.create(size-1,size-1,0));
        q.add(Position.create(size-1,size-1,size-1));

        int count = 0;
        while (!q.isEmpty()) {
            Position pos = q.remove();
            if (touches(pos, size)) {
                set(pos);
                Position i;
                i = Position.create(pos.get(0), pos.get(1), pos.get(2) + 1);
                q.add(i);
                i = Position.create(pos.get(0), pos.get(1), pos.get(2) - 1);
                q.add(i);
                i = Position.create(pos.get(0) + 1, pos.get(1), pos.get(2));
                q.add(i);
                i = Position.create(pos.get(0) - 1, pos.get(1), pos.get(2));
                q.add(i);
                i = Position.create(pos.get(0), pos.get(1) + 1, pos.get(2));
                q.add(i);
                i = Position.create(pos.get(0), pos.get(1) - 1, pos.get(2));
                q.add(i);

                count++;
            }
            Position.free(pos);

        }

        LogHandler.log("Count : " + count);
        LogHandler.log("All: " + (size * size * size));

        Color inside = new Color((float)r / 255f, (float)g / 255f, (float)b / 255f, 1f);
        int insideInt = inside.toIntBits();
        for (int x = 0; x < size; x++)
            for (int y = 0; y < size; y++)
                for (int z = 0; z < size; z++) {
                    if (voxelsOutside[x][y][z] != 1)
                        voxels[x][y][z] = insideInt;
            }
    }

    private static void readSliceZ(IntBuffer buffer, FrameBuffer fbo, ModelBatch modelBatch, ModelInstance instance, OrthographicCamera camera, BoundingBox box, int z, int size) {
        camera.direction.set(0,0,1);
        camera.up.set(0,1,0);

        float slice = box.getDimensions().z / (float)size;
        camera.near = slice;
        camera.far = slice * 2f;

        camera.position.set(box.getCenter());
        camera.position.z = box.getMin().z - slice;
        camera.position.z += (float)z * slice;

        camera.update(true);

        fbo.begin();
        Gdx.graphics.getGL20().glColorMask(true, true, true, true);
        Gdx.graphics.getGL20().glClearColor(0, 0, 0, 1);
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);

        Gdx.graphics.getGL20().glDisable(GL_DEPTH_TEST);
        Gdx.graphics.getGL20().glDisable(GL_BLEND);

        Gdx.graphics.getGL20().glDisable(GL_CULL_FACE);

        modelBatch.begin(camera);
        modelBatch.render(instance);
        modelBatch.end();

        SimpleMath.getFrameBufferPixmap(buffer, 0, 0, fbo.getWidth(), fbo.getHeight());

        fbo.end();

        for (int x = 0; x < size; x++)
            for (int y = 0; y < size; y++) {
                int color = buffer.get(y * size + x);
                if (color != 0xff000000)
                    voxels[x][y][z] = color;
            }

        // Other direction

        camera.direction.set(0,0,-1);
        camera.up.set(0,1,0);

        camera.position.set(box.getCenter());
        camera.position.z = box.getMax().z + slice;
        camera.position.z -= (float)z * slice;

        camera.update();

        fbo.begin();
        Gdx.graphics.getGL20().glColorMask(true, true, true, true);
        Gdx.graphics.getGL20().glClearColor(0, 0, 0, 1);
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);

        Gdx.graphics.getGL20().glDisable(GL_DEPTH_TEST);
        Gdx.graphics.getGL20().glDisable(GL_BLEND);

        Gdx.graphics.getGL20().glDisable(GL_CULL_FACE);

        modelBatch.begin(camera);
        modelBatch.render(instance);
        modelBatch.end();

        SimpleMath.getFrameBufferPixmap(buffer, 0, 0, fbo.getWidth(), fbo.getHeight());

        fbo.end();

        for (int x = 0; x < size; x++)
            for (int y = 0; y < size; y++) {
                int color = buffer.get(y * size + x);
                if (color != 0xff000000)
                    voxels[size - 1 - x][y][size - 1 - z] = color;
            }
    }

    private static void readSliceX(IntBuffer buffer, FrameBuffer fbo, ModelBatch modelBatch, ModelInstance instance, OrthographicCamera camera, BoundingBox box, int x, int size) {
        camera.direction.set(1,0,0);
        camera.up.set(0,1,0);

        float slice = box.getDimensions().x / (float)size;
        camera.near = slice;
        camera.far = slice * 2f;

        camera.position.set(box.getCenter());
        camera.position.x = box.getMin().x - slice;
        camera.position.x += (float)x * slice;

        camera.update(true);

        fbo.begin();
        Gdx.graphics.getGL20().glColorMask(true, true, true, true);
        Gdx.graphics.getGL20().glClearColor(0, 0, 0, 1);
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);

        Gdx.graphics.getGL20().glDisable(GL_DEPTH_TEST);
        Gdx.graphics.getGL20().glDisable(GL_BLEND);

        Gdx.graphics.getGL20().glDisable(GL_CULL_FACE);

        modelBatch.begin(camera);
        modelBatch.render(instance);
        modelBatch.end();

        SimpleMath.getFrameBufferPixmap(buffer, 0, 0, fbo.getWidth(), fbo.getHeight());

        fbo.end();

        for (int z = 0; z < size; z++)
            for (int y = 0; y < size; y++) {
                int color = buffer.get(y * size + z);
                if (color != 0xff000000)
                    voxels[size - 1 - x][y][z] = color;
            }

        // Other direction

        camera.direction.set(-1,0,0);
        camera.up.set(0,1,0);

        camera.position.set(box.getCenter());
        camera.position.x = box.getMax().x + slice;
        camera.position.x -= (float)x * slice;

        camera.update();

        fbo.begin();
        Gdx.graphics.getGL20().glColorMask(true, true, true, true);
        Gdx.graphics.getGL20().glClearColor(0, 0, 0, 1);
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);

        Gdx.graphics.getGL20().glDisable(GL_DEPTH_TEST);
        Gdx.graphics.getGL20().glDisable(GL_BLEND);

        Gdx.graphics.getGL20().glDisable(GL_CULL_FACE);

        modelBatch.begin(camera);
        modelBatch.render(instance);
        modelBatch.end();

        SimpleMath.getFrameBufferPixmap(buffer, 0, 0, fbo.getWidth(), fbo.getHeight());

        fbo.end();

        for (int z = 0; z < size; z++)
            for (int y = 0; y < size; y++) {
                int color = buffer.get(y * size + z);
                if (color != 0xff000000)
                    voxels[x][y][size - 1 - z] = color;
            }
    }

    private static void readSliceY(IntBuffer buffer, FrameBuffer fbo, ModelBatch modelBatch, ModelInstance instance, OrthographicCamera camera, BoundingBox box, int y, int size) {
        camera.direction.set(0,1,0);
        camera.up.set(0,0,1);

        float slice = box.getDimensions().y / (float)size;
        camera.near = slice;
        camera.far = slice * 2f;

        camera.position.set(box.getCenter());
        camera.position.y = box.getMin().y - slice;
        camera.position.y += (float)y * slice;

        camera.update(true);

        fbo.begin();
        Gdx.graphics.getGL20().glColorMask(true, true, true, true);
        Gdx.graphics.getGL20().glClearColor(0, 0, 0, 1);
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);

        Gdx.graphics.getGL20().glDisable(GL_DEPTH_TEST);
        Gdx.graphics.getGL20().glDisable(GL_BLEND);

        Gdx.graphics.getGL20().glDisable(GL_CULL_FACE);

        modelBatch.begin(camera);
        modelBatch.render(instance);
        modelBatch.end();

        SimpleMath.getFrameBufferPixmap(buffer, 0, 0, fbo.getWidth(), fbo.getHeight());

        fbo.end();

        for (int x = 0; x < size; x++)
            for (int z = 0; z < size; z++) {
                int color = buffer.get(z * size + x);
                if (color != 0xff000000)
                    voxels[size - 1 - x][y][z] = color;
            }

        // Other direction

        camera.direction.set(0,-1,0);
        camera.up.set(0,0,1);

        camera.position.set(box.getCenter());
        camera.position.y = box.getMax().y + slice;
        camera.position.y -= (float)y * slice;

        camera.update();

        fbo.begin();
        Gdx.graphics.getGL20().glColorMask(true, true, true, true);
        Gdx.graphics.getGL20().glClearColor(0, 0, 0, 1);
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);

        Gdx.graphics.getGL20().glDisable(GL_DEPTH_TEST);
        Gdx.graphics.getGL20().glDisable(GL_BLEND);

        Gdx.graphics.getGL20().glDisable(GL_CULL_FACE);

        modelBatch.begin(camera);
        modelBatch.render(instance);
        modelBatch.end();

        SimpleMath.getFrameBufferPixmap(buffer, 0, 0, fbo.getWidth(), fbo.getHeight());

        fbo.end();

        for (int x = 0; x < size; x++)
            for (int z = 0; z < size; z++) {
                int color = buffer.get(z * size + x);
                if (color != 0xff000000)
                    voxels[x][size - 1 - y][z] = color;
            }
    }

    private static AssetManager assets = new AssetManager();
    private static boolean readCustom(String file, int size, int r, int g, int b)
    {

        assets.load(file, Model.class);
        assets.finishLoading();
        Model model = assets.get(file, Model.class);
        if (model == null) {
            LogHandler.log("Failed loading model.");
            return false;
        }

        ModelInstance instance = new ModelInstance(model);
        ModelBatch modelBatch = new ModelBatch();

        BoundingBox box = new BoundingBox();
        model.calculateBoundingBox(box);

        FrameBuffer fbo = new FrameBuffer(Pixmap.Format.RGBA8888, size, size, false);
        IntBuffer buffer = BufferUtils.newIntBuffer(size * size);
        OrthographicCamera camera = new OrthographicCamera(box.getDimensions().z, box.getDimensions().y);

        // Test screenshot
//        camera.direction.set(1,0,0);
//        camera.up.set(0,1,0);
//
//        float slice = box.getDimensions().x / (float)size;
//        camera.near = slice;
//        camera.far = 100f;
//
//        camera.position.set(box.getCenter());
//        camera.position.x = box.getMin().x - slice;
//
//        camera.update(true);
//
//        fbo.begin();
//        Gdx.graphics.getGL20().glColorMask(true, true, true, true);
//        Gdx.graphics.getGL20().glClearColor(0, 0, 0, 1);
//        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
//
//        Gdx.graphics.getGL20().glDisable(GL_DEPTH_TEST);
//        Gdx.graphics.getGL20().glDisable(GL_BLEND);
//
//        Gdx.graphics.getGL20().glDisable(GL_CULL_FACE);
//
//        modelBatch.begin(camera);
//        modelBatch.render(instance);
//        modelBatch.end();
//
//        buffer buffer = ScreenUtils.getFrameBufferbuffer(0, 0, fbo.getWidth(), fbo.getHeight());
//        bufferIO.writePNG(Gdx.files.external("TinyVoxel/test.png"), buffer);
//
//        fbo.end();

        voxels = new int[size][size][size];
        for (int x = 0; x < size; x++)
            for (int y = 0; y < size; y++)
                for (int z = 0; z < size; z++) {
                    voxels[x][y][z] = 0xff000000;
                }

        for (int z = 0; z < size; z++)
            readSliceX(buffer, fbo, modelBatch, instance, camera, box, z, size);

        camera = new OrthographicCamera(box.getDimensions().x, box.getDimensions().z);
        for (int z = 0; z < size; z++)
            readSliceY(buffer, fbo, modelBatch, instance, camera, box, z, size);

        camera = new OrthographicCamera(box.getDimensions().x, box.getDimensions().y);
        for (int z = 0; z < size; z++)
            readSliceZ(buffer, fbo, modelBatch, instance, camera, box, z, size);

        shift(size, 1);

        floodFill(size, r, g, b);

        model.dispose();
        modelBatch.dispose();
        fbo.dispose();

        return true;
    }

    private static Color color = new Color();
    private static Vector3 tmp = new Vector3();
    public static boolean read(String filespec, Bundle gridBundle, Vector3 endPos, int r, int g, int b)
    {
        LogHandler.log("File: " + filespec);

        FileHandle file = Gdx.files.internal(filespec);
        if (!file.exists()) {
            LogHandler.log("File doesn't exist.");
            return false;
        }

        int size = 16 * Config.TINY_GRID_SIZE;

        if (!readCustom(filespec, size, r, g, b)) {
            return false;
        }

        LogHandler.log("Read " + size + " sized grid");

        int written = 0;

        for (int x = 0; x < size; x++)
            for (int y = 0; y < size; y++)
                for (int z = 0; z < size; z++) {
                    tmp.set(x, y, z);
                    tmp.scl(1f / (float) Config.TINY_GRID_SIZE);
                    tmp.add(endPos);

                    if (voxels[x][y][z] != 0xff000000) {
                        color.set(voxels[x][y][z]);
                        int tr = voxels[x][y][z] & 0xff;
                        int tg = (voxels[x][y][z] >> 8) & 0xff;
                        int tb = (voxels[x][y][z] >> 16) & 0xff;

                        gridBundle.addVoxel(tmp, tr, tg, tb, false);

                        written++;
                    }
                }

        LogHandler.log("Printed " + written + " voxels");
        return true;
    }
}
