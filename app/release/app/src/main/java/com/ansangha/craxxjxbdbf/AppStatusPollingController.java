package com.ansangha.craxxjxbdbf;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Polls {@code GET /app-status} every 30 seconds using a {@link Handler} + {@link Runnable} loop.
 * HTTP runs on a background thread; UI updates on the main thread.
 * <p>
 * Start when the activity becomes visible ({@link #start(Activity)}), stop when it leaves
 * ({@link #stop()}) — wire from {@code MainActivity.kt} {@code onStart}/{@code onStop}.
 */
public final class AppStatusPollingController {

    private static final long POLL_INTERVAL_MS = 30_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final OkHttpClient client = new OkHttpClient.Builder()
            .callTimeout(25, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    private final UiCallbacks uiCallbacks;
    private WeakReference<Activity> activityRef = new WeakReference<>(null);
    private volatile boolean running = false;
    private String lastBroadcastShown = "";

    public interface UiCallbacks {
        /** When {@code true}, show full-screen maintenance UI over the app. */
        void onMaintenanceMode(boolean show);
    }

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            final Activity activity = activityRef.get();
            if (activity == null || activity.isFinishing()) {
                stop();
                return;
            }
            io.execute(() -> {
                String body = null;
                try {
                    Request req = new Request.Builder()
                            .url(ApiManager.CONTROL_SERVER_BASE + "/app-status")
                            .get()
                            .build();
                    try (Response resp = client.newCall(req).execute()) {
                        if (resp.body() != null) {
                            body = resp.body().string();
                        }
                    }
                } catch (IOException ignored) {
                }

                final String responseBody = body;
                handler.post(() -> {
                    if (!running) return;
                    Activity a = activityRef.get();
                    if (a == null || a.isFinishing()) {
                        stop();
                        return;
                    }
                    handleStatusPayload(a, responseBody);
                    if (running) {
                        handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
                    }
                });
            });
        }
    };

    public AppStatusPollingController(@Nullable UiCallbacks uiCallbacks) {
        this.uiCallbacks = uiCallbacks != null ? uiCallbacks : new UiCallbacks() {
            @Override
            public void onMaintenanceMode(boolean show) {
                // no-op
            }
        };
    }

    /** Begin polling (first request runs immediately). */
    public void start(Activity activity) {
        activityRef = new WeakReference<>(activity);
        running = true;
        handler.removeCallbacks(pollRunnable);
        handler.post(pollRunnable);
    }

    /** Stop the polling loop and cancel pending work. */
    public void stop() {
        running = false;
        handler.removeCallbacks(pollRunnable);
    }

    private void handleStatusPayload(Activity activity, @Nullable String body) {
        if (body == null || body.trim().isEmpty()) {
            return;
        }
        try {
            JSONObject o = new JSONObject(body.trim());

            boolean enabled = !o.has("enabled") || o.optBoolean("enabled", true);
            if (!enabled) {
                stop();
                new AlertDialog.Builder(activity)
                        .setTitle("Unavailable")
                        .setMessage("App disabled by admin")
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, (d, w) -> {
                            d.dismiss();
                            activity.finishAffinity();
                        })
                        .show();
                uiCallbacks.onMaintenanceMode(false);
                return;
            }

            boolean maintenance = o.optBoolean("maintenance", false);
            uiCallbacks.onMaintenanceMode(maintenance);

            String broadcast = o.optString("broadcast", "");
            if (broadcast != null && !broadcast.isEmpty() && !broadcast.equals(lastBroadcastShown)) {
                lastBroadcastShown = broadcast;
                new AlertDialog.Builder(activity)
                        .setTitle("Notice")
                        .setMessage(broadcast)
                        .setPositiveButton(android.R.string.ok, (d, w) -> d.dismiss())
                        .show();
            }
        } catch (Exception ignored) {
        }
    }
}
