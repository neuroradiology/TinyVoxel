package com.toet.TinyVoxel.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.*;
import com.toet.TinyVoxel.Debug.LogHandler;

/**
 * Created by Kajos on 9/7/2014.
 */
public class GUI {
    public Stage stage;
    protected Viewport viewport;
    protected static GUI INSTANCE;

    protected static BitmapFont font;
    protected static TextButton.TextButtonStyle textButtonStyle;
    protected static TextButton.TextButtonStyle textButtonStyle2;

    protected static Label.LabelStyle labelStyle;
    protected static SelectBox.SelectBoxStyle selectBoxStyle;
    protected static CheckBox.CheckBoxStyle checkBoxStyle;

    protected static NinePatchDrawable ninePatch;
    protected static NinePatchDrawable ninePatch2;
    protected static Texture texture;
    protected static Texture texture2;

    protected final static int MAX_LABELS = 15;
    protected Label[] labels = new Label[MAX_LABELS];


    public static GUI get() {
        if (INSTANCE == null)
            INSTANCE = new GUI();

        return INSTANCE;
    }

    public void addToMultiplexer(InputMultiplexer multiplexer) {
        multiplexer.addProcessor(stage);
    }
    public void removeFromMultiplexer(InputMultiplexer multiplexer) {
        multiplexer.removeProcessor(stage);
    }

    private static NinePatch processNinePatchFile(Texture t) {
        final int width = t.getWidth() - 2;
        final int height = t.getHeight() - 2;
        return new NinePatch(new TextureRegion(t, 1, 1, width, height), 8, 8, 8, 8);
    }

    private static boolean isLoaded = false;
    public static void load() {
        if (isLoaded)
            return;

        isLoaded = true;

        font = new BitmapFont(Gdx.files.internal("font/Foobar Pro-Regular-32.fnt"));
        font.setScale(.6f, .6f);
        Skin prepSkin = new Skin();

        // Store the default libgdx font under the name "default".
        prepSkin.add("default", font);

        texture = new Texture(Gdx.files.internal("textures/np.png"));
        texture2 = new Texture(Gdx.files.internal("textures/np2.png"));
        ninePatch = new NinePatchDrawable(processNinePatchFile(texture));
        ninePatch2 = new NinePatchDrawable(processNinePatchFile(texture2));

        checkBoxStyle = new CheckBox.CheckBoxStyle();
        checkBoxStyle.font = prepSkin.getFont("default");
        checkBoxStyle.checkboxOff = ninePatch;
        checkBoxStyle.checkboxOn = ninePatch2;

        textButtonStyle = new TextButton.TextButtonStyle(ninePatch, ninePatch, ninePatch, prepSkin.getFont("default"));
        textButtonStyle.downFontColor = Color.GRAY;
        textButtonStyle.fontColor = Color.WHITE;
        textButtonStyle.overFontColor = Color.BLACK;
        textButtonStyle.font = prepSkin.getFont("default");
        prepSkin.add("default", textButtonStyle);

        textButtonStyle2 = new TextButton.TextButtonStyle(ninePatch2, ninePatch2, ninePatch2, prepSkin.getFont("default"));
        textButtonStyle2.downFontColor = Color.GRAY;
        textButtonStyle2.fontColor = Color.WHITE;
        textButtonStyle2.overFontColor = Color.BLACK;
        textButtonStyle2.font = prepSkin.getFont("default");

        ScrollPane.ScrollPaneStyle scrollPaneStyle = new ScrollPane.ScrollPaneStyle(ninePatch, ninePatch2, ninePatch2, ninePatch2, ninePatch2);
        List.ListStyle listStyle = new List.ListStyle(prepSkin.getFont("default"), Color.BLACK, Color.WHITE, ninePatch2);

        selectBoxStyle = new SelectBox.SelectBoxStyle(prepSkin.getFont("default"), Color.WHITE, ninePatch, scrollPaneStyle, listStyle);
        prepSkin.add("default", selectBoxStyle);

        labelStyle = new Label.LabelStyle();
        labelStyle.font = prepSkin.getFont("default");
        labelStyle.fontColor = Color.WHITE;
        prepSkin.add("default", labelStyle);
    }

    public GUI() {
        load();

        viewport = new StretchViewport(1000,1000);
        stage = new Stage(viewport);
    }

    public Image addPalette(int x, int y, Pixmap palette, InputListener clickListener) {
        Image image = new Image(new Texture(palette));
        image.setPosition(x, y);
        image.addListener(clickListener);
        stage.addActor(image);
        return image;
    }

    public Image addImage(String filename, int x, int y, int width, int height) {
        Image image = new Image(new Texture(Gdx.files.internal(filename)));
        image.setPosition(x, y);
        image.setWidth(width);
        image.setHeight(height);
        image.setOrigin(width / 2, height / 2);
        stage.addActor(image);
        return image;
    }

    public SelectBox addSelectBox(int x, int y, Array items) {
        final SelectBox box = new SelectBox(selectBoxStyle);
        box.setItems(items);
        box.setWidth(150f);
        box.setPosition(x, y);
        stage.addActor(box);
        return box;
    }

    public CheckBox addCheckBox(int x, int y, String text) {
        final CheckBox box = new CheckBox(" "+text, checkBoxStyle);
        box.setPosition(x, y);
        stage.addActor(box);
        return box;
    }

    public TextButton addButton(String name, int x, int y, InputListener clickListener) {
        TextButton button = new TextButton(name, textButtonStyle);
        button.setPosition(x, y);
        button.setTransform(true);
        button.addListener(clickListener);
        button.pad(0);
        stage.addActor(button);
        return button;
    }

    public ImageButton addImageButton(String file, int x, int y, int width, int height, InputListener clickListener) {
        Texture tex = new Texture(Gdx.files.internal(file));
        Drawable draw = new TextureRegionDrawable(new TextureRegion(tex));
        ImageButton button = new ImageButton(draw);
        button.setOrigin(width / 2, height / 2);
        button.setWidth(width);
        button.setHeight(height);
        button.setPosition(x, y);
        button.setTransform(true);
        button.addListener(clickListener);
        button.pad(0);
        stage.addActor(button);
        return button;
    }

    public TextButton addButtonOther(String name, int x, int y, InputListener clickListener) {
        TextButton button = new TextButton(name, textButtonStyle2);
        button.setPosition(x, y);
        button.setTransform(true);
        button.addListener(clickListener);
        button.pad(0);
        stage.addActor(button);
        return button;
    }

    public Label setLabel(int id, String name, int x, int y) {
        if (id >= MAX_LABELS) {
            LogHandler.log("Not enough space for labels!");
            return null;
        }

        Label label = labels[id];
        if (label == null) {
            label = new Label(name, labelStyle);
            label.setPosition(x, y);
            labels[id] = label;
            stage.addActor(label);
            enableLabel(id);
        } else {
            label.setPosition(x,y);
            label.setText(name);
            enableLabel(id);
        }

        return label;
    }

    public void enableLabel(int id) {
        if (labels[id] != null) {
            labels[id].setVisible(true);
        }
    }

    public void disableLabel(int id) {
        if (labels[id] != null) {
            labels[id].setVisible(false);
        }
    }

    public boolean render() {
        stage.act();
        stage.draw();
        return true;
    }

    public void resize(int width, int height) {
        viewport.update(width, height, false);
    }
}
