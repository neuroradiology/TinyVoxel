package com.toet.TinyVoxel.android;

import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.toet.TinyVoxel.Config;
import com.toet.TinyVoxel.GameControllers.TouchPadController;
import com.toet.TinyVoxel.Game;
import com.toet.TinyVoxel.OuyaController;

/**
 *
 * @author Kajos
 */

public class AndroidLauncher extends AndroidApplication {
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useAccelerometer = false;
        config.useCompass = false;
        config.useWakelock = true;

        Config.set(new AndroidConfig());
        initialize(new Game(new OuyaController(), 8), config);
    }
}
