package com.toet.TinyVoxel.Renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;


import static com.badlogic.gdx.graphics.GL20.*;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.toet.TinyVoxel.Config;
import com.toet.TinyVoxel.Debug.LogHandler;
import com.toet.TinyVoxel.Renderer.Tools.BrushUtils;
import com.toet.TinyVoxel.Shaders.ShaderManager;
import com.toet.TinyVoxel.Shadow.ShadowManager;
import com.toet.TinyVoxel.Renderer.Bundles.*;
import com.toet.TinyVoxel.Renderer.Wrapped.WrappedInteger;
import com.toet.TinyVoxel.Time;

/**
 * Created by Kajos on 7/11/2014.
 */
public class Manager implements Disposable {
    private ShaderProgram blockDDAShader, blockStepShader;

    public Array<Bundle> bundleArray = new Array<Bundle>(false, 0, Bundle.class);
    public Array<Bundle> transparentBundleArray = new Array<Bundle>(false, 0, Bundle.class);

    private float lod = Config.FAR;
    private int chunkLod = MathUtils.ceil(lod / (float) Config.GRID_SIZE);

    public Bundle selectedBundle;
    public Bundle mainBundle;

    private Floor floor;

    public Manager() {
        if (Config.OFFSET_DETAIL != 0)
            blockDDAShader = ShaderManager.get().getShader("shaders/TextureDDAFragment.glsl", "shaders/TextureVertex.glsl"); //DDAbackup.txt
        blockStepShader = ShaderManager.get().getShader("shaders/TextureFragment.glsl", "shaders/TextureVertex.glsl");

        floor = new Floor();

        // This is required for proper functioning - there needs to be a main grid to fall back to
        selectedBundle = mainBundle = new GroundBundle();
        selectedBundle.init("main", true);
        selectedBundle.solid = false;
        selectedBundle.updateMatrix();
        bundleArray.add(selectedBundle);

        // Some code to demo the multiple grids

        ArrayBundle cloud = new ArrayBundle();
        cloud.init("house2", true);
        cloud.visible = false;
        bundleArray.add(cloud);
        for (int x = 0; x < Config.BUNDLES; x++)
            for (int z = 0; z < Config.BUNDLES; z++) {
                ArrayBundle bundle2 = new ArrayBundle();
                bundle2.init(cloud);
                bundle2.transform.idt();
                bundle2.transform.translate(
                        ((float)(x - Config.BUNDLES /2) + MathUtils.random(0f, 1f)) * 4 * Config.BUNDLE_SPACING / 2,
                         0f,
                        ((float)(z - Config.BUNDLES /2) + MathUtils.random(0f, 1f)) * 4 * Config.BUNDLE_SPACING / 2);
                bundle2.transform.rotate(Vector3.Y, MathUtils.random(0f, 360f));

                bundle2.updateMatrix();
                bundleArray.add(bundle2);
            }

        ArrayBundle tree2 = new ArrayBundle();
        tree2.init("treeplan1", true);
        tree2.visible = false;
        bundleArray.add(tree2);
        for (int x = 0; x < Config.BUNDLES; x++)
            for (int z = 0; z < Config.BUNDLES; z++) {
                ArrayBundle bundle2 = new ArrayBundle();
                bundle2.init(tree2);
                bundle2.transform.idt();
                bundle2.transform.translate(
                        ((float)(x - Config.BUNDLES /2) + MathUtils.random(0f, 1f)) * 4 * Config.BUNDLE_SPACING / 2,
                        0f,
                        ((float)(z - Config.BUNDLES /2) + MathUtils.random(0f, 1f)) * 4 * Config.BUNDLE_SPACING / 2);
                bundle2.transform.rotate(Vector3.Y, MathUtils.random(0f, 360f));

                bundle2.updateMatrix();
                bundleArray.add(bundle2);
            }

        // Sort on references, put linked bundles next to each other for easy access
        bundleArray.sort();
        transparentBundleArray.sort();
    }

    public void update() {
    }

