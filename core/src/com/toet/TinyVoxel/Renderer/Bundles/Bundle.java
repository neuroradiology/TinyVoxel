package com.toet.TinyVoxel.Renderer.Bundles;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Frustum;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Disposable;
import com.toet.TinyVoxel.Config;
import com.toet.TinyVoxel.Debug.LogHandler;
import com.toet.TinyVoxel.Importer.BinvoxImporter;
import com.toet.TinyVoxel.Importer.MeshImporter;
import com.toet.TinyVoxel.Renderer.Tools.BrushUtils;
import com.toet.TinyVoxel.Renderer.Wrapped.WrappedBoolean;
import com.toet.TinyVoxel.Renderer.Wrapped.WrappedInteger;
import com.toet.TinyVoxel.Time;
import com.toet.TinyVoxel.Util.SimpleMath;
import com.toet.TinyVoxel.Util.StreamUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;

/**
 * Created by Kajos on 8/21/2014.
 */
public abstract class Bundle implements Comparable<Bundle>, Disposable {
    @Override
    public int compareTo(Bundle other){
        return other.references.hashCode() - references.hashCode();
    }

    public float frustumCulledCounter = 0f;
    public float inRangeCounter = 0f;
    public float inLargeRangeCounter = 0f;
    public boolean inFrustum = false;
    public boolean inRange = true;
    public boolean inLargeRange = true;

    public BoundingBox boundingBox;

    public boolean skip = false;

    public Matrix4 transform = new Matrix4();
    public Matrix4 inverseTransform = new Matrix4();

    public String name;

    // Tmp variables
    protected static Vector3 tmp = new Vector3();
    protected static Vector3 tmp2 = new Vector3();
    protected static Vector3 tmp3 = new Vector3();
    protected static Vector3 tmp4 = new Vector3();
    protected static Ray tmpRay = new Ray(new Vector3(), new Vector3());

    protected boolean allGridsInFrustum;

    public boolean visible = true;
    public boolean solid = true;
    public boolean ableToCollide = true;

    protected WrappedInteger gridCount;
    public WrappedInteger references;
    public WrappedBoolean isLoading;

    public Bundle() {
    }

    public abstract void init(String name, boolean load);

    public abstract Bundle copy();

    public abstract void clear();

    //public abstract void init(String name, boolean testGenerate, boolean load);

    //public abstract void init(GridBundle bundle, boolean dontInstantiate);

    protected abstract Bundle instantiateCopy();

    public void updateMatrix() {
        inverseTransform.set(transform).inv();

        transformBuffer.rewind();
        transformBuffer.put(transform.val);
        transformBuffer.rewind();

        inverseTransformBuffer.rewind();
        inverseTransformBuffer.put(inverseTransform.val);
        inverseTransformBuffer.rewind();
    }

    public abstract Grid getGrid(int x, int y, int z);
    public abstract Grid getGridSafe(int x, int y, int z);

    public int getVoxel(Grid grid,
                        int tx, int ty, int tz,
                        int sx, int sy, int sz,
                        int dx, int dy, int dz) {
        int gx = grid.x, gy = grid.y, gz = grid.z;

        sx += dx;
        if (sx < 0)
            tx += sx / Config.TINY_GRID_SIZE - 1;
        else
            tx += sx / Config.TINY_GRID_SIZE;
        sx %= Config.TINY_GRID_SIZE;
        if (sx < 0)
            sx += Config.TINY_GRID_SIZE;

        if (tx < 0)
            gx += tx / Config.GRID_SIZE - 1;
        else
            gx += tx / Config.GRID_SIZE;
        tx %= Config.GRID_SIZE;
        if (tx < 0)
            tx += Config.GRID_SIZE;


        //y
        sy += dy;
        if (sy < 0)
            ty += sy / Config.TINY_GRID_SIZE - 1;
        else
            ty += sy / Config.TINY_GRID_SIZE;
        sy %= Config.TINY_GRID_SIZE;
        if (sy < 0)
            sy += Config.TINY_GRID_SIZE;

        if (ty < 0)
            gy += ty / Config.GRID_SIZE - 1;
        else
            gy += ty / Config.GRID_SIZE;
        ty %= Config.GRID_SIZE;
        if (ty < 0)
            ty += Config.GRID_SIZE;

        //z
        sz += dz;
        if (sz < 0)
            tz += sz / Config.TINY_GRID_SIZE - 1;
        else
            tz += sz / Config.TINY_GRID_SIZE;
        sz %= Config.TINY_GRID_SIZE;
        if (sz < 0)
            sz += Config.TINY_GRID_SIZE;

        if (tz < 0)
            gz += tz / Config.GRID_SIZE - 1;
        else
            gz += tz / Config.GRID_SIZE;
        tz %= Config.GRID_SIZE;
        if (tz < 0)
            tz += Config.GRID_SIZE;

        if (gx != grid.x || gy != grid.y || gz != grid.z)
            grid = getGrid(gx, gy, gz);

        if (grid == null)
            return 0xffffffff;

        TinyGrid tiny = grid.getTinyGrid(tx, ty, tz);
        if (tiny == null)
            return 0xffffffff;

        return tiny.data[sx][sy][sz];
    }

