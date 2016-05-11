package org.gignac.jp.pathological;

import android.content.Context;
import android.os.AsyncTask;

import java.net.URL;

class ReportStatsTask extends AsyncTask<Void, Void, Void> {
    private URL url;

    public ReportStatsTask(Context context, int level, int score,
                           int emptyHolePercentage,
                           int timeRemainingPercentage) {
        try {
            url = new URL("http://pathological.gignac.org/score.php?i=" + Util.adMobID(context) +
                    "&l=" + level + "&s=" + score + "&e=" + emptyHolePercentage +
                    "&t=" + timeRemainingPercentage);
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
