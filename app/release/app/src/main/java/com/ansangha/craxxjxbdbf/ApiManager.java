package com.ansangha.craxxjxbdbf;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * HTTP client for AWS control server (OkHttp on background threads).
 * UI callbacks (dialogs, toasts) are marshalled to the main thread.
 */
public final class ApiManager {

    public static final String CONTROL_SERVER_BASE = "http://100.53.27.75:3000";
    private static final String BASE_URL = CONTROL_SERVER_BASE;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .callTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    private static final ExecutorService IO = Executors.newCachedThreadPool();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static final String TAG = "ApiManager";
    private static final String PREFS = "habitpro_api_prefs";
    private static final String KEY_LAST_LOC_UPLOAD_MS = "last_loc_upload_ms";
    private static final String KEY_USER_ID = "stable_user_id";
    private static final String KEY_REGISTERED = "user_registered_with_server";

    private ApiManager() {
    }

    public interface BooleanCallback {
        void onResult(boolean value);
    }

    private static void toastError(Context ctx, String msg) {
        Context app = ctx.getApplicationContext();
        MAIN.post(() -> Toast.makeText(app, msg, Toast.LENGTH_LONG).show());
    }

    private static void runOnMain(Runnable r) {
        MAIN.post(r);
    }

    /** Stable anonymous id for this install (not hardware id). */
    public static String getOrCreateUserId(Context context) {
        SharedPreferences p = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String id = p.getString(KEY_USER_ID, null);
        if (id == null) {
            id = UUID.randomUUID().toString();
            p.edit().putString(KEY_USER_ID, id).apply();
        }
        return id;
    }

