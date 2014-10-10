package com.toet.TinyVoxel.Renderer.Bundles;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Pool;
import com.toet.TinyVoxel.Config;
import com.toet.TinyVoxel.Debug.LogHandler;
import com.toet.TinyVoxel.Renderer.BlockBuilder;
import com.toet.TinyVoxel.Renderer.Tools.BrushUtils;
import com.toet.TinyVoxel.Util.*;
import com.toet.TinyVoxel.Util.SimpleMath;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.FloatBuffer;

import static com.badlogic.gdx.graphics.GL20.*;

/**
 * Created by Kajos on 20-1-14.
 */

public class Grid implements Disposable {
    public Bundle owner;

    public Matrix4 transform = new Matrix4();
    public Matrix4 inverseTransform = new Matrix4();
    public FloatBuffer transformBuffer = BufferUtils.newFloatBuffer(16);
    public FloatBuffer inverseTransformBuffer = BufferUtils.newFloatBuffer(16);
    public int x, y, z;

    public BoundingBox boundingBox = new BoundingBox();
    public boolean checkBounds = false;

    protected Mesh mesh;
    protected int floatCount;
    protected int verticesCount = 0;

    protected static Pool<TinyGrid> tinyGridPool = new Pool<TinyGrid>() {

        @Override
        protected TinyGrid newObject() {
            return new TinyGrid();
        }
    };

    protected static boolean[][][] grids = new boolean[Config.GRID_SIZE][Config.GRID_SIZE][Config.GRID_SIZE];

    protected int[][][] prevPaletteIds = new int[Config.GRID_SIZE][Config.GRID_SIZE][Config.GRID_SIZE];
    protected TinyGrid[][][] gridContainers = new TinyGrid[Config.GRID_SIZE][Config.GRID_SIZE][Config.GRID_SIZE];

    protected NonBackedTexture palette = null;
    protected int palettesTotal = 0;

    protected boolean allowLoadPalette = true;

    protected static Vector3 tmp = new Vector3(), tmp2 = new Vector3(), tmp3 = new Vector3(), tmp4 = new Vector3();

    public final FloatBuffer matrix = BufferUtils.newFloatBuffer(16);

    public Grid() {
    }

    public int getVerticesCount() {
        return  verticesCount;
    }

    public void findAllShadows() {
        for (int tx = 0; tx < Config.GRID_SIZE; tx++)
            for (int ty = 0; ty < Config.GRID_SIZE; ty++)
                for (int tz = 0; tz < Config.GRID_SIZE; tz++) {
                    TinyGrid tiny = getTinyGrid(tx, ty, tz);
                    if (tiny != null) {
                        if (!tiny.shadowsUpToDate)
                            allowLoadPalette = true;

                        tiny.findShadows(this, tx, ty, tz);
                    }
                }
    }

    public int getPalettesTotal() {
        return palettesTotal;
    }

    public GLTexture getPalette() {
        return palette;
    }

    public int getByteSize() {
        int size = 0; // 4 floats, 4 bytes each
        if (palette != null) {
            size += palette.getWidth() * palette.getHeight() * 4;
        }
        if (mesh != null) {
            size += mesh.getNumVertices() * Config.ATTRIBUTES_SIZE * 4;
        }
        return size;
    }