    public abstract Grid createGrid(int x, int y, int z, boolean updateBoundingBox);

    public abstract void removeGrid(int x, int y, int z);

    public abstract int sizeInMB();

    public abstract void upload();

    public abstract void makeShadows();

    protected int shadowBuildingCount = 0;
    public boolean isBuildingShadows() {
        return shadowBuildingCount > 0;
    }

    Grid[] hasToBeRemoved = new Grid[10];
    int hasToBeRemovedSize = 0;
    public void cleanUpEmptyGrids() {
        if (hasToBeRemovedSize > 0) {
            for (int i = 0; i < hasToBeRemovedSize; i++) {
                Grid grid = hasToBeRemoved[i];
                removeGrid(grid.x, grid.y, grid.z);
                LogHandler.log("Grid is empty and will be removed.");
                hasToBeRemoved[i] = null;
            }
            hasToBeRemovedSize = 0;
            updateBoundingBox();
            if (!boundingBox.isValid()) {
                LogHandler.log("Bundle empty: " + name);
            }
        }
    }


    public boolean gridInFrustumShadow(Grid grid, PerspectiveCamera camera, float lod) {
        if (!grid.checkBounds) {
            return false;
        }

        tmp.set(camera.position);
        tmp.mul(inverseTransform);
        tmp.mul(grid.inverseTransform);

        if (!SimpleMath.sphereAabbOverlap(grid.boundingBox, tmp, lod)) {
            return false;
        } else {
            return true;
        }
    }

    public boolean gridInFrustum(Grid grid, PerspectiveCamera camera, float lod) {
        if (!grid.checkBounds) {
            return false;
        }

        if (allGridsInFrustum) {
            return true;
        }

        tmp.set(camera.position);
        tmp.mul(inverseTransform);
        tmp.mul(grid.inverseTransform);

        if (!SimpleMath.sphereAabbOverlap(grid.boundingBox, tmp, lod)) {
            return false;
        }

        float offset = .5f;
        tmp3.set(((float)grid.x + offset) * Config.GRID_SIZE, ((float)grid.y + offset) * Config.GRID_SIZE, ((float)grid.z + offset) * Config.GRID_SIZE);
        tmp3.mul(transform);

        return camera.frustum.sphereInFrustum(tmp3, Config.GRID_CORNER_LENGTH);
    }

    public int getGridCount() {
        return gridCount.get();
    }

