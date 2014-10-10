package com.toet.TinyVoxel.Renderer.Bundles;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.toet.TinyVoxel.Config;
import com.toet.TinyVoxel.Debug.LogHandler;
import com.toet.TinyVoxel.Renderer.Wrapped.WrappedBoolean;
import com.toet.TinyVoxel.Renderer.Wrapped.WrappedInteger;
import com.toet.TinyVoxel.Util.JobManager;
import com.toet.TinyVoxel.Util.StreamUtil;

import java.io.IOException;
import java.io.OutputStream;


/**
 * Created by Kajos on 20-1-14.
 */
public class ArrayBundle extends Bundle {
    public Array<Grid> gridArray;

    public static final int SIZE = 32;
    public static final int SIZE_HALF = SIZE / 2;
    public static final int MIN_SIZE_HALF = -SIZE_HALF;

    protected Grid[][][] terrainMap;

    public ArrayBundle() {
    }

    @Override
    public void init(String name, boolean load) {
        visible = true;
        solid = true;

        this.name = name;

        transform.idt();

        terrainMap = new Grid[SIZE][SIZE][SIZE];
        boundingBox = new BoundingBox();
        gridArray = new Array<Grid>(Grid.class);
        gridCount = new WrappedInteger(0);
        isLoading = new WrappedBoolean(false);
        references = new WrappedInteger(1);

        if (load) {
            loadAll();
        }

        updateMatrix();
    }

    public void init(ArrayBundle bundle) {
        visible = true;
        solid = true;

        this.name = bundle.name;

        transform.idt();

        terrainMap = bundle.terrainMap;
        boundingBox = bundle.boundingBox;
        gridArray = bundle.gridArray;
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

        for (int i = 0; i < gridArray.size; i++) {
            Grid grid = gridArray.items[i];
            Grid newGrid = bundle.createGrid(grid.x, grid.y, grid.z, false);
            grid.copyTo(newGrid);
            newGrid.owner = this;
        }

        updateBoundingBox();

        bundle.upload();
        return bundle;
    }

    public Bundle copy() {
        ArrayBundle bundle = new ArrayBundle();
        bundle.init(this);
        return bundle;
    }

    @Override
    public Grid getGrid(int x, int y, int z) {
        x += SIZE_HALF;
        y += SIZE_HALF;
        z += SIZE_HALF;

        return getGridDirect(x, y, z);
    }

    @Override
    public Grid getGridSafe(int x, int y, int z) {
        if (x < MIN_SIZE_HALF || y < MIN_SIZE_HALF || z < MIN_SIZE_HALF ||
                x >= SIZE_HALF || y >= SIZE_HALF || z >= SIZE_HALF)
            return null;

        x += SIZE_HALF;
        y += SIZE_HALF;
        z += SIZE_HALF;

        return getGridDirect(x,y,z);
    }

    public Grid getGridDirect(int x, int y, int z) {
        return terrainMap[x][y][z];
    }

    @Override
    public Grid createGrid(int x, int y, int z, boolean updateBoundingBox) {
        if (x < MIN_SIZE_HALF || y < MIN_SIZE_HALF || z < MIN_SIZE_HALF || x >= SIZE_HALF || y >= SIZE_HALF || z >= SIZE_HALF)
            return null;

        int nx = x + SIZE_HALF;
        int ny = y + SIZE_HALF;
        int nz = z + SIZE_HALF;

        Grid fetch = getGridDirect(nx, ny, nz);
        if (fetch != null) {
            return fetch;
        }

        Grid grid = new Grid(); //gridPool.obtain();
        grid.reset();
        grid.init(this, x, y, z);

        gridArray.add(grid);

        terrainMap[nx][ny][nz] = grid;
        gridCount.increment();

        if (updateBoundingBox)
            updateBoundingBox();

        return grid;
    }