    private static TinyGrid tmpTinyGrid = new TinyGrid();
    public void paintAt(Vector3 position, int radius, int color, BrushUtils.Brush brush) {
        int radiusTinyHalf = radius;
        int radiusTiny = radiusTinyHalf * 2;

        tmp4.set(position);
        tmp4.scl(-1f);
        tmp4.scl(Config.TINY_GRID_SIZE);

        int bx = (int)tmp4.x + x * Config.GRID_SIZE * Config.TINY_GRID_SIZE + radiusTinyHalf;
        int by = (int)tmp4.y + y * Config.GRID_SIZE * Config.TINY_GRID_SIZE + radiusTinyHalf;
        int bz = (int)tmp4.z + z * Config.GRID_SIZE * Config.TINY_GRID_SIZE + radiusTinyHalf;

        for (int tx = 0; tx < Config.GRID_SIZE; tx++)
            for (int ty = 0; ty < Config.GRID_SIZE; ty++) {
                for (int tz = 0; tz < Config.GRID_SIZE; tz++) {
                    TinyGrid cont = gridContainers[tx][ty][tz];
                    boolean tinyExists = cont != null;
                    if(!tinyExists) {
                        cont = tmpTinyGrid;
                    }

                    boolean isEmpty = true;
                    boolean hasPainted = false;

                    int ax = bx + tx * Config.TINY_GRID_SIZE;
                    int ay = by + ty * Config.TINY_GRID_SIZE;
                    int az = bz + tz * Config.TINY_GRID_SIZE;

                    if (ax >= radiusTiny || ay >= radiusTiny || az >= radiusTiny ||
                            ax + Config.TINY_GRID_SIZE < 0 || ay + Config.TINY_GRID_SIZE < 0 || az + Config.TINY_GRID_SIZE < 0) {
                        continue;
                    }

                    for (int sx = 0; sx < Config.TINY_GRID_SIZE; sx++) {
                        for (int sy = 0; sy < Config.TINY_GRID_SIZE; sy++)
                            for (int sz = 0; sz < Config.TINY_GRID_SIZE; sz++) {
                                if (tinyExists && cont.data[sx][sy][sz] != 0xffffffff) {
                                    isEmpty = false;
                                    continue;
                                }

                                int shadow = brush.getShadow(ax + sx, ay + sy, az + sz, radiusTiny);
                                if (shadow != -1) {
                                    int result = SimpleMath.setShadow(shadow, color);

                                    if (!tinyExists || cont.data[sx][sy][sz] != result) {
                                        cont.data[sx][sy][sz] = result;
                                        hasPainted = true;
                                    }

                                    isEmpty = false;
                                } else if (!tinyExists) {
                                    cont.data[sx][sy][sz] = 0xffffffff;
                                }
                            }
                    }

                    if (!hasPainted) {
                        if (!tinyExists) {
                            continue;
                        } else {
                            isEmpty = false;
                        }
                    } else {
                        cont.notUpToDate = true;
                        cont.shadowsUpToDate = false;
                    }

                    if(tinyExists) {
                        if (isEmpty) {
                            tinyGridPool.free(cont);
                            gridContainers[tx][ty][tz] = null;

                            allowLoadPalette = true;
                            continue;
                        }
                    } else {
                        if (isEmpty) {
                            continue;
                        }

                        TinyGrid create = tinyGridPool.obtain();
                        cont.copyTo(create);
                        gridContainers[tx][ty][tz] = create;
                        cont = create;
                    }

                    cont.notUpToDate = true;
                    cont.shadowsUpToDate = false;
                    cont.findBoundaryBox();
                    cont.findFullSides();
                    allowLoadPalette = true;
                }
            }

    }

    public void paintVoxelAt(Vector3 position, int color) {
        tmp.set(position);
        tmp.mul(inverseTransform);

        int tx = MathUtils.floor(tmp.x) % Config.GRID_SIZE;
        int ty = MathUtils.floor(tmp.y) % Config.GRID_SIZE;
        int tz = MathUtils.floor(tmp.z) % Config.GRID_SIZE;

        int sx = MathUtils.floor((tmp.x - (float)tx) * (float)Config.TINY_GRID_SIZE);
        int sy = MathUtils.floor((tmp.y - (float)ty) * (float)Config.TINY_GRID_SIZE);
        int sz = MathUtils.floor((tmp.z - (float)tz) * (float)Config.TINY_GRID_SIZE);

        TinyGrid cont = gridContainers[tx][ty][tz];
        boolean tinyExists = cont != null;
        if(!tinyExists) {
            cont = tinyGridPool.obtain();
            for (int dsx = 0; dsx < Config.TINY_GRID_SIZE; dsx++) {
                for (int dsy = 0; dsy < Config.TINY_GRID_SIZE; dsy++)
                    for (int dsz = 0; dsz < Config.TINY_GRID_SIZE; dsz++) {
                        cont.data[dsx][dsy][dsz] = 0xffffffff;
                    }
            }
            gridContainers[tx][ty][tz] = cont;
        }

        int result = color;//shade ? SimpleMath.shadeFunc(color, sx, sy, sz) : color;

        if (tinyExists && cont.data[sx][sy][sz] == result)
            return;

        cont.data[sx][sy][sz] = result;
        //cont.findShadow(this, tx, ty, tz, sx, sy, sz);
        cont.notUpToDate = true;
        cont.shadowsUpToDate = false;

        cont.findBoundaryBox();
        cont.findFullSides();
        allowLoadPalette = true;
    }

