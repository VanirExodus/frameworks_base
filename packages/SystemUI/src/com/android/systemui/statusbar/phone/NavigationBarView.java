/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.systemui.statusbar.phone;

import static com.android.internal.util.vanir.NavbarConstants.*;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.LayoutTransition;
import android.animation.LayoutTransition.TransitionListener;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.StatusBarManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;

import com.android.internal.util.vanir.KeyButtonInfo;
import com.android.internal.util.vanir.NavbarConstants;
import com.android.internal.util.vanir.NavbarUtils;

import com.android.systemui.R;
import com.android.systemui.cm.UserContentObserver;
import com.android.systemui.statusbar.BaseStatusBar;
import com.android.systemui.statusbar.DelegateViewHelper;
import com.android.systemui.statusbar.policy.DeadZone;
import com.android.systemui.statusbar.policy.KeyButtonView;
import com.android.systemui.statusbar.policy.LayoutChangerButtonView;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import android.util.SparseArray;

public class NavigationBarView extends LinearLayout {
    private static final boolean DEBUG = NavbarUtils.DEBUG;
    private final static String TAG = "PhoneStatusBar/NavigationBarView";

    private static final int CHANGER_LEFT_SIDE = 0;
    private static final int CHANGER_RIGHT_SIDE = 1;

    final static boolean SLIPPERY_WHEN_DISABLED = true;
    final static boolean NAVBAR_ALWAYS_AT_RIGHT = true;

    private BaseStatusBar mBar;

    final Display mDisplay;
    View mCurrentView = null;
    View[] mRotatedViews = new View[4];
    private LinearLayout navButtons;
    private LinearLayout lightsOut;

    int mBarSize;
    boolean mVertical;
    boolean landscape;
    boolean mScreenOn;
    boolean mLeftInLandscape;

    boolean mShowMenu;
    int mDisabledFlags = 0;
    int mNavigationIconHints = 0;

    private Drawable mRecentIcon;
    private Drawable mRecentLandIcon;
    private NavigationBarViewTaskSwitchHelper mTaskSwitchHelper;
    private DelegateViewHelper mDelegateHelper;
    private DeadZone mDeadZone;
    private final NavigationBarTransitions mBarTransitions;
    private KeyButtonView mButton;
    private LayoutChangerButtonView mChanger;
    private KeyButtonInfo mInfo;

    // workaround for LayoutTransitions leaving the nav buttons in a weird state (bug 5549288)
    final static boolean WORKAROUND_INVALID_LAYOUT = true;
    final static int MSG_CHECK_INVALID_LAYOUT = 8686;

    // performs manual animation in sync with layout transitions
    private final NavTransitionListener mTransitionListener = new NavTransitionListener();

    private Resources mThemedResources;

    private OnVerticalChangedListener mOnVerticalChangedListener;
    private boolean mIsLayoutRtl;
    private boolean mDelegateIntercepted;

    // Navigation bar customizations
    final boolean mTablet = isTablet(mContext);
    private boolean mNeedsNav;
    private boolean mLegacyMenu;
    private boolean mImeLayout;
    private int mLongPressTimeout;
    private String mIMEKeyLayout;
    private String mDefaultLayout;
    private boolean showingIME;
    private int mButtonLayouts;
    private int mCurrentLayout = 0; //the first one
    private int mRestoredLayout = 0;
    private float mButtonWidth, mMenuButtonWidth, mLayoutChangerWidth;

    //for slightly de-derped layout handling
    private int mNextLayoutIndex;
    private int mDisplayingLayoutIndex;

    private String[] mButtonContainerStrings = new String[5];
    SparseArray<SparseArray<KeyButtonInfo>> mAllButtonContainers = new SparseArray<SparseArray<KeyButtonInfo>>();

    private static final String[] buttonSettings = new String[] {
        Settings.System.NAVIGATION_BAR_BUTTONS,
        Settings.System.NAVIGATION_BAR_BUTTONS_TWO,
        Settings.System.NAVIGATION_BAR_BUTTONS_THREE,
        Settings.System.NAVIGATION_BAR_BUTTONS_FOUR,
        Settings.System.NAVIGATION_BAR_BUTTONS_FIVE
    };

    private ContentObserver mSettingsObserver;

    private class NavTransitionListener implements TransitionListener {
        private boolean mBackTransitioning;
        private boolean mHomeAppearing;
        private long mStartDelay;
        private long mDuration;
        private TimeInterpolator mInterpolator;

        @Override
        public void startTransition(LayoutTransition transition, ViewGroup container,
                View view, int transitionType) {
            if (view.getTag() != null) {
                if (view.getTag().equals(ACTION_BACK)) {
                    mBackTransitioning = true;
                } else if (view.getTag().equals(ACTION_HOME)
                    && transitionType == LayoutTransition.APPEARING) {
                    mHomeAppearing = true;
                    mStartDelay = transition.getStartDelay(transitionType);
                    mDuration = transition.getDuration(transitionType);
                    mInterpolator = transition.getInterpolator(transitionType);
                }
            }
        }

        @Override
        public void endTransition(LayoutTransition transition, ViewGroup container,
                View view, int transitionType) {
            if (view.getTag() != null) {
                if (view.getTag().equals(ACTION_BACK)) {
                    mBackTransitioning = false;
                } else if (view.getTag().equals(ACTION_HOME)
                    && transitionType == LayoutTransition.APPEARING) {
                    mHomeAppearing = false;
                }
            }
        }

        public void onBackAltCleared() {
            if (getButtonView(ACTION_BACK) == null) return;
            // When dismissing ime during unlock, force the back button to run the same appearance
            // animation as home (if we catch this condition early enough).
            if (!mBackTransitioning && getButtonView(ACTION_BACK).getVisibility() == VISIBLE
                    && mHomeAppearing && getButtonView(ACTION_HOME).getAlpha() == 0) {
                getButtonView(ACTION_BACK).setAlpha(0);
                ValueAnimator a = ObjectAnimator.ofFloat(getButtonView(ACTION_BACK), "alpha", 0, 1);
                a.setStartDelay(mStartDelay);
                a.setDuration(mDuration);
                a.setInterpolator(mInterpolator);
                a.start();
            }
        }
    }

