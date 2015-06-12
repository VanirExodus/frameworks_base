/*
* Copyright (C) 2015 Exodus
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.android.internal.util.exodus;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.DisplayInfo;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.util.ArrayList;

/* Support class with the following common device utilities:
 * isNetworkAvailalbe(Context c)                     -  boolean checks network presence/availability
 * isVoiceCapable(Context c)                         -  boolean return if the device is voice capable or a phone
 * isWifiOnly(Context c)                             -  boolean returns whether the device is wifi only
 * isPhone(Context c)                                -  boolean for devices < 600dp
 * isPhablet(Context c)                              -  boolean for devices < 720dp
 * isTablet(Context c)                               -  boolean for devices > 720dp
 * isPackageInstalled(Context c, String packageName) -  boolean check if a package is installed
 * getAllChildren(View v)                            -  return list of all children
 * getAllChildren(View root, Class<T> returnType)    -  return a list of all children of a type
 */

public class DeviceUtils {
	private static final String TAG = "DeviceUtils";

    // Device types
    private static final int DEVICE_PHONE  = 0;
    private static final int DEVICE_PHABLET = 1;
    private static final int DEVICE_TABLET = 2;

    private DeviceUtils() {
    }

    /**
     * Checks device for network connectivity
     *
     * @return If the device has data connectivity
    */
    public static boolean isNetworkAvailable(final Context c) {
        boolean state = false;
        if (c != null) {
            ConnectivityManager cm = (ConnectivityManager) c
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            if (netInfo != null && netInfo.isConnected()) {
                Log.i(TAG, "The device currently has data connectivity");
                state = true;
            } else {
                Log.i(TAG, "The device does not currently have data connectivity");
                state = false;
            }
        }
        return state;
    }

    /**
     * Returns whether the device is voice-capable or a phone
     */
    public static boolean isVoiceCapable(Context c) {
        TelephonyManager telephony =
                (TelephonyManager) c.getSystemService(Context.TELEPHONY_SERVICE);
        return telephony != null && telephony.isVoiceCapable();
    }

    /**
     * Returns whether the device is wifi only
     */
    public static boolean isWifiOnly(Context c) {
        ConnectivityManager cm = (ConnectivityManager)c.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        return (cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE) == false);
    }

    /**
     * Get the display size bucket
     * Notice: one dp (density-independent pixel) is one pixel on a 160 DPI screen
     * A good reference tool is here:
     * http://stefan222devel.blogspot.com/2012/10/android-screen-densities-sizes.html
     * @hide
    */
    private static int getScreenType(Context con) {
        WindowManager wm = (WindowManager)con.getSystemService(Context.WINDOW_SERVICE);
        DisplayInfo outDisplayInfo = new DisplayInfo();
        wm.getDefaultDisplay().getDisplayInfo(outDisplayInfo);
        int shortSize = Math.min(outDisplayInfo.logicalHeight, outDisplayInfo.logicalWidth);
        int shortSizeDp =
            shortSize * DisplayMetrics.DENSITY_DEFAULT / outDisplayInfo.logicalDensityDpi;
        if (shortSizeDp < 600) {
            return DEVICE_PHONE;
        } else if (shortSizeDp < 720) {
            return DEVICE_PHABLET;
        } else {
            return DEVICE_TABLET;
        }
    }

    public static boolean isPhone(Context c) {
        return getScreenType(c) == DEVICE_PHONE;
    }

    public static boolean isPhablet(Context c) {
        return getScreenType(c) == DEVICE_PHABLET;
    }

    public static boolean isTablet(Context c) {
        return getScreenType(c) == DEVICE_TABLET;
    }

    /**
     * Checks device for a package
     *
     * @return If the packages is installed
    */
    public static boolean isPackageInstalled(Context c, final String packageName) {
		final PackageManager pm = c.getPackageManager();
        String mVersion;
        try {
            mVersion = pm.getPackageInfo(packageName, 0).versionName;
            if (mVersion.equals(null)) {
                return false;
            }
        } catch (NameNotFoundException e) {
            return false;
        }
        return true;
    }
    
    /** 
     * utility to iterate a viewgroup recursively and return a list of child views 
    */
    public static ArrayList<View> getAllChildren(View v) {
        if (!(v instanceof ViewGroup)) {
            ArrayList<View> viewArrayList = new ArrayList<View>();
            viewArrayList.add(v);
            return viewArrayList;
        }

        ArrayList<View> result = new ArrayList<View>();
        ViewGroup vg = (ViewGroup) v;
        for (int i = 0; i < vg.getChildCount(); i++) {
            View child = vg.getChildAt(i);
            ArrayList<View> viewArrayList = new ArrayList<View>();
            viewArrayList.add(v);
            viewArrayList.addAll(getAllChildren(child));
            result.addAll(viewArrayList);
        }
        return result;
    }

    /**
     * utility to iterate a viewgroup and return a list of child views of type 
    */
    public static <T extends View> ArrayList<T> getAllChildren(View root, Class<T> returnType) {
        if (!(root instanceof ViewGroup)) {
            ArrayList<T> viewArrayList = new ArrayList<T>();
            try {
                viewArrayList.add(returnType.cast(root));
            } catch (Exception e) {
                // handle all exceptions the same and silently fail
            }
            return viewArrayList;
        }

        ArrayList<T> result = new ArrayList<T>();
        ViewGroup vg = (ViewGroup) root;
        for (int i = 0; i < vg.getChildCount(); i++) {
            View child = vg.getChildAt(i);
            ArrayList<T> viewArrayList = new ArrayList<T>();
            try {
                viewArrayList.add(returnType.cast(root));
            } catch (Exception e) {
                // handle all exceptions the same and silently fail
            }
            viewArrayList.addAll(getAllChildren(child, returnType));
            result.addAll(viewArrayList);        }
        return result;
    }
}
