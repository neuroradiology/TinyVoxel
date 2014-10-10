package com.toet.TinyVoxel.Renderer.Bundles;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.toet.TinyVoxel.Config;
import com.toet.TinyVoxel.Debug.LogHandler;
import com.toet.TinyVoxel.Util.SimpleMath;

/**
 * Created by Kajos on 6/21/2014.
 */
public class TinyGrid {
    public int[][][] data = new int[Config.TINY_GRID_SIZE][Config.TINY_GRID_SIZE][Config.TINY_GRID_SIZE];
    public boolean checkBounds = false;
    public boolean[] fullSides = new boolean[6]; //-x, +x, -y, +y, -z, +z
    public boolean notUpToDate = true;
    public int minX, minY, minZ, maxX, maxY, maxZ;
    public boolean shadowsUpToDate = false;
    private BoundingBox boundingBox = new BoundingBox();

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public void copyTo(TinyGrid tiny) {
        tiny.checkBounds = checkBounds;
        tiny.minX = minX;
        tiny.minY = minY;
        tiny.minZ = minZ;
        tiny.maxX = maxX;
        tiny.maxY = maxY;
        tiny.maxZ = maxZ;

        for (int i = 0; i < fullSides.length; i++)
            tiny.fullSides[i] = fullSides[i];

        tiny.boundingBox = new BoundingBox(boundingBox);
        for (int tx = 0; tx < Config.TINY_GRID_SIZE; tx++)
            for (int ty = 0; ty < Config.TINY_GRID_SIZE; ty++)
                for (int tz = 0; tz < Config.TINY_GRID_SIZE; tz++) {
                    tiny.data[tx][ty][tz] = data[tx][ty][tz];
                }

        tiny.notUpToDate = true;
    }

    public void findBoundaryBox() {
        outerloop:
        for (minZ = 0; minZ < Config.TINY_GRID_SIZE; minZ++)
            for (int tx = 0; tx < Config.TINY_GRID_SIZE; tx++)
                for (int ty = 0; ty < Config.TINY_GRID_SIZE; ty++) {
                    if (data[tx][ty][minZ] != 0xffffffff) {
                        break outerloop;
                    }
                }

        if (minZ == Config.TINY_GRID_SIZE) {
            LogHandler.log("Empty cube!");
            checkBounds = false;
            return;
        }

        outerloop:
        for (maxZ = Config.TINY_GRID_SIZE - 1; maxZ > minZ; maxZ--)
            for (int tx = 0; tx < Config.TINY_GRID_SIZE; tx++)
                for (int ty = 0; ty < Config.TINY_GRID_SIZE; ty++) {
                    if (data[tx][ty][maxZ] != 0xffffffff) {
                        break outerloop;
                    }
                }
        maxZ++;

        outerloop:
        for (minX = 0; minX < Config.TINY_GRID_SIZE; minX++)
            for (int tz = 0; tz < Config.TINY_GRID_SIZE; tz++)
                for (int ty = 0; ty < Config.TINY_GRID_SIZE; ty++) {
                    if (data[minX][ty][tz] != 0xffffffff) {
                        break outerloop;
                    }
                }

        outerloop:
        for (maxX = Config.TINY_GRID_SIZE - 1; maxX > minX; maxX--)
            for (int tz = 0; tz < Config.TINY_GRID_SIZE; tz++)
                for (int ty = 0; ty < Config.TINY_GRID_SIZE; ty++) {
                    if (data[maxX][ty][tz] != 0xffffffff) {
                        break outerloop;
                    }
                }
        maxX++;

        outerloop:
        for (minY = 0; minY < Config.TINY_GRID_SIZE; minY++)
            for (int tz = 0; tz < Config.TINY_GRID_SIZE; tz++)
                for (int tx = 0; tx < Config.TINY_GRID_SIZE; tx++) {
                    if (data[tx][minY][tz] != 0xffffffff) {
                        break outerloop;
                    }
                }

        outerloop:
        for (maxY = Config.TINY_GRID_SIZE - 1; maxY > minY; maxY--)
            for (int tz = 0; tz < Config.TINY_GRID_SIZE; tz++)
                for (int tx = 0; tx < Config.TINY_GRID_SIZE; tx++) {
                    if (data[tx][maxY][tz] != 0xffffffff) {
                        break outerloop;
                    }
                }
        maxY++;

        checkBounds = true;

        if (checkBounds) {
            boundingBox.min.set((float) minX / (float) Config.TINY_GRID_SIZE,
                    (float) minY / (float) Config.TINY_GRID_SIZE,
                    (float) minZ / (float) Config.TINY_GRID_SIZE);
            boundingBox.max.set((float) maxX / (float) Config.TINY_GRID_SIZE,
                    (float) maxY / (float) Config.TINY_GRID_SIZE,
                    (float) maxZ / (float) Config.TINY_GRID_SIZE);
            boundingBox.set(boundingBox.min, boundingBox.max);
        }
    }

