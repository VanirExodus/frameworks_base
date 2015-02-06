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
            ACTION_TORCH,
            ACTION_IME_LAYOUT
    };

    private NavbarUtils() {
    }

    public static Drawable getIconImage(Context mContext, String uri) {
        Drawable actionIcon;

        if (TextUtils.isEmpty(uri)) {
            uri = ACTION_NULL;
        }

        if (uri.startsWith("**")) {
            return NavbarConstants.getActionIcon(mContext, uri);
        } else {  // This must be an app
            try {
                actionIcon = mContext.getPackageManager().getActivityIcon(Intent.parseUri(uri, 0));
            } catch (NameNotFoundException e) {
                e.printStackTrace();
                actionIcon = NavbarConstants.getActionIcon(mContext,
                        ACTION_NULL);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                actionIcon = NavbarConstants.getActionIcon(mContext,
                        ACTION_NULL);
            }
        }
        return actionIcon;
    }

    public static String[] getNavBarActions(Context context) {
        ArrayList<String> mActionsArray = new ArrayList<String>();
        String[] fullActionStringArray = NavbarConstants.NavbarActions();
        // Perfection is achieved, not when there is nothing more to add,
        // but when there is nothing left to take away. --Antoine de Saint-Exupéry, Airman's Odyssey 
        for (String action : fullActionStringArray) {
            if (Arrays.asList(EXCLUDED_FROM_NAVBAR).contains(action)) continue;
            mActionsArray.add(action);
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