    private final OnClickListener mImeSwitcherClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            ((InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE))
                    .showInputMethodPicker();
        }
    };

    private class H extends Handler {
        public void handleMessage(Message m) {
            switch (m.what) {
                case MSG_CHECK_INVALID_LAYOUT:
                    final String how = "" + m.obj;
                    final int w = getWidth();
                    final int h = getHeight();
                    final int vw = mCurrentView.getWidth();
                    final int vh = mCurrentView.getHeight();

                    if (h != vh || w != vw) {
                        Log.w(TAG, String.format(
                            "*** Invalid layout in navigation bar (%s this=%dx%d cur=%dx%d)",
                            how, w, h, vw, vh));
                        if (WORKAROUND_INVALID_LAYOUT) {
                            requestLayout();
                        }
                    }
                    break;
            }
        }
    }

    public NavigationBarView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mDisplay = ((WindowManager)context.getSystemService(
                Context.WINDOW_SERVICE)).getDefaultDisplay();

        final Resources res = getContext().getResources();
        final ContentResolver cr = mContext.getContentResolver();

        mBarSize = res.getDimensionPixelSize(R.dimen.navigation_bar_size);
        mVertical = false;
        mShowMenu = false;
        mDelegateHelper = new DelegateViewHelper(this);
        mTaskSwitchHelper = new NavigationBarViewTaskSwitchHelper(context);

        mButtonWidth = res.getDimensionPixelSize(R.dimen.navigation_key_width);
        mMenuButtonWidth = res.getDimensionPixelSize(R.dimen.navigation_menu_key_width);
        mLayoutChangerWidth = res.getDimensionPixelSize(R.dimen.navigation_layout_changer_width);

        try {
            mNeedsNav = WindowManagerGlobal.getWindowManagerService().needsNavigationBar();
        } catch (RemoteException ex) {
        }

        mRestoredLayout = Settings.System.getInt(cr, Settings.System.NAVIGATION_BAR_RESTORE, 0);
        mLegacyMenu = Settings.System.getInt(cr, Settings.System.NAVIGATION_BAR_SIDEKEYS, 1) == 1;
        mImeLayout = Settings.System.getInt(cr, Settings.System.NAVIGATION_BAR_ARROWS, 0) == 1;
        mButtonLayouts = Settings.System.getInt(cr, Settings.System.NAVIGATION_BAR_ALTERNATE_LAYOUTS, 1);
        for(int i=0;i<mButtonLayouts;i++)
            mButtonContainerStrings[i] = Settings.System.getString(cr, buttonSettings[i]);
        if (mButtonLayouts == 1) {
            mCurrentLayout = 0;
        } else {
            mCurrentLayout = mRestoredLayout;
        }
        mIMEKeyLayout = NavbarConstants.defaultIMEKeyLayout(mContext);
        mDefaultLayout = NavbarConstants.defaultNavbarLayout(mContext);
        mLongPressTimeout = Settings.System.getInt(cr,
                Settings.System.SOFTKEY_LONG_PRESS_CONFIGURATION, ViewConfiguration.getLongPressTimeout());

        mBarTransitions = new NavigationBarTransitions(this);
    }

    public BarTransitions getBarTransitions() {
        return mBarTransitions;
    }

    public void setDelegateView(View view) {
        mDelegateHelper.setDelegateView(view);
    }

    public void setBar(BaseStatusBar phoneStatusBar) {
        mBar = phoneStatusBar;
        mTaskSwitchHelper.setBar(mBar);
        mDelegateHelper.setBar(mBar);
    }

    public void setOnVerticalChangedListener(OnVerticalChangedListener onVerticalChangedListener) {
        mOnVerticalChangedListener = onVerticalChangedListener;
        notifyVerticalChangedListener(mVertical);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        initDownStates(event);
        if (!mDelegateIntercepted && mTaskSwitchHelper.onTouchEvent(event)) {
            return true;
        }
        if (mDeadZone != null && event.getAction() == MotionEvent.ACTION_OUTSIDE) {
            mDeadZone.poke(event);
        }
        if (mDelegateHelper != null && mDelegateIntercepted) {
            boolean ret = mDelegateHelper.onInterceptTouchEvent(event);
            if (ret) return true;
        }
        return super.onTouchEvent(event);
    }

    private void initDownStates(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            mDelegateIntercepted = false;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        initDownStates(event);
        boolean intercept = mTaskSwitchHelper.onInterceptTouchEvent(event);
        if (!intercept) {
            mDelegateIntercepted = mDelegateHelper.onInterceptTouchEvent(event);
            intercept = mDelegateIntercepted;
        } else {
            MotionEvent cancelEvent = MotionEvent.obtain(event);
            cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
            mDelegateHelper.onInterceptTouchEvent(cancelEvent);
            cancelEvent.recycle();
        }
        return intercept;
    }

    private H mHandler = new H();

    public View getCurrentView() {
        return mCurrentView;
    }

    /* Views to return for for PhoneStatusBar */
    public View getHomeButton() {
        return getButtonView(ACTION_HOME);
    }
    public View getRecentsButton() {
        return getButtonView(ACTION_RECENTS);
    }
    public View getBackButton() {
        return getButtonView(ACTION_BACK);
    }

    public View getButtonView(String constant) {
        return mCurrentView.findViewWithTag(constant);
    }

    public View attachedToLayoutButtonView(String constant) {
        final View child = mCurrentView.findViewWithTag(constant);
        if (child instanceof LayoutChangerButtonView) {
            return child;
        }
        return null;
    }

    public void updateResources(Resources res) {
        mThemedResources = res;
        mBarTransitions.updateResources(res);
        for (int i = 0; i < mRotatedViews.length; i++) {
            ViewGroup container = (ViewGroup) mRotatedViews[i];
            if (container != null) {
                updateKeyButtonViewResources(container);
                updateLightsOutResources(container);
            }
        }
    }

    private void updateKeyButtonViewResources(ViewGroup container) {
        if (mCurrentView == null) return;
        for (final String k : NavbarConstants.NavbarActions()) {
            final View child = mCurrentView.findViewWithTag(k);

            if (child instanceof KeyButtonView) {
                ((KeyButtonView) child).setImage(mThemedResources);
            }
        }
    }

    private void updateLightsOutResources(ViewGroup container) {
        ViewGroup lightsOut = (ViewGroup) container.findViewById(R.id.lights_out);
        if (lightsOut != null) {
            final int nChildren = lightsOut.getChildCount();
            for (int i = 0; i < nChildren; i++) {
                final View child = lightsOut.getChildAt(i);
                if (child instanceof ImageView) {
                    final ImageView iv = (ImageView) child;
                    // clear out the existing drawable, this is required since the
                    // ImageView keeps track of the resource ID and if it is the same
                    // it will not update the drawable.
                    iv.setImageDrawable(null);
                    iv.setImageDrawable(mThemedResources.getDrawable(
                            R.drawable.ic_sysbar_lights_out_dot_large));
                }
            }
        }
    }

    @Override
    public void setLayoutDirection(int layoutDirection) {
        super.setLayoutDirection(layoutDirection);
    }

    public void notifyScreenOn(boolean screenOn) {
        mScreenOn = screenOn;
        setDisabledFlags(mDisabledFlags, true);
    }

    public void setNavigationIconHints(int hints) {
        setNavigationIconHints(hints, false);
    }

    public void setNavigationIconHints(int hints, boolean force) {
        if (!force && hints == mNavigationIconHints) return;
        showingIME = (hints & StatusBarManager.NAVIGATION_HINT_BACK_ALT) != 0;

        if (mDisplayingLayoutIndex != NavbarConstants.LAYOUT_IME && showingIME && mNeedsNav
                && ((mButtonLayouts > 1 && mImeLayout) || mButtonLayouts == 1)
                && getButtonView(ACTION_BACK) == null
                && getButtonView(ACTION_HOME) == null) {
            setNextLayout(NavbarConstants.DEFAULT_LAYOUT);
            if (getButtonView(ACTION_BACK) != null)
                ((ImageView) getButtonView(ACTION_BACK)).setImageResource(R.drawable.ic_sysbar_back_ime);
            mNavigationIconHints = hints;
            setMenuVisibility(mShowMenu, true);
            setDisabledFlags(mDisabledFlags, true);
            return;
        }

        if ((mNavigationIconHints & StatusBarManager.NAVIGATION_HINT_BACK_ALT) != 0 && !showingIME) {
            mTransitionListener.onBackAltCleared();
        }
        if (DEBUG) {
            android.widget.Toast.makeText(getContext(),
                "Navigation icon hints = " + hints,
                500).show();
        }

        mNavigationIconHints = hints;

        if (mImeLayout) {
            if (mLegacyMenu && mButtonLayouts == 1) {
                // show hard-coded switchers here when written
                getButtonView(ACTION_IME).setVisibility(showingIME ? View.VISIBLE : View.INVISIBLE);
                getButtonView(ACTION_IME_LAYOUT).setVisibility(showingIME ? View.VISIBLE : View.INVISIBLE);
            }
            if (mButtonLayouts > 1) {
                final int orientation = getResources().getConfiguration().orientation;
                // left side
                if (getButtonView(ACTION_LAYOUT_LEFT) != null) {
                    setLayoutChangerType(getButtonView(ACTION_LAYOUT_LEFT), CHANGER_LEFT_SIDE, orientation);
                } else if (getButtonView(ACTION_IME_LAYOUT) != null) {
                    setLayoutChangerType(getButtonView(ACTION_IME_LAYOUT), CHANGER_LEFT_SIDE, orientation);
                }
                // right side
                if (getButtonView(ACTION_LAYOUT_RIGHT) != null) {
                    setLayoutChangerType(getButtonView(ACTION_LAYOUT_RIGHT), CHANGER_RIGHT_SIDE, orientation);
                } else if (getButtonView(ACTION_IME) != null) {
                    setLayoutChangerType(getButtonView(ACTION_IME), CHANGER_RIGHT_SIDE, orientation);
                } else if (getButtonView(ACTION_MENU) != null) {
                    setLayoutChangerType(getButtonView(ACTION_MENU), CHANGER_RIGHT_SIDE, orientation);
                }
            }
            if (!showingIME) {
                notifyLayoutChange(0);
            } else {
                if (getButtonView(ACTION_BACK) != null)
                        ((ImageView) getButtonView(ACTION_BACK)).setImageResource(R.drawable.ic_sysbar_back_ime);
            }
        } else {
            if (getButtonView(ACTION_BACK) != null) {
                if (showingIME) {
                    ((ImageView) getButtonView(ACTION_BACK)).setImageResource(R.drawable.ic_sysbar_back_ime);
                } else {
                    ((KeyButtonView) getButtonView(ACTION_BACK)).setImage();
                }
            }
        }

        setMenuVisibility(mShowMenu, true);
        setDisabledFlags(mDisabledFlags, true);
    }

    private void setLayoutChangerType(View v, int side, int orientation) {
        switch (side) {
            case CHANGER_LEFT_SIDE:
               ((LayoutChangerButtonView) v).setImeLayout(
                        showingIME, orientation, mTablet);
                break;
            case CHANGER_RIGHT_SIDE:
               ((LayoutChangerButtonView) v).setImeSwitch(
                        showingIME, orientation, mTablet);
                break;
        }
    }

    public void notifyLayoutChange(int direction) {
        mNextLayoutIndex = direction;
        if (direction == NavbarConstants.LAYOUT_IME) {
            if (mDisplayingLayoutIndex == NavbarConstants.LAYOUT_IME) {
                setNextLayout(mCurrentLayout);
            } else {
                setNextLayout(NavbarConstants.LAYOUT_IME);
            }
        } else {
            mCurrentLayout = (mCurrentLayout + direction + mButtonLayouts) % mButtonLayouts;
            if (mCurrentLayout >= mButtonLayouts) mCurrentLayout = mButtonLayouts - 1;
            setNextLayout(mCurrentLayout);
        }
    }

    private void setNextLayout(int index) {
        synchronized(this) {
            mNextLayoutIndex = index;
            mHandler.post(mNotifyLayoutChanged);
        }
    }

    final Runnable mNotifyLayoutChanged = new Runnable() {
        @Override
        public void run() {
            mDisplayingLayoutIndex = mNextLayoutIndex;
            setupNavigationButtons(mAllButtonContainers.get(mDisplayingLayoutIndex));
        }
    };

    public void setDisabledFlags(int disabledFlags) {
        setDisabledFlags(disabledFlags, false);
    }

    public void setDisabledFlags(int disabledFlags, boolean force) {
        if (!force && mDisabledFlags == disabledFlags) return;

        mDisabledFlags = disabledFlags;

        if (mAllButtonContainers.get(mCurrentLayout).size() == 0) return; // no buttons yet!

        final boolean disableHome = ((disabledFlags & View.STATUS_BAR_DISABLE_HOME) != 0);
        final boolean disableRecent = ((disabledFlags & View.STATUS_BAR_DISABLE_RECENT) != 0);
        final boolean disableBack = ((disabledFlags & View.STATUS_BAR_DISABLE_BACK) != 0)
                && ((mNavigationIconHints & StatusBarManager.NAVIGATION_HINT_BACK_ALT) == 0);
        final boolean disableSearch = ((disabledFlags & View.STATUS_BAR_DISABLE_SEARCH) != 0);

        if (SLIPPERY_WHEN_DISABLED) {
            setSlippery(disableHome && disableRecent && disableBack && disableSearch);
        }

        ViewGroup navButtons = (ViewGroup) mCurrentView.findViewById(R.id.nav_buttons);
        if (navButtons != null) {
            LayoutTransition lt = navButtons.getLayoutTransition();
            if (lt != null) {
                if (!lt.getTransitionListeners().contains(mTransitionListener)) {
                    lt.addTransitionListener(mTransitionListener);
                }
                if (!mScreenOn && mCurrentView != null) {
                    lt.disableTransitionType(
                            LayoutTransition.CHANGE_APPEARING |
                            LayoutTransition.CHANGE_DISAPPEARING |
                            LayoutTransition.APPEARING |
                            LayoutTransition.DISAPPEARING);
                }
            }
        }

        KeyButtonView[] allButtons = getAllButtons();
        for (KeyButtonView button : allButtons) {
            if (button != null) {
                Object tag = button.getTag();
                if (tag == null) {
                    setVisibleOrInvisible(button, !disableHome);
                } else if (ACTION_HOME.equals(tag)) {
                    setVisibleOrInvisible(button, !disableHome);
                } else if (ACTION_BACK.equals(tag)) {
                    setVisibleOrInvisible(button, !disableBack);
                } else if (ACTION_RECENTS.equals(tag)) {
                    setVisibleOrInvisible(button, !disableRecent);
                } else {
                    setVisibleOrInvisible(button, !disableRecent);
                }
            }
        }

        if (getButtonView(ACTION_BACK) != null)
                getButtonView(ACTION_BACK)   .setVisibility(disableBack       ? View.INVISIBLE : View.VISIBLE);
        if (getButtonView(ACTION_HOME) != null)
                getButtonView(ACTION_HOME)   .setVisibility(disableHome       ? View.INVISIBLE : View.VISIBLE);
        if (getButtonView(ACTION_RECENTS) != null)
                getButtonView(ACTION_RECENTS).setVisibility(disableRecent     ? View.INVISIBLE : View.VISIBLE);

        mBarTransitions.applyBackButtonQuiescentAlpha(mBarTransitions.getMode(), true /*animate*/);

        if (mButtonLayouts == 1) {
            if (mLegacyMenu) {
                if (mImeLayout) {
                    if (getButtonView(ACTION_IME) != null)
                        getButtonView(ACTION_IME).setVisibility(showingIME ? View.VISIBLE : View.INVISIBLE);
                    if (getButtonView(ACTION_IME_LAYOUT) != null) getButtonView(ACTION_IME_LAYOUT)
                        .setVisibility(showingIME ? View.VISIBLE : View.INVISIBLE);
                } else {
                    if (getButtonView(ACTION_MENU) != null) getButtonView(ACTION_MENU)
                        .setVisibility(mShowMenu ? View.VISIBLE : View.INVISIBLE);
                }
            }
        } else {
            if (!showingIME) {
                if ((LayoutChangerButtonView) getButtonView(ACTION_LAYOUT_LEFT) != null)
                        ((LayoutChangerButtonView) getButtonView(ACTION_LAYOUT_LEFT)).setDrawingAlpha(0.20f);
                if ((LayoutChangerButtonView) getButtonView(ACTION_LAYOUT_RIGHT) != null)
                        ((LayoutChangerButtonView) getButtonView(ACTION_LAYOUT_RIGHT)).setDrawingAlpha(0.20f);
            }
        }
    }

    private void setVisibleOrInvisible(View view, boolean visible) {
        if (view != null) {
            view.setVisibility(visible ? VISIBLE : INVISIBLE);
        }
    }

    private void setVisibleOrGone(View view, boolean visible) {
        if (view != null) {
            view.setVisibility(visible ? VISIBLE : GONE);
        }
    }

    public void setSlippery(boolean newSlippery) {
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) getLayoutParams();
        if (lp != null) {
            boolean oldSlippery = (lp.flags & WindowManager.LayoutParams.FLAG_SLIPPERY) != 0;
            if (!oldSlippery && newSlippery) {
                lp.flags |= WindowManager.LayoutParams.FLAG_SLIPPERY;
            } else if (oldSlippery && !newSlippery) {
                lp.flags &= ~WindowManager.LayoutParams.FLAG_SLIPPERY;
            } else {
                return;
            }
            WindowManager wm = (WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE);
            wm.updateViewLayout(this, lp);
        }
    }

    public void setMenuVisibility(final boolean show) {
        setMenuVisibility(show, false);
    }

    public void setMenuVisibility(final boolean show, final boolean force) {
        if (!force && mShowMenu == show) return;

        mShowMenu = show;

        // Only show Menu if IME switcher not shown.
        final boolean shouldShow = mShowMenu &&
                ((mNavigationIconHints & StatusBarManager.NAVIGATION_HINT_IME_SHOWN) == 0);

        if (mLegacyMenu && !showingIME) {
            if (mButtonLayouts != 1) {
                if (attachedToLayoutButtonView(ACTION_LAYOUT_RIGHT) != null) {
                    ((LayoutChangerButtonView) getButtonView(ACTION_LAYOUT_RIGHT)).setMenuAction(
                            shouldShow, getResources().getConfiguration().orientation, mTablet);
                } else if (attachedToLayoutButtonView(ACTION_MENU) != null) {
                    ((LayoutChangerButtonView) getButtonView(ACTION_MENU)).setMenuAction(
                            shouldShow, getResources().getConfiguration().orientation, mTablet);
                }
            } else {
                if (!mImeLayout && (getButtonView(ACTION_MENU) != null)) setVisibleOrInvisible(getButtonView(ACTION_MENU), mShowMenu);
            }
        }
    }

    @Override
    public void onFinishInflate() {
        mRotatedViews[Surface.ROTATION_0] =
                mRotatedViews[Surface.ROTATION_180] = findViewById(R.id.rot0);

        mRotatedViews[Surface.ROTATION_90] = findViewById(R.id.rot90);

        mRotatedViews[Surface.ROTATION_270] = NAVBAR_ALWAYS_AT_RIGHT
                ? findViewById(R.id.rot90)
                : findViewById(R.id.rot270);

        mCurrentView = mRotatedViews[Surface.ROTATION_0];

        updateRTLOrder();
        loadButtonArrays();
        setDisabledFlags(mDisabledFlags);
    }

   @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        final ContentResolver r = mContext.getContentResolver();

        if (mSettingsObserver == null) {
            mSettingsObserver = new ContentObserver(new Handler()) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    if (uri.equals(Settings.System.getUriFor(Settings.System.SOFTKEY_LONG_PRESS_CONFIGURATION))) {
                        mLongPressTimeout = Settings.System.getInt(r,
                                Settings.System.SOFTKEY_LONG_PRESS_CONFIGURATION, ViewConfiguration.getLongPressTimeout());
                    } else {
                        mImeLayout = Settings.System.getInt(r, Settings.System.NAVIGATION_BAR_ARROWS, 0) == 1;
                        mLegacyMenu = Settings.System.getInt(r, Settings.System.NAVIGATION_BAR_SIDEKEYS, 1) == 1;
                        mButtonLayouts = Settings.System.getInt(r, Settings.System.NAVIGATION_BAR_ALTERNATE_LAYOUTS, 1);

                        for(int i=0;i<mButtonLayouts;i++)
                            mButtonContainerStrings[i] = Settings.System.getString(r, buttonSettings[i]);
                        loadButtonArrays();
                    }
            }};

            for(int i=0;i<5;i++)
                r.registerContentObserver(Settings.System.getUriFor(buttonSettings[i]), false, mSettingsObserver);
            r.registerContentObserver(Settings.System.getUriFor(Settings.System.NAVIGATION_BAR_SIDEKEYS),
                    false, mSettingsObserver);
            r.registerContentObserver(Settings.System.getUriFor(Settings.System.NAVIGATION_BAR_ARROWS),
                    false, mSettingsObserver);
            r.registerContentObserver(Settings.System.getUriFor(Settings.System.NAVIGATION_IME_LAYOUT),
                    false, mSettingsObserver);
            r.registerContentObserver(Settings.System.getUriFor(Settings.System.NAVIGATION_BAR_ALTERNATE_LAYOUTS),
                    false, mSettingsObserver);
            r.registerContentObserver(Settings.System.getUriFor(Settings.System.SOFTKEY_LONG_PRESS_CONFIGURATION),
                    false, mSettingsObserver);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        final ContentResolver r = mContext.getContentResolver();

        if (mSettingsObserver != null) {
            r.unregisterContentObserver(mSettingsObserver);
            mSettingsObserver = null;
        }
    }

    private void loadButtonArrays() {
        mAllButtonContainers.clear();
        mAllButtonContainers.put(NavbarConstants.DEFAULT_LAYOUT, getButtonsArray(mDefaultLayout));
        mAllButtonContainers.put(NavbarConstants.LAYOUT_IME, getButtonsArray(mIMEKeyLayout));
        for (int j = 0; j < mButtonLayouts; j++) {
            if (mButtonContainerStrings[j] == null || TextUtils.isEmpty(mButtonContainerStrings[j])) {
                mAllButtonContainers.put(j,getButtonsArray(mDefaultLayout));
            } else {
                mAllButtonContainers.put(j,getButtonsArray(mButtonContainerStrings[j]));
            }
        }
        if (mCurrentLayout >= mButtonLayouts) mCurrentLayout = mButtonLayouts - 1;
        setupNavigationButtons(mAllButtonContainers.get(mCurrentLayout));
    }

    private SparseArray<KeyButtonInfo> getButtonsArray(final String userButtonString) {
        final String[] userButtons = userButtonString.split("\\|");
        final SparseArray<KeyButtonInfo> mButtonsContainer = new SparseArray<KeyButtonInfo>();
        int key = 0;
        for (String button : userButtons) {
            final String[] actions = button.split(",", 4);
            mButtonsContainer.put(key, new KeyButtonInfo(actions[0], actions[1], actions[2], actions[3]));
            key = key + 1;
        }
        return mButtonsContainer;
    }

    private void setupNavigationButtons(SparseArray<KeyButtonInfo> buttonsArray) {
        final boolean stockThreeButtonLayout = buttonsArray.size() == 3;
        final int length = buttonsArray.size();
        final int separatorSize = (int) mMenuButtonWidth;

        for (int i = 0; i <= 1; i++) {
            landscape = (i == 1);

            navButtons = (LinearLayout) (landscape ? mRotatedViews[Surface.ROTATION_90]
                    .findViewById(R.id.nav_buttons) : mRotatedViews[Surface.ROTATION_0]
                    .findViewById(R.id.nav_buttons));
            lightsOut = (LinearLayout) (landscape ? mRotatedViews[Surface.ROTATION_90]
                    .findViewById(R.id.lights_out) : mRotatedViews[Surface.ROTATION_0]
                    .findViewById(R.id.lights_out));
            navButtons.removeAllViews();
            lightsOut.removeAllViews();

            // multiple layouts: left-side layout changer
            if (mButtonLayouts > 1) {
                if (!mImeLayout || (mImeLayout && !showingIME)) {
                    mInfo = new KeyButtonInfo(ACTION_LAYOUT_LEFT);
                    mChanger = new LayoutChangerButtonView(mContext, null);
                    mChanger.setButtonActions(mInfo);
                    if (mTablet) {
                        mChanger.setImageResource(R.drawable.ic_sysbar_layout_left);
                    } else {
                        mChanger.setImageResource(landscape
                                ? R.drawable.ic_sysbar_layout_left_landscape
                                : R.drawable.ic_sysbar_layout_left);
                    }
                    mChanger.setLayoutParams(getLayoutParams(landscape, mLayoutChangerWidth, 0f));

                    addButton(navButtons, mChanger, landscape);
                    addLightsOutButton(lightsOut, mChanger, landscape, false);
                }
                if (mImeLayout && showingIME) {
                    mInfo = new KeyButtonInfo(ACTION_IME_LAYOUT);
                    mChanger = new LayoutChangerButtonView(mContext, null);
                    mChanger.setButtonActions(mInfo);
                    mChanger.setImageResource(R.drawable.ic_sysbar_ime_arrows);
                    mChanger.setLayoutParams(getLayoutParams(landscape, mLayoutChangerWidth, 0f));

                    addButton(navButtons, mChanger, landscape);
                    addLightsOutButton(lightsOut, mChanger, landscape, false);
                }
            }

            // single layout: AOSP key spacing on left side
            if (mLegacyMenu && mButtonLayouts == 1) {
                    if (mImeLayout) {
                    mInfo = new KeyButtonInfo(ACTION_IME_LAYOUT);
                    mChanger = new LayoutChangerButtonView(mContext, null);
                    mChanger.setButtonActions(mInfo);
                    mChanger.setImageResource(R.drawable.ic_sysbar_ime_arrows);
                    mChanger.setLayoutParams(getLayoutParams(landscape, mTablet ? mMenuButtonWidth : separatorSize, 0f));

                    addButton(navButtons, mChanger, landscape);
                    addLightsOutButton(lightsOut, mChanger, landscape, false);
                    mChanger.setVisibility(showingIME ? View.VISIBLE : View.INVISIBLE);
                } else {
                    addSeparator(navButtons, landscape, mTablet ? (int) mMenuButtonWidth : separatorSize, 0f);
                    addSeparator(lightsOut, landscape, mTablet ? (int) mMenuButtonWidth : separatorSize, 0f);
                }
                if (mTablet) {
                    addSeparator(navButtons, landscape, 0, stockThreeButtonLayout ? 1f : 0.5f);
                    addSeparator(lightsOut, landscape, 0, stockThreeButtonLayout ? 1f : 0.5f);
                }
            }

            // add the custom buttons
            for (int j = 0; j < length; j++) {
                mInfo = buttonsArray.get(j);
                mButton = new KeyButtonView(mContext, null);
                mButton.setDeviceOrientation(landscape, mTablet);
                mButton.setButtonActions(mInfo);
                mButton.setLongPressTimeout(mLongPressTimeout);
                mButton.setLayoutParams(getLayoutParams(landscape, mButtonWidth, mTablet ? 1f : 0.5f));

                if (!mButton.mHasBlankSingleAction) {
                    addButton(navButtons, mButton, landscape);
                    addLightsOutButton(lightsOut, mButton, landscape, false);
                } else {
                    addSeparator(navButtons, landscape, (int) mButtonWidth, mTablet ? 1f : 0.5f);
                    addSeparator(lightsOut, landscape, (int) mButtonWidth, mTablet ? 1f : 0.5f);
                }
            }

            // single layout: legacy menu button/AOSP spacing on right side
            if (mLegacyMenu && mButtonLayouts == 1) {
                mInfo = new KeyButtonInfo(mImeLayout
                                    ? mShowMenu
                                        ? ACTION_MENU
                                        : ACTION_IME
                                    : ACTION_MENU);
                mChanger = new LayoutChangerButtonView(mContext, null);
                mChanger.setButtonActions(mInfo);
                mChanger.setImageResource(mImeLayout
                                   ? mShowMenu
                                        ? R.drawable.ic_sysbar_menu
                                        : R.drawable.ic_ime_switcher_default
                                    : R.drawable.ic_sysbar_menu);
                mChanger.setLayoutParams(getLayoutParams(landscape, mTablet ? mMenuButtonWidth : separatorSize, 0f));
                mChanger.setVisibility(mShowMenu || (mImeLayout && showingIME) ? View.VISIBLE : View.INVISIBLE);

                if (mTablet) {
                    addSeparator(navButtons, landscape, 0,  stockThreeButtonLayout ? 1f : 0.5f);
                    addSeparator(lightsOut, landscape, 0,  stockThreeButtonLayout ? 1f : 0.5f);
                }
                addButton(navButtons, mChanger, landscape);
                addLightsOutButton(lightsOut, mChanger, landscape, true);
            }

            // multiple layouts: right-side layout changer button/ime switcher/legacy menu
            if (mButtonLayouts > 1) {
                if (!mImeLayout || (mImeLayout && !showingIME)) {
                    mInfo = new KeyButtonInfo(mShowMenu
                            ? ACTION_MENU
                            : ACTION_LAYOUT_RIGHT);
                    mChanger = new LayoutChangerButtonView(mContext, null);
                    mChanger.setButtonActions(mInfo);
                    if (mTablet) {
                        mChanger.setImageResource(mShowMenu
                                ? R.drawable.ic_sysbar_menu
                                : R.drawable.ic_sysbar_layout_right);
                    } else {
                         mChanger.setImageResource(mShowMenu
                                ? R.drawable.ic_sysbar_menu
                                : landscape
                                        ? R.drawable.ic_sysbar_layout_right_landscape
                                        : R.drawable.ic_sysbar_layout_right);
                    }
                    mChanger.setLayoutParams(getLayoutParams(landscape, mLayoutChangerWidth, 0f));

                    addButton(navButtons, mChanger, landscape);
                    addLightsOutButton(lightsOut, mChanger, landscape, false);
                }
                if (mImeLayout && showingIME) {
                    mInfo = new KeyButtonInfo(ACTION_IME);
                    mChanger = new LayoutChangerButtonView(mContext, null);
                    mChanger.setButtonActions(mInfo);
                    mChanger.setImageResource(R.drawable.ic_ime_switcher_default);
                    mChanger.setLayoutParams(getLayoutParams(landscape, mLayoutChangerWidth, 0f));

                    addButton(navButtons, mChanger, landscape);
                    addLightsOutButton(lightsOut, mChanger, landscape, false);
                }
            }
        }
        invalidate();

        // Reset the navigation search assistant
        if (getButtonView(ACTION_HOME) != null && mBar != null) {
        boolean needsHomeActionListener = !((KeyButtonView) getButtonView(ACTION_HOME)).mHasLongAction;
        if (needsHomeActionListener) mBar.setHomeActionListener();
        }
        // Save the layout for the next reboot
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                Settings.System.putIntForUser(mContext.getContentResolver(),
                        Settings.System.NAVIGATION_BAR_RESTORE, mCurrentLayout,
                        UserHandle.USER_CURRENT);
            }
        });
    }

    public boolean isVertical() {
        return mVertical;
    }

    public void setLeftInLandscape(boolean leftInLandscape) {
        mLeftInLandscape = leftInLandscape;
        mDeadZone.setStartFromRight(leftInLandscape);
    }

    public void reorient() {
        final int rot = mDisplay.getRotation();
        for (int i=0; i<4; i++) {
            mRotatedViews[i].setVisibility(View.GONE);
        }
        mCurrentView = mRotatedViews[rot];
        mCurrentView.setVisibility(View.VISIBLE);

        mDeadZone = (DeadZone) mCurrentView.findViewById(R.id.deadzone);
        mDeadZone.setStartFromRight(mLeftInLandscape);

        // force the low profile & disabled states into compliance
        mBarTransitions.init(mVertical);
        setMenuVisibility(mShowMenu, true /* force */);

        if (DEBUG) {
            Log.d(TAG, "reorient(): rot=" + mDisplay.getRotation());
        }

        // swap to x coordinate if orientation is not in vertical
        if (mDelegateHelper != null) {
            mDelegateHelper.setSwapXY(mVertical);
        }
        updateTaskSwitchHelper();

        setNavigationIconHints(mNavigationIconHints, true);
        setDisabledFlags(mDisabledFlags, true /* force */);
    }

    private void updateTaskSwitchHelper() {
        boolean isRtl = (getLayoutDirection() == View.LAYOUT_DIRECTION_RTL);
        mTaskSwitchHelper.setBarState(mVertical, isRtl);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        mDelegateHelper.setInitialTouchRegion(getButtonView(ACTION_HOME),
                getButtonView(ACTION_BACK),
                getButtonView(ACTION_RECENTS));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (DEBUG) Log.d(TAG, String.format(
                    "onSizeChanged: (%dx%d) old: (%dx%d)", w, h, oldw, oldh));

        final boolean newVertical = w > 0 && h > w;
        if (newVertical != mVertical) {
            mVertical = newVertical;
            //Log.v(TAG, String.format("onSizeChanged: h=%d, w=%d, vert=%s", h, w, mVertical?"y":"n"));
            reorient();
            notifyVerticalChangedListener(newVertical);
        }

        postCheckForInvalidLayout("sizeChanged");
        super.onSizeChanged(w, h, oldw, oldh);
    }

    private void notifyVerticalChangedListener(boolean newVertical) {
        if (mOnVerticalChangedListener != null) {
            mOnVerticalChangedListener.onVerticalChanged(newVertical);
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateRTLOrder();
        updateTaskSwitchHelper();
    }

    public KeyButtonView[] getAllButtons() {
        ViewGroup view = (ViewGroup) mCurrentView.findViewById(R.id.nav_buttons);
        int N = view.getChildCount();
        KeyButtonView[] views = new KeyButtonView[N];

        int workingIdx = 0;
        for (int i = 0; i < N; i++) {
            View child = view.getChildAt(i);
            if (child instanceof KeyButtonView) {
                views[workingIdx++] = (KeyButtonView) child;
            }
        }
        return views;
    }

    /**
     * In landscape, the LinearLayout is not auto mirrored since it is vertical. Therefore we
     * have to do it manually
     */
    private void updateRTLOrder() {
        boolean isLayoutRtl = getResources().getConfiguration()
                .getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        if (mIsLayoutRtl != isLayoutRtl) {

            // We swap all children of the 90 and 270 degree layouts, since they are vertical
            View rotation90 = mRotatedViews[Surface.ROTATION_90];
            swapChildrenOrderIfVertical(rotation90.findViewById(R.id.nav_buttons));

            View rotation270 = mRotatedViews[Surface.ROTATION_270];
            if (rotation90 != rotation270) {
                swapChildrenOrderIfVertical(rotation270.findViewById(R.id.nav_buttons));
            }
            mIsLayoutRtl = isLayoutRtl;
        }
    }


    /**
     * Swaps the children order of a LinearLayout if it's orientation is Vertical
     *
     * @param group The LinearLayout to swap the children from.
     */
    private void swapChildrenOrderIfVertical(View group) {
        if (group instanceof LinearLayout) {
            LinearLayout linearLayout = (LinearLayout) group;
            if (linearLayout.getOrientation() == VERTICAL) {
                int childCount = linearLayout.getChildCount();
                ArrayList<View> childList = new ArrayList<>(childCount);
                for (int i = 0; i < childCount; i++) {
                    childList.add(linearLayout.getChildAt(i));
                }
                linearLayout.removeAllViews();
                for (int i = childCount - 1; i >= 0; i--) {
                    linearLayout.addView(childList.get(i));
                }
            }
        }
    }

    /*
    @Override
    protected void onLayout (boolean changed, int left, int top, int right, int bottom) {
        if (DEBUG) Log.d(TAG, String.format(
                    "onLayout: %s (%d,%d,%d,%d)",
                    changed?"changed":"notchanged", left, top, right, bottom));
        super.onLayout(changed, left, top, right, bottom);
    }

    // uncomment this for extra defensiveness in WORKAROUND_INVALID_LAYOUT situations: if all else
    // fails, any touch on the display will fix the layout.
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (DEBUG) Log.d(TAG, "onInterceptTouchEvent: " + ev.toString());
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            postCheckForInvalidLayout("touch");
        }
        return super.onInterceptTouchEvent(ev);
    }
    */


    private String getResourceName(int resId) {
        if (resId != 0) {
            final android.content.res.Resources res = getContext().getResources();
            try {
                return res.getResourceName(resId);
            } catch (android.content.res.Resources.NotFoundException ex) {
                return "(unknown)";
            }
        } else {
            return "(null)";
        }
    }

    private void postCheckForInvalidLayout(final String how) {
        mHandler.obtainMessage(MSG_CHECK_INVALID_LAYOUT, 0, 0, how).sendToTarget();
    }

    private static String visibilityToString(int vis) {
        switch (vis) {
            case View.INVISIBLE:
                return "INVISIBLE";
            case View.GONE:
                return "GONE";
            default:
                return "VISIBLE";
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("NavigationBarView {");
        final Rect r = new Rect();
        final Point size = new Point();
        mDisplay.getRealSize(size);

        pw.println(String.format("      this: " + PhoneStatusBar.viewInfo(this)
                        + " " + visibilityToString(getVisibility())));

        getWindowVisibleDisplayFrame(r);
        final boolean offscreen = r.right > size.x || r.bottom > size.y;
        pw.println("      window: "
                + r.toShortString()
                + " " + visibilityToString(getWindowVisibility())
                + (offscreen ? " OFFSCREEN!" : ""));

        pw.println(String.format("      mCurrentView: id=%s (%dx%d) %s",
                        getResourceName(mCurrentView.getId()),
                        mCurrentView.getWidth(), mCurrentView.getHeight(),
                        visibilityToString(mCurrentView.getVisibility())));

        pw.println(String.format("      disabled=0x%08x vertical=%s menu=%s",
                        mDisabledFlags,
                        mVertical ? "true" : "false",
                        mShowMenu ? "true" : "false"));

        dumpButton(pw, "back", getButtonView(ACTION_BACK));
        dumpButton(pw, "home", getButtonView(ACTION_HOME));
        dumpButton(pw, "rcnt", getButtonView(ACTION_RECENTS));
        dumpButton(pw, "menu", getButtonView(ACTION_MENU));

        pw.println("    }");
    }

    private static void dumpButton(PrintWriter pw, String caption, View button) {
        pw.print("      " + caption + ": ");
        if (button == null) {
            pw.print("null");
        } else {
            pw.print(PhoneStatusBar.viewInfo(button)
                    + " " + visibilityToString(button.getVisibility())
                    + " alpha=" + button.getAlpha()
                    );
            if (button instanceof KeyButtonView) {
                pw.print(" drawingAlpha=" + ((KeyButtonView)button).getDrawingAlpha());
                pw.print(" quiescentAlpha=" + ((KeyButtonView)button).getQuiescentAlpha());
            }
        }
        pw.println();
    }

    private void addSeparator(LinearLayout layout, boolean landscape, int size, float weight) {
        Space separator = new Space(mContext);
        separator.setLayoutParams(getLayoutParams(landscape, size, weight));
        if (landscape && !mTablet) {
            layout.addView(separator, 0);
        } else {
            layout.addView(separator);
        }
    }

    private void addButton(ViewGroup root, View v, boolean landscape) {
        if (landscape && !mTablet)
            root.addView(v, 0);
        else
            root.addView(v);
    }

    public LinearLayout.LayoutParams getLayoutParams(boolean landscape, float px, float weight) {
        if (weight != 0) {
            px = 0;
        }
        return landscape && !mTablet ?
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) px, weight) :
                new LinearLayout.LayoutParams((int) px, LinearLayout.LayoutParams.MATCH_PARENT, weight);
    }

    private void addLightsOutButton(LinearLayout root, View v, boolean landscape, boolean empty) {
        ImageView addMe = new ImageView(mContext);
        addMe.setLayoutParams(v.getLayoutParams());
        addMe.setImageResource(empty ? R.drawable.ic_sysbar_lights_out_dot_large
                : R.drawable.ic_sysbar_lights_out_dot_small);
        addMe.setScaleType(ImageView.ScaleType.CENTER);
        addMe.setVisibility(empty ? View.INVISIBLE : View.VISIBLE);
        if (landscape && !mTablet)
            root.addView(addMe, 0);
        else
            root.addView(addMe);
    }

    public static boolean isTablet(Context context) {
        boolean xlarge = ((context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == 4);
        boolean large = ((context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE);
        return (xlarge || large);
    }

    public interface OnVerticalChangedListener {
        void onVerticalChanged(boolean isVertical);
    }
}
