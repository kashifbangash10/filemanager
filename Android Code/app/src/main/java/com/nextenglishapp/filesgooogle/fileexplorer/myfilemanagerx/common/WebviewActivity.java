













package com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.common;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.webkit.URLUtil;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.widget.Toolbar;

import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.R;

import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.DocumentsActivity.BROWSABLE;

/**
 * This Activity is used as a fallback when there is no browser installed that supports
 * Chrome Custom Tabs
 */
public class WebviewActivity extends ActionBarActivity {
    public static final String TAG = "About";
    public static final String EXTRA_URL = "extra.url";
    public static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 4.1.1; Galaxy Nexus Build/JRO03C) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.166 Mobile Safari/535.19";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        Toolbar mToolbar = findViewById(R.id.toolbar);

        String url = getIntent().getDataString();
        WebView webView = findViewById(R.id.webview);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (URLUtil.isNetworkUrl(url)) {
                    return false;
                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    intent.addCategory(BROWSABLE);
                    startActivity(intent);
                }
                return true;
            }
        });
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.getSettings().setUserAgentString(USER_AGENT);
        if (!TextUtils.isEmpty(url)) {
            String name = url.contains("cloudrail.com") ? getString(R.string.name) : url;
            mToolbar.setTitle(name);
            webView.loadUrl(url);
        }
    }

    @Override
    public String getTag() {
        return TAG;
    }
}