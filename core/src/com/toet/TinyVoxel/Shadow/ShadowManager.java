package com.toet.TinyVoxel.Shadow;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.toet.TinyVoxel.Shaders.ShaderManager;

import static com.badlogic.gdx.graphics.GL20.*;
import static com.badlogic.gdx.graphics.GL20.GL_BACK;
import static com.badlogic.gdx.graphics.GL20.GL_CULL_FACE;

/**
 * Created by Kajos on 9/18/2014.
 */
public class ShadowManager {
    private int SHADOWMAP_RESOLUTION = 512;

    private float lod = 1f;

    private static ShadowManager INSTANCE;

    public static ShadowManager get() {
        if (INSTANCE == null)
            INSTANCE = new ShadowManager();

        return INSTANCE;
    }

    FrameBuffer fbo;
    Vector3 lightInvDir = new Vector3(0.5f,2,2);

    public OrthographicCamera camera;
    public ShaderProgram shader;

    private Quaternion rotation = new Quaternion();
    private Matrix4 rotationMatrix = new Matrix4();
    private Matrix4 rotationInverseMatrix = new Matrix4();

    public ShadowManager() {
        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, SHADOWMAP_RESOLUTION, SHADOWMAP_RESOLUTION, false);
        camera = new OrthographicCamera(1,1);
        lightInvDir.nor();
        camera.direction.set(lightInvDir);
        camera.update(true);

        camera.view.getRotation(rotation);
        rotationMatrix.rotate(rotation);
        rotationInverseMatrix.set(rotationMatrix);
        rotationInverseMatrix.inv();

        shader = ShaderManager.get().getShader("shaders/ShadowFragment.glsl", "shaders/ShadowVertex.glsl");
    }

    public void setLod(float val) {
        lod = val * 2f;

        camera.setToOrtho(true, lod, lod);
        camera.near = -lod;
        camera.far = lod*2f;

        camera.direction.set(lightInvDir);
    }

    Vector3 tmp = new Vector3();
    private void setPosition(Vector3 pos) {
        tmp.set(pos);
        tmp.mul(rotationMatrix);

        float f = lod/(float)SHADOWMAP_RESOLUTION;
        tmp.x = MathUtils.round(tmp.x/f)*f;
        tmp.y = MathUtils.round(tmp.y/f)*f;

        tmp.mul(rotationInverseMatrix);

        camera.position.set(tmp);
    }

    public void begin(Vector3 pos, float lod) {
        setLod(lod);
        setPosition(pos);
        camera.update(true);

        matrix.set(biasMatrix);
        matrix.mul(camera.combined);

        fbo.begin();
        Gdx.graphics.getGL20().glClearColor(1f, 1f, 1f, 1f);
        Gdx.graphics.getGL20().glColorMask(true, false, false, false);

        Gdx.graphics.getGL20().glDepthMask(false);

        Gdx.graphics.getGL20().glDisable(GL_DEPTH_TEST);
        Gdx.graphics.getGL20().glClear(GL_COLOR_BUFFER_BIT);

        Gdx.graphics.getGL20().glEnable(GL_CULL_FACE);
        Gdx.graphics.getGL20().glCullFace(GL_BACK);

        shader.begin();
        shader.setUniformMatrix("cameraTrans", camera.combined);
    }

    Matrix4 matrix = new Matrix4();
    Matrix4 biasMatrix = new Matrix4(new float[] {
            0.5f, 0.0f, 0.0f, 0.0f,
            0.0f, 0.5f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.5f, 0.0f,
            0.5f, 0.5f, 0.5f, 1.0f}
    );

    public Matrix4 depthBiasMatrix() {
        return matrix;
    }

    public void end() {
        shader.end();
        fbo.end();
    }

    public Texture getTexture() {
        return fbo.getColorBufferTexture();
    }

    public void dispose() {
        fbo.dispose();
    }
}