    public void findFullSides() {
        fullSides[0] = minX == 0;
        fullSides[1] = maxX == Config.TINY_GRID_SIZE;
        fullSides[2] = minY == 0;
        fullSides[3] = maxY == Config.TINY_GRID_SIZE;
        fullSides[4] = minZ == 0;
        fullSides[5] = maxZ == Config.TINY_GRID_SIZE;

        if (fullSides[0]) {
            outerloop:
            for (int ty = 0; ty < Config.TINY_GRID_SIZE; ty++)
                for (int tz = 0; tz < Config.TINY_GRID_SIZE; tz++)
                    if (data[0][ty][tz] == 0xffffffff) {
                        fullSides[0] = false;
                        break outerloop;
                    }
        }

        if (fullSides[1]) {
            outerloop:
            for (int ty = 0; ty < Config.TINY_GRID_SIZE; ty++)
                for (int tz = 0; tz < Config.TINY_GRID_SIZE; tz++)
                    if (data[Config.TINY_GRID_SIZE - 1][ty][tz] == 0xffffffff) {
                        fullSides[1] = false;
                        break outerloop;
                    }
        }

        if (fullSides[2]) {
            outerloop:
            for (int tz = 0; tz < Config.TINY_GRID_SIZE; tz++)
                for (int tx = 0; tx < Config.TINY_GRID_SIZE; tx++)
                    if (data[tx][0][tz] == 0xffffffff) {
                        fullSides[2] = false;
                        break outerloop;
                    }
        }

        if (fullSides[3]) {
            outerloop:
            for (int tz = 0; tz < Config.TINY_GRID_SIZE; tz++)
                for (int tx = 0; tx < Config.TINY_GRID_SIZE; tx++)
                    if (data[tx][Config.TINY_GRID_SIZE - 1][tz] == 0xffffffff) {
                        fullSides[3] = false;
                        break outerloop;
                    }
        }

        if (fullSides[4]) {
            outerloop:
            for (int tx = 0; tx < Config.TINY_GRID_SIZE; tx++)
                for (int ty = 0; ty < Config.TINY_GRID_SIZE; ty++)
                    if (data[tx][ty][0] == 0xffffffff) {
                        fullSides[4] = false;
                        break outerloop;
                    }
        }

        if (fullSides[5]) {
            outerloop:
            for (int tx = 0; tx < Config.TINY_GRID_SIZE; tx++)
                for (int ty = 0; ty < Config.TINY_GRID_SIZE; ty++)
                    if (data[tx][ty][Config.TINY_GRID_SIZE - 1] == 0xffffffff) {
                        fullSides[5] = false;
                        break outerloop;
                    }
        }
    }
    public void findShadows(Grid grid, int gx, int gy, int gz) {
        if (shadowsUpToDate)
            return;

        for (int tx = 0; tx < Config.TINY_GRID_SIZE; tx++)
            for (int ty = 0; ty < Config.TINY_GRID_SIZE; ty++)
                for (int tz = 0; tz < Config.TINY_GRID_SIZE; tz++) {
                    findShadow(grid, gx, gy, gz, tx, ty, tz);
                }

        shadowsUpToDate = true;
    }

    public void findShadow(Grid grid, int gx, int gy, int gz, int tx, int ty, int tz) {
        if (data[tx][ty][tz] == 0xffffffff) {
            return;
        }
        boolean closedIn = true;

        notUpToDate = true;

        outerloop:
        for (int x = -1; x < 2; x++)
            for (int y = -1; y < 2; y++)
                for (int z = -1; z < 2; z++) {
                    if (x == 0 && y == 0 && z == 0)
                        continue;

                    if (grid.owner.getVoxel(grid, gx, gy, gz, tx, ty, tz, x, y, z) == 0xffffffff){
                        closedIn = false;
                        break outerloop;
                    }
                }

        if (closedIn) {
            int shadow = SimpleMath.shadeLookUp(tx,ty,tz);
            data[tx][ty][tz] = SimpleMath.setShadow(shadow, data[tx][ty][tz]);
            return;
        }

        int diff = 1;
        int diffPlusOne = diff + 1;

        int count = diff * 2 + 1;
        count = count * count * count;
        int light = 0;
        for (int x = -diff; x < diffPlusOne; x++)
            for (int y = -diff; y < diffPlusOne; y++)
                for (int z = -diff; z < diffPlusOne; z++) {

                    int rx = x >= 0 ? x + 1 : x;
                    int ry = y >= 0 ? y + 1 : y;
                    int rz = z >= 0 ? z + 1 : z;

                    if (grid.owner.getVoxel(grid, gx, gy, gz, tx, ty, tz, rx, ry, rz) == 0xffffffff){
                        light++;
                    } else {
                        light--;
                    }
                 }

        float calc = (float)light /(float)count / 2f + .75f;
        int result = (int)(calc * 255f);

        result = MathUtils.clamp(result, 0, 254);
        data[tx][ty][tz] = SimpleMath.setShadow(result, data[tx][ty][tz]);
    }
}
