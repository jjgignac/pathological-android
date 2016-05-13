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

import android.app.*;
import android.net.Uri;
import android.os.*;
import android.content.pm.*;
import android.content.*;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

@SuppressWarnings("unused")
public class MainActivity extends Activity
{
    private LevelSelectView v;
    private MutableMusicPlayer music;
    public static long lastInterstitialTime = 0;  // milliseconds since epoch
    public static final long minInterstitialDelay = 60000 * 15;  // milliseconds

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        MutableMusicPlayer.isMuted = getSharedPreferences(
                "org.gignac.jp.pathological.Pathological", Context.MODE_PRIVATE)
                .getBoolean("mute", false);

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
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        v = (LevelSelectView)findViewById(R.id.levelSelect);

        music = new MutableMusicPlayer(this, R.raw.intro,
                (ImageView)findViewById(R.id.mute_music));
    }

    @Override
    public void onPause() {
        super.onPause();
        music.pause();
    }

    @Override
    public void onResume() {
        super.onResume();
        v.onResume();
        music.resume();
    }

    @Override
    public void onStart() {
        super.onStart();
        music.start();
    }

    @Override
    public void onStop() {
        super.onStop();
        music.stop();
    }

    public void showIntroMenu(View v) {
        final PopupMenu menu = new PopupMenu(this, v);
        menu.getMenuInflater().inflate(R.menu.intromenu, menu.getMenu());
        menu.show();
    }

    public void showAboutDialog(MenuItem item) {
        final PackageInfo info;
        try {
            info = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch(Exception e) {
            return;
        }

        final SpannableString text = new SpannableString(
                getString(R.string.about_dialog, info.versionName));
        Linkify.addLinks(text, Linkify.WEB_URLS);

        final AlertDialog d = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.about) + " " + getString(R.string.app_name))
                .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setMessage(text)
                .show();

        // Make links clickable.  Must be called after show()
        ((TextView)d.findViewById(android.R.id.message))
                .setMovementMethod(LinkMovementMethod.getInstance());
    }

    public void showLicenceDialog(MenuItem item) {
        WebView wv = new WebView(this);
        wv.loadUrl("file:///android_asset/gpl-3.0-standalone.html");
        wv.getSettings().setLoadWithOverviewMode(true);
        wv.getSettings().setUseWideViewPort(true);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.app_name) + " " + getString(R.string.license))
                .setView(wv)
                .setPositiveButton(R.string.open_in_browser,new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("http://www.gnu.org/licenses/gpl.html")));
                    }
                })
                .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    public void showHowToPlayDialog(MenuItem item) {
        WebView wv = new WebView(this);
        wv.loadUrl("file:///android_asset/howtoplay.html");

        new AlertDialog.Builder(this)
                .setTitle(R.string.how_to_play)
                .setView(wv)
                .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    public void toggleMusic(View v) {
        music.toggleMute();
    }

    public void share(View v) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT,
                getString(R.string.share_text,
                        "https://play.google.com/store/apps/details?id=" +
                                getClass().getPackage().getName()));
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }
}
