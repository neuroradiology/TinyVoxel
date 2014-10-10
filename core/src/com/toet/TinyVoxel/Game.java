/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.toet.TinyVoxel;

import com.badlogic.gdx.*;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.toet.TinyVoxel.Character.Character;
import com.toet.TinyVoxel.Debug.LogHandler;
import com.toet.TinyVoxel.GameControllers.*;
import com.toet.TinyVoxel.Renderer.Tools.BrushUtils;
import com.toet.TinyVoxel.Renderer.Tools.GridUtils;
import com.toet.TinyVoxel.Screens.GUI;
import com.toet.TinyVoxel.Screens.Menu;
import com.toet.TinyVoxel.Shaders.ShaderManager;
import com.toet.TinyVoxel.Renderer.*;
import com.toet.TinyVoxel.Renderer.Bundles.ArrayBundle;
import com.toet.TinyVoxel.Renderer.Bundles.Bundle;
import com.toet.TinyVoxel.Util.*;

import static com.badlogic.gdx.graphics.GL20.*;

/**
 *
 * @author Kajos
 */
public class Game extends ApplicationAdapter {
    private static AssetManager assetManager;

    public static AssetManager getAssetManager() {
        if (assetManager == null)
            assetManager = new AssetManager();

        return assetManager;
    }

    public static void refresh() {
        if (assetManager != null)
            assetManager.dispose();
        assetManager = null;
    }

    PerspectiveCamera camera;
    Character character;
    Manager manager;
    CharacterController controller;
    Music music;
    Preferences prefs;

    int selectedMode = MODE.BRUSH.ordinal();
    int selectedBrush = 0;

    boolean showMainMenu;
    boolean releaseMenuButton;
    boolean firstRun;

    FrameBuffer postFBO;
    FullscreenQuad quad;

    InputMultiplexer inputMultiplexer;

    boolean raycastFront = false;

    String snapGridStrings[] = {"Snap per " + Config.TINY_GRID_SIZE * 2, "Snap per " + Config.TINY_GRID_SIZE, "Snap per one", "Snap per brush size", "Snap off"};
    final float snapGridValues[] = {.5f, 1f, Config.TINY_GRID_SIZE, 0, 0};
    int snapGrid = snapGridValues.length-1;

    CheckBox createGridCheckbox;
    SelectBox modesBox;

    public Game() {
        this.controller = new KeyBoardController();
    }
    
    public Game(CharacterController controller) {
        this.controller = controller;
    }

    public Game(CharacterController controller, int prescaler) {
        this.controller = controller;
        Config.INTERNAL_RESOLUTION_PRESCALER = prescaler;
    }

    TextButton surveyButton;
    boolean loading = false;
    Image loadingImage;
    ImageButton settingsButton;

    @Override
    public void create() {

        MathUtils.random.setSeed(0);

        Gdx.app.setLogLevel(Application.LOG_DEBUG);

        SimpleMath.init();

        ShaderManager.refresh();
        refresh();
        LogHandler.exitOnGLError();

        quad = new FullscreenQuad(
                Config.get().getPostFBOShader() ? ShaderManager.get().getShader("shaders/ToonFragment.glsl", "shaders/PlainVertex.glsl") : null, true, false);

        LogHandler.exitOnGLError();

        LogHandler.log("Version: " + Config.VERSION);

        showMainMenu = false;
        releaseMenuButton = false;
        firstRun = true;

        //music = Gdx.audio.newMusic(Gdx.files.internal("sounds/Olan Mill - Pine.mp3"));
        //music.setVolume(0.3f);
       // music.play();
        //music.setLooping(true);

        prefs = Gdx.app.getPreferences("TinyVoxel");
        Config.INVERSE_MOUSE = prefs.getBoolean("mouseInvert", Config.INVERSE_MOUSE);
        Config.INTERNAL_RESOLUTION_PRESCALER = prefs.getInteger("resolution", Config.INTERNAL_RESOLUTION_PRESCALER);

        manager = new Manager();

        camera = new PerspectiveCamera(Config.get().FOV, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = Config.get().NEAR;
        camera.far = Config.get().FAR;
        camera.update(true);

        startLevel();

        surveyButton = GUI.get().addButtonOther("If you have the time, help me out by filling out this survey", 500, 100, new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.net.openURI("http://www.vrpaint.com/poll/");
            }
        });
        final TextButton brushPlus = GUI.get().addButton("+ Brush size", 530, 950, new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                growBrush();
            }
        });
        final TextButton brushMin = GUI.get().addButton("- Brush size", 530, 900, new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                shrinkBrush();
            }
        });
        final TextButton createShadows = GUI.get().addButton("Update shadows", 800, 950, new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                manager.makeShadows();
            }
        });
        GUI.get().addButton("Select main grid", 20, 950, new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                manager.selectBundle(manager.mainBundle);
            }
        });

        // Lower end of screen
        GUI.get().setLabel(9, snapGridStrings[snapGrid], 440, 50);
        GUI.get().addButton("Switch snap size", 440, 0, new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                snapGrid++;
                snapGrid %= snapGridValues.length;

                GUI.get().setLabel(9, snapGridStrings[snapGrid], 440, 50);
            }
        });