    static Frustum tmpFrustum = new Frustum();
    Matrix4 tmpMatrix = new Matrix4();
    Matrix4 tmpMatrix2 = new Matrix4();
    public boolean bundleInFrustum(PerspectiveCamera camera, float lod) {
        inLargeRangeCounter -= Time.getDelta();
        if (inLargeRangeCounter < 0f) {
            inLargeRangeCounter = Config.FRUSTRUM_IN_RANGE_LARGE;
            if (!boundingBox.isValid()) {
                inLargeRange = false;
                return inLargeRange;
            } else {
                tmp.set(camera.position);
                tmp.mul(inverseTransform);

                if (!SimpleMath.sphereAabbOverlap(boundingBox, tmp, lod + 100f)) {
                    inLargeRange = false;
                    return inLargeRange;
                } else {
                    inLargeRange = true;
                }
            }
        } else if (!inLargeRange) {
            return false;
        }

        inRangeCounter -= Time.getDelta();
        if (inRangeCounter < 0f) {
            inRangeCounter = Config.FRUSTRUM_IN_RANGE;
            if (!boundingBox.isValid()) {
                inRange = false;
                return inRange;
            } else {
                tmp.set(camera.position);
                tmp.mul(inverseTransform);

                if (!SimpleMath.sphereAabbOverlap(boundingBox, tmp, lod)) {
                    inRange = false;
                    return inRange;
                } else {
                    inRange = true;
                }
            }
        } else if (!inRange) {
            return false;
        }

        frustumCulledCounter -= Time.getDelta();
        if (frustumCulledCounter < 0f) {
            frustumCulledCounter = Config.FRUSTRUM_IN_SIGHT;
        } else {
            return inFrustum;
        }

        if (getGridCount() == 1) {
            allGridsInFrustum = false;
            inFrustum = true;
            return inFrustum;
        }

        Vector3 dir = tmp;
        Vector3 pos = tmp2;
        Vector3 up = tmp3;

        dir.set(camera.direction);
        dir.rot(inverseTransform);

        pos.set(camera.position);
        pos.mul(inverseTransform);

        up.set(camera.up);
        up.rot(inverseTransform);

        tmpMatrix.set(camera.projection);
        tmpMatrix2.setToLookAt(pos, tmp4.set(pos).add(dir), up);
        Matrix4.mul(tmpMatrix.val, tmpMatrix2.val);

        Matrix4.inv(tmpMatrix.val);
        tmpFrustum.update(tmpMatrix);

        allGridsInFrustum = allGridsInFrustrumFast(boundingBox, tmpFrustum);

        boolean result = true;
        if (!allGridsInFrustum) {
            result = tmpFrustum.boundsInFrustum(boundingBox);
        }

        inFrustum = result;
        return inFrustum;
    }

    public boolean allGridsInFrustrumFast(BoundingBox boundingBox, Frustum frustum) {
        for (Vector3 p : boundingBox.getCorners()) {
            if (!frustum.pointInFrustum(p)) {
                return false;
            }
        }

        return true;
    }

    private FloatBuffer transformBuffer = BufferUtils.newFloatBuffer(16);
    private FloatBuffer inverseTransformBuffer = BufferUtils.newFloatBuffer(16);

    public int drawTerrain(int x, int y, int z, PerspectiveCamera camera, ShaderProgram shader, float lod, int uniform, boolean doSetMatrix) {
        Grid grid = getGridSafe(x, y, z);
        return drawTerrain(grid, camera, shader, lod, uniform, doSetMatrix);
    }

    int transformUniformArray[] = {-1,-1,-1, -1};
    int inverseTransformUniformArray[] = {-1,-1,-1,-1};
    int gridTransformUniformArray[] = {-1,-1,-1,-1};

    public int drawTerrain(Grid grid, PerspectiveCamera camera, ShaderProgram shader, float lod, int uniform, boolean doSetMatrix) {
        if (grid == null) {
            return 0;
        } else if (!grid.checkBounds) {
            if (hasToBeRemoved.length > hasToBeRemovedSize) {
                hasToBeRemoved[hasToBeRemovedSize] = grid;
                hasToBeRemovedSize++;
            }
            return 0;
        } else if (!grid.hasPalettes()) {
            return 0;
        }

        LogHandler.exitOnGLError();
        if (!gridInFrustum(grid, camera, lod)) {//tmp2.dot(camera.direction) < .5f) { //
            return 0;
        }

        LogHandler.exitOnGLError();

        if (doSetMatrix) {
            if (transformUniformArray[uniform] < 0) {
                transformUniformArray[uniform] = shader.getUniformLocation("worldTrans");
            }
            Gdx.gl.glUniformMatrix4fv(transformUniformArray[uniform], 1, false, transformBuffer);
            if (inverseTransformUniformArray[uniform] < 0) {
                inverseTransformUniformArray[uniform] = shader.getUniformLocation("inverseWorldTrans");
            }
            Gdx.gl.glUniformMatrix4fv(inverseTransformUniformArray[uniform], 1, false, inverseTransformBuffer);
        }

        LogHandler.exitOnGLError();
        Gdx.graphics.getGL20().glBindTexture(GL20.GL_TEXTURE_2D, grid.getPalette().getTextureObjectHandle());

        shader.setUniformf("paletteSize", grid.getPalettesTotal());

        if (gridTransformUniformArray[uniform] < 0) {
            gridTransformUniformArray[uniform] = shader.getUniformLocation("gridTrans");
        }
        Gdx.gl.glUniformMatrix4fv(gridTransformUniformArray[uniform], 1, false, grid.transformBuffer);

        LogHandler.exitOnGLError();
        grid.render(shader);

        LogHandler.exitOnGLError();
        return grid.getPalettesTotal();
    }

