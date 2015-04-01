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

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashMap;

public class NavbarConstants {
    public static final String ASSIST_ICON_METADATA_NAME = "com.android.systemui.action_assist_icon";

    public static final int LAYOUT_IME = -2;
    public static final int DEFAULT_LAYOUT = -3;

    public static final String ACTION_APP = "**app**";
    public static final String ACTION_BACK = "**back**";
    public static final String ACTION_HOME = "**home**";
    public static final String ACTION_RECENTS = "**recents**";
    public static final String ACTION_BLANK = "**blank**";
    public static final String ACTION_GESTURE_ACTIONS = "**gesture_actions**";
    public static final String ACTION_SCREENSHOT = "**screenshot**";
    public static final String ACTION_SLEEP = "**sleep**";
    public static final String ACTION_KILL = "**kill**";
    public static final String ACTION_LAST_APP = "**last_app**";
    public static final String ACTION_MENU = "**menu**";
    public static final String ACTION_NOTIFICATIONS = "**notifications**";
    public static final String ACTION_IME = "**ime**";
    public static final String ACTION_ASSIST = "**assist**";
    public static final String ACTION_SEARCH = "**search**";
    public static final String ACTION_VOICEASSIST = "**voiceassist**";
    public static final String ACTION_RING_SILENT = "**ring_silent**";
    public static final String ACTION_RING_VIB_SILENT = "**ring_vib_silent**";
    public static final String ACTION_RING_VIB = "**ring_vib**";
    public static final String ACTION_TORCH = "**torch**";
    public static final String ACTION_LAYOUT_LEFT = "**layout_left**";
    public static final String ACTION_LAYOUT_RIGHT = "**layout_right**";
    public static final String ACTION_ARROW_LEFT = "**arrow_left**";
    public static final String ACTION_ARROW_RIGHT = "**arrow_right**";
    public static final String ACTION_ARROW_UP = "**arrow_up**";
    public static final String ACTION_ARROW_DOWN = "**arrow_down**";
    public static final String ACTION_IME_LAYOUT = "**ime_layout**";
    public static final String ACTION_POWER = "**power**";
    public static final String ACTION_NULL = "**null**";

    //not presently utilized, but gives the gist of NavbarConstants' methods without scrolling down (YAY)
    public interface AwesomeGuts {

        int getStringId();                         //returns the resource id of this NavbarConstant's user-facing name
        String getProperName(Context mContext);          //getStrings ^

        Resources getMyPackageResources(Context context); //gets a Resources where the drawable can be found (i.e. from mDrawablePackage)
        int getDrawableId(Context context);             //gets the identifier from ^
        Drawable getDrawable(Context mContext);         //gets the drawable from ^ and ^^
    }