//        GUI.get().addButton("Grab mouse (ESC)", 200, 0, new ClickListener(){
//            @Override
//            public void clicked(InputEvent event, float x, float y) {
//                controller.grabMouse();
//            }
//        });
        GUI.get().addButton("Raycast / Front (Z)", 800, 0, new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                raycastFront = !raycastFront;
            }
        });
        loadingImage = GUI.get().addImage("textures/settings.png", 50, 150, 50, 50);

        // End lower end of screen

        createGridCheckbox = GUI.get().addCheckBox(20,900,"Create grid on brush");

        // Brush select
        Array<String> brushUtils = new Array<String> ();
        int count2 = BrushUtils.get().getCount();
        for (int i = 0; i < count2; i++) {
            brushUtils.add(BrushUtils.get().getBrush(i).name);
        }
        final SelectBox boxBrush = GUI.get().addSelectBox(350, 900, brushUtils);
        boxBrush.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                selectedBrush = boxBrush.getSelectedIndex();
                updateMode();
                updateBrush();
            }
        });

        // Palette
        final Pixmap palette = new Pixmap(Gdx.files.internal("textures/palette.png"));
        final Image image = GUI.get().addPalette(730, 730, palette, new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                Color color = new Color(palette.getPixel((int)x,palette.getHeight() - (int)y));
                Config.get().TOOLS_COLOR.set(color.r, color.g, color.b);
                updateBrush();

                return true;
            }
            @Override
            public void touchDragged (InputEvent event, float x, float y, int pointer) {
                Color color = new Color(palette.getPixel((int)x,palette.getHeight() - (int)y));
                Config.get().TOOLS_COLOR.set(color.r, color.g, color.b);
                updateBrush();
            }
        });

        // Mode select
        GUI.get().setLabel(11, "Mode: ", 290, 950);
        Array<String> modesArray = new Array<String> ();
        int count = modes.length;
        for (int i = 0; i < count; i++) {
            modesArray.add(modes[i]);
        }
        modesBox = GUI.get().addSelectBox(350, 950, modesArray);
        modesBox.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                selectedMode = modesBox.getSelectedIndex();
                if (selectedMode == MODE.BRUSH.ordinal() || selectedMode == MODE.BRUSH_ERASE.ordinal()) {
                    GUI.get().setLabel(12, "Brush: ", 290, 900);
                    boxBrush.setVisible(true);
                    image.setVisible(true);
                    brushMin.setVisible(true);
                    brushPlus.setVisible(true);
                    createShadows.setVisible(true);
                    createGridCheckbox.setVisible(true);
                } else {
                    GUI.get().disableLabel(12);
                    boxBrush.setVisible(false);
                    image.setVisible(false);
                    brushMin.setVisible(false);
                    brushPlus.setVisible(false);
                    createShadows.setVisible(false);
                    createGridCheckbox.setVisible(false);
                }
                updateMode();
            }
        });
        modesBox.setSelectedIndex(selectedMode);
        if (selectedMode == MODE.BRUSH.ordinal() || selectedMode == MODE.BRUSH_ERASE.ordinal()) {
            GUI.get().setLabel(12, "Brush: ", 290, 900);
            boxBrush.setVisible(true);
            image.setVisible(true);
            brushMin.setVisible(true);
            brushPlus.setVisible(true);
            createShadows.setVisible(true);
            createGridCheckbox.setVisible(true);
        } else {
            GUI.get().disableLabel(12);
            boxBrush.setVisible(false);
            image.setVisible(false);
            brushMin.setVisible(false);
            brushPlus.setVisible(false);
            createShadows.setVisible(false);
            createGridCheckbox.setVisible(false);
        }

        GUI.get().setLabel(0, "View: Left mouse button", 20, 800);
        GUI.get().setLabel(1, "Action: Middle/right mouse button or Q", 20, 770);
        GUI.get().setLabel(2, "Jump: Space or Ctrl", 20, 740);
        GUI.get().setLabel(3, "Sprint: Shift", 20, 710);
        GUI.get().setLabel(4, "Movement: A,S,W,D or arrows", 20, 680);

        GUI.get().addButtonOther("Menu", 20, 0, new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Menu.get().enable(inputMultiplexer, controller);
            }

        });

        Menu.get().addButton("Invert mouse", 400, 400, new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Config.INVERSE_MOUSE = !Config.INVERSE_MOUSE;
                prefs.putBoolean("mouseInvert", Config.INVERSE_MOUSE);
                prefs.flush();
            }
        });
        Menu.get().addButton("+ Raise resolution", 400, 470, new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                higherRes();
            }
        });
        Menu.get().addButton("- Lower resolution", 400, 540, new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                lowerRes();
            }
        });
        Menu.get().addButton("Continue", 400, 610, new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Menu.get().disable(inputMultiplexer, controller);
            }
        });

        updateMode();
        updateBrush();

        inputMultiplexer = new InputMultiplexer();
        Menu.get().addToMultiplexer(inputMultiplexer);
        GUI.get().addToMultiplexer(inputMultiplexer);
        inputMultiplexer.addProcessor(controller);
        Gdx.input.setInputProcessor(inputMultiplexer);

        Menu.get().enable(inputMultiplexer, controller);
    }

    private void lowerRes() {
        if (Config.INTERNAL_RESOLUTION_PRESCALER < 8) {
            Config.INTERNAL_RESOLUTION_PRESCALER++;
            resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            prefs.putInteger("resolution", Config.INTERNAL_RESOLUTION_PRESCALER);
            prefs.flush();
        }
    }

    private void higherRes() {
        if (Config.INTERNAL_RESOLUTION_PRESCALER > 1) {
            Config.INTERNAL_RESOLUTION_PRESCALER--;
            resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            prefs.putInteger("resolution", Config.INTERNAL_RESOLUTION_PRESCALER);
            prefs.flush();
        }
    }

    private void updateMode() {
        snapGridValues[snapGridValues.length - 2] = (float)Config.TINY_GRID_SIZE / (float)(Config.get().TOOLS_SIZE * 2);

        boolean brushMode = selectedMode == MODE.BRUSH.ordinal();
        boolean brushEraseMode = selectedMode == MODE.BRUSH_ERASE.ordinal();

        GridUtils.get().removeSelection(manager);
        cursorGrid = null;

        if (brushMode || brushEraseMode) {
            updateBrush();
            cursorGrid = BrushUtils.get().selected;
        } else {
            BrushUtils.get().removeSelection(manager);
        }
    }

    Vector3 WHITE = new Vector3(1,1,1);
    private void updateBrush() {
        if (selectedMode == MODE.BRUSH_ERASE.ordinal())
            updateBrush(WHITE);
        else
            updateBrush(Config.get().TOOLS_COLOR);
    }

    private void updateBrush(Vector3 color) {
        snapGridValues[snapGridValues.length - 2] = (float)Config.TINY_GRID_SIZE / (float)(Config.get().TOOLS_SIZE * 2);

        if (selectedMode != MODE.BRUSH_ERASE.ordinal() && selectedMode != MODE.BRUSH.ordinal())
            return;

        BrushUtils.get().addSelection(manager);
        BrushUtils.get().selected.clear();

        if (Config.get().TOOLS_SIZE > 1) {
            int r = (int)(color.x * 255f);
            int g = (int)(color.y * 255f);
            int b = (int)(color.z * 255f);

            BrushUtils.get().selected.clear();

            BrushUtils.get().selected.solid = false;
            BrushUtils.get().selected.addBrushLocal(Vector3.Zero, Config.get().TOOLS_SIZE, r, g, b, BrushUtils.get().getBrush(selectedBrush));
            BrushUtils.get().selected.solid = true;
        } else {
            int r = (int)(color.x * 255f);
            int g = (int)(color.y * 255f);
            int b = (int)(color.z * 255f);

            BrushUtils.get().selected.clear();

            BrushUtils.get().selected.solid = false;
            BrushUtils.get().selected.addVoxelLocal(Vector3.Zero, r, g, b, true);
            BrushUtils.get().selected.solid = true;
        }
    }

    private void growBrush() {
        if (Config.get().TOOLS_SIZE < Config.MAX_TOOLS_SIZE) {
            Config.get().TOOLS_SIZE++;
        }
        updateMode();
    }

    private void shrinkBrush() {
        if (Config.get().TOOLS_SIZE > 2) {
            Config.get().TOOLS_SIZE--;
        } else {
            Config.get().TOOLS_SIZE = 1;
        }
        updateMode();
    }

    Quaternion rotation = new Quaternion();
    public void setRoundedToolsPosition(Bundle bundle, Vector3 pos, boolean copySelectedBundleRotation) {
        if (!copySelectedBundleRotation) {
            bundle.transform.idt();
        }
        if (snapGrid == snapGridValues.length-1) {
            bundle.transform.setToTranslation(pos);
        } else {
            tmp.set(pos);
            tmp.scl(snapGridValues[snapGrid]);
            int x = (int) tmp.x;
            int y = (int) tmp.y;
            int z = (int) tmp.z;
            bundle.transform.setToTranslation(((float) x) / snapGridValues[snapGrid],
                    ((float) y + .5f) / snapGridValues[snapGrid],
                    ((float) z) / snapGridValues[snapGrid]);

        }
        if (copySelectedBundleRotation) {
            manager.selectedBundle.transform.getRotation(rotation);
            bundle.transform.rotate(rotation);
        }
        bundle.updateMatrix();
    }

    Vector3 tmp = new Vector3(), tmp2 = new Vector3();
    Ray tmpRay = new Ray(new Vector3(), new Vector3());
    Quaternion tmpQuat = new Quaternion();
    Matrix4 tmpMat = new Matrix4();

    int prevFPS = 0;
    int prevMB = 0;
    Vector3 raycastHit = new Vector3();
    Bundle raycastBundle = null;
    Bundle cursorGrid = null;

    private enum MODE { NONE, BRUSH, BRUSH_ERASE, SELECT, ROTATE, COPY, DRAG, ERASE };
    private static String modes[] = {"None", "Brush", "Brush erase", "Grid select", "Grid rotate", "Grid copy", "Grid drag", "Grid erase"};

    private float mbCounter = 0f;

    @Override
    public void render() {
        if (Game.getAssetManager().update()) {
            loading = manager.isLoading();

            if (loading) {
                loadingImage.setVisible(true);
                loadingImage.rotateBy(Time.getDelta() * 100f);
            } else {
                loadingImage.setVisible(false);
            }

            float x = (float)Math.sin(Time.getTime() * 4f) * 10f + 500f;
            surveyButton.setPosition(x, 100);

            Time.tick();

            controller.update();

            JobManager.get().workOne();

            tmpRay.set(camera.getPickRay(controller.getX(), controller.getY()));
            raycastBundle = manager.collidesAllWith(tmpRay, raycastHit);

            character.update(manager);

            if (cursorGrid != null) {
                if (raycastFront) {
                    if (raycastBundle != null) {
                        boolean copySelectedBundleRotation = true;
                        if (selectedMode == MODE.DRAG.ordinal() || selectedMode == MODE.COPY.ordinal()) {
                            tmp2.set(cursorGrid.boundingBox.getCenter());
                            tmp2.y = 0f;
                            raycastHit.sub(tmp2);
                            copySelectedBundleRotation = false;
                        }
                        setRoundedToolsPosition(cursorGrid, raycastHit, copySelectedBundleRotation);
                        cursorGrid.updateMatrix();
                        cursorGrid.visible = true;
                    } else {
                        cursorGrid.visible = false;
                    }
                } else {
                    tmp.set(camera.direction);
                    boolean copySelectedBundleRotation = true;
                    if (selectedMode == MODE.DRAG.ordinal() || selectedMode == MODE.COPY.ordinal()) {
                        tmp.scl(Config.PLACE_GRID_DISTANCE * cursorGrid.boundingBox.getDimensions().len());
                        tmp2.set(cursorGrid.boundingBox.getCenter());
                        tmp2.y = 0f;
                        tmp.sub(tmp2);
                        copySelectedBundleRotation = false;
                    } else {
                        tmp.scl(Config.TOOLS_DISTANCE);
                    }
                    tmp.add(camera.position);
                    setRoundedToolsPosition(cursorGrid, tmp, copySelectedBundleRotation);
                    cursorGrid.updateMatrix();
                    cursorGrid.visible = true;
                }
            }

//            for (int i = 0; i < 100; i++) {
//                //manager.collidesWith(tmpRay, tmp);
//                tmp.set(MathUtils.random(0f, 100f), MathUtils.random(0f, 10f), MathUtils.random(0f, 100f));
//                //manager.collidesWith(tmp);
//                manager.collidesSphereAllWith(tmp, 20f);
//            }

            if (postFBO != null) postFBO.begin();
            manager.update();
            manager.render(camera);

            // Highlighting boxes
            Gdx.graphics.getGL20().glEnable(GL_BLEND);
            Gdx.graphics.getGL20().glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            if (selectedMode != MODE.NONE.ordinal() && selectedMode != MODE.BRUSH.ordinal() && selectedMode != MODE.BRUSH_ERASE.ordinal()) {
                if (raycastBundle != null && raycastBundle != manager.selectedBundle) {
                    Box.get().begin(camera);
                    Box.get().render(raycastBundle.boundingBox, raycastBundle.transform, Config.HOVER_GRID_COLOR);
                    Box.get().end();
                }
            }

            Box.get().begin(camera);
            Box.get().render(manager.selectedBundle.boundingBox, manager.selectedBundle.transform, Config.SELECTED_GRID_COLOR);
            Box.get().end();

            Gdx.graphics.getGL20().glCullFace(GL_FRONT);
            if (selectedMode != MODE.NONE.ordinal() && selectedMode != MODE.BRUSH.ordinal() && selectedMode != MODE.BRUSH_ERASE.ordinal()) {
                if (raycastBundle != null && raycastBundle != manager.selectedBundle) {
                    Box.get().begin(camera);
                    Box.get().render(raycastBundle.boundingBox, raycastBundle.transform, Config.HOVER_GRID_COLOR);
                    Box.get().end();
                }
            }

            Box.get().begin(camera);
            Box.get().render(manager.selectedBundle.boundingBox, manager.selectedBundle.transform, Config.SELECTED_GRID_COLOR);
            Box.get().end();

            Gdx.graphics.getGL20().glDisable(GL_BLEND);
            if (postFBO != null) postFBO.end();

            manager.drawShadows(camera);

            Gdx.graphics.getGL20().glDisable(GL20.GL_DEPTH_TEST);
            Gdx.graphics.getGL20().glDisable(GL20.GL_CULL_FACE);

            if (postFBO != null) quad.render(postFBO.getColorBufferTexture());

            boolean menuOpen = Menu.get().render();
            if (controller.getReleaseDragPress())
                Menu.get().disable(inputMultiplexer, controller);

            if (!menuOpen) {
                // Buttons for editing
                if (selectedMode == MODE.SELECT.ordinal()) {
                    if (controller.getReleaseActionPress() &&
                            raycastBundle != null) {
                        manager.selectBundle(raycastBundle);
                    }

                } else if (selectedMode == MODE.ROTATE.ordinal()) {
                    if (raycastBundle != null && controller.getBoolean(CharacterController.ACTION.Action) && raycastBundle != manager.mainBundle) {
                        manager.selectBundle(raycastBundle);

                        tmp.set(camera.direction);
                        tmp.crs(Vector3.Y);

                        tmp2.set(Vector3.Y);

                        tmpQuat.setFromMatrix(manager.selectedBundle.inverseTransform);
                        tmpMat.set(tmpQuat);

                        tmp.mul(tmpMat);
                        tmp2.mul(tmpMat);

                        manager.selectedBundle.transform.rotate(tmp2, controller.getFloat(CharacterController.ACTION.AlternativeX) * Config.GRID_ROTATE_SPEED);
                        manager.selectedBundle.transform.rotate(tmp, controller.getFloat(CharacterController.ACTION.AlternativeY) * Config.GRID_ROTATE_SPEED);
                        manager.selectedBundle.updateMatrix();
                    }

                } else if (selectedMode == MODE.DRAG.ordinal()) {
                    if (controller.getReleaseActionPress()) {

                        if (raycastBundle != null && cursorGrid == null) {
                            if (manager.selectedBundle != raycastBundle) {
                                manager.selectBundle(raycastBundle);
                            } else if (manager.selectedBundle != manager.mainBundle) {
                                GridUtils.get().select(manager.selectedBundle, manager);
                                cursorGrid = GridUtils.get().selected;
                            }
                        } else if (cursorGrid != null) {
                            GridUtils.get().place(manager);
                            GridUtils.get().removeSelection(manager);
                            manager.selectBundle(cursorGrid);
                            cursorGrid = null;
                        }
                    }

                } else if (selectedMode == MODE.COPY.ordinal()) {
                    if (controller.getReleaseActionPress()) {

                        if (raycastBundle != null && cursorGrid == null) {
                            if (manager.selectedBundle != raycastBundle) {
                                manager.selectBundle(raycastBundle);
                            } else if (manager.selectedBundle != manager.mainBundle) {
                                GridUtils.get().selectCopy(manager.selectedBundle, manager);
                                cursorGrid = GridUtils.get().selected;
                            }
                        } else if (cursorGrid != null) {
                            GridUtils.get().placeCopy(manager);
                            GridUtils.get().removeSelection(manager);
                            manager.selectBundle(cursorGrid);
                            cursorGrid = null;
                        }
                    }

                } else if (selectedMode == MODE.ERASE.ordinal()) {
                    if (controller.getReleaseActionPress()) {

                        if (raycastBundle != null) {
                            if (manager.selectedBundle != raycastBundle) {
                                manager.selectBundle(raycastBundle);
                            } else {
                                manager.removeBundle(manager.selectedBundle);
                            }
                        }
                    }
                } else if (selectedMode == MODE.BRUSH.ordinal()) {
                    if (((!raycastFront && controller.getBoolean(CharacterController.ACTION.Action)) ||
                            (raycastFront && controller.getReleaseActionPress())) &&
                            cursorGrid != null && cursorGrid.visible == true) {

                        cursorGrid.transform.getTranslation(tmp);

                        if (createGridCheckbox.isChecked()) {
                            ArrayBundle cloud = new ArrayBundle();
                            cloud.init("new", false);
                            tmp2.set(tmp);
                            tmp2.add(Config.GRID_SIZE / 2, Config.GRID_SIZE / 2, Config.GRID_SIZE / 2);
                            cloud.transform.setToTranslation(tmp2);
                            cloud.updateMatrix();
                            manager.bundleArray.add(cloud);

                            manager.selectBundle(cloud);

                            createGridCheckbox.toggle();
                        }
                        int r = (int) (Config.get().TOOLS_COLOR.x * 255f);
                        int g = (int) (Config.get().TOOLS_COLOR.y * 255f);
                        int b = (int) (Config.get().TOOLS_COLOR.z * 255f);

                        if (Config.get().TOOLS_SIZE > 1)
                            manager.addBrush(tmp, Config.get().TOOLS_SIZE, r, g, b, BrushUtils.get().getBrush(selectedBrush));
                        else
                            manager.addVoxel(tmp, r, g, b, true);
                    }
                } else if (selectedMode == MODE.BRUSH_ERASE.ordinal()) {
                    if (((!raycastFront && controller.getBoolean(CharacterController.ACTION.Action)) ||
                            (raycastFront && controller.getReleaseActionPress())) &&
                            cursorGrid != null && cursorGrid.visible == true) {
                        cursorGrid.transform.getTranslation(tmp);
                        if (Config.get().TOOLS_SIZE > 1)
                            manager.removeBrush(tmp, Config.get().TOOLS_SIZE, BrushUtils.get().getBrush(selectedBrush));
                        else
                            manager.removeVoxel(tmp, true);
                    }
                }

                if (Gdx.input.isKeyJustPressed(Input.Keys.Z)) {
                    raycastFront = !raycastFront;
                }
                if (Gdx.input.isKeyPressed(Input.Keys.P)) {
                    manager.saveAll();
                }

                // Fps counter
                int newFPS = Gdx.graphics.getFramesPerSecond();
                if (prevFPS != newFPS)
                    GUI.get().setLabel(7, "FPS:" + newFPS, 100, 50);
                prevFPS = newFPS;

                //MB counter
                mbCounter -= Time.getDelta();
                if (mbCounter < 0) {
                    int newMB = manager.sizeInMB();
                    if (prevMB != newMB)
                        GUI.get().setLabel(13, "MB used on GPU:" + manager.sizeInMB(), 100, 100);
                    prevMB = newMB;
                    mbCounter = 2f;
                }

                GUI.get().render();
            }
            Time.emptyDelta = false;
        } else {
            Gdx.graphics.getGL20().glColorMask(true, true, true, true);
            Gdx.graphics.getGL20().glClearColor(1f - Game.getAssetManager().getProgress(), 0, 0, 1f);
            Gdx.graphics.getGL20().glClear(GL20.GL_COLOR_BUFFER_BIT);
        }
    }

    public void startLevel() {
        character = new Character(controller, camera);
        character.start();

        firstRun = false;
        showMainMenu = false;
        Time.emptyDelta = true;
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;

        if (postFBO != null)
            postFBO.dispose();
        postFBO = new FrameBuffer(Pixmap.Format.RGBA8888, width / Config.INTERNAL_RESOLUTION_PRESCALER, height / Config.INTERNAL_RESOLUTION_PRESCALER, true);
        postFBO.getColorBufferTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        GUI.get().resize(width, height);
        Menu.get().resize(width, height);

        LogHandler.log("Resized");
    }

    @Override
    public void pause() {
        //terrain.saveAll();
        LogHandler.exitOnGLError();
        Time.emptyDelta = true;
    }

    @Override
    public void resume() {
        LogHandler.exitOnGLError();
    }

    @Override
    public void dispose() {

        quad.dispose();
        postFBO.dispose();
        //skybox.dispose();
        //music.dispose();

        if (character != null)
            character.dispose();

        manager.dispose();

        ShaderManager.disposeAll();
    }
}
