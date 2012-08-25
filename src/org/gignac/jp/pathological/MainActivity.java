package org.gignac.jp.pathological;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.content.pm.*;

public class MainActivity extends Activity
{
	private LevelSelectView v;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
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
