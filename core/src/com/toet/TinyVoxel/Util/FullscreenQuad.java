/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.toet.TinyVoxel.Util;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.toet.TinyVoxel.Debug.LogHandler;
import com.badlogic.gdx.Gdx;

import static com.badlogic.gdx.graphics.GL20.*;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.toet.TinyVoxel.Shaders.ShaderManager;

/**
 *
 * @author Kajos
 */
public class FullscreenQuad {
    
    private final Mesh mesh;
    private final ShaderProgram shader;
    
    public FullscreenQuad(ShaderProgram shaderOverride, boolean flipVertical, boolean flipHorizontal) {
        if (shaderOverride == null) {
            //blitShader
            shader = ShaderManager.get().getShader("shaders/PlainFragment.glsl", "shaders/PlainVertex.glsl");
        } else
            shader = shaderOverride;
            
        //Vertices
        float[] vertices = new float[] {
                                    flipHorizontal ? 1.0f: -1f, flipVertical ? 1f : -1.0f, 0, 0, 1,    
                                    flipHorizontal ? -1.0f: 1f, flipVertical ? 1f : -1.0f, 0, 1, 1,     
                                    flipHorizontal ? -1.0f: 1f, flipVertical ? -1.0f: 1f, 0, 1, 0,    
                                    flipHorizontal ? 1.0f: -1f, flipVertical ? -1.0f: 1f, 0, 0, 0};
        
        //Mesh
        VertexAttribute attr = new VertexAttribute(VertexAttributes.Usage.Position, 3, "position");
        VertexAttribute attr2 = new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "texCoords");
        mesh = new Mesh(true, 4, 0, attr, attr2);
        mesh.setVertices(vertices);
    }

    private String[] textureStrings = new String[]{"texture0", "texture1", "texture2", "texture3", "texture4"};

    public void render(Texture... textures) {
        setStates();

        shader.begin();

        // active texture 0 at last
        for(int i = textures.length - 1; i >= 0; i--){
            textures[i].bind(i);
            shader.setUniformi(textureStrings[i], i);
        }

        mesh.render(shader, GL_TRIANGLE_FAN);

        shader.end();
    }

    public void render(Texture texture) {
        setStates();

        shader.begin();

        // active texture 0 at last
        texture.bind(0);
        shader.setUniformi(textureStrings[0], 0);

        mesh.render(shader, GL_TRIANGLE_FAN);

        shader.end();
    }

    public static void setStates() {
        Gdx.graphics.getGL20().glDisable(GL20.GL_DEPTH_TEST);
        Gdx.graphics.getGL20().glDisable(GL20.GL_BLEND);
        Gdx.graphics.getGL20().glDisable(GL_CULL_FACE);
        Gdx.graphics.getGL20().glDepthMask(false);
        Gdx.graphics.getGL20().glColorMask(true, true, true, true);
    }

    public void render(float value, Texture[] textures) {
        setStates();

        shader.begin();

        // active texture 0 at last
        for(int i = textures.length - 1; i >= 0; i--){
            textures[i].bind(i);
            shader.setUniformi(textureStrings[i], i);
        }
        shader.setUniformf("value", value);

        mesh.render(shader, GL_TRIANGLE_FAN);

        shader.end();
    }

    public void render(int value1, int value2, int value3, Matrix4 matrix, Texture[] textures) {
        setStates();

        shader.begin();

        // active texture 0 at last
        for(int i = textures.length - 1; i >= 0; i--){
            textures[i].bind(i);
            shader.setUniformi(textureStrings[i], i);
        }
        shader.setUniformf("value1", value1);
        shader.setUniformf("value2", value2);
        shader.setUniformf("value3", value3);
        shader.setUniformMatrix("matrix", matrix);

        mesh.render(shader, GL_TRIANGLE_FAN);

        shader.end();
    }

    public void render(Matrix4 mat1, Matrix4 mat2, Cubemap cubemap, Texture[] textures) {
        setStates();

        shader.begin();

        cubemap.bind(textures.length);
        shader.setUniformi("cubemap", textures.length);
        // active texture 0 at last
        for(int i = textures.length - 1; i >= 0; i--){
            textures[i].bind(i);
            shader.setUniformi(textureStrings[i], i);
        }
        shader.setUniformMatrix("mat1", mat1);
        shader.setUniformMatrix("mat2", mat2);

        mesh.render(shader, GL_TRIANGLE_FAN);

        shader.end();
    }

    public void render(Vector3 position, Texture[] textures) {
        setStates();

        shader.begin();

        // active texture 0 at last
        for(int i = textures.length - 1; i >= 0; i--){
            textures[i].bind(i);
            shader.setUniformi(textureStrings[i], i);
        }
        shader.setUniformf("position", position);

        mesh.render(shader, GL_TRIANGLE_FAN);

        shader.end();
    }
    
    public void dispose() {
        mesh.dispose();
    }
}
