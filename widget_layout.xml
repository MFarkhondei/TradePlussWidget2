package ir.tradeplus.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.Executors;

public class ConfigActivity extends Activity {

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private EditText etUrl, etUser, etToken;
    private TextView tvError;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);
        setResult(RESULT_CANCELED);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        etUrl = findViewById(R.id.etUrl);
        etUser = findViewById(R.id.etUser);
        etToken = findViewById(R.id.etToken);
        tvError = findViewById(R.id.tvError);
        btnSave = findViewById(R.id.btnSave);

        // if reopened for editing (not from widget-add flow), prefill existing values
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID && WidgetPrefs.isConfigured(this, appWidgetId)) {
            etUrl.setText(WidgetPrefs.getUrl(this, appWidgetId));
            etUser.setText(WidgetPrefs.getUser(this, appWidgetId));
            etToken.setText(WidgetPrefs.getToken(this, appWidgetId));
        }

        btnSave.setOnClickListener(v -> onSaveClicked());
    }

    private void onSaveClicked() {
        String url = etUrl.getText().toString().trim();
        String user = etUser.getText().toString().trim();
        String token = etToken.getText().toString().trim();

        if (url.isEmpty() || user.isEmpty() || token.isEmpty()) {
            showError("همه فیلدها الزامی هستند");
            return;
        }
        if (!url.startsWith("http")) {
            showError("آدرس Web App معتبر نیست");
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("در حال بررسی اتصال...");
        tvError.setVisibility(View.GONE);

        Handler mainHandler = new Handler(Looper.getMainLooper());
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                WidgetApi.fetch(url, user, token);
                mainHandler.post(() -> finishWithSuccess(url, user, token));
            } catch (Exception e) {
                mainHandler.post(() -> {
                    btnSave.setEnabled(true);
                    btnSave.setText(getString(R.string.btn_save));
                    showError("اتصال ناموفق بود: " + e.getMessage());
                });
            }
        });
    }

    private void finishWithSuccess(String url, String user, String token) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Toast.makeText(this, "این صفحه فقط برای تنظیم ویجت است", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        WidgetPrefs.save(this, appWidgetId, url, user, token);

        Intent updateIntent = new Intent(this, TradePlusWidgetProvider.class);
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{appWidgetId});
        sendBroadcast(updateIntent);

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }
}
