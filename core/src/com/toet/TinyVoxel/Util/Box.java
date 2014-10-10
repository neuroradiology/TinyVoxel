package com.toet.TinyVoxel.Util;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.toet.TinyVoxel.Shaders.ShaderManager;

import java.nio.FloatBuffer;

/**
 * Created by Kajos on 9/13/2014.
 */
public class Box {
    private static Box INSTANCE = null;

    public static Box get() {
        if (INSTANCE == null)
            INSTANCE = new Box();

        return INSTANCE;
    }

    Mesh mesh;
    ShaderProgram shader;

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

    public Box() {
        mesh = new Mesh(Mesh.VertexDataType.VertexBufferObject, true, CUBE.length / 3, 0, createAttributes());
        FloatBuffer vertices = mesh.getVerticesBuffer();
        vertices.rewind();
        vertices.limit(CUBE.length);
        for (int i = 0; i < CUBE.length; i += 3) {
            if (CUBE[i])
                vertices.put(1f);
            else
                vertices.put(0f);

            if (CUBE[i + 1])
                vertices.put(1f);
            else
                vertices.put(0f);

            if (CUBE[i + 2])
                vertices.put(1f);
            else
                vertices.put(0f);
        }

        vertices.rewind();

        shader = ShaderManager.get().getShader("shaders/BoxFragment.glsl", "shaders/BoxVertex.glsl");
    }

    protected VertexAttribute[] createAttributes() {
        VertexAttribute attr = new VertexAttribute(VertexAttributes.Usage.Position, 3, "position");
        return new VertexAttribute[] { attr };
    }

    public void begin(PerspectiveCamera camera) {
        shader.begin();
        shader.setUniformMatrix("cameraTrans", camera.combined);
    }

    public void render(Matrix4 dimensions) {
        shader.setUniformMatrix("modelTrans", dimensions);
        mesh.render(shader, GL20.GL_LINES);
    }

    Matrix4 tmp = new Matrix4();
    Matrix4 tmp2 = new Matrix4();
    public void render(BoundingBox dimensions, Matrix4 transform, Color color) {
        tmp2.idt();
        tmp2.translate(dimensions.getMin());
        tmp2.scl(dimensions.getDimensions());

        tmp.set(transform);

        tmp.mul(tmp2);

        shader.setUniformMatrix("modelTrans", tmp);
        shader.setUniformf("color", color);
        mesh.render(shader, GL20.GL_TRIANGLES);
    }

    public void end() {
        shader.end();
    }
}
