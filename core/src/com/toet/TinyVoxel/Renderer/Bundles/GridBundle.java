package com.toet.TinyVoxel.Renderer.Bundles;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.*;
import com.toet.TinyVoxel.Config;
import com.toet.TinyVoxel.Debug.LogHandler;
import com.toet.TinyVoxel.Renderer.Wrapped.WrappedBoolean;
import com.toet.TinyVoxel.Renderer.Wrapped.WrappedInteger;
import com.toet.TinyVoxel.Util.JobManager;
import com.toet.TinyVoxel.Util.StreamUtil;

import java.io.*;


/**
 * Created by Kajos on 20-1-14.
 */
public class GridBundle extends Bundle {
    protected IntMap<IntMap<IntMap<Grid>>> terrainMap;

    public GridBundle() {
    }

    @Override
    public void init(String name, boolean load) {
        visible = true;
        solid = true;

        this.name = name;

        transform.idt();

        terrainMap = new IntMap<IntMap<IntMap<Grid>>>();
        boundingBox = new BoundingBox();
        gridCount = new WrappedInteger(0);
        isLoading = new WrappedBoolean(false);
        references = new WrappedInteger(1);

        if (load) {
            loadAll();
        }

        updateMatrix();
    }

    public void init(GridBundle bundle) {
        visible = true;
        solid = true;

        this.name = bundle.name;

        transform.idt();

        terrainMap = bundle.terrainMap;
        boundingBox = bundle.boundingBox;
        isLoading = bundle.isLoading;
        gridCount = bundle.gridCount;
        references = bundle.references;
        references.increment();

        updateMatrix();
    }

    @Override
    protected Bundle instantiateCopy() {
        GridBundle bundle = new GridBundle();
        bundle.init("instantiate", false);

        bundle.boundingBox.set(bundle.boundingBox);

        for (IntMap<IntMap<Grid>> entry : terrainMap.values()) {
            for (IntMap<Grid> entry2 : entry.values()) {
                for (Grid entry3 : entry2.values()) {
                    Grid grid = entry3;
                    Grid newGrid = bundle.createGrid(grid.x, grid.y, grid.z, false);
                    grid.copyTo(newGrid);
                    newGrid.owner = this;
                }
            }
        }

        updateBoundingBox();

        bundle.upload();
        return bundle;
    }

    public Bundle copy() {
        GridBundle bundle = new GridBundle();
        bundle.init(this);
        return bundle;
    }

    @Override
    public Grid getGrid(int x, int y, int z) {
        IntMap<IntMap<Grid>> tx = terrainMap.get(x);
        if (tx == null) {
            return null;
        }
        IntMap<Grid> ty = tx.get(y);
        if (ty == null) {
            return null;
        }
        return ty.get(z);
    }

    @Override
    public Grid getGridSafe(int x, int y, int z) {
        return getGrid(x,y,z);
    }

    @Override
    public Grid createGrid(int x, int y, int z, boolean updateBoundingBox) {
        Grid fetch = getGrid(x, y, z);
        if (fetch != null) {
            return fetch;
        }

        Grid grid = new Grid(); //gridPool.obtain();
        grid.reset();
        grid.init(this, x, y, z);
        gridCount.increment();

        IntMap<IntMap<Grid>> tx = terrainMap.get(x);
        if (tx == null) {
            tx = new IntMap<IntMap<Grid>>();
            terrainMap.put(x, tx);
        }
        IntMap<Grid> ty = tx.get(y);
        if (ty == null) {
            ty = new IntMap<Grid>();
            tx.put(y, ty);
        }
        ty.put(z, grid);

        if (updateBoundingBox)
            updateBoundingBox();

        return grid;
    }

    @Override
    public void removeGrid(int x, int y, int z) {
        if (terrainMap.get(x) == null || terrainMap.get(x).get(y) == null)
            return;

        Grid grid = terrainMap.get(x).get(y).get(z);
        if (grid != null) {
            IntMap<IntMap<Grid>> tx = terrainMap.get(x);
            IntMap<Grid> ty = tx.get(y);

            ty.remove(z);

            if (ty.size == 0) {
                tx.remove(y);
            }
            if (tx.size == 0) {
                terrainMap.remove(x);
            }

            grid.dispose();
            gridCount.decrement();
            //gridPool.free(grid);
        }
    }