    public int drawTerrainShadow(int x, int y, int z, PerspectiveCamera camera, ShaderProgram shader, float lod, int uniform, boolean doSetMatrix) {
        Grid grid = getGrid(x, y, z);
        return drawTerrainShadow(grid, camera, shader, lod, uniform, doSetMatrix);
    }

    public int drawTerrainShadow(Grid grid, PerspectiveCamera camera, ShaderProgram shader, float lod, int uniform, boolean doSetMatrix) {
        if (grid == null) {
            return 0;
        } else if (!grid.checkBounds) {
            return 0;
        } else if (!grid.hasPalettes()) {
            return 0;
        }

        LogHandler.exitOnGLError();
        if (!gridInFrustumShadow(grid, camera, lod)) {
            return 0;
        }

        LogHandler.exitOnGLError();

        if (doSetMatrix) {
            if (transformUniformArray[uniform] < 0) {
                transformUniformArray[uniform] = shader.getUniformLocation("worldTrans");
            }
            Gdx.gl.glUniformMatrix4fv(transformUniformArray[uniform], 1, false, transformBuffer);
        }

        if (gridTransformUniformArray[uniform] < 0) {
            gridTransformUniformArray[uniform] = shader.getUniformLocation("gridTrans");
        }
        Gdx.gl.glUniformMatrix4fv(gridTransformUniformArray[uniform], 1, false, grid.transformBuffer);

        LogHandler.exitOnGLError();
        grid.render(shader);

        LogHandler.exitOnGLError();
        return grid.getPalettesTotal();
    }

    public abstract Grid getFirstGrid();

    public abstract void drawShadows(PerspectiveCamera camera, ShaderProgram shader, float lod, int uniform);

    public abstract void drawAll(PerspectiveCamera camera, ShaderProgram shader, float lod, int uniform);

    private int drawLayerOne(Grid grid, int halfDepth, PerspectiveCamera camera, ShaderProgram shader, float lod, int uniform, boolean setMatrix) {
        if (grid == null)
            return 0;

        tmp.set(camera.position);
        tmp.mul(inverseTransform);
        tmp.scl(1f / (float) Config.GRID_SIZE);

        int viewpointX = MathUtils.floor(tmp.x);
        int viewpointY = MathUtils.floor(tmp.y);
        int viewpointZ = MathUtils.floor(tmp.z);

        boolean b1 = grid.x <= viewpointX + halfDepth && grid.x >= viewpointX - halfDepth;
        boolean b2 = grid.y <= viewpointY + halfDepth && grid.y >= viewpointY - halfDepth;
        boolean b3 = grid.z <= viewpointZ + halfDepth && grid.z >= viewpointZ - halfDepth;

        boolean c1 = grid.x == viewpointX + halfDepth || grid.x == viewpointX - halfDepth;
        boolean c2 = grid.y == viewpointY + halfDepth || grid.y == viewpointY - halfDepth;
        boolean c3 = grid.z == viewpointZ + halfDepth || grid.z == viewpointZ - halfDepth;

        if ((c1 && b2 && b3) || (c2 && b1 && b3) || (c3 && b2 && b1)) {
            return drawTerrain(grid, camera, shader, lod, uniform, true);
        }
        return 0;
    }

