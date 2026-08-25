package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

  private var webViewInstance: WebView? = null

  private val requestPermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
      // Permissions result handled
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val neededPermissions = mutableListOf<String>()
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
      neededPermissions.add(Manifest.permission.RECORD_AUDIO)
    }
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      neededPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    if (neededPermissions.isNotEmpty()) {
      requestPermissionLauncher.launch(neededPermissions.toTypedArray())
    }

    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        webViewInstance?.let { wv ->
          wv.evaluateJavascript(
            "if (typeof closeAnyOpenDrawer === 'function' && closeAnyOpenDrawer()) { true; } else { false; }",
            { result ->
              if (result == "false" || result == null) {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
              }
            }
          )
        } ?: run {
          isEnabled = false
          onBackPressedDispatcher.onBackPressed()
          isEnabled = true
        }
      }
    })

    setContent {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black)
          .systemBarsPadding()
      ) {
        JarvisWebViewContainer(
          modifier = Modifier
            .fillMaxSize()
            .testTag("jarvis_hud_webview"),
          onWebViewCreated = { webViewInstance = it }
        )
      }
    }
  }

  override fun onPause() {
    super.onPause()
    webViewInstance?.onPause()
  }

  override fun onResume() {
    super.onResume()
    webViewInstance?.onResume()
  }

  override fun onDestroy() {
    webViewInstance?.destroy()
    webViewInstance = null
    super.onDestroy()
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun JarvisWebViewContainer(
  modifier: Modifier = Modifier,
  onWebViewCreated: (WebView) -> Unit = {}
) {
  AndroidView(
    modifier = modifier,
    factory = { context ->
      WebView(context).apply {
        setBackgroundColor(0) // Transparent / Pure Black background
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        
        webViewClient = WebViewClient()
        webChromeClient = object : WebChromeClient() {
          override fun onPermissionRequest(request: PermissionRequest?) {
            request?.grant(request.resources)
          }

          override fun onGeolocationPermissionsShowPrompt(
            origin: String?,
            callback: GeolocationPermissions.Callback?
          ) {
            callback?.invoke(origin, true, false)
          }
        }

        settings.apply {
          javaScriptEnabled = true
          domStorageEnabled = true
          databaseEnabled = true
          mediaPlaybackRequiresUserGesture = false
          allowFileAccess = true
          allowContentAccess = true
          cacheMode = WebSettings.LOAD_DEFAULT
          loadWithOverviewMode = true
          useWideViewPort = true
        }

        loadUrl("file:///android_asset/index.html")
        onWebViewCreated(this)
      }
    }
  )
}


