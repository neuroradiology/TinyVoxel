package com.toet.TinyVoxel.Renderer.Tools;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.toet.TinyVoxel.Config;
import com.toet.TinyVoxel.Renderer.Bundles.ArrayBundle;
import com.toet.TinyVoxel.Renderer.Bundles.Bundle;
import com.toet.TinyVoxel.Renderer.Manager;

/**
 * Created by Kajos on 9/4/2014.
 */
public class BrushUtils {
    public static int SIZE = 32;

    private static BrushUtils INSTANCE;

    public Bundle selected = null;

    public void addSelection(Manager manager) {
        if (selected == null) {
            selected = new ArrayBundle();
            selected.init("tools", false);
            selected.updateMatrix();
            selected.solid = true;
            selected.visible = true;
            selected.ableToCollide = false;
            if (Config.get().getTransparentTools())
                manager.transparentBundleArray.add(selected);
            else
                manager.bundleArray.add(selected);
        }
    }

    public void removeSelection(Manager manager) {
        if (selected != null) {
            if (Config.get().getTransparentTools())
                manager.transparentBundleArray.removeValue(selected, true);
            else
                manager.bundleArray.removeValue(selected, true);

            selected.dispose();

            selected = null;
        }
    }

    public Array<Brush> BRUSHES = new Array<Brush>(Brush.class);

    public static BrushUtils get() {
        if (INSTANCE == null) {
            INSTANCE = new BrushUtils();
            INSTANCE.initBrushes();
        }
        return INSTANCE;
    }

    public void initBrushes() {
        BRUSHES.clear();
        BRUSHES.add(new Sphere());
        BRUSHES.add(new Cube());
        BRUSHES.add(new RampBox1());
        BRUSHES.add(new RampBox2());
        BRUSHES.add(new RampBox3());
        BRUSHES.add(new RampBox4());
        BRUSHES.add(new RampBox5());
        BRUSHES.add(new RampBox6());
    }

    public Brush getBrush(int id) {
        return BRUSHES.items[id];
    }

    public int getCount() {
        return BRUSHES.size;
    }

    public static int convert(int val, int size) {
        return val * SIZE / size;
    }

    public abstract class Brush {
        public String name = "";
        Vector3 tmp = new Vector3();

        private int shadows[][][] = new int[SIZE][SIZE][SIZE];
        protected boolean array[][][] = new boolean[SIZE][SIZE][SIZE];

        public abstract void init();

        public Brush() {
            init();
        }

        private void calculateSingleShadow(int ax, int ay, int az) {
            if (!array[ax][ay][az]) {
                shadows[ax][ay][az] = -1;
                return;
            }

            tmp.set(ax-SIZE/2,ay-SIZE/2,az-SIZE/2).nor();
            int result = (int)Math.abs(tmp.dot(Config.LIGHT_DIRECTION) * 128f);
            result += 126; // make sure it doesn't reach 256
            shadows[ax][ay][az] = result;
        }

        public void calculateShadows() {
            for (int x = 0; x < SIZE; x++)
                for (int y = 0; y < SIZE; y++)
                    for (int z = 0; z < SIZE; z++) {
                        calculateSingleShadow(x,y,z);
                    }
        }

        public int getShadow(int x, int y, int z, int size){
            int ax = convert(x, size);
            int ay = convert(y, size);
            int az = convert(z, size);

            if (ax < 0 || ay < 0 || az < 0 || ax >= SIZE || ay >= SIZE || az >= SIZE)
                return -1;

            return shadows[ax][ay][az];
        }

        public boolean get(int x, int y, int z, int size) {
            int ax = convert(x, size);
            int ay = convert(y, size);
            int az = convert(z, size);

            if (ax < 0 || ay < 0 || az < 0 || ax >= SIZE || ay >= SIZE || az >= SIZE)
                return false;

            return array[ax][ay][az];
        }
    }

    public class Sphere extends Brush {

        @Override
        public void init() {
            float len = SIZE/2;
            len = len * len;
            for (int x = 0; x < SIZE; x++)
                for (int y = 0; y < SIZE; y++)
                    for (int z = 0; z < SIZE; z++) {
                        tmp.set(x - SIZE/2, y - SIZE/2, z - SIZE/2);
                        array[x][y][z] = tmp.len2() < len;
                    }

            calculateShadows();
            name = "Sphere";
        }
    }
    public class Cube extends Brush {

        @Override
        public void init() {
            for (int x = 0; x < SIZE; x++)
                for (int y = 0; y < SIZE; y++)
                    for (int z = 0; z < SIZE; z++) {
                        array[x][y][z] = true;
                    }

            calculateShadows();
            name = "Cube";
        }
    }
    public class RampBox1 extends Brush {

        @Override
        public void init() {
            for (int x = 0; x < SIZE; x++)
                for (int y = 0; y < SIZE; y++)
                    for (int z = 0; z < SIZE; z++) {
                        array[x][y][z] = x < y;
                    }

            calculateShadows();
            name = "Rramp 1";
        }
    }
    public class RampBox2 extends Brush {

        @Override
        public void init() {
            for (int x = 0; x < SIZE; x++)
                for (int y = 0; y < SIZE; y++)
                    for (int z = 0; z < SIZE; z++) {
                        array[x][y][z] = x > y;
                    }

            calculateShadows();
            name = "Rramp 2";
        }
    }
    public class RampBox3 extends Brush {

        @Override
        public void init() {
            for (int x = 0; x < SIZE; x++)
                for (int y = 0; y < SIZE; y++)
                    for (int z = 0; z < SIZE; z++) {
                        array[x][y][z] = z > y;
                    }

            calculateShadows();
            name = "Rramp 3";
        }
    }
    public class RampBox4 extends Brush {

        @Override
        public void init() {
            for (int x = 0; x < SIZE; x++)
                for (int y = 0; y < SIZE; y++)
                    for (int z = 0; z < SIZE; z++) {
                        array[x][y][z] = z < y;
                    }

            calculateShadows();
            name = "Rramp 4";
        }
    }
    public class RampBox5 extends Brush {

        @Override
        public void init() {
            for (int x = 0; x < SIZE; x++)
                for (int y = 0; y < SIZE; y++)
                    for (int z = 0; z < SIZE; z++) {
                        array[x][y][z] = x < z;
                    }

            calculateShadows();
            name = "Rramp 5";
        }
    }
    public class RampBox6 extends Brush {

        @Override
        public void init() {
            for (int x = 0; x < SIZE; x++)
                for (int y = 0; y < SIZE; y++)
                    for (int z = 0; z < SIZE; z++) {
                        array[x][y][z] = x > z;
                    }

            calculateShadows();
            name = "Rramp 6";
        }
    }
}