    public int drawLayer(int halfDepth, PerspectiveCamera camera, ShaderProgram shader, float lod, int uniform) {
        return drawLayer(halfDepth, camera, shader, lod, uniform, true);
    }

    public int drawLayer(int halfDepth, PerspectiveCamera camera, ShaderProgram shader, float lod, int uniform, boolean setMatrix) {
        if (getGridCount() == 1) {
            return drawLayerOne(getFirstGrid(), halfDepth, camera, shader, lod, uniform, setMatrix);
        }

        tmp.set(camera.position);
        tmp.mul(inverseTransform);
        tmp.scl(1f / (float) Config.GRID_SIZE);

        int viewpointX = MathUtils.floor(tmp.x);
        int viewpointY = MathUtils.floor(tmp.y);
        int viewpointZ = MathUtils.floor(tmp.z);

        int drawn = 0;

        boolean doSetMatrix = setMatrix;

        int tmpX = (int)(boundingBox.min.x / Config.GRID_SIZE_F);
        int tmpY = (int)(boundingBox.min.y / Config.GRID_SIZE_F);
        int tmpZ = (int)(boundingBox.min.z / Config.GRID_SIZE_F);

        int tmp2X = (int)(boundingBox.max.x / (float)Config.GRID_SIZE);
        int tmp2Y = (int)(boundingBox.max.y / (float)Config.GRID_SIZE);
        int tmp2Z = (int)(boundingBox.max.z / (float)Config.GRID_SIZE);

        if (halfDepth == 0) {
            if (viewpointX < tmpX || viewpointY < tmpY || viewpointZ < tmpZ ||
                    viewpointX >= tmp2X || viewpointY >= tmp2Y || viewpointZ >= tmp2Z) {
                return 0;
            }
            int add = drawTerrain(viewpointX, viewpointY, viewpointZ, camera, shader, lod, uniform, doSetMatrix);
            drawn += add;
            return drawn;
        }

        int depth = halfDepth * 2;

        int start = -halfDepth;
        int end = start + depth + 1;

        int startX = start + viewpointX, startY = start + viewpointY, startZ = start + viewpointZ;
        int endX = end + viewpointX, endY = end + viewpointY, endZ = end + viewpointZ;

        int oStartX = startX, oStartY = startY, oStartZ = startZ;
        int oEndX = endX, oEndY = endY, oEndZ = endZ;

        startX = MathUtils.clamp(startX, tmpX, tmp2X - 1);
        startY = MathUtils.clamp(startY, tmpY, tmp2Y - 1);
        startZ = MathUtils.clamp(startZ, tmpZ, tmp2Z - 1);

        //endX = MathUtils.clamp(endX, tmpX, tmp2X);
        endY = MathUtils.clamp(endY, tmpY, tmp2Y);
        endZ = MathUtils.clamp(endZ, tmpZ, tmp2Z);

        int startXPlus1 = MathUtils.clamp(oStartX + 1, tmpX, tmp2X);
        int startZPlus1 = MathUtils.clamp(oStartZ + 1, tmpZ, tmp2Z);

        int endXMin1 = MathUtils.clamp(oEndX - 1, tmpX, tmp2X);
        //int endYMin1 = MathUtils.clamp(oEndY - 1, tmpY, tmp2Y);
        int endZMin1 = MathUtils.clamp(oEndZ - 1, tmpZ, tmp2Z);

        int aEndXMin1 = MathUtils.clamp(oEndX - 1, tmpX, tmp2X - 1);
        int aEndYMin1 = MathUtils.clamp(oEndY - 1, tmpY, tmp2Y - 1);
        int aEndZMin1 = MathUtils.clamp(oEndZ - 1, tmpZ, tmp2Z - 1);

        int tz;
        if (oStartZ == startZ) {
            tz = startZ;
            for (int x = startXPlus1; x < endXMin1; x++) {
                for (int y = startY; y < endY; y++) {
                    int add = drawTerrain(x, y, tz, camera, shader, lod, uniform, doSetMatrix);
                    if (add > 0) {
                        doSetMatrix = false;
                        drawn += add;
                    }
                }
            }
        }
        if (oEndZ - 1 == aEndZMin1) {
            tz = aEndZMin1;
            for (int x = startXPlus1; x < endXMin1; x++) {
                for (int y = startY; y < endY; y++) {
                    int add = drawTerrain(x, y, tz, camera, shader, lod, uniform, doSetMatrix);
                    if (add > 0) {
                        doSetMatrix = false;
                        drawn += add;
                    }
                }
            }
        }
        int tx;
        if (oStartX == startX) {
            tx = startX;
            for (int z = startZ; z < endZ; z++) {
                for (int y = startY; y < endY; y++) {
                    int add = drawTerrain(tx, y, z, camera, shader, lod, uniform, doSetMatrix);
                    if (add > 0) {
                        doSetMatrix = false;
                        drawn += add;
                    }
                }
            }
        }
        if (oEndX - 1 == aEndXMin1) {
            tx = aEndXMin1;
            for (int z = startZ; z < endZ; z++) {
                for (int y = startY; y < endY; y++) {
                    int add = drawTerrain(tx, y, z, camera, shader, lod, uniform, doSetMatrix);
                    if (add > 0) {
                        doSetMatrix = false;
                        drawn += add;
                    }
                }
            }
        }
        int ty;
        if (oStartY == startY) {
            ty = startY;
            for (int x = startXPlus1; x < endXMin1; x++) {
                for (int z = startZPlus1; z < endZMin1; z++) {
                    int add = drawTerrain(x, ty, z, camera, shader, lod, uniform, doSetMatrix);
                    if (add > 0) {
                        doSetMatrix = false;
                        drawn += add;
                    }
                }
            }
        }
        if (oEndY - 1 == aEndYMin1) {
            ty = aEndYMin1;
            for (int x = startXPlus1; x < endXMin1; x++) {
                for (int z = startZPlus1; z < endZMin1; z++) {
                    int add = drawTerrain(x, ty, z, camera, shader, lod, uniform, doSetMatrix);
                    if (add > 0) {
                        doSetMatrix = false;
                        drawn += add;
                    }
                }
            }
        }

        return drawn;
    }

