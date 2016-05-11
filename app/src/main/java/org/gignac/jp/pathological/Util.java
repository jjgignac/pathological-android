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

import android.content.Context;
import android.provider.Settings;

import com.google.android.gms.ads.AdRequest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class Util {
    public static AdRequest getAdMobRequest(Context context) {
        AdRequest.Builder adRequestBuilder = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR);

        if( !BuildConfig.BUILD_TYPE.equals("release")) {
            // Register as a test device
            String android_id = Settings.Secure.getString(
                    context.getContentResolver(), Settings.Secure.ANDROID_ID);
            String adMobID = md5(android_id).toUpperCase();
            AdRequest adReq = adRequestBuilder.addTestDevice(adMobID).build();
            if( !adReq.isTestDevice(context)) return null;
        }

        return adRequestBuilder.build();
    }

    private static String md5(final String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new Error(e);
        }
    }
}
