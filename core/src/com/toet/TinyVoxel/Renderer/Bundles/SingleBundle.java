package com.toet.TinyVoxel.Renderer.Bundles;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
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
public class SingleBundle extends Bundle {
    public Grid singleGrid = null;

    public SingleBundle() {
    }

    @Override
    public void init(String name, boolean load) {
        visible = true;
        solid = true;

        this.name = name;

        transform.idt();

        boundingBox = new BoundingBox();
        gridCount = new WrappedInteger(0);
        isLoading = new WrappedBoolean(false);
        references = new WrappedInteger(1);

        if (load) {
            loadAll();
        }

        updateMatrix();
    }

    public void init(SingleBundle bundle) {
        visible = true;
        solid = true;

        this.name = bundle.name;

        transform.idt();

        boundingBox = bundle.boundingBox;
        singleGrid = bundle.singleGrid;
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

        Grid newGrid = bundle.createGrid(singleGrid.x, singleGrid.y, singleGrid.z, false);
        singleGrid.copyTo(newGrid);
        newGrid.owner = this;

        updateBoundingBox();

        bundle.upload();
        return bundle;
    }

    public Bundle copy() {
        SingleBundle bundle = new SingleBundle();
        bundle.init(this);
        return bundle;
    }

    @Override
    public Grid getGrid(int x, int y, int z) {

        return getGridDirect(x, y, z);
    }

    @Override
    public Grid getGridSafe(int x, int y, int z) {

        return getGridDirect(x,y,z);
    }

    public Grid getGridDirect(int x, int y, int z) {
        if (singleGrid != null && x == singleGrid.x && y == singleGrid.y && z == singleGrid.z)
            return singleGrid;
        else
            return null;
    }

    @Override
    public Grid createGrid(int x, int y, int z, boolean updateBoundingBox) {
        if (singleGrid != null) {
            return getGridDirect(x,y,z);
        }

        Grid grid = new Grid(); //gridPool.obtain();
        grid.reset();
        grid.init(this, x, y, z);

        singleGrid = grid;
        gridCount.increment();

        if (updateBoundingBox)
            updateBoundingBox();

        return grid;
    }

    @Override
    public void removeGrid(int x, int y, int z) {
        if (x != singleGrid.x && y != singleGrid.y && z != singleGrid.z)
            return;

        if (singleGrid != null) {
            singleGrid.dispose();
            singleGrid = null;
            gridCount.decrement();
        }
    }

    @Override
    public int sizeInMB() {
        int size = 0;

        if (singleGrid != null) {
            size += singleGrid.getByteSize();
        }

        size /= 1024 * 1024;

        return size;
    }

    @Override
    public void upload() {
        if (singleGrid != null) {
            singleGrid.loadPalette();
        }
    }

    @Override
    public void makeShadows() {
        if (isBuildingShadows()) {
            LogHandler.log("Is building shadows already..");
            return;
        }

        shadowBuildingCount = getGridCount();
        if (singleGrid != null) {
            JobManager.get().postRunnable(new Runnable() {

                @Override
                public void run() {
                    singleGrid.findAllShadows();
                    singleGrid.loadPalette();

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
        return singleGrid;
    }

    @Override
    public void drawAll(PerspectiveCamera camera, ShaderProgram shader, float lod, int uniform) {
        boolean doSetMatrix = true;
        if (singleGrid != null) {
            if (drawTerrain(singleGrid, camera, shader, lod, uniform, doSetMatrix) > 0)
                        doSetMatrix = false;
        }
    }

    @Override
    public void drawShadows(PerspectiveCamera camera, ShaderProgram shader, float lod, int uniform) {
        boolean doSetMatrix = true;
        if (singleGrid != null) {

            if (drawTerrainShadow(singleGrid, camera, shader, lod, uniform, doSetMatrix) > 0)
                        doSetMatrix = false;
        }
    }

    @Override
    public void clear() {
        if (singleGrid != null) {
            singleGrid.dispose();
            singleGrid = null;
            gridCount.decrement();
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

        if (singleGrid != null) {

            if (!singleGrid.checkBounds)
                return null;

            tmp.set(singleGrid.x, singleGrid.y, singleGrid.z);
            tmp.add(0.5f);
            tmp.scl(Config.GRID_SIZE);
            float myDist = tmpRay.origin.dst(tmp) - diff;

            if (myDist * myDist > maxDistance) {
                return null;
            }

            if (singleGrid.collidesWith(tmpRay, intersection, maxDistance)) {
                maxDistance = tmpRay.origin.dst2(intersection);
                intersection.mul(transform);
                foundGrid = singleGrid;
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

            if (singleGrid != null) {

                singleGrid.save(out);

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

        if (singleGrid != null) {

            if (singleGrid.x < minX)
                minX = singleGrid.x;

            if (singleGrid.x > maxX)
                maxX = singleGrid.x;

            if (singleGrid.y < minY)
                minY = singleGrid.y;

            if (singleGrid.y > maxY)
                maxY = singleGrid.y;

            if (singleGrid.z < minZ)
                minZ = singleGrid.z;

            if (singleGrid.z > maxZ)
                maxZ = singleGrid.z;

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

        if (singleGrid != null) {
            singleGrid.dispose();
        }

        LogHandler.log("Disposed grids.");
    }

//    @Override
//    public int drawLayer(int halfDepth, PerspectiveCamera camera, ShaderProgram shader, float lod, int uniform, boolean setMatrix) {
//        if (singleGrid == null)
//            return 0;
//
//        tmp.set(camera.position);
//        tmp.mul(inverseTransform);
//        tmp.scl(1f / (float) Config.GRID_SIZE);
//
//        int viewpointX = MathUtils.floor(tmp.x);
//        int viewpointY = MathUtils.floor(tmp.y);
//        int viewpointZ = MathUtils.floor(tmp.z);
//
//        boolean b1 = singleGrid.x <= viewpointX + halfDepth && singleGrid.x >= viewpointX - halfDepth;
//        boolean b2 = singleGrid.y <= viewpointY + halfDepth && singleGrid.y >= viewpointY - halfDepth;
//        boolean b3 = singleGrid.z <= viewpointZ + halfDepth && singleGrid.z >= viewpointZ - halfDepth;
//
//        boolean c1 = singleGrid.x == viewpointX + halfDepth || singleGrid.x == viewpointX - halfDepth;
//        boolean c2 = singleGrid.y == viewpointY + halfDepth || singleGrid.y == viewpointY - halfDepth;
//        boolean c3 = singleGrid.z == viewpointZ + halfDepth || singleGrid.z == viewpointZ - halfDepth;
//
//        if ((c1 && b2 && b3) || (c2 && b1 && b3) || (c3 && b2 && b1)) {
//            return drawTerrain(singleGrid, camera, shader, lod, uniform, true);
//        }
//        return 0;
//    }
}
