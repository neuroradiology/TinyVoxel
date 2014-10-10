package com.toet.TinyVoxel.Util;

import com.badlogic.gdx.utils.Pool;
import com.toet.TinyVoxel.Config;

/**
 * Created by Kajos on 6/21/2014.
 */
public class Position implements Comparable<Position> {
    int back[] = new int[3];
    int hashCode = 0;

    public int get(int id) {
        return back[id];
    }

    @Override
    public boolean equals(Object in) {
        if (!(in instanceof Position))
            return false;
        Position comp = (Position) in;

        if (comp.get(0) != get(0))
            return false;

        if (comp.get(1) != get(1))
            return false;

        if (comp.get(2) != get(2))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public int compareTo(Position o) {
        if (get(0)!=o.get(0)) return Integer.compare(get(0), o.get(0));
        if (get(1)!=o.get(1)) return Integer.compare(get(1), o.get(1));
        return Integer.compare(get(2), o.get(2));
    }

    public static final int SPREAD = (int)(Config.FAR / (float)Config.GRID_SIZE);

    public void set(int x, int y, int z) {
        back[0] = x;
        back[1] = y;
        back[2] = z;

        hashCode = SPREAD * SPREAD * get(0);
        hashCode ^= SPREAD * get(1);
        hashCode ^= get(2);
    }

    private static Pool<Position> listPool = new Pool<Position>() {
        @Override
        protected Position newObject() {
            return new Position();
        }
    };

    public static Position create(int x, int y, int z) {
        Position key = listPool.obtain();
        key.set(x,y,z);
        return key;
    }

    public static void free(Position key) {
        listPool.free(key);
    }
}
