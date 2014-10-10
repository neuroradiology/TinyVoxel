package com.toet.TinyVoxel.client;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.ui.*;
import com.toet.TinyVoxel.Config;
import com.toet.TinyVoxel.Game;

public class HtmlLauncher extends GwtApplication {
    static final int WIDTH = 800;
    static final int HEIGHT = 400;
    static HtmlLauncher instance;

    @Override
    public GwtApplicationConfiguration getConfig () {
        GwtApplicationConfiguration config = new GwtApplicationConfiguration(WIDTH, HEIGHT);
        config.useVsync = true;
        config.fps = 30;

        Element element = Document.get().getElementById("embed-html");
        element.getParentElement().getStyle().setOverflow(Style.Overflow.HIDDEN);
        element.getParentElement().getStyle().setBackgroundColor("#336699");

        HorizontalPanel topPanel = new HorizontalPanel();
        topPanel.setWidth("100%");
        topPanel.setHeight("100%");
        topPanel.getElement().getStyle().setPadding(0, Style.Unit.PX);
        topPanel.getElement().getStyle().setMargin(0, Style.Unit.PX);
        topPanel.getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);

        HorizontalPanel panel = new HorizontalPanel();
        panel.setWidth("100%");
        panel.setHeight("100%");
        panel.getElement().getStyle().setPadding(0, Style.Unit.PX);
        panel.getElement().getStyle().setMargin(0, Style.Unit.PX);
        panel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        panel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);

        topPanel.add(panel);

        element.appendChild(topPanel.getElement());

        config.rootPanel = panel;

        if (!Config.IS_DEBUG) {
            config.log = new TextArea();
            config.log.setVisible(false);
        }
        return config;
    }

    @Override
    public ApplicationListener getApplicationListener() {
        instance = this;
        setLogLevel(LOG_NONE);
       if (!Config.IS_DEBUG) {
            setLoadingListener(new LoadingListener() {
                @Override
                public void beforeSetup() {

                }

                @Override
                public void afterSetup() {
                    scaleCanvas();
                    setupResizeHook();
                }
            });
       }
        Config.set(new GwtConfig());
        return new Game();
    }

    void scaleCanvas() {
        Element element = Document.get().getElementById("embed-html");
        element.getStyle().setTop(0, Style.Unit.PX);
        element.getStyle().setLeft(0, Style.Unit.PX);
        element.getStyle().setPosition(Style.Position.ABSOLUTE);
        element.setAttribute("width", "" + getWindowInnerWidth() + "px");
        element.setAttribute("height", "" + getWindowInnerHeight() + "px");
        element.getStyle().setPadding(0, Style.Unit.PX);
        element.getStyle().setMargin(0, Style.Unit.PX);
        element.getStyle().setOverflow(Style.Overflow.HIDDEN);
        element.getParentElement().getStyle().setOverflow(Style.Overflow.HIDDEN);
        element.getParentElement().getStyle().setBackgroundColor("#336699");

        int innerWidth = getWindowInnerWidth();
        int innerHeight = getWindowInnerHeight();

        NodeList<Element> nl = element.getElementsByTagName("canvas");

        if (nl != null && nl.getLength() > 0) {
            Element canvas = nl.getItem(0);
            canvas.setAttribute("width", "" + innerWidth + "px");
            canvas.setAttribute("height", "" + innerHeight + "px");
            canvas.getStyle().setPadding(0, Style.Unit.PX);
            canvas.getStyle().setMargin(0, Style.Unit.PX);
            canvas.getStyle().setWidth(innerWidth, Style.Unit.PX);
            canvas.getStyle().setHeight(innerHeight, Style.Unit.PX);
        }
    }

    native int getWindowInnerWidth() /*-{
        return $wnd.innerWidth;
    }-*/;

    native int getWindowInnerHeight() /*-{
        return $wnd.innerHeight;
    }-*/;

    native void setupResizeHook() /*-{
        var htmlLauncher_onWindowResize = $entry(@com.toet.TinyVoxel.client.HtmlLauncher::handleResize());
        $wnd.addEventListener('resize', htmlLauncher_onWindowResize, false);
    }-*/;

    public static void handleResize() {
        instance.scaleCanvas();
    }
}