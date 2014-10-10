package com.toet.TinyVoxel.Util;

import com.badlogic.gdx.utils.Array;
import com.toet.TinyVoxel.Time;

import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * Created by Kajos on 9/9/2014.
 */
public class JobManager {
    private static JobManager manager = null;

    public static JobManager get() {
        if (manager == null)
            manager = new JobManager();

        return manager;
    }

    private Queue<Runnable> runnables = new LinkedList<Runnable>();

    public void postRunnable(Runnable run) {
        runnables.add(run);
    }

    public void workOne() {
        if (runnables.isEmpty())
            return;

        Runnable run = runnables.remove();
        run.run();
        Time.emptyDelta = true;
    }
}