    public void removeVoxel(Vector3 position, boolean loadImmediately) {
        if (solid || !inRange || !inLargeRange)
            return;

        tmp2.set(position);
        tmp2.mul(inverseTransform);
        removeVoxelLocal(tmp2, loadImmediately);
    }

    public void removeVoxelLocal(Vector3 position, boolean loadImmediately) {
        if (solid || !inRange || !inLargeRange)
            return;

        tmp.set(position);

        int x = MathUtils.floor(tmp.x / (float)Config.GRID_SIZE);
        int y = MathUtils.floor(tmp.y / (float)Config.GRID_SIZE);
        int z = MathUtils.floor(tmp.z / (float)Config.GRID_SIZE);

        Grid grid = getGridSafe(x, y, z);
        if (grid != null) {
            grid.removeVoxelAt(tmp);

            if (loadImmediately) {
                grid.loadPalette();
                updateBoundingBox();
            }
        }
    }

    public void addVoxel(Vector3 position, int r, int g, int b, boolean loadImmediately) {
        if (solid)
            return;

        tmp2.set(position);
        tmp2.mul(inverseTransform);
        addVoxelLocal(tmp2, r, g, b, loadImmediately);
    }

    public void addVoxelLocal(Vector3 position, int r, int g, int b, boolean loadImmediately) {
        if (solid)
            return;

        tmp.set(position);

        int color;
        if (r >= 255 && g >= 255 && b >= 255) {
            color = 0xfffffffe;
        } else {
            tmp2.set(r, g, b);
            color = SimpleMath.RGB888ToInt(tmp2);
        }

        int x = MathUtils.floor(tmp.x / (float)Config.GRID_SIZE);
        int y = MathUtils.floor(tmp.y / (float)Config.GRID_SIZE);
        int z = MathUtils.floor(tmp.z / (float)Config.GRID_SIZE);

        Grid grid = createGrid(x, y, z, false);
        if (grid == null) {
            return;
        }
        grid.paintVoxelAt(tmp, color);

        if (loadImmediately) {
            grid.loadPalette();
            updateBoundingBox();
        }
    }

