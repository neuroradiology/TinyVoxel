package com.toet.TinyVoxel.Renderer.Wrapped;

/**
 * Created by Kajos on 9/23/2014.
 */
public class WrappedInteger {
    int value;

    public WrappedInteger(int value) {
        set(value);
    }

    public void set(int value) {
        this.value = value;
    }

    public int get() {
        return value;
    }

    public void increment() {
        value++;
    }

    public void decrement() {
        value--;
    }
}
