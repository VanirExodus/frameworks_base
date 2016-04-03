package android.gesture;


import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManagerPolicy.PointerEventListener

/**
 * Created by ryanarcher on 4/3/16.
 */
public class ExodusGestureListener implements PointerEventListener {

    private static final String TAG = ExodusGestureListener.class.getSimpleName();
    private static final boolean DEBUG = true;
    private static final int NUM_POINTER_TWO = 2;
    private static final int NUM_POINTER_THREE = 3;
    private static final long SWIPE_TIMEOUT_MS = 500;
    private static final int MAX_TRACKED_POINTERS = 32;
    private static final int UNTRACKED_POINTER = -1;
    private static final int TWO_SWIPE_DISTANCE = 350;
    private static final int THREE_SWIPE_DISTANCE = 350;
    private final int GESTURE_SWIPE_MASK = 15;
    private final int POINTER_1_MASK = 2;
    private final int POINTER_2_MASK = 4;
    private final int POINTER_3_MASK = 8;
    private final int POINTER_4_MASK = 10;
    private final int POINTER_NONE_MASK = 1;
    private final Callbacks mCallbacks;
    private final int[] mDownPointerId = new int[MAX_TRACKED_POINTERS];
    private final float[] mDownX = new float[MAX_TRACKED_POINTERS];
    private final float[] mDownY = new float[MAX_TRACKED_POINTERS];
    private final long[] mDownTime = new long[MAX_TRACKED_POINTERS];
    private int mDownPointers;
    private boolean mSwipeFireable = false;
    private int mSwipeMask = 1;

    public ExodusGestureListener(Context context, Callbacks callbacks) {
        mCallbacks = checkNull("callback", callbacks);
    }


    private static <T> T checkNull(String name, T arg) {

        if (arg == null) {
            throw new IllegalArgumentException(name + "must not be null");
        }


        return arg;
    }

    @Override
    public void onPointerEvent(MotionEvent motionEvent) {
        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mSwipeFireable = true;
                mDownPointers = 0;
                captureDown(motionEvent, 0);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                captureDown(motionEvent, motionEvent.getActionIndex());
                break;
            case MotionEvent.ACTION_MOVE:
                if (DEBUG) Log.d(TAG, "count3" + motionEvent.getPointerCount());
                if (mSwipeFireable) {
                    detectSwipe(motionEvent);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mSwipeMask == GESTURE_SWIPE_MASK) {
                    final int swipe = detectSwipe(motionEvent);
                    mSwipeFireable = swipe == POINTER_NONE_MASK;
                    mSwipeMask = 1;
                    if (swipe == POINTER_1_MASK) {
                        mCallbacks.onSwipeTwoFingerDown();
                    } else if (swipe == POINTER_2_MASK) {
                        mCallbacks.onSwipeTwoFingerUp();
                    } else if (swipe == POINTER_3_MASK) {
                        mCallbacks.onSwipeThreeFingerDown();
                    } else if (swipe == POINTER_4_MASK) {
                        mCallbacks.onSwipeThreeFingerUp();
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                mSwipeFireable = false;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                break;
            default:
                if (DEBUG) Log.d(TAG, "Ignoring " + motionEvent);
        }
    }


    private void captureDown(MotionEvent event, int pointerIndex) {
        final int pointerId = event.getPointerId(pointerIndex);
        final int i = findIndex(pointerId);
        final int pointerCount = event.getPointerCount();
        if (DEBUG) Log.d(TAG, "pointer " + pointerId +
                " down pointerIndex=" + pointerIndex + " trackingIndex=" + i);
        if (i != UNTRACKED_POINTER) {
            mDownX[i] = event.getX(pointerIndex);
            mDownY[i] = event.getY(pointerIndex);
            mDownTime[i] = event.getEventTime();
            if (DEBUG) Log.d(TAG, "pointer " + pointerId +
                    " down x=" + mDownX[i] + " y=" + mDownY[i]);
        }
        if (pointerCount == NUM_POINTER_TWO) {
            mSwipeFireable = true;
            return;
        } else if (pointerCount == NUM_POINTER_THREE) {
            mSwipeFireable = true;
            return;
        }
        mSwipeFireable = false;
    }

    private int findIndex(int pointerId) {
        for (int i = 0; i < mDownPointers; i++) {
            if (mDownPointerId[i] == pointerId) {
                return i;
            }
        }
        if (mDownPointers == MAX_TRACKED_POINTERS || pointerId == MotionEvent.INVALID_POINTER_ID) {
            return UNTRACKED_POINTER;
        }
        mDownPointerId[mDownPointers++] = pointerId;
        return mDownPointers - 1;
    }

    private int detectSwipe(MotionEvent move) {
        move.getHistorySize();
        final int pointerCount = move.getPointerCount();
        for (int p = 0; p < pointerCount; p++) {
            final int pointerId = move.getPointerId(p);
            final int i = findIndex(pointerId);
            if (i != UNTRACKED_POINTER) {
                detectSwipe(i, move.getEventTime(), move.getX(p), move.getY(p));
            }
        }
        return POINTER_NONE_MASK;
    }

    private int detectSwipe(int i, long time, float x, float y) {
        final float fromX = mDownX[i];
        final float fromY = mDownY[i];
        final long elapsed = time - mDownTime[i];
        if (DEBUG) Log.d(TAG, "pointer " + mDownPointerId[i]
                + " moved (" + fromX + "->" + x + "," + fromY + "->" + y + ") in " + elapsed);
        if (mSwipeMask < GESTURE_SWIPE_MASK
                && y > fromY + TWO_SWIPE_DISTANCE
                && elapsed < SWIPE_TIMEOUT_MS) {
            return POINTER_1_MASK;
        } else if (mSwipeMask < GESTURE_SWIPE_MASK
                && y < fromY + TWO_SWIPE_DISTANCE
                && elapsed < SWIPE_TIMEOUT_MS) {
            return POINTER_2_MASK;
        } else if (mSwipeMask < GESTURE_SWIPE_MASK
                && y > fromY + THREE_SWIPE_DISTANCE
                && elapsed < SWIPE_TIMEOUT_MS) {
            return POINTER_3_MASK;

        } else if (mSwipeMask < GESTURE_SWIPE_MASK
                && y < fromY + THREE_SWIPE_DISTANCE
                && elapsed < SWIPE_TIMEOUT_MS) {
            return POINTER_4_MASK;
        }

        return POINTER_NONE_MASK;
    }

    public interface Callbacks {
        void onSwipeTwoFingerDown();

        void onSwipeTwoFingerUp();

        void onSwipeThreeFingerDown();

        void onSwipeThreeFingerUp();
    }