    public void addBrush(Vector3 position, int radius, int r, int g, int b, BrushUtils.Brush brush) {
        if (solid)
            return;

        tmp2.set(position);
        tmp2.mul(inverseTransform);
        addBrushLocal(tmp2, radius, r, g, b, brush);
    }

    public void addBrushLocal(Vector3 position, int radius, int r, int g, int b, BrushUtils.Brush brush) {
        if (solid)
            return;

        tmp.set(position);

        int color;
        if (r >= 255 && g >= 255 && b >= 255) {
            color = 0xfffffffe;
        } else {
            color = SimpleMath.RGB888ToInt(r, g, b);
        }

        float frad = (float)radius / (float)Config.TINY_GRID_SIZE;

        int startTinyPointX = MathUtils.floor((tmp.x - frad) / Config.GRID_SIZE);
        int startTinyPointY = MathUtils.floor((tmp.y - frad) / Config.GRID_SIZE);
        int startTinyPointZ = MathUtils.floor((tmp.z - frad) / Config.GRID_SIZE);

        int endTinyPointX = MathUtils.ceil((tmp.x + frad) / Config.GRID_SIZE);
        int endTinyPointY = MathUtils.ceil((tmp.y + frad) / Config.GRID_SIZE);
        int endTinyPointZ = MathUtils.ceil((tmp.z + frad) / Config.GRID_SIZE);

        for (int x = startTinyPointX; x < endTinyPointX; x++) {
            for (int y = startTinyPointY; y < endTinyPointY; y++) {
                for (int z = startTinyPointZ; z < endTinyPointZ; z++) {
                    Grid grid = createGrid(x, y, z, false);
                    if (grid == null) {
                        continue;
                    }

                    grid.paintAt(tmp, radius, color, brush);
                    grid.loadPalette();
                }
            }
        }

        updateBoundingBox();
    }

    public void removeBrush(Vector3 position, int radius, BrushUtils.Brush brush) {
        if (solid || !inRange || !inLargeRange)
            return;

        tmp.set(position);
        tmp.mul(inverseTransform);

        if (!SimpleMath.sphereAabbOverlap(boundingBox, tmp, radius))
            return;

        float frad = (float)radius / (float)Config.TINY_GRID_SIZE;

        int startTinyPointX = MathUtils.floor((tmp.x - frad) / Config.GRID_SIZE);
        int startTinyPointY = MathUtils.floor((tmp.y - frad) / Config.GRID_SIZE);
        int startTinyPointZ = MathUtils.floor((tmp.z - frad) / Config.GRID_SIZE);

        int endTinyPointX = MathUtils.ceil((tmp.x + frad) / Config.GRID_SIZE);
        int endTinyPointY = MathUtils.ceil((tmp.y + frad) / Config.GRID_SIZE);
        int endTinyPointZ = MathUtils.ceil((tmp.z + frad) / Config.GRID_SIZE);

        int count = 0;

        for (int x = startTinyPointX; x < endTinyPointX; x++) {
            for (int y = startTinyPointY; y < endTinyPointY; y++) {
                for (int z = startTinyPointZ; z < endTinyPointZ; z++) {

                    Grid grid = getGridSafe(x, y, z);
                    if (grid == null) {
                        continue;
                    }

                    grid.removePaintAt(tmp, radius, brush);
                    grid.loadPalette();
                }
            }
        }

        if (count > 0)
            LogHandler.log("Placed " + count + " grids");
    }

    public boolean collidesWith(Vector3 point) {
        if (!visible || !inRange || !inLargeRange || !ableToCollide)
            return false;

        tmp.set(point);
        tmp.mul(inverseTransform);

        return collidesWithLocal(tmp);
    }

    public boolean collidesWithLocal(Vector3 point) {
        int viewpointX = MathUtils.floor(point.x / (float) Config.GRID_SIZE);
        int viewpointY = MathUtils.floor(point.y / (float) Config.GRID_SIZE);
        int viewpointZ = MathUtils.floor(point.z / (float) Config.GRID_SIZE);

        Grid grid = getGridSafe(viewpointX, viewpointY, viewpointZ);
        if (grid == null)
            return false;

        return grid.pointInBoundingBox(point);
    }