    public void removeVoxelAt(Vector3 position) {
        tmp.set(position);
        tmp.mul(inverseTransform);

        int tx = MathUtils.floor(tmp.x) % Config.GRID_SIZE;
        int ty = MathUtils.floor(tmp.y) % Config.GRID_SIZE;
        int tz = MathUtils.floor(tmp.z) % Config.GRID_SIZE;

        int sx = MathUtils.floor((tmp.x - (float)tx) * (float)Config.TINY_GRID_SIZE);
        int sy = MathUtils.floor((tmp.y - (float)ty) * (float)Config.TINY_GRID_SIZE);
        int sz = MathUtils.floor((tmp.z - (float)tz) * (float)Config.TINY_GRID_SIZE);

        TinyGrid cont = gridContainers[tx][ty][tz];
        boolean tinyExists = cont != null;

        if(!tinyExists)
            return;

        if (cont.data[sx][sy][sz] == 0xffffffff)
            return;

        cont.data[sx][sy][sz] = 0xffffffff;
        cont.notUpToDate = true;
        cont.shadowsUpToDate = false;

        cont.findBoundaryBox();
        cont.findFullSides();
        allowLoadPalette = true;
    }

    public void removePaintAt(Vector3 position, int radius, BrushUtils.Brush brush) {
        int radiusTinyHalf = radius;
        int radiusTiny = radius * 2;

        tmp4.set(position);
        tmp4.scl(-1f);
        tmp4.scl(Config.TINY_GRID_SIZE);

        int bx = (int)tmp4.x + x * Config.GRID_SIZE * Config.TINY_GRID_SIZE + radiusTinyHalf;
        int by = (int)tmp4.y + y * Config.GRID_SIZE * Config.TINY_GRID_SIZE + radiusTinyHalf;
        int bz = (int)tmp4.z + z * Config.GRID_SIZE * Config.TINY_GRID_SIZE + radiusTinyHalf;
        for (int tx = 0; tx < Config.GRID_SIZE; tx++)
            for (int ty = 0; ty < Config.GRID_SIZE; ty++) {
                for (int tz = 0; tz < Config.GRID_SIZE; tz++) {
                    TinyGrid cont = gridContainers[tx][ty][tz];
                    boolean tinyExists = cont != null;
                    if(!tinyExists) {
                        continue;
                    }

                    boolean isEmpty = true;

                    boolean hasPainted = false;

                    int ax = bx + tx * Config.TINY_GRID_SIZE;
                    int ay = by + ty * Config.TINY_GRID_SIZE;
                    int az = bz + tz * Config.TINY_GRID_SIZE;

                    if (ax >= radiusTiny || ay >= radiusTiny || az >= radiusTiny ||
                            ax + Config.TINY_GRID_SIZE < 0 || ay + Config.TINY_GRID_SIZE < 0 || az + Config.TINY_GRID_SIZE < 0) {
                        continue;
                    }

                    for (int sx = 0; sx < Config.TINY_GRID_SIZE; sx++) {
                        for (int sy = 0; sy < Config.TINY_GRID_SIZE; sy++)
                            for (int sz = 0; sz < Config.TINY_GRID_SIZE; sz++) {
                                if (brush.get(ax + sx, ay + sy, az + sz, radiusTiny)) {
                                    if (cont.data[sx][sy][sz] != 0xffffffff) {
                                        cont.data[sx][sy][sz] = 0xffffffff;
                                        hasPainted = true;
                                    }
                                }
                            }
                    }
                    if (hasPainted) {
                        outerloop:
                        for (int sx = 0; sx < Config.TINY_GRID_SIZE; sx++)
                            for (int sy = 0; sy < Config.TINY_GRID_SIZE; sy++)
                                for (int sz = 0; sz < Config.TINY_GRID_SIZE; sz++) {
                                    if (cont.data[sx][sy][sz] != 0xffffffff) {
                                        isEmpty = false;
                                        break outerloop;
                                    }
                                }

                        cont.notUpToDate = true;
                        cont.shadowsUpToDate = false;
                    } else {
                        isEmpty = false;
                    }

                    if (isEmpty) {
                        tinyGridPool.free(cont);
                        gridContainers[tx][ty][tz] = null;
                    } else {
                        cont.notUpToDate = true;
                        cont.shadowsUpToDate = false;
                        cont.findBoundaryBox();
                        cont.findFullSides();
                    }
                    allowLoadPalette = true;
                }
            }

    }

