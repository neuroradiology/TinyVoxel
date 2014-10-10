package com.toet.TinyVoxel.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Created by Kajos on 6/23/2014.
 */
public class StreamUtil {
    static ByteBuffer wrapped = ByteBuffer.allocateDirect(4); // big-endian by default

    public static int getInt(InputStream in) throws IOException {
        int v1 = (in.read() & 0xff) << 24;
        int v2 = (in.read() & 0xff) << 16;
        int v3 = (in.read() & 0xff) << 8;
        int v4 = in.read() & 0xff;

        return v1 | v2 | v3 | v4;
    }

    public static int getRGBInt(InputStream in) throws IOException {
        int b = (in.read() & 0xff) << 16;
        int g = (in.read() & 0xff) << 8;
        int r = (in.read() & 0xff) << 0;

        return b | g | r;
    }

    public static int getInt(byte[] array) {
        wrapped.rewind();
        wrapped.put(array);
        wrapped.rewind();

        return wrapped.getInt();
    }

    public static void putInt(OutputStream out, int nr) throws IOException {
        wrapped.rewind();
        wrapped.putInt(nr);
        wrapped.rewind();

        for (int i = 0; i < 4; i++)
            out.write(wrapped.get());
    }

    public static void putRGBInt(OutputStream out, int nr) throws IOException {
        int b = (nr >> 16) & 0xff;
        int g = (nr >> 8) & 0xff;
        int r = (nr >> 0) & 0xff;

        out.write(b);
        out.write(g);
        out.write(r);
    }
}
