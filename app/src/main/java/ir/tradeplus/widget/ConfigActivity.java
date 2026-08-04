package ir.tradeplus.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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

        // NOTE: we deliberately do NOT test the network connection here before finishing.
        // Widget-configuration activities must return quickly (some launchers, incl. Samsung
        // One UI, cancel the "add widget" flow with a generic "Couldn't add widget" error if
        // the configure Activity takes more than a few seconds) and Apps Script Web Apps can
        // have a slow cold-start response (several seconds). The actual fetch — with a longer
        // timeout — happens afterwards in the widget provider itself, which can retry safely.
        finishWithSuccess(url, user, token);
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
