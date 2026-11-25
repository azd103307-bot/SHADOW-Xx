package com.ward.quran

import android.app.Activity
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.graphics.Color
import androidx.webkit.WebViewAssetLoader

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var assetLoader: WebViewAssetLoader
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(16, 57, 47)
        window.navigationBarColor = Color.rgb(16, 57, 47)
        assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
        webView = WebView(this)
        webView.setBackgroundColor(Color.rgb(245, 242, 233))
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: android.webkit.WebResourceRequest): android.webkit.WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }
        }
        webView.webChromeClient = WebChromeClient()
        setContentView(webView)
        if (savedInstanceState == null) webView.loadUrl("https://appassets.androidplatform.net/assets/index.html")
        else webView.restoreState(savedInstanceState)
    }
    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }
    @Deprecated("Deprecated in Android")
    override fun onBackPressed() {
        if (this::webView.isInitialized && webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