    public void init(Bundle owner, int x, int y, int z) {
        this.owner = owner;
        this.x = x;
        this.y = y;
        this.z = z;

        transform.idt();
        transform.translate(x * Config.GRID_SIZE, y * Config.GRID_SIZE, z * Config.GRID_SIZE);

        inverseTransform.set(transform);
        inverseTransform.inv();

        transformBuffer.rewind();
        transformBuffer.put(transform.val);
        transformBuffer.rewind();

        inverseTransformBuffer.rewind();
        inverseTransformBuffer.put(inverseTransform.val);
        inverseTransformBuffer.rewind();

        this.matrix.clear();
        BufferUtils.copy(transform.val, this.matrix, transform.val.length, 0);
    }

    public boolean loadPalette() {
        if (!allowLoadPalette) {
            return false;
        }
        allowLoadPalette = false;

        updateBoundingBox();

        int oldPalletSize = palettesTotal;

        if (mesh == null)
            mesh = new Mesh(Mesh.VertexDataType.VertexBufferObject, false, Config.MAX_VERTEX_SIZE, 0, createAttributes());

        createVertices();

        if (!hasPalettes())
            return false;

        int newPalletSize = palettesTotal;


        if (palette == null) {
            palette = new NonBackedTexture();
            palette.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            palette.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        }

        int[] freePalettes = new int[oldPalletSize];
        int countFree = 0;

        boolean cleanPalette = false;

        if (oldPalletSize != newPalletSize) {
            Config.get().rewindPalette();

            palette.setWidth(Config.TINY_GRID_TOTAL);
            palette.setHeight(newPalletSize);

            cleanPalette = true;
        } else {
            for (int gx = 0; gx < Config.GRID_SIZE; gx++)
                for (int gy = 0; gy < Config.GRID_SIZE; gy++)
                    for (int gz = 0; gz < Config.GRID_SIZE; gz++) {
                        if (prevPaletteIds[gx][gy][gz] >= 0) {
                            if (!grids[gx][gy][gz]) {
                                freePalettes[countFree] = prevPaletteIds[gx][gy][gz];
                                countFree++;
                            } else {
                                TinyGrid cont = getTinyGrid(gx, gy, gz);
                                if (cont.notUpToDate) {
                                    freePalettes[countFree] = prevPaletteIds[gx][gy][gz];
                                    countFree++;
                                }
                            }
                        }
                    }

            cleanPalette = false;
        }

        palette.bind();
        FloatBuffer vertices = mesh.getVerticesBuffer();


        int count = 0;
        int i = 0;
        for (int gx = 0; gx < Config.GRID_SIZE; gx++)
            for (int gy = 0; gy < Config.GRID_SIZE; gy++)
                for (int gz = 0; gz < Config.GRID_SIZE; gz++) {
                    if (grids[gx][gy][gz]) {
                        TinyGrid cont = gridContainers[gx][gy][gz];

                        int start = 0;
                        if (i != 0)
                            start = verticesPointer[i - 1];
                        int end = verticesPointer[i];
                        i++;

                        int paletteId;

                        if (!cleanPalette) {
                            if (!cont.notUpToDate) {
                                // already uploaded and not updated
                                paletteId = prevPaletteIds[gx][gy][gz];
                            } else {
                                countFree--;

                                paletteId = freePalettes[countFree];

                                count++;

                                Config.get().rewindSinglePalette();
                                for (int ty = 0; ty < Config.TINY_GRID_SIZE; ty++)
                                    for (int tz = 0; tz < Config.TINY_GRID_SIZE; tz++) {
                                        for (int tx = 0; tx < Config.TINY_GRID_SIZE; tx++) {
                                            int color = cont.data[tx][ty][tz];
                                            Config.get().putSinglePalette(color);
                                        }
                                    }

                                Config.get().uploadSinglePalette(paletteId);
                            }

                        } else {
                            paletteId = count;
                            count++;

                            for (int ty = 0; ty < Config.TINY_GRID_SIZE; ty++)
                                for (int tz = 0; tz < Config.TINY_GRID_SIZE; tz++) {
                                    for (int tx = 0; tx < Config.TINY_GRID_SIZE; tx++) {
                                        int color = cont.data[tx][ty][tz];
                                        Config.get().putPalette(color);
                                    }
                                }

                        }

                        prevPaletteIds[gx][gy][gz] = paletteId;

                        BlockBuilder.fillInPalette(vertices, start, end, paletteId);

                        cont.notUpToDate = false;
                    } else {
                        prevPaletteIds[gx][gy][gz] = -1;
                    }
                }

        if (cleanPalette) {
           // ErrorHandler.log("Large Upload");
            Config.get().uploadPalette(palette.getWidth(), palette.getHeight());
        }
        LogHandler.exitOnGLError();

        return true;
    }

