package com.toet.TinyVoxel.Util;

import com.toet.TinyVoxel.Debug.LogHandler;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Kajos on 8/22/2014.
 */
public class RLEOutputStream extends OutputStream {
    private int prev;
    private boolean first = true;
    private int count;

    private OutputStream out;

    public RLEOutputStream(OutputStream out) {
        this.out = out;
    }

    @Override
    public void write(int b) {
        throw new UnsupportedOperationException();
    }

    public void writeInt(int b) {
        try {
            if (first) {
                count = 1;
                prev = b;
                StreamUtil.putInt(out, prev);
                first = false;
            } else if (b != prev || count == 255) {
                out.write(count);
                prev = b;
                StreamUtil.putInt(out, prev);
                count = 1;
            } else {
                count++;
            }
        }catch (IOException ex) {
            LogHandler.log("Write error.");
        }
    }

    public void writeRGBInt(int b) {
        b &= 0x00ffffff;
        try {
            if (first) {
                count = 1;
                prev = b;
                StreamUtil.putRGBInt(out, prev);
                first = false;
            } else if (b != prev || count == 255) {
                out.write(count);
                prev = b;
                StreamUtil.putRGBInt(out, prev);
                count = 1;
            } else {
                count++;
            }
        }catch (IOException ex) {
            LogHandler.log("Write error.");
        }
    }

    public void finalize() {
        try {
            out.write(count);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        finish();
        out.close();
        super.close();
    }

    public void finish() throws IOException {
        if (!first) {
            out.write(count);
        }
    }
}
