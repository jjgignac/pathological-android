package org.gignac.jp.pathological;

import android.content.Context;
import android.os.AsyncTask;

import java.net.URL;

class ReportStatsTask extends AsyncTask<Void, Void, Void> {
    private URL url;

    public static final int REASON_COMPLETED = 0;
    public static final int REASON_LAUNCH_TIMEOUT = 1;
    public static final int REASON_BOARD_TIMEOUT = 2;
    public static final int REASON_STARTED = 3;

    public ReportStatsTask(Context context, int level) {
        try {
            url = new URL("http://pathological.gignac.org/score.php?i=" + Util.adMobID(context) +
                    "&v=" + BuildConfig.VERSION_NAME +
                    (BuildConfig.BUILD_TYPE.equals("release") ? "" : "d") +
                    "&l=" + level + "&c=" + REASON_STARTED);
        } catch( Exception e) {
            //
        }
    }

    public ReportStatsTask(Context context, int level, int score,
                           int emptyHolePercentage,
                           int timeRemainingPercentage,
                           int reason) {

        try {
            url = new URL("http://pathological.gignac.org/score.php?i=" + Util.adMobID(context) +
                    "&v=" + BuildConfig.VERSION_NAME +
                    (BuildConfig.BUILD_TYPE.equals("release") ? "" : "d") +
                    "&l=" + level + "&s=" + score + "&e=" + emptyHolePercentage +
                    "&t=" + timeRemainingPercentage + "&c=" + reason);
        } catch( Exception e) {
            //
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            // We don't care about the response
            if(url != null) url.openStream().close();
        } catch( Exception e) {
            //
        }
        return null;
    }
}
