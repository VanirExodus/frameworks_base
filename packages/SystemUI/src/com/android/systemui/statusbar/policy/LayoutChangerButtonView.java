/*
 * Copyright (C) 2014 VanirAOSP
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

package com.android.systemui.statusbar.policy;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;

import com.android.internal.util.vanir.KeyButtonInfo;
import com.android.internal.util.vanir.NavbarUtils;
import com.android.internal.util.vanir.NavbarConstants;
import static com.android.internal.util.vanir.NavbarConstants.*;
import com.android.internal.util.vanir.VanirActions;
import com.android.systemui.R;

public class LayoutChangerButtonView extends KeyButtonView {
    private static final String TAG = "StatusBar.LayoutChangerButtonView";

    public LayoutChangerButtonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LayoutChangerButtonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setButtonActions(KeyButtonInfo actions) {
        this.mActions = actions;
        setTag(mActions.singleAction);
        setImage();
    }

    public void setImeLayout(boolean show, int isVertical, boolean isTablet) {
        if (show) {
            mActions.singleAction = ACTION_IME_LAYOUT;
            setImageResource(R.drawable.ic_sysbar_ime_arrows);
        } else {
            mActions.singleAction = ACTION_LAYOUT_LEFT;
            if (isTablet) {
                setImageResource(R.drawable.ic_sysbar_layout_left);
            } else {
                setImageResource(isVertical != 1
                        ? R.drawable.ic_sysbar_layout_left_landscape
                        : R.drawable.ic_sysbar_layout_left);
            }
        }

        updateAlpha();
    }

    public void setImeSwitch(boolean show, int isVertical, boolean isTablet) {
        if (show) {
            mActions.singleAction = ACTION_IME;
            setImageResource(R.drawable.ic_ime_switcher_default);
        } else {
            mActions.singleAction = ACTION_LAYOUT_RIGHT;
            if (isTablet) {
                setImageResource(R.drawable.ic_sysbar_layout_right);
            } else {
                setImageResource(isVertical != 1
                        ? R.drawable.ic_sysbar_layout_right_landscape
                        : R.drawable.ic_sysbar_layout_right);
            }
        }
        
        updateAlpha();
    }

    public void updateAlpha() {
		if (mActions.singleAction == ACTION_LAYOUT_RIGHT
				|| mActions.singleAction == ACTION_LAYOUT_LEFT) {
			setDrawingAlpha(DEFAULT_LAYOUT_CHANGER_ALPHA);
		} else {
			setDrawingAlpha(DEFAULT_QUIESCENT_ALPHA);
		}
	}

    public void setMenuAction(boolean show, int isVertical, boolean isTablet) {
        if (show) {
            mActions.singleAction = ACTION_MENU;
            setImageResource(R.drawable.ic_sysbar_menu);
        } else {
            mActions.singleAction = ACTION_LAYOUT_RIGHT;
            if (isTablet) {
                setImageResource(R.drawable.ic_sysbar_layout_right);
            } else {
                setImageResource(isVertical != 1
                        ? R.drawable.ic_sysbar_layout_right_landscape
                        : R.drawable.ic_sysbar_layout_right);
            }
        }
        
        updateAlpha();
    }

    @Override
    public void setImage() {
        setImageDrawable(NavbarUtils.getIconImage(mContext, mActions.singleAction));
        updateAlpha();
    }

    @Override
    public void setImage(final Resources res) { }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        mPm.cpuBoost(750000);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                setPressed(true);
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                break;
            case MotionEvent.ACTION_MOVE:
                int x = (int) ev.getX();
                int y = (int) ev.getY();
                setPressed(x >= -mTouchSlop
                        && x < getWidth() + mTouchSlop
                        && y >= -mTouchSlop
                        && y < getHeight() + mTouchSlop);
                break;
            case MotionEvent.ACTION_CANCEL:
                setPressed(false);
                break;
            case MotionEvent.ACTION_UP:
                if (isPressed()) {
                    playSoundEffect(SoundEffectConstants.CLICK);
                }
                setPressed(false);
                doSinglePress();
                break;
        }
        return true;
    }

    @Override
    protected void doSinglePress() {
        if (callOnClick()) {
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
        }
        VanirActions.launchAction(mContext, mActions.singleAction);
    }
}
