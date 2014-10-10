package com.toet.TinyVoxel.Renderer.Wrapped;

/**
 * Created by Kajos on 9/23/2014.
 */
public class WrappedBoolean {
    boolean value;

    public WrappedBoolean(boolean value) {
        set(value);
    }

    public void set(boolean value) {
        this.value = value;
    }

    public boolean get() {
        return value;
    }
}