    public boolean hasPalettes() {
        if (palettesTotal > 0)
            return true;

        return false;
    }

    public void render(ShaderProgram shader) {
        if (verticesCount > 0)
            mesh.render(shader, GL_TRIANGLES, 0, verticesCount);
    }

    public void reset() {
        palettesTotal = 0;
        floatCount = 0;
        verticesCount = 0;

        for (int gx = 0; gx < Config.GRID_SIZE; gx++)
            for (int gy = 0; gy < Config.GRID_SIZE; gy++)
                for (int gz = 0; gz < Config.GRID_SIZE; gz++) {
                    prevPaletteIds[gx][gy][gz] = -1;
                }
    }

    public void dispose() {
        for (int x = 0; x < Config.GRID_SIZE; x++)
            for (int y = 0; y < Config.GRID_SIZE; y++)
                for (int z = 0; z < Config.GRID_SIZE; z++) {
                    if (gridContainers[x][y][z] != null)
                        tinyGridPool.free(gridContainers[x][y][z]);
                    gridContainers[x][y][z] = null;
                }

        if (mesh != null) {
            mesh.dispose();
            mesh = null;
        }
        if (palette != null) {
            palette.dispose();
            palette = null;
        }
    }

    protected VertexAttribute[] createAttributes() {
        VertexAttribute attr = new VertexAttribute(VertexAttributes.Usage.Position, 3, "position");
        VertexAttribute attr2 = new VertexAttribute(VertexAttributes.Usage.Generic, 1, "normalColor");
        VertexAttribute attr3 = new VertexAttribute(VertexAttributes.Usage.Generic, 1, "palette");
        return new VertexAttribute[] {attr, attr2, attr3 };
    }

    public TinyGrid getTinyGrid(int x, int y, int z) {
        return gridContainers[x][y][z];
    }

    private static int[] verticesPointer = new int[Config.GRID_TOTAL];
    public void createVertices() {
        FloatBuffer vertices = mesh.getVerticesBuffer();
        vertices.rewind();
        vertices.limit(Config.MAX_FLOAT_SIZE);

        floatCount = 0;
        palettesTotal = 0;

        for (int gx = 0; gx < Config.GRID_SIZE; gx++)
            for (int gy = 0; gy < Config.GRID_SIZE; gy++)
                for (int gz = 0; gz < Config.GRID_SIZE; gz++) {
                    if (BlockBuilder.generateBox(vertices, gx, gy, gz, this)) {
                        grids[gx][gy][gz] = true;
                        verticesPointer[palettesTotal] = vertices.position();
                        palettesTotal++;
                    } else {
                        grids[gx][gy][gz] = false;
                    }
                }


        floatCount = vertices.position();
        vertices.limit(floatCount);
        vertices.rewind();
        verticesCount = floatCount / Config.ATTRIBUTES_SIZE; // pos + uv
    }

    public void updateBoundingBox() {
        checkBounds = false;

        int minX = Config.GRID_SIZE, minY = Config.GRID_SIZE, minZ = Config.GRID_SIZE;
        int maxX = 0, maxY = 0, maxZ = 0;

        for (int gx = 0; gx < Config.GRID_SIZE; gx++)
            for (int gy = 0; gy < Config.GRID_SIZE; gy++)
                for (int gz = 0; gz < Config.GRID_SIZE; gz++) {
                    if (gridContainers[gx][gy][gz] != null) {
                        if (gx < minX)
                            minX = gx;

                        if (gx > maxX)
                            maxX = gx;

                        if (gy < minY)
                            minY = gy;

                        if (gy > maxY)
                            maxY = gy;

                        if (gz < minZ)
                            minZ = gz;

                        if (gz > maxZ)
                            maxZ = gz;

                        checkBounds = true;
                    }
                }

        if (checkBounds) {
            maxX++;
            maxY++;
            maxZ++;
            boundingBox.min.set(minX, minY, minZ);
            boundingBox.max.set(maxX, maxY, maxZ);
            boundingBox.set(boundingBox.min, boundingBox.max);
        }
    }

