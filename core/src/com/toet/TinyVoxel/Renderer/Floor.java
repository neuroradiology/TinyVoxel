package com.toet.TinyVoxel.Renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.toet.TinyVoxel.Config;
import com.toet.TinyVoxel.Shaders.ShaderManager;
import com.toet.TinyVoxel.Shadow.ShadowManager;

import static com.badlogic.gdx.graphics.GL20.GL_TRIANGLES;

/**
 * Created by Kajos on 8/10/2014.
 */
public class Floor {
    public float VERTICES[] = new float[] {
        -Config.FAR, 0f, Config.FAR,
            Config.FAR, 0f, Config.FAR,
        -Config.FAR, 0f, -Config.FAR,

        -Config.FAR, 0f, -Config.FAR,
            Config.FAR, 0f, Config.FAR,
            Config.FAR, 0f, -Config.FAR
    };

    Mesh mesh;
    ShaderProgram shader;

    Matrix4 model = new Matrix4();

    Texture texture;

    public Floor() {
        mesh = new Mesh(Mesh.VertexDataType.VertexBufferObject, true, VERTICES.length / 3, 0, createAttributes());
        mesh.getVerticesBuffer().rewind();
        mesh.getVerticesBuffer().limit(VERTICES.length);
        mesh.getVerticesBuffer().put(VERTICES);
        mesh.getVerticesBuffer().rewind();

        shader = ShaderManager.get().getShader("shaders/FloorFragment.glsl", "shaders/FloorVertex.glsl");

        texture = new Texture(Gdx.files.internal("textures/desert4.jpg"), true);
        texture.setFilter(Texture.TextureFilter.MipMap, Texture.TextureFilter.Nearest);
        texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

        model.idt();
        model.setToScaling(.05f, 1f, .05f);
    }

    public boolean collidesPoint(Vector3 position) {
        return position.y < 0.0f;
    }

    public boolean collidesSphere(Vector3 position, float radius) {
        return position.y < radius;
    }

    public boolean collidesWith(Ray ray, Vector3 intersection, float maxDistance) {
        if (ray.direction.y > 0f)
            return false;

        float dist = ray.origin.y / -ray.direction.y;
        if (dist > maxDistance)
            return false;

        intersection.set(ray.direction);
        intersection.scl(dist);
        intersection.add(ray.origin);

        return true;
    }

    public void render(PerspectiveCamera camera, float lod) {
        shader.begin();
        shader.setUniformf("backgroundColor", Config.get().BACKGROUND_COLOR);
        shader.setUniformf("lod", lod);
        shader.setUniformf("cameraPos", camera.position);
        shader.setUniformMatrix("modelTrans", model);
        shader.setUniformMatrix("cameraTrans", camera.combined);
        shader.setUniformMatrix("shadowTrans", ShadowManager.get().depthBiasMatrix());
        texture.bind(0);
        shader.setUniformi("texture", 0);
        ShadowManager.get().getTexture().bind(1);
        shader.setUniformi("shadowTexture", 1);
        mesh.render(shader, GL_TRIANGLES, 0, VERTICES.length / 3);
        shader.end();
    }

    private VertexAttribute[] createAttributes() {
        VertexAttribute attr = new VertexAttribute(VertexAttributes.Usage.Position, 3, "position");
        return new VertexAttribute[] {attr };
    }

    public void dispose() {
        shader.dispose();
        texture.dispose();
        mesh.dispose();
    }
}
