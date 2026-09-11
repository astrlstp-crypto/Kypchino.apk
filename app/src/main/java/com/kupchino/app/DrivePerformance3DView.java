package com.kupchino.app;

import android.content.Context;
import android.opengl.EGL14;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Performance wrapper for Kupchino Drive MAX.
 * Adds explicit V-Sync control and an independent frame limiter from 30 to 240 FPS.
 */
public class DrivePerformance3DView extends DriveUltra3DView {
    private volatile int targetFps = 120;
    private volatile boolean vsyncEnabled = true;
    private long lastFrameStartNs = 0L;

    public DrivePerformance3DView(Context context) {
        super(context);
    }

    public void setPerformance(int fps, boolean vsync) {
        targetFps = Math.max(30, Math.min(240, fps));
        vsyncEnabled = vsync;
        lastFrameStartNs = 0L;
        queueEvent(this::applySwapInterval);
    }

    public int getTargetFps() {
        return targetFps;
    }

    public boolean isVsyncEnabled() {
        return vsyncEnabled;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        super.onSurfaceCreated(gl, config);
        applySwapInterval();
        lastFrameStartNs = 0L;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        paceFrame();
        super.onDrawFrame(gl);
    }

    private void applySwapInterval() {
        try {
            EGL14.eglSwapInterval(EGL14.eglGetCurrentDisplay(), vsyncEnabled ? 1 : 0);
        } catch (Throwable ignored) {
            // Some Android GPU drivers ignore swap interval changes. The FPS limiter still works.
        }
    }

    private void paceFrame() {
        final int fps = Math.max(30, Math.min(240, targetFps));
        final long targetFrameNs = 1_000_000_000L / fps;
        long now = System.nanoTime();

        if (lastFrameStartNs != 0L) {
            long elapsed = now - lastFrameStartNs;
            long remaining = targetFrameNs - elapsed;
            if (remaining > 0L) {
                long millis = remaining / 1_000_000L;
                int nanos = (int) (remaining % 1_000_000L);
                try {
                    Thread.sleep(millis, nanos);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        lastFrameStartNs = System.nanoTime();
    }
}
