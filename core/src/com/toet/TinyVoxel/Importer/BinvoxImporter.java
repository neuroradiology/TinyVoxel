package com.toet.TinyVoxel.Importer;

/**
 * Created by Kajos on 8/7/2014.
 */
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector3;
import com.toet.TinyVoxel.Config;
import com.toet.TinyVoxel.Debug.LogHandler;
import com.toet.TinyVoxel.Renderer.Bundles.Bundle;

import java.io.IOException;
import java.io.InputStream;

public class BinvoxImporter {
    private static byte[] voxels;
    private static int depth, height, width;
    private static int size;

    private static boolean readBinvox(InputStream inputStream) throws Exception
    {
        DataInputStream binvox_data = new DataInputStream(inputStream);

        //
        // read header
        //
        String line = binvox_data.readLine();  // deprecated function though
        if (!line.startsWith("#binvox")) {
            LogHandler.log("Error: first line reads [" + line + "] instead of [#binvox]");
            return false;
        }

        String version_string = line.substring(8);
        int version = Integer.parseInt(version_string);
        LogHandler.log("reading binvox version " + version);

        depth = height = width = 0;
        boolean done = false;

        while(!done) {

            line = binvox_data.readLine();

            if (line.startsWith("data")) done = true;
            else {
                if (line.startsWith("dim")) {
                    String[] dimensions = line.split(" ");
                    depth = Integer.parseInt(dimensions[1]);
                    height = Integer.parseInt(dimensions[2]);
                    width = Integer.parseInt(dimensions[3]);
                }
                else {
                    if (line.startsWith("translate")) {
                        // tx = binvox_data.readDouble();
                        // ty = binvox_data.readDouble();
                        // tz = binvox_data.readDouble();
                    }
                    else {
                        if (line.startsWith("scale")) {
                            // scale = binvox_data.readDouble();
                        }
                        else {
                            LogHandler.log("  unrecognized keyword [" + line + "], skipping");
                        }
                    }
                }
            }
        }  // while

        if (!done) {
            LogHandler.log("  error reading header");
            return false;
        }
        if (depth == 0) {
            LogHandler.log("  missing dimensions in header");
            return false;
        }

        size = width * height * depth;
        voxels = new byte[size];

        //
        // read voxel data
        //
        byte value;
        int count;
        int index = 0;
        int end_index = 0;
        int nr_voxels = 0;

        // *input >> value;  // read the linefeed char

        while(end_index < size) {

            value = binvox_data.readByte();
            // idiotic Java language doesn't have unsigned types, so we have to use an int for 'count'
            // and make sure that we don't interpret it as a negative number if bit 7 (the sign bit) is on
            count = binvox_data.readByte() & 0xff;

            end_index = index + count;
            if (end_index > size) return false;
            for(int i = index; i < end_index; i++) voxels[i] = value;

            if (value > 0) nr_voxels += count;
            index = end_index;

        }  // while

        LogHandler.log("  read " + nr_voxels + " voxels");
        return true;

    }  // read_binvox

    public static boolean readBinvox(String filespec, final Bundle gridBundle, final Vector3 endPos, final int r, final int g, final int b)
    {
        LogHandler.log("File: " + filespec);

        InputStream in = null;
        try {
            FileHandle file = Gdx.files.internal(filespec);
            if (!file.exists()) {
                LogHandler.log("File doesn't exist.");
                return false;
            }

            in = file.read();

            readBinvox(in);

            LogHandler.log("Read " + size + " voxels");

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    Vector3 tmp = new Vector3();
                    for (int z = 0; z < depth; z++) {
                        int i = z * width * height + y * width + x;
                        tmp.set(x, y, z);
                        tmp.scl(1f / (float) Config.TINY_GRID_SIZE);
                        tmp.add(endPos);
                        if (voxels[i] > 0) {
                            gridBundle.addVoxel(tmp, r, g, b, false);
                        }
                    }
                }
            }

            LogHandler.log("Printed voxels");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try
            {
                if (in != null)
                    in.close();
            }
            catch (IOException e)
            {
                return false;
            }
        }


        return true;
    }
}
