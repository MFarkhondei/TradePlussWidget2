package ir.tradeplus.widget;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Fetches widget data from the TradePlus Apps Script Web App. */
public class WidgetApi {

    public static class ApiException extends Exception {
        public ApiException(String message) { super(message); }
    }

    public static JSONObject fetch(String baseUrl, String username, String token) throws Exception {
        String sep = baseUrl.contains("?") ? "&" : "?";
        String urlStr = baseUrl + sep + "action=widget"
                + "&user=" + URLEncoder.encode(username, "UTF-8")
                + "&token=" + URLEncoder.encode(token, "UTF-8");

        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setInstanceFollowRedirects(true);

            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            if (is == null) throw new ApiException("پاسخی از سرور دریافت نشد");

            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            JSONObject json = new JSONObject(sb.toString());
            if (!json.optBoolean("success", false)) {
                throw new ApiException(json.optString("message", "خطای نامشخص"));
            }
            return json;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
