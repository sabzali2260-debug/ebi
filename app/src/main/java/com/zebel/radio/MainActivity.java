package com.zebel.radio;
import android.Manifest;
import android.app.Activity;
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
    private WebView webView;
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
        s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false); s.setSupportMultipleWindows(false);
        s.setSupportZoom(false); s.setBuiltInZoomControls(false); s.setDisplayZoomControls(false);
        s.setAllowFileAccess(false); s.setAllowContentAccess(false);
        if (Build.VERSION.SDK_INT >= 21) s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webView.addJavascriptInterface(new ZebelBridge(), "ZebelNative");
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) { return loadAsset(r.getUrl()); }
            @Override public void onPageFinished(WebView v, String url) { v.evaluateJavascript("document.documentElement.classList.add('android-native-app')", null); }
        });
        root.addView(webView, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        loadPage();
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
    }
    private void enterImmersive() {
        Window w=getWindow(); w.setStatusBarColor(BG); w.setNavigationBarColor(Color.BLACK);
        if(Build.VERSION.SDK_INT>=29){w.setStatusBarContrastEnforced(false);w.setNavigationBarContrastEnforced(false);}
        if(Build.VERSION.SDK_INT>=28)w.getAttributes().layoutInDisplayCutoutMode=android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        int f=View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        w.getDecorView().setSystemUiVisibility(f);
    }
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus)enterImmersive();}
    private void loadPage(){
        try(InputStream in=getAssets().open("index.html");ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buf=new byte[16384];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);
            webView.loadDataWithBaseURL("https://appassets.androidplatform.net/",out.toString("UTF-8"),"text/html","UTF-8",null);
        }catch(IOException e){
            String fail="<html><body style='background:#07090c;color:#ffad4b;font:16px sans-serif;padding:24px'><h2>ZEBEL RADIO</h2><p>خطا در بارگذاری فایل داخلی برنامه</p></body></html>";
            webView.loadDataWithBaseURL("https://appassets.androidplatform.net/",fail,"text/html","UTF-8",null);
        }
    }
    private WebResourceResponse loadAsset(Uri uri){
        if(uri==null||!"appassets.androidplatform.net".equals(uri.getHost()))return null;
        String path=uri.getPath();if(path==null||path.equals("/")||path.contains(".."))return null;
        String asset=path.startsWith("/")?path.substring(1):path;
        try{
            InputStream stream=getAssets().open(asset);String mime=mimeType(asset);
            String enc=(mime.startsWith("text/")||mime.contains("json")||mime.contains("javascript"))?"UTF-8":null;
            Map<String,String> h=new HashMap<>();h.put("Cache-Control","public, max-age=86400");
            return new WebResourceResponse(mime,enc,200,"OK",h,stream);
        }catch(IOException ignored){return null;}
    }
    private static String mimeType(String p){
        String x=p.toLowerCase(Locale.ROOT);
        if(x.endsWith(".png"))return "image/png";if(x.endsWith(".jpg")||x.endsWith(".jpeg"))return "image/jpeg";
        if(x.endsWith(".svg"))return "image/svg+xml";if(x.endsWith(".webmanifest"))return "application/manifest+json";
        if(x.endsWith(".json"))return "application/json";if(x.endsWith(".js"))return "application/javascript";
        if(x.endsWith(".css"))return "text/css";if(x.endsWith(".html"))return "text/html";return "application/octet-stream";
    }
    public final class ZebelBridge{
        @JavascriptInterface public void onPlaybackStarted(String station){ZebelRadioService.start(MainActivity.this,station==null?"ZEBEL RADIO":station);}
        @JavascriptInterface public void onPlaybackPaused(){ZebelRadioService.stop(MainActivity.this);}
    }
    @Override public void onBackPressed(){if(webView!=null&&webView.canGoBack())webView.goBack();else super.onBackPressed();enterImmersive();}
    @Override protected void onResume(){super.onResume();enterImmersive();if(webView!=null){webView.onResume();webView.resumeTimers();}}
    @Override protected void onPause(){if(webView!=null)webView.onPause();super.onPause();}
    @Override protected void onDestroy(){if(webView!=null){webView.removeJavascriptInterface("ZebelNative");webView.stopLoading();webView.setWebChromeClient(null);webView.setWebViewClient(null);webView.destroy();webView=null;}super.onDestroy();}
}
