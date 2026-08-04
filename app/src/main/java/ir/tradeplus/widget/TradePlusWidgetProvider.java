package ir.tradeplus.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TradePlusWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "TradePlusWidget";
    public static final String ACTION_REFRESH = "ir.tradeplus.widget.ACTION_REFRESH";
    public static final String EXTRA_WIDGET_ID = "extra_widget_id";

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#,###");

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
            int[] ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
            if (ids == null) {
                int single = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
                ids = (single != AppWidgetManager.INVALID_APPWIDGET_ID) ? new int[]{single} : new int[0];
            }
            handleUpdate(context, ids);
            return;
        }

        if (ACTION_REFRESH.equals(action)) {
            int id = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                handleUpdate(context, new int[]{id});
            }
            return;
        }

        super.onReceive(context, intent);
    }

    private void handleUpdate(Context context, int[] appWidgetIds) {
        if (appWidgetIds == null || appWidgetIds.length == 0) return;
        final Context appContext = context.getApplicationContext();
        final PendingResult result = goAsync();
        EXECUTOR.execute(() -> {
            try {
                AppWidgetManager manager = AppWidgetManager.getInstance(appContext);
                for (int id : appWidgetIds) {
                    updateOneWidget(appContext, manager, id);
                }
            } catch (Throwable t) {
                Log.e(TAG, "update failed", t);
            } finally {
                result.finish();
            }
        });
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        handleUpdate(context, appWidgetIds);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            WidgetPrefs.clear(context, id);
        }
    }

    private static void updateOneWidget(Context context, AppWidgetManager manager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        // refresh button always wired
        Intent refreshIntent = new Intent(context, TradePlusWidgetProvider.class);
        refreshIntent.setAction(ACTION_REFRESH);
        refreshIntent.putExtra(EXTRA_WIDGET_ID, appWidgetId);
        PendingIntent refreshPending = PendingIntent.getBroadcast(
                context, appWidgetId, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.btnRefresh, refreshPending);

        // tapping the total-assets value always lets the user (re)configure the widget
        openConfigOnTap(context, views, appWidgetId);

        if (!WidgetPrefs.isConfigured(context, appWidgetId)) {
            views.setTextViewText(R.id.tvTotalAssets, "0");
            views.setTextViewText(R.id.tvUpdatedAt, context.getString(R.string.not_configured));
            manager.updateAppWidget(appWidgetId, views);
            return;
        }

        String url = WidgetPrefs.getUrl(context, appWidgetId);
        String user = WidgetPrefs.getUser(context, appWidgetId);
        String token = WidgetPrefs.getToken(context, appWidgetId);

        try {
            JSONObject data = WidgetApi.fetch(url, user, token);
            fillViews(context, views, data);
        } catch (Exception e) {
            Log.e(TAG, "fetch failed", e);
            views.setTextViewText(R.id.tvUpdatedAt, "خطا در دریافت اطلاعات");
        }

        manager.updateAppWidget(appWidgetId, views);
    }

    private static void openConfigOnTap(Context context, RemoteViews views, int appWidgetId) {
        Intent configIntent = new Intent(context, ConfigActivity.class);
        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        configIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pending = PendingIntent.getActivity(
                context, appWidgetId, configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.tvTotalAssets, pending);
    }

    private static void fillViews(Context context, RemoteViews views, JSONObject data) throws Exception {
        long total = data.optLong("totalAssetsToman", 0);
        long dailyProfit = data.optLong("dailyProfitToman", 0);
        double dailyProfitPercent = data.optDouble("dailyProfitPercent", 0);
        long dailyBuy = data.optLong("dailyBuyToman", 0);
        String updatedAt = data.optString("updatedAt", "");

        views.setTextViewText(R.id.tvTotalAssets, NUMBER_FORMAT.format(total));
        views.setTextViewText(R.id.tvUpdatedAt, updatedAt);
        views.setTextViewText(R.id.tvDailyBuy, NUMBER_FORMAT.format(dailyBuy) + " تومان");

        boolean profitNegative = dailyProfit < 0;
        String profitStr = (profitNegative ? "-" : "+") + NUMBER_FORMAT.format(Math.abs(dailyProfit))
                + " (" + String.format(Locale.US, "%.2f", Math.abs(dailyProfitPercent)) + "%)";
        views.setTextViewText(R.id.tvDailyProfit, profitStr);
        views.setTextColor(R.id.tvDailyProfit, profitNegative
                ? context.getResources().getColor(R.color.red)
                : context.getResources().getColor(R.color.green));
        views.setImageViewResource(R.id.ivProfitArrow, profitNegative ? R.drawable.ic_arrow_down : R.drawable.ic_arrow_up);

        // chart
        JSONArray weeklyValues = data.optJSONArray("weeklyValues");
        if (weeklyValues != null && weeklyValues.length() > 0) {
            long[] vals = new long[weeklyValues.length()];
            for (int i = 0; i < vals.length; i++) vals[i] = weeklyValues.optLong(i, 0);
            Bitmap chart = ChartRenderer.render(vals, 600, 220);
            views.setImageViewBitmap(R.id.ivChart, chart);
        }

        // asset rows (max 5 shown)
        JSONArray items = data.optJSONArray("items");
        int[] rowIds = {R.id.rowAsset1, R.id.rowAsset2, R.id.rowAsset3, R.id.rowAsset4, R.id.rowAsset5};
        int[] symbolIds = {R.id.tvSymbol1, R.id.tvSymbol2, R.id.tvSymbol3, R.id.tvSymbol4, R.id.tvSymbol5};
        int[] nameIds = {R.id.tvName1, R.id.tvName2, R.id.tvName3, R.id.tvName4, R.id.tvName5};
        int[] valueIds = {R.id.tvValue1, R.id.tvValue2, R.id.tvValue3, R.id.tvValue4, R.id.tvValue5};
        int[] percentIds = {R.id.tvPercent1, R.id.tvPercent2, R.id.tvPercent3, R.id.tvPercent4, R.id.tvPercent5};

        int count = items != null ? Math.min(items.length(), rowIds.length) : 0;
        for (int i = 0; i < rowIds.length; i++) {
            if (i < count) {
                JSONObject item = items.getJSONObject(i);
                String name = item.optString("coinName", "-");
                String symbol = item.optString("symbol", "-");
                long value = item.optLong("currentValue", 0);
                double percent = item.optDouble("profitPercent", 0);
                boolean negative = percent < 0;

                views.setViewVisibility(rowIds[i], android.view.View.VISIBLE);
                views.setTextViewText(symbolIds[i], symbol.length() > 0 ? symbol.substring(0, 1) : "?");
                views.setTextViewText(nameIds[i], name);
                views.setTextViewText(valueIds[i], NUMBER_FORMAT.format(value));
                views.setTextViewText(percentIds[i], (negative ? "" : "+") + String.format(Locale.US, "%.2f", percent) + "%");
                views.setTextColor(percentIds[i], negative
                        ? context.getResources().getColor(R.color.red)
                        : context.getResources().getColor(R.color.green));
                views.setInt(percentIds[i], "setBackgroundResource",
                        negative ? R.drawable.badge_down_bg : R.drawable.badge_up_bg);
            } else {
                views.setViewVisibility(rowIds[i], android.view.View.GONE);
            }
        }
    }
}
