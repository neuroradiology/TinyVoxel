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

/**
 *
 * @author Kajos
 */

public class AndroidLauncher extends AndroidApplication {

    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final RelativeLayout layout = new RelativeLayout(this);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useAccelerometer = false;
        config.useCompass = false;
        config.useWakelock = true;

        Config.set(new AndroidConfig());
        View gameView = initializeForView(new Game(new TouchPadController(), 4), config);

        layout.addView(gameView);
        setContentView(layout);
    }
}