    public boolean collidesSphereWith(Vector3 point, float radius) {
        if (!checkBounds) {
            return false;
        }

        tmp.set(point);
        tmp.mul(inverseTransform);

        if (!SimpleMath.sphereAabbOverlap(boundingBox, tmp, radius))
            return false;

        for (int tx = 0; tx < Config.GRID_SIZE; tx++)
            for (int ty = 0; ty < Config.GRID_SIZE; ty++)
                for (int tz = 0; tz < Config.GRID_SIZE; tz++) {
                    TinyGrid cont = gridContainers[tx][ty][tz];
                    if (cont == null)
                        continue;

                    tmp2.set(tmp);
                    tmp2.sub(tx, ty, tz);
                    if (SimpleMath.sphereAabbOverlap(cont.getBoundingBox(), tmp2, radius)) {
                        return true;
                    }
                }

        return false;
    }

    protected static Ray tmpRay = new Ray(new Vector3(), new Vector3());
    protected static Ray tmpRay2 = new Ray(new Vector3(), new Vector3());

    public boolean collidesWith(Ray ray, Vector3 intersection, float maxDistance) {
        tmpRay.set(ray);
        tmpRay.origin.add(-x, -y, -z);
        //tmpRay.mul(inverseTransform);

        if (!Intersector.intersectRayBoundsFast(tmpRay, boundingBox)) {
            return false;
        }

        boolean result = false;

        float myDist;
        for (int x = 0; x < Config.GRID_SIZE; x++)
            for (int y = 0; y < Config.GRID_SIZE; y++)
                for (int z = 0; z < Config.GRID_SIZE; z++) {
                    TinyGrid cont = getTinyGrid(x, y, z);
                    if (cont == null || !cont.checkBounds)
                        continue;

                    tmp2.set(x, y, z);
                    tmp2.add(.5f);

                    myDist = tmpRay.origin.dst(tmp2) - 1.0f;
                    if (myDist * myDist > maxDistance)
                        continue;

                    tmpRay2.set(tmpRay);
                    tmpRay2.origin.sub(x,y,z);
                    tmpRay2.set(tmpRay2.origin, tmpRay2.direction);

                    if (Intersector.intersectRayBounds(tmpRay2, cont.getBoundingBox(), tmp)) {
                        tmp.add(x,y,z);
                        myDist = tmpRay.origin.dst2(tmp);
                        if (myDist > maxDistance)
                            continue;

                        maxDistance = myDist;
                        intersection.set(tmp);
                        intersection.mul(transform);
                        result = true;
                    }
                }

        return result;
    }

    public boolean pointInBoundingBox(Vector3 point) {
        if (!checkBounds) {
            return false;
        }

        tmp.set(point);
        tmp.mul(inverseTransform);
        if (boundingBox.contains(tmp)) {
            int gx = MathUtils.floor(tmp.x);
            int gy = MathUtils.floor(tmp.x);
            int gz = MathUtils.floor(tmp.x);

            if (gx == Config.GRID_SIZE || gy == Config.GRID_SIZE || gz == Config.GRID_SIZE)
                return false;

            TinyGrid cont = getTinyGrid(gx, gy, gz);
            if (cont == null)
                return false;

            if (!cont.checkBounds)
                return false;

            int tx = (int)(tmp.x * (float)Config.TINY_GRID_SIZE) % Config.TINY_GRID_SIZE;
            int ty = (int)(tmp.y * (float)Config.TINY_GRID_SIZE) % Config.TINY_GRID_SIZE;
            int tz = (int)(tmp.z * (float)Config.TINY_GRID_SIZE) % Config.TINY_GRID_SIZE;
            return cont.data[tx][ty][tz] != 0xffffffff;
        }
        return false;
    }

