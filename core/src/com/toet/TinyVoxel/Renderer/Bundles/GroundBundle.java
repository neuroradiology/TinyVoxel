package com.toet.TinyVoxel.Renderer.Bundles;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.toet.TinyVoxel.Config;
import com.toet.TinyVoxel.Renderer.Tools.BrushUtils;
import com.toet.TinyVoxel.Util.SimpleMath;

/**
 * Created by Kajos on 8/12/2014.
 */
public class GroundBundle extends GridBundle {
    @Override

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
                    if (y < 0)
                        continue;

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
}