    public void render(final PerspectiveCamera camera) {
        int frLod = chunkLod * Config.GRID_SIZE;

        for (int i = 0; i < bundleArray.size; i++) {
            Bundle bundle = bundleArray.items[i];
            bundle.cleanUpEmptyGrids();
            removeBundleIfEmpty(bundle);
            // Do frustum culling
            if (bundle != selectedBundle && (!bundle.visible || !bundle.bundleInFrustum(camera, frLod))) {
                bundle.skip = true;
                continue;
            }
            bundle.skip = false;
        }

        for (int i = 0; i < transparentBundleArray.size; i++) {
            Bundle bundle = transparentBundleArray.items[i];
            bundle.cleanUpEmptyGrids();
            removeBundleIfEmpty(bundle);
            if (bundle != selectedBundle && (!bundle.visible || !bundle.bundleInFrustum(camera, frLod))) {
                bundle.skip = true;
                continue;
            }
            bundle.skip = false;
        }

        Gdx.graphics.getGL20().glClearColor(Config.get().BACKGROUND_COLOR.r, Config.get().BACKGROUND_COLOR.g, Config.get().BACKGROUND_COLOR.b, 1f);

        Gdx.graphics.getGL20().glColorMask(true, true, true, true);

        Gdx.graphics.getGL20().glEnable(GL_DEPTH_TEST);

        Gdx.graphics.getGL20().glClearDepthf(1);
        Gdx.graphics.getGL20().glDepthFunc(GL_LESS);

        Gdx.graphics.getGL20().glDepthMask(true);
        Gdx.graphics.getGL20().glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);

        Gdx.graphics.getGL20().glEnable(GL_CULL_FACE);
        Gdx.graphics.getGL20().glCullFace(GL_BACK);

        Gdx.graphics.getGL20().glDisable(GL_BLEND);

        int offsetDetail = Config.OFFSET_DETAIL;
        int drawn = 0;
        boolean setLod = false;
        float newLod = lod;

        int halfDepth = 0;

        floor.render(camera, lod);

        Gdx.graphics.getGL20().glActiveTexture(GL20.GL_TEXTURE0);

        boolean highDetailClose = Config.OFFSET_DETAIL != 0;

        if (highDetailClose)
        {
            blockDDAShader.begin();

            blockDDAShader.setUniformf("alphaOverwrite", 1f);
            blockDDAShader.setUniformf("backgroundColor", Config.get().BACKGROUND_COLOR);
            blockDDAShader.setUniformf("cameraPos", camera.position);
            blockDDAShader.setUniformMatrix("cameraTrans", camera.combined);

            blockDDAShader.setUniformf("gridSize", Config.GRID_SIZE);

            blockDDAShader.setUniformi("voxelTexture", 0);

            // debug

            blockDDAShader.setUniformf("lod", lod);
            blockDDAShader.setUniformf("detailLod", Config.GRID_SIZE * offsetDetail - Config.GRID_SIZE);

            int add = 1;
            while (halfDepth < offsetDetail) {

                for (int i = 0; i < bundleArray.size; i++) {
                    final Bundle bundle = bundleArray.items[i];
                    if (bundle.skip)
                        continue;

                    bundle.drawLayer(halfDepth, camera, blockDDAShader, lod, 0);
                }


                halfDepth += add;
            }

            blockDDAShader.end();
        }

        {
            blockStepShader.begin();

            blockStepShader.setUniformf("alphaOverwrite", 1f);
            blockStepShader.setUniformf("backgroundColor", Config.get().BACKGROUND_COLOR);
            blockStepShader.setUniformf("cameraPos", camera.position);
            blockStepShader.setUniformMatrix("cameraTrans", camera.combined);

            blockStepShader.setUniformf("gridSize", Config.GRID_SIZE);

            blockStepShader.setUniformi("voxelTexture", 0);

            blockStepShader.setUniformf("lod", lod);

            int add = 1;
            while (halfDepth <= chunkLod) {
                if (!setLod && drawn > Config.get().getLOD()) {
                    setLod = true;
                    float delta = MathUtils.clamp(Time.getDelta(), 0f, 1f) * Config.LOD_SPEED;
                    newLod *= 1f - delta;
                    newLod += halfDepth * Config.GRID_SIZE * delta;
                }

                for (int i = 0; i < bundleArray.size; i++) {
                    final Bundle bundle = bundleArray.items[i];
                    if (bundle.skip)
                        continue;

                    drawn += bundle.drawLayer(halfDepth, camera, blockStepShader, lod, 2);
                }

                halfDepth += add;
            }

            blockStepShader.end();
        }

