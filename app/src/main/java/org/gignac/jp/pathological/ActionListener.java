package org.gignac.jp.pathological;
import android.view.*;
import android.content.*;

class ActionListener
    implements View.OnClickListener,
        DialogInterface.OnClickListener
{
    private final Game game;

    public ActionListener( Game game) {
        this.game = game;
    }

    public void onClick(View v)
    {
        switch(v.getId()) {
        case R.id.retry:
            game.playLevel(game.level);
            break;
        case R.id.pause:
            game.pause();
            break;
        }
    }

    public void onClick(DialogInterface d, int b)
    {
        switch(b) {
            case DialogInterface.BUTTON_POSITIVE:
                game.playLevel(game.level);
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                game.finish();
                break;
            case DialogInterface.BUTTON_NEUTRAL:
                game.nextLevel();
                break;
        }
    }
}
