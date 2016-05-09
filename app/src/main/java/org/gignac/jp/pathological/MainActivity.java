package org.gignac.jp.pathological;

import android.app.*;
import android.net.Uri;
import android.os.*;
import android.content.pm.*;
import android.content.*;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.PopupMenu;

@SuppressWarnings("unused")
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
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        v = (LevelSelectView)findViewById(R.id.levelSelect);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        v.onResume();
    }

    public void showIntroMenu(View v) {
        final PopupMenu menu = new PopupMenu(this, v);
        menu.getMenuInflater().inflate(R.menu.intromenu, menu.getMenu());
        menu.show();
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
}