    public static enum NavbarConstant implements AwesomeGuts {
        //                    Action                  Proper name identifier                             Icon package+identifier name
        ACTION_APP            ("**app**",             com.android.internal.R.string.action_app,           null),
        ACTION_BACK           ("**back**",            com.android.internal.R.string.action_back,          "com.android.systemui:drawable/ic_sysbar_back"),
        ACTION_HOME           ("**home**",            com.android.internal.R.string.action_home,          "com.android.systemui:drawable/ic_sysbar_home"),
        ACTION_RECENTS        ("**recents**",         com.android.internal.R.string.action_recents,       "com.android.systemui:drawable/ic_sysbar_recent"),
        ACTION_BLANK          ("**blank**",           com.android.internal.R.string.action_blank,         "com.android.systemui:drawable/ic_sysbar_blank"),
        ACTION_GESTURE_ACTIONS("**gesture_actions**", com.android.internal.R.string.gesture_actions,      "com.android.systemui:drawable/ic_sysbar_gesture"),
        ACTION_KILL           ("**kill**",            com.android.internal.R.string.action_kill,          "com.android.systemui:drawable/ic_sysbar_killtask"),
        ACTION_LAST_APP       ("**last_app**",        com.android.internal.R.string.action_last_app,      "com.android.systemui:drawable/ic_sysbar_lastapp"),
        ACTION_MENU           ("**menu**",            com.android.internal.R.string.action_menu,          "com.android.systemui:drawable/ic_sysbar_menu_big"),
        ACTION_NOTIFICATIONS  ("**notifications**",   com.android.internal.R.string.action_notifications, "com.android.systemui:drawable/ic_sysbar_notifications"),
        ACTION_IME            ("**ime**",             com.android.internal.R.string.action_ime,           "com.android.systemui:drawable/ic_sysbar_ime_switcher"),
        ACTION_TORCH          ("**torch**",           com.android.internal.R.string.action_torch,         "com.android.systemui:drawable/ic_sysbar_torch"),
        ACTION_SCREENSHOT     ("**screenshot**",      com.android.internal.R.string.action_screenshot,    "com.android.systemui:drawable/ic_sysbar_screenshot"),
        ACTION_ASSIST         ("**assist**",          com.android.internal.R.string.action_assist,        "com.android.systemui:drawable/ic_sysbar_assist"),
        ACTION_SEARCH         ("**search**",          com.android.internal.R.string.action_search,        "com.android.systemui:drawable/ic_sysbar_search"),
        ACTION_VOICEASSIST    ("**voiceassist**",     com.android.internal.R.string.action_voiceassist,   "com.android.systemui:drawable/ic_sysbar_voiceassist"),
        ACTION_SLEEP          ("**sleep**",           com.android.internal.R.string.action_sleep,         "com.android.systemui:drawable/ic_sysbar_power"),
        ACTION_RING_SILENT    ("**ring_silent**",     com.android.internal.R.string.action_silent,        "com.android.systemui:drawable/ic_sysbar_silent"),
        ACTION_RING_VIB_SILENT("**ring_vib_silent**", com.android.internal.R.string.action_silent_vib,    "com.android.systemui:drawable/ic_sysbar_silent_vib"),
        ACTION_RING_VIB       ("**ring_vib**",        com.android.internal.R.string.action_vib,           "com.android.systemui:drawable/ic_sysbar_vib"),

        /* unassignable actions */
        ACTION_LAYOUT_LEFT    ("**layout_left**",     com.android.internal.R.string.action_null,          "com.android.systemui:drawable/ic_sysbar_layout_left"),
        ACTION_LAYOUT_RIGHT   ("**layout_right**",    com.android.internal.R.string.action_null,          "com.android.systemui:drawable/ic_sysbar_layout_right"),
        ACTION_ARROW_LEFT     ("**arrow_left**",      com.android.internal.R.string.action_null,          "com.android.systemui:drawable/ic_sysbar_ime_left"),
        ACTION_ARROW_RIGHT    ("**arrow_right**",     com.android.internal.R.string.action_null,          "com.android.systemui:drawable/ic_sysbar_ime_right"),
        ACTION_ARROW_UP       ("**arrow_up**",        com.android.internal.R.string.action_null,          "com.android.systemui:drawable/ic_sysbar_ime_up"),
        ACTION_ARROW_DOWN     ("**arrow_down**",      com.android.internal.R.string.action_null,          "com.android.systemui:drawable/ic_sysbar_ime_down"),
        ACTION_IME_LAYOUT     ("**ime_layout**",      com.android.internal.R.string.action_null,          "com.android.systemui:drawable/ic_sysbar_ime_arrows"),

        /* disabled or special actions?? */
        ACTION_POWER          ("**power**",           com.android.internal.R.string.action_null,          null),
        ACTION_NULL           ("**null**",            com.android.internal.R.string.action_null,          null);

        private final String mAction;
        private final int mStringId;
        private String mDrawablePackage;
        private String mDrawableUnresolvedIdentifier;
        private int mDrawableId = 0;

