package com.toet.TinyVoxel.Renderer;


import com.toet.TinyVoxel.Config;
import com.toet.TinyVoxel.Renderer.Bundles.Grid;
import com.toet.TinyVoxel.Renderer.Bundles.TinyGrid;

import java.nio.FloatBuffer;

/**
 * Created by Kajos on 20-1-14.
 */
public class BlockBuilder {
    public static float low = .0001f;
    public static final boolean[] CUBE = {

            // x-
            false,false,false,
            false,false, true,
            false, true, true,

            false,false,false,
            false, true, true,
            false, true,false,

            // x+
            true, true, true,
            true,false,false,
            true, true,false,

            true,false,false,
            true, true, true,
            true,false, true,

            // y-
            true,false, true,
            false,false, true,
            false,false,false,

            true,false, true,
            false,false,false,
            true,false,false,

            // y+
            true, true, true,
            true, true,false,
            false, true,false,

            true, true, true,
            false, true,false,
            false, true, true,

            // z-
            true, true,false,
            false,false,false,
            false, true,false,

            true, true,false,
            true,false,false,
            false,false,false,

            // z+
            true, true, true,
            false, true, true,
            true, false, true,

            false, true, true,
            false, false, true,
            true,false, true,
    };

    public static TinyGrid getNeighbor(Grid grid, int x, int y, int z, int dx, int dy, int dz) {
        if (dx < 0) {
            TinyGrid cont = null;
            if (x == 0) {
                Grid nGrid = grid.owner.getGridSafe(grid.x - 1, grid.y, grid.z);
                if (nGrid != null) {
                    cont = nGrid.getTinyGrid(Config.GRID_SIZE - 1, y, z);
                }
            } else {
                cont = grid.getTinyGrid(x - 1, y, z);
            }
            return cont;

        } else if (dy < 0) {
            TinyGrid cont = null;
            if (y == 0) {
                Grid nGrid = grid.owner.getGridSafe(grid.x, grid.y - 1, grid.z);
                if (nGrid != null) {
                    cont = nGrid.getTinyGrid(x, Config.GRID_SIZE - 1, z);
                }
            } else {
                cont = grid.getTinyGrid(x, y - 1, z);
            }
            return cont;

        } else if (dz < 0) {
            TinyGrid cont = null;
            if (z == 0) {
                Grid nGrid = grid.owner.getGridSafe(grid.x, grid.y, grid.z - 1);
                if (nGrid != null) {
                    cont = nGrid.getTinyGrid(x, y, Config.GRID_SIZE - 1);
                }
            } else {
                cont = grid.getTinyGrid(x, y, z - 1);
            }
            return cont;

        } else if (dx > 0) {
            TinyGrid cont = null;
            if (x >= Config.GRID_SIZE - 1) {
                Grid nGrid = grid.owner.getGridSafe(grid.x + 1, grid.y, grid.z);
                if (nGrid != null) {
                    cont = nGrid.getTinyGrid(0, y, z);
                }
            } else {
                cont = grid.getTinyGrid(x + 1, y, z);
            }
            return cont;

        } else if (dy > 0) {
            TinyGrid cont = null;
            if (y >= Config.GRID_SIZE - 1) {
                Grid nGrid = grid.owner.getGridSafe(grid.x, grid.y + 1, grid.z);
                if (nGrid != null) {
                    cont = nGrid.getTinyGrid(x, 0, z);
                }
            } else {
                cont = grid.getTinyGrid(x, y + 1, z);
            }
            return cont;

        } else if (dz > 0) {
            TinyGrid cont = null;
            if (z >= Config.GRID_SIZE - 1) {
                Grid nGrid = grid.owner.getGridSafe(grid.x, grid.y, grid.z + 1);
                if (nGrid != null) {
                    cont = nGrid.getTinyGrid(x, y, 0);
                }
            } else {
                cont = grid.getTinyGrid(x, y, z + 1);
            }
            return cont;

        }
        return null;
    }

    public static void fillInPalette(FloatBuffer vertices, int start, int end, int palette) {
        for (int i = start + 4; i < end; i+=5) {
            vertices.put(i, palette);
        }
    }

    static boolean[] skipArray = new boolean[6];
    static int skipCount;
    public static boolean generateBox(FloatBuffer vertices, int x, int y, int z, Grid grid) {
        TinyGrid cont = grid.getTinyGrid(x, y, z);
        if (cont == null)
            return false;

        TinyGrid gCont;
        for(int i = 0; i < skipArray.length; i++) {
            skipArray[i] = false;
        }
        skipCount = 0;

        if (cont.minX == 0) {
            gCont = getNeighbor(grid, x, y, z, -1, 0, 0);
            if (gCont != null) {
                if (gCont.fullSides[1]) {
                    skipArray[0] = true;
                    skipCount++;
                }
            }
        }

        if (cont.maxX == Config.TINY_GRID_SIZE) {
            gCont = getNeighbor(grid, x, y, z, +1, 0, 0);
            if (gCont != null) {
                if (gCont.fullSides[0]) {
                    skipArray[1] = true;
                    skipCount++;
                }
            }
        }

        if (cont.minY == 0) {
            gCont = getNeighbor(grid, x, y, z, 0, -1, 0);
            if (gCont != null) {
                if (gCont.fullSides[3]) {
                    skipArray[2] = true;
                    skipCount++;
                }
            }
        }

        if (cont.maxY == Config.TINY_GRID_SIZE) {
            gCont = getNeighbor(grid, x, y, z, 0, +1, 0);
            if (gCont != null) {
                if (gCont.fullSides[2]) {
                    skipArray[3] = true;
                    skipCount++;
                }
            }
        }

        if (cont.minZ == 0) {
            gCont = getNeighbor(grid, x, y, z, 0, 0, -1);
            if (gCont != null) {
                if (gCont.fullSides[5]) {
                    skipArray[4] = true;
                    skipCount++;
                }
            }
        }

        if (cont.maxZ == Config.TINY_GRID_SIZE) {
            gCont = getNeighbor(grid, x, y, z, 0, 0, +1);
            if (gCont != null) {
                if (gCont.fullSides[4]) {
                    skipArray[5] = true;
                    skipCount++;
                }
            }
        }

        FloatBuffer write = vertices;

        float divide = (float)(Config.TINY_GRID_SIZE);

        float minXf = (float)cont.minX / divide;
        float minYf = (float)cont.minY / divide;
        float minZf = (float)cont.minZ / divide;

        minXf += low;
        minYf += low;
        minZf += low;

        float maxXf = (float)cont.maxX / divide;
        float maxYf = (float)cont.maxY / divide;
        float maxZf = (float)cont.maxZ / divide;

        maxXf -= low;
        maxYf -= low;
        maxZf -= low;

        minXf += x;
        minYf += y;
        minZf += z;
        maxXf += x;
        maxYf += y;
        maxZf += z;

        boolean hasWritten = false;

        if (skipCount != 6) {
            int side;

            for (int i = 0; i < CUBE.length; i += 3) {
                side = i / (3 * 6);
                if (skipArray[side])
                    continue;

                if (CUBE[i])
                    write.put(maxXf);
                else
                    write.put(minXf);

                if (CUBE[i + 1])
                    write.put(maxYf);
                else
                    write.put(minYf);

                if (CUBE[i + 2])
                    write.put(maxZf);
                else
                    write.put(minZf);

                if (side < 2) { // x
                    write.put(.5f);
                } else if (side < 4) { // y
                    write.put(1f);
                } else { // z
                    write.put(1.5f);
                }

                // palette is filled in later
                write.put(0f);

                hasWritten = true;
            }
        }

        return hasWritten;
    }
}