        // Set new dynamic LOD
        boolean skipTransparent = true;
        for (int i = 0; i < transparentBundleArray.size; i++) {
            Bundle bundle = transparentBundleArray.items[i];
            if (!bundle.skip) {
                skipTransparent = false;
                break;
            }
        }
        if (skipTransparent) {
            if (!setLod) {
                float delta = MathUtils.clamp(Time.getDelta(), 0f, 1f) * Config.LOD_SPEED;
                lod *= 1f - delta;
                lod += Config.FAR * delta;
            } else
                lod = newLod;

            lod = Math.min(lod, Config.FAR);
            chunkLod = MathUtils.ceil(lod / (float) Config.GRID_SIZE);
            camera.far = lod;
            return;
        }

        /*
        Form here on is the transparent rendering part..
         */

        // Transparent write depth

        Gdx.graphics.getGL20().glColorMask(false, false, false, true);

        halfDepth = 0;

        if (highDetailClose)
        {
            blockDDAShader.begin();

            blockDDAShader.setUniformf("alphaOverwrite", 1f);
            blockDDAShader.setUniformf("backgroundColor", Config.get().BACKGROUND_COLOR);
            blockDDAShader.setUniformf("cameraPos", camera.position);
            blockDDAShader.setUniformMatrix("cameraTrans", camera.combined);

            blockDDAShader.setUniformf("gridSize", Config.GRID_SIZE);

            blockDDAShader.setUniformi("voxelTexture", 0);
            blockDDAShader.setUniformi("paletteCoordinates", 1);

            blockDDAShader.setUniformf("lod", lod);
            blockDDAShader.setUniformf("detailLod", Config.GRID_SIZE * offsetDetail - Config.GRID_SIZE / 2);

            int add = 1;
            while (halfDepth < offsetDetail) {
                for (int i = 0; i < transparentBundleArray.size; i++) {
                    final Bundle bundle = transparentBundleArray.items[i];
                    if (bundle.skip)
                        continue;

                    bundle.drawLayer(halfDepth, camera, blockDDAShader, lod, 0);
                }

                halfDepth += add;
            }
            blockDDAShader.end();
        }
        {
            blockStepShader.begin();

            blockStepShader.setUniformf("alphaOverwrite", 1f);
            blockStepShader.setUniformf("backgroundColor", Config.get().BACKGROUND_COLOR);
            blockStepShader.setUniformf("cameraPos", camera.position);
            blockStepShader.setUniformMatrix("cameraTrans", camera.combined);

            blockStepShader.setUniformf("gridSize", Config.GRID_SIZE);

            blockStepShader.setUniformi("voxelTexture", 0);

            blockStepShader.setUniformf("lod", lod);

            int add = 1;
            while (halfDepth <= chunkLod) {
                for (int i = 0; i < transparentBundleArray.size; i++) {
                    Bundle bundle = transparentBundleArray.items[i];
                    if (bundle.skip)
                        continue;

                    bundle.drawLayer(halfDepth, camera, blockStepShader, lod, 2);
                }

                halfDepth += add;
            }

            blockStepShader.end();
        }

        //Transparent write color
        float alpha = 0.75f;

