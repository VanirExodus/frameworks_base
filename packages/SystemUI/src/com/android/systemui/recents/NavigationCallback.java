package com.android.systemui.recents;

import android.graphics.drawable.Drawable;

public interface NavigationCallback {

    final static int NAVBAR_BACK_HINT = 0;
    final static int NAVBAR_RECENTS_HINT = 1;

    void setNavigationIconHints(int button, int hints, boolean force);
    int getNavigationIconHints();

}