    @Override
    public int sizeInMB() {
        int size = 0;

        for (IntMap<IntMap<Grid>> entry : terrainMap.values()) {
            for (IntMap<Grid> entry2 : entry.values()) {
                for (Grid entry3 : entry2.values()) {
                    Grid grid = entry3;
                    size += grid.getByteSize();
                }
            }
        }

        size /= 1024 * 1024; //hack

        return size;
    }

    @Override
    public void upload() {
        for (IntMap<IntMap<Grid>> entry : terrainMap.values()) {
            for (IntMap<Grid> entry2 : entry.values()) {
                for (Grid entry3 : entry2.values()) {
                    Grid grid = entry3;
                    grid.loadPalette();
                }
            }
        }
    }

    @Override
    public void makeShadows() {
        if (isBuildingShadows()) {
            LogHandler.log("Is building shadows already..");
            return;
        }

        shadowBuildingCount = getGridCount();
        for (IntMap<IntMap<Grid>> entry : terrainMap.values()) {
            for (IntMap<Grid> entry2 : entry.values()) {
                for (final Grid entry3 : entry2.values()) {
                    JobManager.get().postRunnable(new Runnable() {

                        @Override
                        public void run() {
                            Grid grid = entry3;
                            grid.findAllShadows();
                            grid.loadPalette();

                            shadowBuildingCount--;
                        }
                    });
                }
            }
        }
        JobManager.get().postRunnable(new Runnable() {

            @Override
            public void run() {
                updateBoundingBox();
            }
        });
    }

    @Override
    public Grid getFirstGrid() {
        return terrainMap.values().next().values().next().values().next();
    }

    @Override
    public void drawAll(PerspectiveCamera camera, ShaderProgram shader, float lod, int uniform) {
        boolean doSetMatrix = true;
        for (IntMap<IntMap<Grid>> entry : terrainMap.values()) {
            for (IntMap<Grid> entry2 : entry.values()) {
                for (Grid entry3 : entry2.values()) {
                    Grid grid = entry3;
                    if (drawTerrain(grid, camera, shader, lod, uniform, doSetMatrix) > 0)
                        doSetMatrix = false;
                }
            }
        }
    }

    @Override
    public void drawShadows(PerspectiveCamera camera, ShaderProgram shader, float lod, int uniform) {
        boolean doSetMatrix = true;
        for (IntMap<IntMap<Grid>> entry : terrainMap.values()) {
            for (IntMap<Grid> entry2 : entry.values()) {
                for (Grid entry3 : entry2.values()) {
                    Grid grid = entry3;
                    if (drawTerrainShadow(grid, camera, shader, lod, uniform, doSetMatrix) > 0)
                        doSetMatrix = false;
                }
            }
        }
    }

    @Override
    public void clear() {
        for (IntMap<IntMap<Grid>> entry : terrainMap.values()) {
            for (IntMap<Grid> entry2 : entry.values()) {
                for (Grid entry3 : entry2.values()) {
                    Grid grid = entry3;
                    grid.dispose();
                    //gridPool.free(grid);
                }
            }
        }
        terrainMap.clear();
        updateBoundingBox();
    }

