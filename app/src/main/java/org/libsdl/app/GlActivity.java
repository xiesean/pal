package org.libsdl.app;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.sean.pal95.R;

public class GlActivity extends AppCompatActivity {
    TextView textView;
    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gl);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.inflateMenu(R.menu.gl);
        toolbar.setOnMenuItemClickListener(item -> GlActivity.this.onOptionsItemSelected(item));

        textView = findViewById(R.id.gongl);
        webView = findViewById(R.id.gongl_web);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        //设置不用系统浏览器打开,直接显示在当前Webview
        webView.setWebViewClient(new WebViewClient());
        WebSettings settings = webView.getSettings();
        settings.setUserAgentString("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.134 Safari/537.36");
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(true);
        settings.setJavaScriptEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setGeolocationEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true); // 允许访问文件
        settings.setPluginState(WebSettings.PluginState.ON);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE); // 不加载缓存内容
        settings.setBlockNetworkImage(false);//解决图片不显示
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        showGl(false);
    }

    private void showGl(boolean isVideo) {
        textView.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        if (isVideo) {
            startActivity(new Intent(GlActivity.this, GlVideoActivity.class));
        } else {
            webView.loadUrl("https://hsoftxl.github.io/gl/xjgl/p1.htm");
            webView.loadUrl("javascript:window.location.reload( true )");
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gl, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.toolbar_pic:
                showGl(false);
                break;
            case R.id.toolbar_video:
                showGl(true);
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}