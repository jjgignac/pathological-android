/*
 * Copyright (C) 2016  John-Paul Gignac
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.gignac.jp.pathological;
import android.view.*;
import android.content.*;

class ActionListener
    implements View.OnClickListener,
        DialogInterface.OnClickListener
{
    private final GameActivity game;

    public ActionListener( GameActivity game) {
        this.game = game;
    }

    public void onClick(View v)
    {
        switch(v.getId()) {
        case R.id.retry:
            game.playLevel(game.level);
            break;
        case R.id.pause:
            game.togglePause();
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