    /**
     * GET /app-status — {@code true} if app is enabled.
     * If disabled, shows a non-cancelable maintenance dialog.
     * On network failure, returns {@code true} so the app stays usable offline.
     */
    public static void checkAppStatus(Activity activity, BooleanCallback callback) {
        IO.execute(() -> {
            boolean enabled = true;
            try {
                Request req = new Request.Builder()
                        .url(BASE_URL + "/app-status")
                        .get()
                        .build();
                try (Response resp = CLIENT.newCall(req).execute()) {
                    String body = resp.body() != null ? resp.body().string() : "";
                    if (!resp.isSuccessful()) {
                        toastError(activity, "App status check failed: HTTP " + resp.code());
                        runOnMain(() -> callback.onResult(true));
                        return;
                    }
                    enabled = parseAppEnabled(body);
                }
            } catch (IOException e) {
                toastError(activity, "Network error: " + e.getMessage());
                runOnMain(() -> callback.onResult(true));
                return;
            }

            boolean finalEnabled = enabled;
            if (!finalEnabled) {
                runOnMain(() -> new AlertDialog.Builder(activity)
                        .setTitle("Maintenance")
                        .setMessage("App is under maintenance")
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, (d, w) -> d.dismiss())
                        .show());
            }
            runOnMain(() -> callback.onResult(finalEnabled));
        });
    }

    private static boolean parseAppEnabled(String json) {
        if (json == null || json.trim().isEmpty()) return true;
        try {
            JSONObject o = new JSONObject(json.trim());
            if (o.has("enabled")) return o.optBoolean("enabled", true);
            if (o.has("appEnabled")) return o.optBoolean("appEnabled", true);
            if (o.has("isEnabled")) return o.optBoolean("isEnabled", true);
            if (o.has("active")) return o.optBoolean("active", true);
            if (o.has("status")) {
                String s = o.optString("status", "").toLowerCase();
                if (s.equals("disabled") || s.equals("maintenance") || s.equals("off")) return false;
            }
            return true;
        } catch (Exception e) {
            // Plain text fallback
            String lower = json.toLowerCase();
            if (lower.contains("disabled") || lower.contains("maintenance")) return false;
            return true;
        }
    }

    /**
     * POST /register-user with JSON {@code {"userId","userName"}}.
     */
    public static void registerUser(Context context, String userId, String userName) {
        IO.execute(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("userId", userId);
                json.put("userName", userName);
                RequestBody body = RequestBody.create(json.toString(), JSON);
                Request req = new Request.Builder()
                        .url(BASE_URL + "/register-user")
                        .post(body)
                        .build();
                try (Response resp = CLIENT.newCall(req).execute()) {
                    if (!resp.isSuccessful()) {
                        toastError(context, "Register failed: HTTP " + resp.code());
                        return;
                    }
                    context.getApplicationContext()
                            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean(KEY_REGISTERED, true)
                            .apply();
                }
            } catch (Exception e) {
                toastError(context, "Register error: " + e.getMessage());
            }
        });
    }

    /** Calls {@link #registerUser(Context, String, String)} once per install. */
    public static void registerUserIfFirstLaunch(Context context, String userId, String userName) {
        SharedPreferences p = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (p.getBoolean(KEY_REGISTERED, false)) return;
        registerUser(context, userId, userName);
    }

    /**
     * POST /sync-habits — body {@code {"userId","habits": [...]}}.
     * {@code habitsJsonArray} must be a JSON array string of objects with snake_case keys:
     * id, name, description, category, frequency, streak_days, best_streak, is_completed_today,
     * last_completed_at, current_count, target_count, is_active (see {@code HabitSyncWireDto}).
     */
    public static void syncHabits(Context context, String userId, String habitsJsonArray) {
        IO.execute(() -> {
            try {
                JSONObject root = new JSONObject();
                root.put("userId", userId);
                root.put("habits", new JSONArray(habitsJsonArray));
                RequestBody body = RequestBody.create(root.toString(), JSON);
                Request req = new Request.Builder()
                        .url(BASE_URL + "/sync-habits")
                        .post(body)
                        .build();
                try (Response resp = CLIENT.newCall(req).execute()) {
                    if (!resp.isSuccessful()) {
                        toastError(context, "Sync habits failed: HTTP " + resp.code());
                    }
                }
            } catch (Exception e) {
                toastError(context, "Sync habits error: " + e.getMessage());
            }
        });
    }

    /**
     * POST /log with JSON {@code {"message":"..."}}.
     */
    public static void sendLog(Context context, String message) {
        IO.execute(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("message", message);
                RequestBody body = RequestBody.create(json.toString(), JSON);
                Request req = new Request.Builder()
                        .url(BASE_URL + "/log")
                        .post(body)
                        .build();
                try (Response resp = CLIENT.newCall(req).execute()) {
                    if (!resp.isSuccessful()) {
                        toastError(context, "Log send failed: HTTP " + resp.code());
                    }
                }
            } catch (Exception e) {
                toastError(context, "Log error: " + e.getMessage());
            }
        });
    }

    /**
     * GET /check-ban/{userId} — {@code true} if banned.
     * If banned, shows dialog and finishes the activity.
     * On network error, assumes not banned.
     */
    public static void checkBan(Activity activity, String userId, BooleanCallback callback) {
        IO.execute(() -> {
            boolean banned = false;
            HttpUrl base = HttpUrl.parse(BASE_URL);
            if (base == null) {
                toastError(activity, "Invalid API base URL");
                runOnMain(() -> callback.onResult(false));
                return;
            }
            HttpUrl url = base.newBuilder()
                    .addPathSegment("check-ban")
                    .addPathSegment(userId)
                    .build();
            Request req = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response resp = CLIENT.newCall(req).execute()) {
                String body = resp.body() != null ? resp.body().string() : "";
                if (!resp.isSuccessful()) {
                    toastError(activity, "Ban check failed: HTTP " + resp.code());
                    runOnMain(() -> callback.onResult(false));
                    return;
                }
                banned = parseBanned(body);
            } catch (IOException e) {
                toastError(activity, "Network error: " + e.getMessage());
                runOnMain(() -> callback.onResult(false));
                return;
            }

            boolean finalBanned = banned;
            if (finalBanned) {
                runOnMain(() -> new AlertDialog.Builder(activity)
                        .setTitle("Access denied")
                        .setMessage("Your account has been restricted.")
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, (d, w) -> {
                            d.dismiss();
                            activity.finishAffinity();
                        })
                        .show());
            }
            runOnMain(() -> callback.onResult(finalBanned));
        });
    }

    private static boolean parseBanned(String json) {
        if (json == null || json.trim().isEmpty()) return false;
        try {
            JSONObject o = new JSONObject(json.trim());
            if (o.has("banned")) return o.optBoolean("banned", false);
            if (o.has("isBanned")) return o.optBoolean("isBanned", false);
            if (o.has("ban")) return o.optBoolean("ban", false);
            return false;
        } catch (Exception e) {
            String lower = json.toLowerCase();
            return lower.contains("\"banned\":true") || lower.contains("banned=true");
        }
    }

    /**
     * TODO(server): POST family/location when the portal contract is ready. Throttled logging only.
     */
    public static void uploadLocation(Context context, double lat, double lng, float accuracy, long timeMs) {
        Context app = context.getApplicationContext();
        SharedPreferences p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        long last = p.getLong(KEY_LAST_LOC_UPLOAD_MS, 0L);
        if (now - last < 300_000L) {
            return;
        }
        p.edit().putLong(KEY_LAST_LOC_UPLOAD_MS, now).apply();
        Log.i(TAG, "uploadLocation (stub/throttled): lat=" + lat + " lng=" + lng
                + " acc=" + accuracy + " timeMs=" + timeMs);
        // TODO(server): IO.execute(() -> { ... });
    }

    /** TODO(server): POST aggregated usage JSON when the portal exists. */
    public static void uploadUsageSummary(Context context, String jsonSummary) {
        Log.i(TAG, "uploadUsageSummary (stub): " + jsonSummary);
        // TODO(server): IO.execute(() -> { ... });
    }
}
