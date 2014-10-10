package com.toet.TinyVoxel.Util;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Kajos on 8/22/2014.
 */
public class RLEInputStream extends InputStream {
    private InputStream in;
    private int count = 0;
    private int color;

    public RLEInputStream(InputStream in) {
        this.in = in;
    }

    @Override
    public int read() throws IOException {
        throw new UnsupportedOperationException();
    }

    public int readInt() throws IOException {
        if (count == 0) {
            color = StreamUtil.getInt(in);
            count = in.read();
        }
        count--;
        return color;
    }

    public int readRGBInt() throws IOException {
        if (count == 0) {
            color = StreamUtil.getRGBInt(in);
            color |= 0xff000000;
            count = in.read();
        }
        count--;
        return color;
    }

    // Stupid way because it can't throw IOException
    @Override
    public int available() {
        return count >= 0 ? 1 : 0;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
