package org.gignac.jp.pathological;
import android.view.*;

public class ButtonListener
	implements View.OnClickListener
{
	Game game;

	public ButtonListener( Game game) {
		this.game = game;
	}

	public void onClick(View v)
	{
		switch(v.getId()) {
		case R.id.prevlevel:
			if(game.level > 0)
				game.playLevel(game.level-1);
			break;
		case R.id.nextlevel:
			if(game.level < game.numlevels-1)
				game.playLevel(game.level+1);
			break;
		case R.id.retry:
			game.playLevel(game.level);
			break;
		case R.id.pause:
			game.pause();
			break;
		}
	}
}