        Gdx.graphics.getGL20().glEnable(GL_BLEND);
        Gdx.graphics.getGL20().glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        Gdx.graphics.getGL20().glColorMask(true, true, true, false);
        Gdx.graphics.getGL20().glDepthFunc(GL_EQUAL);
        halfDepth = 0;
        if (highDetailClose)
        {
            blockDDAShader.begin();

            blockDDAShader.setUniformf("alphaOverwrite", alpha);
            blockDDAShader.setUniformf("backgroundColor", Config.get().BACKGROUND_COLOR);
            blockDDAShader.setUniformf("cameraPos", camera.position);
            blockDDAShader.setUniformMatrix("cameraTrans", camera.combined);

            blockDDAShader.setUniformf("gridSize", Config.GRID_SIZE);

            blockDDAShader.setUniformi("voxelTexture", 0);
            //blockDDAShader.setUniformi("paletteCoordinates", 1);

            // debug

            blockDDAShader.setUniformf("lod", lod);
            blockDDAShader.setUniformf("detailLod", Config.GRID_SIZE * offsetDetail - Config.GRID_SIZE / 2);

            int add = 1;
            while (halfDepth < offsetDetail) {
                for (int i = 0; i < transparentBundleArray.size; i++) {
                    Bundle bundle = transparentBundleArray.items[i];
                    if (bundle.skip)
                        continue;

                    bundle.drawLayer(halfDepth, camera, blockDDAShader, lod, 0);
                }

                halfDepth += add;
            }
            blockDDAShader.end();
        }
        {
            blockStepShader.begin();

            blockStepShader.setUniformf("alphaOverwrite", alpha);
            blockStepShader.setUniformf("backgroundColor", Config.get().BACKGROUND_COLOR);
            blockStepShader.setUniformf("cameraPos", camera.position);
            blockStepShader.setUniformMatrix("cameraTrans", camera.combined);

            blockStepShader.setUniformf("gridSize", Config.GRID_SIZE);

            blockStepShader.setUniformi("voxelTexture", 0);

            blockStepShader.setUniformf("lod", lod);

            int add = 1;
            while (halfDepth <= chunkLod) {
                for (int i = 0; i < transparentBundleArray.size; i++) {
                    Bundle bundle = transparentBundleArray.items[i];
                    if (bundle.skip)
                        continue;

                    bundle.drawLayer(halfDepth, camera, blockStepShader, lod, 2);
                }

                halfDepth += add;
            }

            blockStepShader.end();
        }

        // Set new dynamic LOD
        if (!setLod) {
            float delta = MathUtils.clamp(Time.getDelta(), 0f, 1f) * Config.LOD_SPEED;
            lod *= 1f - delta;
            lod += Config.FAR * delta;
        } else
            lod = newLod;

        lod = Math.min(lod, Config.FAR);
        chunkLod = MathUtils.ceil(lod / (float) Config.GRID_SIZE);
        camera.far = lod;

        Gdx.graphics.getGL20().glDisable(GL_BLEND);
        Gdx.graphics.getGL20().glDepthFunc(GL_LESS);

