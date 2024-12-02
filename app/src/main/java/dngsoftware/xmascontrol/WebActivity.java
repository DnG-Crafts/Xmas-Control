package dngsoftware.xmascontrol;

import static android.webkit.WebSettings.LOAD_NO_CACHE;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.webkit.HttpAuthHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebActivity extends Activity {
    private WebView webView;
    private String FPPAuth = "";

    @SuppressLint({"ClickableViewAccessibility", "SetJavaScriptEnabled"})
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_view);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            final String webUrl = extras.getString("URL");
            if (extras.containsKey("AUTH"))
            {
                FPPAuth = extras.getString("AUTH");
            }
            if(webUrl != null && !webUrl.isEmpty()) {

                webView = findViewById(R.id.webView1);
                WebSettings webSettings = webView.getSettings();
                webSettings.setJavaScriptEnabled(true);
                webSettings.setDomStorageEnabled(true);
                webSettings.setAllowContentAccess(true);
                webSettings.setGeolocationEnabled(false);
                webSettings.setCacheMode(LOAD_NO_CACHE);
                webSettings.setLoadWithOverviewMode(true);
                webSettings.setUseWideViewPort(true);
                webSettings.setSupportZoom(true);
                webSettings.setBuiltInZoomControls(true);
                webSettings.setDisplayZoomControls(false);
                webView.setWebChromeClient(new WebChromeClient());
                webView.loadUrl(webUrl);
                webView.setWebViewClient(new WebViewClient() {

                    @Override
                    public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
                        if (FPPAuth.contains(":")) {
                            String[] auth = FPPAuth.split(":");
                            handler.proceed(auth[0], auth[1]);
                        }
                        super.onReceivedHttpAuthRequest(view, handler, host, realm);
                    }

                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        view.loadUrl(url);
                        return false;
                    }

                    @Override
                    public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {

                        if(event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_NEXT)
                        {
                            if(webView.canGoForward())
                            {
                                webView.goForward();
                                return true;
                            }
                        }

                        if(event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                        {
                            if(webView.canGoBack())
                            {
                                webView.goBack();
                            }
                            else
                            {
                                finish();
                            }
                            return true;
                        }

                        return super.shouldOverrideKeyEvent(view, event);
                    }

                    public void onPageFinished( WebView view, String url ) {
                        super.onPageFinished(webView, url );
                    }

                });


                webView.setOnKeyListener((v, keyCode, event) -> {
                    if(event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK)
                    {
                        if(webView.canGoBack())
                        {
                            webView.goBack();
                        }
                        else
                        {
                            finish();
                        }
                        return true;
                    }
                    return false;
                });


                webView.setOnTouchListener((v, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_DOWN && event.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
                        if(webView.canGoBack()){
                            webView.goBack();
                        }
                        else
                        {
                            finish();
                        }
                        return true;
                    }
                    return false;
                });
            }
            else
            {finish();}
        }
        else
        {finish();}
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    public void onBackPressed() {
        if (webView != null) {
            if (webView.canGoBack()) {
                webView.goBack();
            } else {
                finish();
            }
        }
    }
}