    @Override
    public Grid collidesWith(Ray ray, Vector3 intersection, float maxDistance) {
        if (!visible || !inRange || !inLargeRange || !ableToCollide)
            return null;

        tmpRay.set(ray);
        tmpRay.mul(inverseTransform);

        Grid foundGrid = null;

        if (!Intersector.intersectRayBoundsFast(tmpRay, boundingBox)) {
            return null;
        }

        float diff = (float)Config.GRID_SIZE / 2f * 1.73f;

        for (IntMap<IntMap<Grid>> entry : terrainMap.values()) {
            for (IntMap<Grid> entry2 : entry.values()) {
                for (Grid entry3 : entry2.values()) {
                    Grid grid = entry3;

                    if (!grid.checkBounds)
                        continue;

                    tmp.set(grid.x, grid.y, grid.z);
                    tmp.add(0.5f);
                    tmp.scl(Config.GRID_SIZE);
                    float myDist = tmpRay.origin.dst(tmp) - diff;

                    if (myDist * myDist > maxDistance) {
                        continue;
                    }

                    if (grid.collidesWith(tmpRay, intersection, maxDistance)) {
                        maxDistance = tmpRay.origin.dst2(intersection);
                        intersection.mul(transform);
                        foundGrid = grid;
                    }
                }
            }
        }

        return foundGrid;
    }

    static Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.POST);
    @Override
    public void saveAll() {
        if (terrainMap.size == 0) {
            LogHandler.log("Not saving " + name + ", because empty terrainmap.");
            return;
        }

        OutputStream out = Gdx.files.external("TinyVoxel/" + name + "_" + Config.TINY_GRID_SIZE + "_" + Config.GRID_SIZE  +
                (Config.SAVE_SHADE ? ".rlelvl" : ".rlergb" )).write(false);

        try {
            StreamUtil.putInt(out, getGridCount());

            LogHandler.log("Count writing grids: " + getGridCount());

            LogHandler.log("Saving!");

            for (IntMap<IntMap<Grid>> entry : terrainMap.values()) {
                for (IntMap<Grid> entry2 : entry.values()) {
                    for (Grid entry3 : entry2.values()) {
                        Grid grid = entry3;
                        grid.save(out);
                    }
                }
            }

            LogHandler.log("Done saving!");

            out.close();
        } catch (IOException ex) {
            LogHandler.log("Failure saving!");
        }

//        String url = "http://vrpaint.com/voxel/assets/models/upload.php?file=" + name + "_" + Config.TINY_GRID_SIZE + "_" + Config.GRID_SIZE  + ".blk";
//        request.setUrl(url);
//
//        byte[] array = out.toByteArray();
//        InputStream input = new ByteArrayInputStream(array);
//        request.setContent(input, array.length);
//
//        Gdx.net.sendHttpRequest(request, new SaveListener(this));
    }

    @Override
    public void updateBoundingBox() {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        boolean checkBounds = false;

        for (IntMap<IntMap<Grid>> entry : terrainMap.values()) {
            for (IntMap<Grid> entry2 : entry.values()) {
                for (Grid entry3 : entry2.values()) {
                    Grid grid = entry3;

                    int x = grid.x;
                    int y = grid.y;
                    int z = grid.z;

                    if (x < minX)
                        minX = x;

                    if (x > maxX)
                        maxX = x;

                    if (y < minY)
                        minY = y;

                    if (y > maxY)
                        maxY = y;

                    if (z < minZ)
                        minZ = z;

                    if (z > maxZ)
                        maxZ = z;

                    checkBounds = true;
                }
            }
        }

        if (!checkBounds) {
            boundingBox.min.set(1,1,1);
            boundingBox.max.set(-1,-1,-1);
        } else {
            maxX++;
            maxY++;
            maxZ++;
            boundingBox.min.set(minX * Config.GRID_SIZE, minY * Config.GRID_SIZE, minZ * Config.GRID_SIZE);
            boundingBox.max.set(maxX * Config.GRID_SIZE, maxY * Config.GRID_SIZE, maxZ * Config.GRID_SIZE);
        }
        boundingBox.set(boundingBox.min, boundingBox.max);

        inRangeCounter = 0;
    }

    @Override
    public void dispose() {
        references.decrement();

        if (references.get() > 0)
            return;

        for (IntMap<IntMap<Grid>> entry : terrainMap.values()) {
            for (IntMap<Grid> entry2 : entry.values()) {
                for (Grid entry3 : entry2.values()) {
                    Grid grid = entry3;
                    grid.dispose();
                }
            }
        }

        LogHandler.log("Disposed grids.");
    }

}
