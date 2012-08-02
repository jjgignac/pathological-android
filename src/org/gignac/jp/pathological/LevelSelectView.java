package org.gignac.jp.pathological;
import android.view.*;
import android.content.*;
import android.util.*;
import android.graphics.*;

public class LevelSelectView extends View
{
	private GameResources gr;

	public LevelSelectView( Context context, AttributeSet a)
	{
		super(context,a);
	}

	public void setup(GameResources gr) {
		this.gr = gr;
	}

	@Override
	public void onDraw( Canvas c) {
		c.drawBitmap( Preview.create(gr,7,0.3f), 0, 0, null);
	}
}
