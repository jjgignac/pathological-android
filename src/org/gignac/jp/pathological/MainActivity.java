package org.gignac.jp.pathological;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.content.pm.*;

public class MainActivity extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		setRequestedOrientation(
			ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
		GameResources gr = new GameResources(this);
		Sprite.setResources(getResources());
		((LevelSelectView)findViewById(R.id.levelSelect)).setup(gr);
    }
}