    public boolean collidesSphereWith(Vector3 point, float radius) {
        if (!visible || !inRange || !inLargeRange || !ableToCollide)
            return false;

        tmp.set(point);
        tmp.mul(inverseTransform);

        if (!SimpleMath.sphereAabbOverlap(boundingBox, tmp, radius)) {
            return false;
        }

        int startTinyPointX = MathUtils.floor((tmp.x - radius) / Config.GRID_SIZE);
        int startTinyPointY = MathUtils.floor((tmp.y - radius) / Config.GRID_SIZE);
        int startTinyPointZ = MathUtils.floor((tmp.z - radius) / Config.GRID_SIZE);

        int endTinyPointX = MathUtils.ceil((tmp.x + radius) / Config.GRID_SIZE) + 1;
        int endTinyPointY = MathUtils.ceil((tmp.y + radius) / Config.GRID_SIZE) + 1;
        int endTinyPointZ = MathUtils.ceil((tmp.z + radius) / Config.GRID_SIZE) + 1;

        for (int x = startTinyPointX; x < endTinyPointX; x++) {
            for (int y = startTinyPointY; y < endTinyPointY; y++) {
                for (int z = startTinyPointZ; z < endTinyPointZ; z++) {

                    Grid grid = getGridSafe(x, y, z);
                    if (grid == null) {
                        continue;
                    }

                    if (grid.collidesSphereWith(tmp, radius))
                        return true;
                }
            }
        }
        return false;
    }

    public abstract Grid collidesWith(Ray ray, Vector3 intersection, float maxDistance);

    public abstract void saveAll();

    public void loadAll() {
        isLoading.set(true);

        FileHandle file = Gdx.files.internal("models/" + name + "_" + Config.TINY_GRID_SIZE + "_" + Config.GRID_SIZE + (Config.SAVE_SHADE ? ".rlelvl" : ".rlergb" ));

        if (!file.exists()) {
            boolean state = solid;

            solid = false;

            boolean hasLoaded = true;
            if (!MeshImporter.read("models/" + name + ".g3dj", this, new Vector3(0f, 0f, 0f), 128, 10, 10)) {
                if (!MeshImporter.read("models/" + name + ".g3db", this, new Vector3(0f, 0f, 0f), 128, 10, 10)) {
                    if (!MeshImporter.read("models/" + name + ".obj", this, new Vector3(0f, 0f, 0f), 128, 10, 10)) {
                        if (!BinvoxImporter.readBinvox("models/" + name + ".binvox", this, new Vector3(0f, 0f, 0f), 128, 255, 128)) {
                            hasLoaded = false;
                        }
                    }
                }
            }

            solid = state;


            if (hasLoaded) {
                if (Config.SAVE_SHADE) {
                    upload();
                    updateBoundingBox();
                } else
                    makeShadows();

                isLoading.set(false);
            }
        } else {
            clear();

            long start = System.currentTimeMillis();

            try {
                InputStream inExp = file.read();

                int count = StreamUtil.getInt(inExp);

                LogHandler.log("Count loading grids: " + count);

                int it = 0;
                while (inExp.available() > 0) {
                    int x = StreamUtil.getInt(inExp);
                    int y = StreamUtil.getInt(inExp);
                    int z = StreamUtil.getInt(inExp);

                    Grid grid = createGrid(x, y, z, false);
                    if (grid == null) {
                        // This shouldn't happen, but I allow it for now..
                        Grid.skipLoad(inExp);
                        //throw new IOException();
                    } else {
                        grid.load(inExp);
                    }

                    it++;

                    if (it % 10 == 0) {
                        LogHandler.log("Loading: " + ((float) it * 100f / (float) count));
                    }
                }
                inExp.close();
            } catch (IOException ex) {
                LogHandler.log("Failure loading!");
            }

            LogHandler.log("Took " + (float) (System.currentTimeMillis() - start) / 1000f + " seconds");

            if (Config.SAVE_SHADE) {
                upload();
                updateBoundingBox();
            } else
                makeShadows();

            isLoading.set(false);
        }
    }

    public abstract void updateBoundingBox();

    public abstract void dispose();
}
