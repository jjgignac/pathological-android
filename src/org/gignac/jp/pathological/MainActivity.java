package org.gignac.jp.pathological;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		GameResources gr = new GameResources(this);
		Sprite.setResources(getResources());
		((LevelSelectView)findViewById(R.id.levelSelect)).setup(gr);
    }
}
