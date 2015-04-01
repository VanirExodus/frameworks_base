/*
 * Copyright (C) 2014 VanirAOSP && The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.util.vanir;

import static com.android.internal.util.vanir.NavbarConstants.*;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.util.vanir.NavbarConstants;
import com.android.internal.util.vanir.NavbarConstants.NavbarConstant;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.ArrayList;

public class NavbarUtils {
    private static final String TAG = NavbarUtils.class.getSimpleName();

    // Debugging for the navbar can be toggled here...
    public static final boolean DEBUG = false;

    // These items are excluded from settings and cannot be set as targets
    private static final String[] EXCLUDED_FROM_NAVBAR = {
            ACTION_RING_SILENT,
            ACTION_RING_VIB,
            ACTION_RING_VIB_SILENT,
            ACTION_NULL,
            ACTION_POWER,
            ACTION_LAYOUT_LEFT,
            ACTION_LAYOUT_RIGHT,
            ACTION_ARROW_LEFT,
            ACTION_ARROW_RIGHT,
            ACTION_ARROW_UP,
            ACTION_ARROW_DOWN,
            ACTION_IME_LAYOUT
    };

    private NavbarUtils() {
    }

    public static Drawable getIconImage(Context mContext, String uri) {
        if (TextUtils.isEmpty(uri)) {
            uri = ACTION_NULL;
        }

        if (uri.startsWith("**")) {
            return NavbarConstants.getActionIcon(mContext, uri);
        } else {  // This must be an app
            try {
                return mContext.getPackageManager().getActivityIcon(Intent.parseUri(uri, 0));
            } catch (NameNotFoundException e) {
            } catch (URISyntaxException e) {
            }
        }
        return NavbarConstants.getActionIcon(mContext, ACTION_NULL);
    }

    public static Drawable getLandscapeIconImage(Context context, String uri) {
        Drawable actionIcon;

        if (TextUtils.isEmpty(uri)) {
            uri = ACTION_NULL;
        }

        if (uri.startsWith("**")) {
			switch (uri) {
				case ACTION_HOME:
				    return getSystemUIDrawable(context, "com.android.systemui:drawable/ic_sysbar_home_land");
				case ACTION_RECENTS:
				    return getSystemUIDrawable(context, "com.android.systemui:drawable/ic_sysbar_recent_land");
				case ACTION_BACK:
				    return getSystemUIDrawable(context, "com.android.systemui:drawable/ic_sysbar_back_land");
				case ACTION_MENU:
				    return getSystemUIDrawable(context, "com.android.systemui:drawable/ic_sysbar_menu_big_land");
				case ACTION_SEARCH:
				    return getSystemUIDrawable(context, "com.android.systemui:drawable/ic_sysbar_search_land");
				case ACTION_KILL:
                    return getSystemUIDrawable(context, "com.android.systemui:drawable/ic_sysbar_killtask_land");
				case ACTION_ASSIST:
				    return getSystemUIDrawable(context, "com.android.systemui:drawable/ic_sysbar_assist_land");
				case ACTION_VOICEASSIST:
				    return getSystemUIDrawable(context, "com.android.systemui:drawable/ic_sysbar_voiceassist_land");
				case ACTION_POWER:
				    return getSystemUIDrawable(context, "com.android.systemui:drawable/ic_sysbar_power_land");
				case ACTION_TORCH:
				    return getSystemUIDrawable(context, "com.android.systemui:drawable/ic_sysbar_torch_land");
				case ACTION_LAST_APP:
				    return getSystemUIDrawable(context, "com.android.systemui:drawable/ic_sysbar_lastapp_land");
				case ACTION_NOTIFICATIONS:
				    return getSystemUIDrawable(context, "com.android.systemui:drawable/ic_sysbar_notifications_land");
				case ACTION_IME:
				    return getSystemUIDrawable(context, "com.android.systemui:drawable/ic_sysbar_ime_switcher_land");
				case ACTION_GESTURE_ACTIONS:
				    return getSystemUIDrawable(context, "com.android.systemui:drawable/ic_sysbar_gesture_land");
				case ACTION_SCREENSHOT:
				    return getSystemUIDrawable(context, "com.android.systemui:drawable/ic_sysbar_screenshot_land");
				case ACTION_SLEEP:
				    return getSystemUIDrawable(context, "com.android.systemui:drawable/ic_sysbar_power_land");
				case ACTION_RING_VIB:
				case ACTION_RING_SILENT:
				case ACTION_RING_VIB_SILENT:
				case ACTION_IME_LAYOUT:
				case ACTION_ARROW_LEFT:
				case ACTION_ARROW_RIGHT:
				case ACTION_ARROW_UP:
				case ACTION_ARROW_DOWN:
				case ACTION_LAYOUT_LEFT:
				case ACTION_LAYOUT_RIGHT:
				case ACTION_NULL:
				case ACTION_BLANK:
                    return getSystemUIDrawable(context, "com.android.systemui:drawable/ic_sysbar_null");
            }
        } else {  // This must be an app
            try {
                actionIcon = context.getPackageManager().getActivityIcon(Intent.parseUri(uri, 0));
            } catch (NameNotFoundException e) {
            } catch (URISyntaxException e) {
            }
        }
        return NavbarConstants.getActionIcon(context, ACTION_NULL);
    }

    public static Drawable getSystemUIDrawable(Context mContext, String DrawableID) {
        PackageManager pm = mContext.getPackageManager();
        int resId = 0;
        Drawable d = null;
        if (pm != null) {
            Resources mSystemUiResources = null;
            try {
                mSystemUiResources = pm.getResourcesForApplication("com.android.systemui");
            } catch (Exception e) {
            }

            if (mSystemUiResources != null && DrawableID != null) {
                resId = mSystemUiResources.getIdentifier(DrawableID, null, null);
            }
            if (resId > 0) {
                try {
                    d = mSystemUiResources.getDrawable(resId);
                } catch (Exception e) {
                }
            }
        }
        return d;
    }

    public static String[] getNavBarActions(Context context) {
        ArrayList<String> mActionsArray = new ArrayList<String>();
        // Perfection is achieved, not when there is nothing more to add,
        // but when there is nothing left to take away. --Antoine de Saint-Exupéry, Airman's Odyssey
        for (String action : NavbarConstants.NavbarActions()) {
            if (Arrays.asList(EXCLUDED_FROM_NAVBAR).contains(action)) continue;
            mActionsArray.add(action);
        }
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            mActionsArray.remove(NavbarConstant.ACTION_TORCH);
        }
        String[] mActions = new String[mActionsArray.size()];
        mActions = mActionsArray.toArray(mActions);
        return mActions;
    }

    public static String getProperSummary(Context mContext, String uri) {
        if (TextUtils.isEmpty(uri)) {
            uri = ACTION_NULL;
        }

        if (uri.startsWith("**")) {
            return NavbarConstants.getProperName(mContext, uri);
        } else {  // This must be an app
            try {
                Intent intent = Intent.parseUri(uri, 0);
                if (Intent.ACTION_MAIN.equals(intent.getAction())) {
                    return getFriendlyActivityName(mContext, intent);
                }
                return getFriendlyShortcutName(mContext, intent);
            } catch (URISyntaxException e) {
                return NavbarConstants.getProperName(mContext, ACTION_NULL);
            }
        }
    }

    private static String getFriendlyActivityName(Context mContext, Intent intent) {
        PackageManager pm = mContext.getPackageManager();
        ActivityInfo ai = intent.resolveActivityInfo(pm, PackageManager.GET_ACTIVITIES);
        String friendlyName = null;

        if (ai != null) {
            friendlyName = ai.loadLabel(pm).toString();
            if (friendlyName == null) {
                friendlyName = ai.name;
            }
        }

        return (friendlyName != null) ? friendlyName : intent.toUri(0);
    }

    private static String getFriendlyShortcutName(Context mContext, Intent intent) {
        String activityName = getFriendlyActivityName(mContext, intent);
        String name = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);

        if (activityName != null && name != null) {
            return activityName + ": " + name;
        }
        return name != null ? name : intent.toUri(0);
    }
}
