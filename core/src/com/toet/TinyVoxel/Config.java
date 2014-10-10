/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.toet.TinyVoxel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.toet.TinyVoxel.Renderer.BlockBuilder;
import com.toet.TinyVoxel.Renderer.Tools.BrushUtils;

/**
 *
 * @author Kajos
 */
public abstract class Config {
    private static Config instance;

    public static Config get() {
        return instance;
    }

    // Set platform dependent config
    public static void set(Config config) {
        instance = config;
    }

    // If debug is on, a debug textfield is shown for HTML version
    public static final boolean IS_DEBUG = true;

    // OpenGL error handling - only to be used for debugging of course
    public static final boolean ENABLE_ERROR_HANDLER = false;

    // Whether to save the shadows in the file
    public static final boolean SAVE_SHADE = false;

    // For debug purposes
    public static final boolean NO_CLIPPING = false;

    // How often to render the shadows
    public static final float SHADOW_FRAMES = 1f;

    // These functions are to be implemented platform specific.
    // Writing to textures (in a fast manner) needs to happen in a different way for
    // WebGL.
    public abstract void putPalette(int color);
    public abstract void putSinglePalette(int color);
    public abstract void uploadPalette(int width, int height);
    public abstract void uploadSinglePalette(int paletteId);
    public abstract void rewindPalette();
    public abstract void rewindSinglePalette();

    // Grid hover color
    public static Color SELECTED_GRID_COLOR = new Color(1f, 0f, 0f, 0.15f);
    // Selected grid color
    public static Color HOVER_GRID_COLOR = new Color(0f, 0f, 1f, 0.25f);

    // Times for how often to do culling etc.
    public static final float FRUSTRUM_IN_RANGE = .5f;
    public static final float FRUSTRUM_IN_RANGE_LARGE = 4f;

    public static final float FRUSTRUM_IN_SIGHT = .1f;

    // Tools
    public static final float TOOLS_DISTANCE = 5f;
    public static final float PLACE_GRID_DISTANCE = .5f;
    public static final int MAX_TOOLS_SIZE = BrushUtils.SIZE / 2;
    public int TOOLS_SIZE = MAX_TOOLS_SIZE / 2;
    public Vector3 TOOLS_COLOR = new Vector3(.5f,.5f,.5f);

    // Debug

    public static final Vector3 LIGHT_DIRECTION = new Vector3(1,2,1).nor();

    // Only used in initialization of manager
    public static final int BUNDLES = 32;
    public static final int BUNDLE_SPACING = 16;

    // Grid size
    /*
    Make sure that TINY_GRID_TOTAL < 1024 and GRID_TOTAL < 1024. OpenGL has maximum texture sizes, too be safe I kept below 1024x1024.
    The 3D voxel data is mapped to a 2D texture, as 3D textures are not supported in GLES and alike.
    If this structure is changed - for instance 16x16 (X*Z) in width and GRID_TOAL * 16 (Y) in height - it is easily accomplished to have 16x16x16 tiny grids.
     */
    public static final int GRID_SIZE = 12; // 16 tiny data size y * 4*4*4 (64) = 1024 is max size of texture and fbo (for palette)
    public static final float GRID_SIZE_F = (float)GRID_SIZE;           // fbo will be 1024 (16 * 64) * 32 (16 * 16)

    public static final int GRID_TOTAL = GRID_SIZE * GRID_SIZE * GRID_SIZE;
    public static final float GRID_CORNER_LENGTH = new Vector3(Config.GRID_SIZE / 2, Config.GRID_SIZE / 2, Config.GRID_SIZE / 2).len();

    // Tiny grid size
    public static final int TINY_GRID_SIZE = 8;
    public static final int TINY_GRID_TOTAL = TINY_GRID_SIZE * TINY_GRID_SIZE * TINY_GRID_SIZE;

    // Grid rotate tool
    public static final float GRID_ROTATE_SPEED = 100f;

    public static final float LOD_SPEED = 0.5f;

    // First .. grids are using the DDA shader. If set to 0 then DDA is deactivated.
    public static final int OFFSET_DETAIL = 2;

    public abstract int getLOD();
    public abstract boolean getPostFBOShader();
    public abstract boolean getTransparentTools();

    public static final short VERSION = 5;

    // Resolution scaler; higher is lower resolution
    public static int INTERNAL_RESOLUTION_PRESCALER = 1;

    // Camera field of view
    public float FOV = 65f;

    // Maximum far distance (can change by LOD)
    public static final float FAR = 150f;
    // Near distance (fixed)
    public static final float NEAR = .4f;

    public static boolean INVERSE_MOUSE = true;
    public float DRAG_SPEED = 350f;

    // Background color
    public Color BACKGROUND_COLOR = new Color(.7f,.7f,1f,0f);

    // Vertex constants
    public static final int MAX_BLOCK_SIZE = Config.GRID_SIZE * Config.GRID_SIZE * Config.GRID_SIZE;
    public static final int MAX_VERTEX_SIZE = MAX_BLOCK_SIZE * BlockBuilder.CUBE.length / 3; // 4 vertices per each of 6 fullSides
    public static final int ATTRIBUTES_SIZE = 5;
    public static final int MAX_FLOAT_SIZE = MAX_VERTEX_SIZE * ATTRIBUTES_SIZE; // 4 floats per vertex
}