        private NavbarConstant(final String a, final int s, final String unresolved_drawable_id) {
            mAction = a;
            mStringId = s;
            mDrawableUnresolvedIdentifier = unresolved_drawable_id;
            if (unresolved_drawable_id != null && unresolved_drawable_id.contains(":")) {
                mDrawablePackage = unresolved_drawable_id.split(":")[0];
            }
        }

        private static HashMap<String, Resources> mPackageResources = new HashMap<String, Resources>();

        //gets a Resources for the package referred to in this constant's unresolved drawable string
        public Resources getMyPackageResources(Context context) {
            if (mDrawablePackage == null)
                return context.getResources();

            Resources res = null;
            synchronized(mPackageResources) {
                res = mPackageResources.get(mDrawablePackage); //check the map
                if (res == null) { //if absent, look it up
                    try {
                        mPackageResources.put(mDrawablePackage,
                                (res = context.getPackageManager().getResourcesForApplication(mDrawablePackage)));
                    } catch (Exception e) {
                    }
                }
            }
            return res;
        }

        //returns the resource Id associated with this NavbarConstant's drawable (the resource is in systemui)
        public int getDrawableId(Context context) {
            if (mDrawableUnresolvedIdentifier != null) {
                final Resources res = getMyPackageResources(context);
                if (res == null) {
                    return 0; //return an empty drawable because something is __wrong__
                }
                try {
                    mDrawableId = res.getIdentifier(mDrawableUnresolvedIdentifier, null, null);
                } catch(Exception e) {
                    return 0;
                }
                mDrawableUnresolvedIdentifier = null;
            }
            return mDrawableId;
        }

        //returns the drawable for this specific NavbarConstant
        public Drawable getDrawable(Context context) {
            final Resources res = getMyPackageResources(context);
            Drawable dr = null;
            if (res != null)
                try {
                    dr = res.getDrawable(getDrawableId(context));
                } catch (Exception e) {
                    //something ain't right. force re-finding package reses
                    synchronized(mPackageResources) {
                        mPackageResources.remove(mDrawablePackage);
                    }
                    return null;
                }
            return dr;
        }

        //**parse** an action, returning the associated NavbarConstant
        public static NavbarConstant fromAction(String string) {
            if (string == null || TextUtils.isEmpty(string))
                return NavbarConstant.ACTION_NULL;

            // assumes relationship between enum name and action string
            //   input   **x**
            //   output  ACTION_X
            if (string.startsWith("**")) {
                final String suffix = string.replace("**","").toUpperCase();
                return NavbarConstant.valueOf("ACTION_"+suffix);
            }

            // not in ENUM, must be an intent
            return NavbarConstant.ACTION_APP;
        }

        public String getProperName(Context context) { return context.getResources().getString(mStringId); }

        public int getStringId() { return mStringId; }

        @Override
        public String toString() { return mAction; }

    }

    //returns a string array containing the actions of all NavbarConstants
    public static String[] NavbarActions() {
        return Arrays.toString(NavbarConstant.values()).split("[\\[\\] \\,]+");
    }

    // Will return a string for the associated action, but will need the caller's context to get resources.
    public static String getProperName(Context context, String actionstring) {
        return NavbarConstant.fromAction(actionstring).getProperName(context);
    }

    // Will return a Drawable for the associated action, and may need the caller's context to ninja somebody else's resources.
    public static Drawable getActionIcon(Context context,String actionstring) {
        return NavbarConstant.fromAction(actionstring).getDrawable(context);
    }

    //for compatibility with existing usages of this class
    public static NavbarConstant fromString(String actionstring) {
        return NavbarConstant.fromAction(actionstring);
    }

    public static String defaultNavbarLayout(Context context) {
        Resources res = context.getResources();
        return res.getString(com.android.internal.R.string.def_navbar_layout);
    }

    public static String defaultIMEKeyLayout(Context context) {
        Resources res = context.getResources();
        return res.getString(com.android.internal.R.string.def_ime_layout);
    }
}
