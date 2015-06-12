/**
 * Copyright (C) 2015 Team Exodus
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.android.internal.util.exodus;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.ProgressDialog;
import android.app.Application;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.ContentResolver;
import android.content.DialogInterface;

import android.provider.Settings;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.AsyncTask;

import android.util.Log;

/**
 * This class intended for Exodus Morph Features as of now. Option avaliable in @see SettingActivty.java i.e. Developer options
 * (1. EXODUS , 2. CYANOGEN, 3. AOSP)
 * @Author	Martin Rodemann
 * @Version	1.0 (Android 5.1.x)
 * @Since	2015-MAY-22
 * --------------------
 * ChangeHistory:
 * 1. Added logic for restart UI and popup confirmation. Modified by Raja Mungamuri
 */
public class SettingsUtils {

    private static final String TAG = "SettingsUtils";

    public static final int MORPH_MODE_EXODUS = 0;
    public static final int MORPH_MODE_CYANOGENMOD = 1;
    public static final int MORPH_MODE_AOSP = 2;

    public static int CurrentMorphMode(ContentResolver resolver) {
        return MORPH_MODE_EXODUS;
    }

    public static boolean isMorphExodus(ContentResolver resolver) {
        return (CurrentMorphMode(resolver) == MORPH_MODE_EXODUS);
    }

    public static boolean isMorphCyanogenMod(ContentResolver resolver) {
        return (CurrentMorphMode(resolver) == MORPH_MODE_CYANOGENMOD);
    }

    public static boolean isMorphAosp(ContentResolver resolver) {
        return (CurrentMorphMode(resolver) == MORPH_MODE_AOSP);
    }
    /**
     * This method morphs the ROM to EXODUS or CYANOGEN or AOSP.
     * @see exodus_string.xml, exodus_symbols.xml, SettingActivity.java.
     */
    public static void setCurrentMorphMode(final Context aContext, final int modeValue) {
        AlertDialog.Builder builder = new AlertDialog.Builder(aContext);
        builder.setMessage(aContext.getResources().getString(com.android.internal.R.string.confirmation_msg));

        builder.setPositiveButton("Proceed",  new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (modeValue == MORPH_MODE_CYANOGENMOD || modeValue == MORPH_MODE_AOSP) {
                    disableExodusFeatures();
                }
                if (modeValue == MORPH_MODE_AOSP) {
                    disableCmFeatures();
                }
                doSystemUIReboot(aContext);

                dialog.dismiss();
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private static void disableExodusFeatures() {
        //todo disable ExodusFeatures when cm or aosp is selected
    }

    private static void disableCmFeatures() {
        //todo if Aosp is selected we need to disable exodus options
    }

    private static void doSystemUIReboot(final Context context) {

        final IActivityManager am = ActivityManagerNative.asInterface(
                ServiceManager.checkService("activity"));
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                ProgressDialog dialog = new ProgressDialog(context);
                dialog.setMessage(context.getResources().getString(com.android.internal.R.string.restarting_ui));
                dialog.setCancelable(false);
                dialog.setIndeterminate(true);
                dialog.show();
            }
            @Override
            protected Void doInBackground(Void... params) {
               // Give the user a half second to see the dialog ;)
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // Ignore
                }
                // Restart the UI
                try {
                    am.restart();
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to restart");
                }
                return null;
            }
        };
        task.execute();
    }
}
