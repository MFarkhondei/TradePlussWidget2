package ir.tradeplus.widget;

import android.content.Context;
import android.content.SharedPreferences;

/** Per-widget-instance stored settings (Web App URL, username, token). */
public class WidgetPrefs {

    private static final String PREFS_NAME = "ir.tradeplus.widget.PREFS";

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void save(Context context, int appWidgetId, String url, String user, String token) {
        prefs(context).edit()
                .putString("url_" + appWidgetId, url)
                .putString("user_" + appWidgetId, user)
                .putString("token_" + appWidgetId, token)
                .apply();
    }

    public static String getUrl(Context context, int appWidgetId) {
        return prefs(context).getString("url_" + appWidgetId, null);
    }

    public static String getUser(Context context, int appWidgetId) {
        return prefs(context).getString("user_" + appWidgetId, null);
    }

    public static String getToken(Context context, int appWidgetId) {
        return prefs(context).getString("token_" + appWidgetId, null);
    }

    public static boolean isConfigured(Context context, int appWidgetId) {
        return getUrl(context, appWidgetId) != null
                && getUser(context, appWidgetId) != null
                && getToken(context, appWidgetId) != null;
    }

    public static void clear(Context context, int appWidgetId) {
        prefs(context).edit()
                .remove("url_" + appWidgetId)
                .remove("user_" + appWidgetId)
                .remove("token_" + appWidgetId)
                .apply();
    }
}