    @Override
    public void removeGrid(int x, int y, int z) {
        int nx = x + SIZE_HALF;
        int ny = y + SIZE_HALF;
        int nz = z + SIZE_HALF;

        if (nx < 0 || ny < 0 || nz < 0 || nx >= SIZE || ny >= SIZE || nz >= SIZE)
            return;

        Grid grid = getGridDirect(nx,ny,nz);
        if (grid != null) {
            gridArray.removeValue(grid, true);
            grid.dispose();
            terrainMap[nx][ny][nz] = null;
            gridCount.decrement();
        }
    }

    @Override
    public int sizeInMB() {
        int size = 0;

        for (int i = 0; i < gridArray.size; i++) {
            Grid grid = gridArray.items[i];

            size += grid.getByteSize();
        }

        size /= 1024 * 1024;

        return size;
    }

    @Override
    public void upload() {
        for (int i = 0; i < gridArray.size; i++) {
            Grid grid = gridArray.items[i];
            grid.loadPalette();
        }
    }

    @Override
    public void makeShadows() {
        if (isBuildingShadows()) {
            LogHandler.log("Is building shadows already..");
            return;
        }

        shadowBuildingCount = getGridCount();
        for (int i = 0; i < gridArray.size; i++) {
            final Grid grid = gridArray.items[i];
            JobManager.get().postRunnable(new Runnable() {

                @Override
                public void run() {
                    grid.findAllShadows();
                    grid.loadPalette();

                    shadowBuildingCount--;
                }
            });
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
        return gridArray.first();
    }

    @Override
    public void drawAll(PerspectiveCamera camera, ShaderProgram shader, float lod, int uniform) {
        boolean doSetMatrix = true;
        for (int i = 0; i < gridArray.size; i++) {
            Grid grid = gridArray.items[i];

            if (drawTerrain(grid, camera, shader, lod, uniform, doSetMatrix) > 0)
                        doSetMatrix = false;
        }
    }

    @Override
    public void drawShadows(PerspectiveCamera camera, ShaderProgram shader, float lod, int uniform) {
        boolean doSetMatrix = true;
        for (int i = 0; i < gridArray.size; i++) {
            Grid grid = gridArray.items[i];

            if (drawTerrainShadow(grid, camera, shader, lod, uniform, doSetMatrix) > 0)
                        doSetMatrix = false;
        }
    }

    @Override
    public void clear() {
        for (int i = 0; i < gridArray.size; i++) {
            Grid grid = gridArray.items[i];

            terrainMap[grid.x+SIZE_HALF][grid.y+SIZE_HALF][grid.z+SIZE_HALF] = null;
            grid.dispose();
        }
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

        for (int i = 0; i < gridArray.size; i++) {
            final Grid grid = gridArray.items[i];

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

        return foundGrid;
    }

    static Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.POST);
    @Override
    public void saveAll() {
        if (getGridCount() == 0) {
            LogHandler.log("Not saving " + name + ", because empty terrainmap.");
            return;
        }

        int count = 0;
        OutputStream out = Gdx.files.external("TinyVoxel/" + name + "_" + Config.TINY_GRID_SIZE + "_" + Config.GRID_SIZE  +
                (Config.SAVE_SHADE ? ".rlelvl" : ".rlergb" )).write(false);

        try {
            StreamUtil.putInt(out, getGridCount());

            LogHandler.log("Count writing grids: " + getGridCount());

            for (int i = 0; i < gridArray.size; i++) {
                Grid grid = gridArray.items[i];
                grid.save(out);

                if (count % 10 == 0)
                    System.out.println("Saving: " + ((float) count * 100f / (float) getGridCount()));

                count++;
            }

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

        for (int i = 0; i < gridArray.size; i++) {
            Grid grid = gridArray.items[i];

            if (grid.x < minX)
                minX = grid.x;

            if (grid.x > maxX)
                maxX = grid.x;

            if (grid.y < minY)
                minY = grid.y;

            if (grid.y > maxY)
                maxY = grid.y;

            if (grid.z < minZ)
                minZ = grid.z;

            if (grid.z > maxZ)
                maxZ = grid.z;

            checkBounds = true;
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

        for (int i = 0; i < gridArray.size; i++) {
            Grid grid = gridArray.items[i];
            if (grid == null)
                continue;
            grid.dispose();
        }

        LogHandler.log("Disposed grids.");
    }

}
