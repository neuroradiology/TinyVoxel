package com.toet.TinyVoxel.Renderer.Tools;

import com.toet.TinyVoxel.Config;
import com.toet.TinyVoxel.Renderer.Bundles.Bundle;
import com.toet.TinyVoxel.Renderer.Manager;

/**
 * Created by Kajos on 9/13/2014.
 */
public class GridUtils {
    private static GridUtils INSTANCE = null;

    public Bundle selected = null;
    private boolean isCopy = false;

    public static GridUtils get() {
        if (INSTANCE == null)
            INSTANCE = new GridUtils();

        return INSTANCE;
    }

    public void select(Bundle bundle, Manager manager) {
        removeSelection(manager);

        isCopy = false;

        selected = bundle;

        selected.updateMatrix();
        selected.solid = true;
        selected.visible = true;
        selected.ableToCollide = false;

        manager.transparentBundleArray.removeValue(bundle, true);
        manager.bundleArray.removeValue(bundle, true);

        if (Config.get().getTransparentTools()) {
            manager.transparentBundleArray.add(selected);
            manager.transparentBundleArray.sort();
        } else {
            manager.bundleArray.add(selected);
            manager.bundleArray.sort();
        }
    }

    public void selectCopy(Bundle bundle, Manager manager) {
        removeSelection(manager);

        isCopy = true;

        selected = bundle.copy();
        selected.solid = true;
        selected.visible = true;
        selected.ableToCollide = false;

        if (Config.get().getTransparentTools()) {
            manager.transparentBundleArray.add(selected);
            manager.transparentBundleArray.sort();
        } else {
            manager.bundleArray.add(selected);
            manager.bundleArray.sort();
        }

    }

    public void removeSelection(Manager manager) {
        if (selected != null) {
            if (isCopy) {
                removeSelectionCopy(manager);
                return;
            }

            if (Config.get().getTransparentTools())
                manager.transparentBundleArray.removeValue(selected, true);
            else
                manager.bundleArray.removeValue(selected, true);

            selected.solid = true;
            selected.visible = true;
            selected.ableToCollide = true;

            manager.bundleArray.add(selected);
            manager.bundleArray.sort();

            selected = null;
        }
    }

    private void removeSelectionCopy(Manager manager) {
        if (selected != null) {
            if (Config.get().getTransparentTools()) {
                manager.transparentBundleArray.removeValue(selected, true);
                manager.transparentBundleArray.sort();
            } else {
                manager.bundleArray.removeValue(selected, true);
                manager.bundleArray.sort();
            }

            selected.dispose();

            selected = null;
        }
    }

    public void place(Manager manager) {
        if (selected == null)
            return;

        Bundle newBundle = selected;
        newBundle.solid = true;
        newBundle.visible = true;
        newBundle.ableToCollide = true;
        newBundle.updateMatrix();

        if (Config.get().getTransparentTools()) {
            manager.transparentBundleArray.removeValue(selected, true);
            manager.transparentBundleArray.sort();
        } else {
            manager.bundleArray.removeValue(selected, true);
            manager.bundleArray.sort();
        }

        manager.bundleArray.add(newBundle);

        selected = null;
    }

    public void placeCopy(Manager manager) {
        if (selected == null)
            return;

        if (Config.get().getTransparentTools()) {
            manager.transparentBundleArray.removeValue(selected, true);
            manager.transparentBundleArray.sort();
        } else {
            manager.bundleArray.removeValue(selected, true);
            manager.bundleArray.sort();
        }

        Bundle newBundle = selected;
        newBundle.solid = true;
        newBundle.visible = true;
        newBundle.ableToCollide = true;
        newBundle.updateMatrix();
        manager.bundleArray.add(newBundle);

        selected = null;
    }
}
