package com.zebel.radio;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(7, 9, 12);
    private static final int REQUEST_NOTIFICATIONS = 4108;
    private static final int REQUEST_BACKGROUND_IMAGE = 4109;
    private WebView webView;
    private android.webkit.ValueCallback<Uri[]> backgroundFileCallback;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enterImmersive();
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(BG);
        webView = new WebView(this);
        webView.setBackgroundColor(BG);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setSupportMultipleWindows(false);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setUserAgentString(s.getUserAgentString() + " ZEBELRadio/1.3");
        if (Build.VERSION.SDK_INT >= 21) {
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        webView.addJavascriptInterface(new ZebelBridge(), "ZebelNative");
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(
                    WebView view, android.webkit.ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (backgroundFileCallback != null) {
                    backgroundFileCallback.onReceiveValue(null);
                }
                backgroundFileCallback = callback;
                try {
                    Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    pick.addCategory(Intent.CATEGORY_OPENABLE);
                    pick.setType("image/*");
                    pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivityForResult(
                            Intent.createChooser(pick, "انتخاب تصویر پس‌زمینه"),
                            REQUEST_BACKGROUND_IMAGE);
                    return true;
                } catch (Exception e) {
                    backgroundFileCallback = null;
                    callback.onReceiveValue(null);
                    return false;
                }
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                return loadPackagedAsset(r.getUrl());
            }
            @Override public void onPageFinished(WebView v, String url) {
                v.evaluateJavascript("document.documentElement.classList.add('android-native-app')", null);
            }
        });

        root.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);
        loadPage();
        requestNotificationPermission();
    }

    private void enterImmersive() {
        Window w = getWindow();
        w.setStatusBarColor(BG);
        w.setNavigationBarColor(Color.BLACK);
        if (Build.VERSION.SDK_INT >= 29) {
            w.setStatusBarContrastEnforced(false);
            w.setNavigationBarContrastEnforced(false);
        }
        if (Build.VERSION.SDK_INT >= 28) {
            w.getAttributes().layoutInDisplayCutoutMode =
                    android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        w.getDecorView().setSystemUiVisibility(flags);
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) enterImmersive();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        }
    }

    private void loadPage() {
        try (InputStream in = getAssets().open("index.html");
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[16384];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            webView.loadDataWithBaseURL("https://appassets.androidplatform.net/",
                    out.toString("UTF-8"), "text/html", "UTF-8", null);
        } catch (IOException e) {
            String fail = "<html><body style='background:#07090c;color:#ffad4b;font:16px sans-serif;padding:24px'><h2>ZEBEL RADIO</h2><p>خطا در بارگذاری فایل داخلی برنامه</p></body></html>";
            webView.loadDataWithBaseURL("https://appassets.androidplatform.net/", fail, "text/html", "UTF-8", null);
        }
    }

    private WebResourceResponse loadPackagedAsset(Uri uri) {
        if (uri == null || !"appassets.androidplatform.net".equals(uri.getHost())) return null;
        String path = uri.getPath();
        if (path == null || path.equals("/") || path.contains("..")) return null;
        String asset = path.startsWith("/") ? path.substring(1) : path;
        try {
            InputStream stream = getAssets().open(asset);
            String mime = mimeType(asset);
            String encoding = (mime.startsWith("text/") || mime.contains("json") || mime.contains("javascript")) ? "UTF-8" : null;
            Map<String, String> headers = new HashMap<>();
            headers.put("Cache-Control", "public, max-age=86400");
            return new WebResourceResponse(mime, encoding, 200, "OK", headers, stream);
        } catch (IOException ignored) {
            return null;
        }
    }

    private static String mimeType(String path) {
        String p = path.toLowerCase(Locale.ROOT);
        if (p.endsWith(".png")) return "image/png";
        if (p.endsWith(".jpg") || p.endsWith(".jpeg")) return "image/jpeg";
        if (p.endsWith(".svg")) return "image/svg+xml";
        if (p.endsWith(".webmanifest")) return "application/manifest+json";
        if (p.endsWith(".json")) return "application/json";
        if (p.endsWith(".js")) return "application/javascript";
        if (p.endsWith(".css")) return "text/css";
        if (p.endsWith(".html")) return "text/html";
        return "application/octet-stream";
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_BACKGROUND_IMAGE && backgroundFileCallback != null) {
            Uri[] result = null;
            if (resultCode == Activity.RESULT_OK && data != null) {
                try {
                    result = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
                } catch (Exception ignored) {
                    if (data.getData() != null) result = new Uri[]{data.getData()};
                }
            }
            backgroundFileCallback.onReceiveValue(result);
            backgroundFileCallback = null;
        }
        enterImmersive();
    }

    public final class ZebelBridge {
        @JavascriptInterface public void onPlaybackStarted(String station) {
            ZebelRadioService.start(MainActivity.this, station == null ? "ZEBEL RADIO" : station);
        }
        @JavascriptInterface public void onPlaybackPaused() {
            ZebelRadioService.stop(MainActivity.this);
        }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
        enterImmersive();
    }

    @Override protected void onResume() {
        super.onResume();
        enterImmersive();
        if (webView != null) { webView.onResume(); webView.resumeTimers(); }
    }

    @Override protected void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override protected void onDestroy() {
        if (backgroundFileCallback != null) {
            backgroundFileCallback.onReceiveValue(null);
            backgroundFileCallback = null;
        }
        if (webView != null) {
            webView.removeJavascriptInterface("ZebelNative");
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