    public boolean load(InputStream in) throws IOException {
        int count = StreamUtil.getInt(in);

        LogHandler.log("Count: " + count);

        for (int i = 0; i < count; i++) {

            int gx = StreamUtil.getInt(in);
            int gy = StreamUtil.getInt(in);
            int gz = StreamUtil.getInt(in);


            TinyGrid tinyGrid = tinyGridPool.obtain();

            RLEInputStream rlein = new RLEInputStream(in);
            for (int tx = 0; tx < Config.TINY_GRID_SIZE; tx++) {
                for (int ty = 0; ty < Config.TINY_GRID_SIZE; ty++) {
                    for (int tz = 0; tz < Config.TINY_GRID_SIZE; tz++) {
                        tinyGrid.data[tx][ty][tz] = Config.SAVE_SHADE ? rlein.readInt() : rlein.readRGBInt();
                    }
                }
            }

            tinyGrid.findBoundaryBox();
            tinyGrid.findFullSides();
            gridContainers[gx][gy][gz] = tinyGrid;

            //ErrorHandler.log("Done: x:" + gx + " y: " + gy + " z: " + gz);
        }

        return true;
    }

    public static boolean skipLoad(InputStream in) throws IOException {
        int count = StreamUtil.getInt(in);

        LogHandler.log("Count: " + count);

        for (int i = 0; i < count; i++) {

            int gx = StreamUtil.getInt(in);
            int gy = StreamUtil.getInt(in);
            int gz = StreamUtil.getInt(in);

            RLEInputStream rlein = new RLEInputStream(in);
            for (int tx = 0; tx < Config.TINY_GRID_SIZE; tx++) {
                for (int ty = 0; ty < Config.TINY_GRID_SIZE; ty++) {
                    for (int tz = 0; tz < Config.TINY_GRID_SIZE; tz++) {
                        if (Config.SAVE_SHADE)
                            rlein.readInt();
                        else
                            rlein.readRGBInt();
                    }
                }
            }

            //ErrorHandler.log("Done: x:" + gx + " y: " + gy + " z: " + gz);
        }

        return true;
    }

    public void save(OutputStream out) throws IOException {
        StreamUtil.putInt(out, x);
        StreamUtil.putInt(out, y);
        StreamUtil.putInt(out, z);

        int total = 0;

        for (int gx = 0; gx < Config.GRID_SIZE; gx++)
            for (int gy = 0; gy < Config.GRID_SIZE; gy++)
                for (int gz = 0; gz < Config.GRID_SIZE; gz++)
                    if (gridContainers[gx][gy][gz] != null)
                        total++;

        StreamUtil.putInt(out, total);

        for (int gx = 0; gx < Config.GRID_SIZE; gx++)
           for (int gy = 0; gy < Config.GRID_SIZE; gy++)
               for (int gz = 0; gz < Config.GRID_SIZE; gz++) {
                   if (gridContainers[gx][gy][gz] != null) {
                       StreamUtil.putInt(out, gx);
                       StreamUtil.putInt(out, gy);
                       StreamUtil.putInt(out, gz);

                       // add tiny data to pixmap
                       TinyGrid tinyGrid = gridContainers[gx][gy][gz];
                       tinyGrid.findBoundaryBox();
                       tinyGrid.findFullSides();

                       RLEOutputStream rle = new RLEOutputStream(out);
                       for (int tx = 0; tx < Config.TINY_GRID_SIZE; tx++) {
                           for (int ty = 0; ty < Config.TINY_GRID_SIZE; ty++) {
                               for (int tz = 0; tz < Config.TINY_GRID_SIZE; tz++) {
                                    if (Config.SAVE_SHADE)
                                       rle.writeInt(tinyGrid.data[tx][ty][tz]);
                                    else
                                        rle.writeRGBInt(tinyGrid.data[tx][ty][tz]);
                               }
                           }
                       }
                       rle.finalize();
                   }
               }
   }

    public void copyTo(Grid grid) {
        for (int gx = 0; gx < Config.GRID_SIZE; gx++)
            for (int gy = 0; gy < Config.GRID_SIZE; gy++)
                for (int gz = 0; gz < Config.GRID_SIZE; gz++) {
                    if (gridContainers[gx][gy][gz] != null) {
                        TinyGrid tiny = tinyGridPool.obtain();
                        gridContainers[gx][gy][gz].copyTo(tiny);
                        grid.gridContainers[gx][gy][gz] = tiny;
                    }
                }

        grid.allowLoadPalette = true;
    }
}