        LogHandler.exitOnGLError();
    }

    public void selectBundle(Bundle bundle) {
        selectedBundle.solid = true;
        selectedBundle = bundle;
        selectedBundle.solid = false;
    }

    float drawCounter = 0;
    public void drawShadows(PerspectiveCamera camera) {
        drawCounter -= Time.getDelta();
        if (drawCounter < 0) {
            drawCounter = Config.SHADOW_FRAMES;
        } else {
            return;
        }

        ShadowManager.get().begin(camera.position, lod);

        ShaderProgram shadowShader = ShadowManager.get().shader;

        for (int i = 0; i < bundleArray.size; i++) {
            Bundle bundle = bundleArray.items[i];
            if (!bundle.visible || !bundle.inLargeRange || !bundle.inRange)
                continue;
            bundle.drawShadows(camera, shadowShader, lod, 3);
        }

        ShadowManager.get().end();
    }

    public boolean collidesWith(final Vector3 point) {
        for (int i = 0; i < bundleArray.size; i++) {
            final Bundle bundle = bundleArray.items[i];
            if (bundle.skip)
                continue;

            if (bundle.collidesWith(point)) {
                return true;
            }
        }

        return false;
    }

    public boolean collidesTransparentWith(final Vector3 point) {
        for (int i = 0; i < transparentBundleArray.size; i++) {
            final Bundle bundle = transparentBundleArray.items[i];
            if (bundle.skip)
                continue;

            if (bundle.collidesWith(point)) {
                return true;
            }
        }

        return false;
    }

    public boolean collidesAllWith(Vector3 point) {
        if (collidesWith(point))
            return true;
        else if (collidesTransparentWith(point))
            return true;
        else
            return false;
    }

    public boolean collidesSphereTransparentWith(final Vector3 point, final float radius) {
        for (int i = 0; i < transparentBundleArray.size; i++) {
            final Bundle bundle = transparentBundleArray.items[i];
            if (bundle.skip)
                continue;

            if (bundle.collidesSphereWith(point, radius)) {
                return true;
            }
        }

        return false;
    }

    public boolean collidesSphereWith(final Vector3 point, final float radius) {
        for (int i = 0; i < bundleArray.size; i++) {
            final Bundle bundle = bundleArray.items[i];
            if (bundle.skip)
                continue;

            if (bundle.collidesSphereWith(point, radius)) {
                return true;
            }
        }

        return false;
    }

    public boolean collidesSphereAllWith(Vector3 point, float radius) {
        if (floor.collidesSphere(point, radius)) {
            return true;
        } else if (collidesSphereWith(point, radius))
            return true;
        else if (collidesSphereTransparentWith(point, radius))
            return true;
        else
            return false;
    }

    public Bundle collidesWith(Ray ray, Vector3 intersection) {
        float maxDistance = Config.FAR * Config.FAR;
        Bundle foundBundle = null;
        for (int i = 0; i < bundleArray.size; i++) {
            Bundle bundle = bundleArray.items[i];
            Grid grid = bundle.collidesWith(ray, intersection, maxDistance);
            if (grid != null) {
                maxDistance = ray.origin.dst2(intersection);
                foundBundle = bundle;
            }
        }
        return foundBundle;
    }

    public Bundle collidesTransparentWith(Ray ray, Vector3 intersection) {
        float maxDistance = Config.FAR * Config.FAR;
        Bundle foundBundle = null;
        for (int i = 0; i < transparentBundleArray.size; i++) {
            Bundle bundle = transparentBundleArray.items[i];
            Grid grid = bundle.collidesWith(ray, intersection, maxDistance);
            if (grid != null) {
                maxDistance = ray.origin.dst2(intersection);
                foundBundle = bundle;
            }
        }
        return foundBundle;
    }

    public Bundle collidesAllWith(Ray ray, Vector3 intersection) {
        float maxDistance = Config.FAR * Config.FAR;
        Bundle foundBundle = null;

        if (floor.collidesWith(ray, intersection, maxDistance)) {
            maxDistance = ray.origin.dst2(intersection);
            foundBundle = mainBundle;
        }

        for (int i = 0; i < bundleArray.size; i++) {
            Bundle bundle = bundleArray.items[i];
            Grid grid = bundle.collidesWith(ray, intersection, maxDistance);
            if (grid != null) {
                maxDistance = ray.origin.dst2(intersection);
                foundBundle = bundle;
            }
        }
        for (int i = 0; i < transparentBundleArray.size; i++) {
            Bundle bundle = transparentBundleArray.items[i];
            Grid grid = bundle.collidesWith(ray, intersection, maxDistance);
            if (grid != null) {
                maxDistance = ray.origin.dst2(intersection);
                foundBundle = bundle;
            }
        }
        return foundBundle;
    }

    public void removeBundle(Bundle bundle) {
        if (bundle != mainBundle) {
            LogHandler.log("Removed bundle: " + bundle.name);
            bundleArray.removeValue(bundle, true);
            transparentBundleArray.removeValue(bundle, true);
            bundle.dispose();
            selectBundle(mainBundle);
            bundleArray.sort();
            transparentBundleArray.sort();
        }
    }

    public void removeBundleIfEmpty(Bundle bundle) {
        if (bundle.getGridCount() == 0 && !bundle.isLoading.get()) {
            removeBundle(bundle);
        }
    }

    public int sizeInMB() {
        int size = 0;

        WrappedInteger prev = null;
        for (int i = 0; i < bundleArray.size; i++) {
            Bundle bundle = bundleArray.items[i];
            if (prev == bundle.references)
                continue;

            prev = bundle.references;
            size += bundle.sizeInMB();
        }

        for (int i = 0; i < transparentBundleArray.size; i++) {
            Bundle bundle = transparentBundleArray.items[i];
            if (prev == bundle.references)
                continue;

            prev = bundle.references;
            size += bundle.sizeInMB();
        }

        return size;
    }


    public boolean isLoading() {
        for (int i = 0; i < bundleArray.size; i++) {
            Bundle bundle = bundleArray.items[i];
            if (bundle.isLoading.get())
                return true;
        }

        for (int i = 0; i < transparentBundleArray.size; i++) {
            Bundle bundle = transparentBundleArray.items[i];
            if (bundle.isLoading.get())
                return true;
        }

        return false;
    }

    public void addVoxel(Vector3 position, int r, int g, int b, boolean loadImmediately) {
        addVoxel(mainBundle, position, r, g, b, loadImmediately);
    }

    public void addVoxel(Bundle bundle, Vector3 position, int r, int g, int b, boolean loadImmediately) {
        bundle.addVoxel(position, r, g, b, loadImmediately);
    }

    public void removeVoxel(Vector3 position, boolean loadImmediately) {
        for (int i = 0; i < bundleArray.size; i++) {
            Bundle bundle = bundleArray.items[i];
            removeVoxel(bundle, position, loadImmediately);
        }
        for (int i = 0; i < transparentBundleArray.size; i++) {
            Bundle bundle = transparentBundleArray.items[i];
            removeVoxel(bundle, position, loadImmediately);
        }
    }

    public void removeVoxel(Bundle bundle, Vector3 position, boolean loadImmediately) {
        bundle.removeVoxel(position, loadImmediately);

    }

    public void addBrush(Vector3 position, int radius, int r, int g, int b, BrushUtils.Brush brush) {
        addBrush(selectedBundle, position, radius, r, g, b, brush);
    }

    public void addBrush(Bundle bundle, Vector3 position, int radius, int r, int g, int b, BrushUtils.Brush brush) {
        bundle.addBrush(position, radius, r, g, b, brush);
    }

    public void addBrushLocal(Bundle bundle, Vector3 position, int radius, int r, int g, int b, BrushUtils.Brush brush) {
        bundle.addBrushLocal(position, radius, r, g, b, brush);
    }

    public void removeBrush(Vector3 position, int radius, BrushUtils.Brush brush) {
        removeBrush(selectedBundle, position, radius, brush);
    }
    public void removeBrush(Bundle bundle, Vector3 position, int radius, BrushUtils.Brush brush) {
        bundle.removeBrush(position, radius, brush);
    }

    public void saveAll() {
        WrappedInteger prev = null;

        for (int i = 0; i < bundleArray.size; i++) {
            Bundle bundle = bundleArray.items[i];
            if (prev == bundle.references)
                continue;

            prev = bundle.references;
            bundle.saveAll();
        }
        for (int i = 0; i < transparentBundleArray.size; i++) {
            Bundle bundle = transparentBundleArray.items[i];
            if (prev == bundle.references)
                continue;

            prev = bundle.references;
            bundle.saveAll();
        }
    }

    public void makeShadows() {
        WrappedInteger prev = null;
        for (int i = 0; i < bundleArray.size; i++) {
            Bundle bundle = bundleArray.items[i];
            if (prev == bundle.references || bundle == BrushUtils.get().selected)
                continue;

            prev = bundle.references;
            bundle.makeShadows();
        }
        for (int i = 0; i < transparentBundleArray.size; i++) {
            Bundle bundle = transparentBundleArray.items[i];
            if (prev == bundle.references || bundle == BrushUtils.get().selected)
                continue;

            prev = bundle.references;
            bundle.makeShadows();
        }
    }

    public void dispose() {
        for (int i = 0; i < bundleArray.size; i++) {
            Bundle bundle = bundleArray.items[i];
            bundle.dispose();
        }
        for (int i = 0; i < transparentBundleArray.size; i++) {
            Bundle bundle = transparentBundleArray.items[i];
            bundle.dispose();
        }
        if (Config.OFFSET_DETAIL != 0)
            blockDDAShader.dispose();

        floor.dispose();
    }
}
