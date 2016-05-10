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

    public void toggleMusic(View v) {
        music.toggleMute();
    }
}
