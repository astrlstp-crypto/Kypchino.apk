package com.kupchino.app;

import android.app.Activity;
import android.app.Application;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

/** Automatically switches Kupchino Drive to landscape while keeping the main app portrait. */
public class KupchinoApplication extends Application {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Activity currentActivity;
    private int lastRequested = Integer.MIN_VALUE;

    private final Runnable orientationWatcher = new Runnable() {
        @Override public void run() {
            Activity activity = currentActivity;
            if (activity != null && !activity.isFinishing()) {
                View decor = activity.getWindow().getDecorView();
                boolean driveVisible = containsDriveView(decor);
                int wanted = driveVisible
                        ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                if (wanted != lastRequested) {
                    lastRequested = wanted;
                    activity.setRequestedOrientation(wanted);
                }
                handler.postDelayed(this, 150);
            }
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override public void onActivityResumed(Activity activity) {
                currentActivity = activity;
                lastRequested = Integer.MIN_VALUE;
                handler.removeCallbacks(orientationWatcher);
                handler.post(orientationWatcher);
            }

            @Override public void onActivityPaused(Activity activity) {
                if (currentActivity == activity) {
                    handler.removeCallbacks(orientationWatcher);
                }
            }

            @Override public void onActivityDestroyed(Activity activity) {
                if (currentActivity == activity) currentActivity = null;
            }

            @Override public void onActivityCreated(Activity a, Bundle b) {}
            @Override public void onActivityStarted(Activity a) {}
            @Override public void onActivityStopped(Activity a) {}
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
        });
    }

    private boolean containsDriveView(View view) {
        if (view instanceof DrivePerformance3DView || view instanceof DriveUltra3DView || view instanceof Drive3DView) {
            return true;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (containsDriveView(group.getChildAt(i))) return true;
            }
        }
        return false;
    }
}
