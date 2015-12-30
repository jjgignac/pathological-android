package org.gignac.jp.pathological;

import android.app.*;
import android.os.*;
import android.content.pm.*;
import android.content.*;

public class MainActivity extends Activity
{
    private LevelSelectView v;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Prevent multiple instances of the app
        if (!isTaskRoot()) {
            final Intent intent = getIntent();
            final String intentAction = intent.getAction(); 
            if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) &&
                intentAction != null && intentAction.equals(Intent.ACTION_MAIN)) {
                finish();
                return;       
            }
        }

        setContentView(R.layout.main);
        setRequestedOrientation(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        v = (LevelSelectView)findViewById(R.id.levelSelect);
    }

    @Override
    public void onPause() {
        super.onPause();
        v.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        v.onResume();
    }
}
