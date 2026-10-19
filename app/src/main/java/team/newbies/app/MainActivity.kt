package team.newbies.app

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import team.newbies.core.model.UrlRoute
import team.newbies.core.router.UrlRouter

class MainActivity : AppCompatActivity() {

    companion object {
        private const val SPLASH_TIMEOUT_MS = 4500L
    }

    private lateinit var rootLayout: View
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var webView: WebView
    private lateinit var splashOverlay: FrameLayout
    private lateinit var splashGlow: View
    private lateinit var splashLogo: ImageView

    private var splashAnimatorSet: AnimatorSet? = null
    private var isSplashDismissed = false
    private val splashHandler = Handler(Looper.getMainLooper())
    private val splashTimeoutRunnable = Runnable { dismissSplash() }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable Material 3 Edge-to-Edge display
        setupEdgeToEdge()

        setContentView(R.layout.activity_main)

        initViews()
        applyWindowInsets()
        startSplashAnimation()
        setupWebView()
        setupSwipeRefresh()
        setupBackNavigation()
        requestNotificationPermissionIfNeeded()
 
        val initialUrl = resolveTargetUrl(intent)
        loadWebPage(initialUrl)

        // Safety fallback timer so splash always dismisses even if connection is slow
        splashHandler.postDelayed(splashTimeoutRunnable, SPLASH_TIMEOUT_MS)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val targetUrl = resolveTargetUrl(intent)
        if (targetUrl.isNotEmpty()) {
            loadWebPage(targetUrl)
        }
    }

    private fun resolveTargetUrl(intent: Intent?): String {
        val targetUrl = intent?.getStringExtra("target_url")
        if (!targetUrl.isNullOrEmpty()) return targetUrl

        val data = intent?.data
        if (data != null) {
            val scheme = data.scheme?.lowercase()
            if (scheme == "newbiesteam") {
                val host = data.host ?: ""
                val path = data.path ?: ""
                val route = (host + path).trimStart('/')
                val hash = if (route.isNotEmpty()) {
                    if (route.startsWith("#")) route else "#$route"
                } else ""
                return "${UrlRouter.DEFAULT_HUB_URL}/$hash"
            }
            return data.toString()
        }
        return UrlRouter.DEFAULT_HUB_URL
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
    }

    private fun initViews() {
        rootLayout = findViewById(R.id.mainRootLayout)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        webView = findViewById(R.id.webView)
        splashOverlay = findViewById(R.id.splashOverlay)
        splashGlow = findViewById(R.id.splashGlow)
        splashLogo = findViewById(R.id.splashLogo)
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply padding to root so content fits below status bar and above navigation bar
            rootLayout.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun startSplashAnimation() {
        val scaleX = ObjectAnimator.ofFloat(splashLogo, View.SCALE_X, 0.92f, 1.06f).apply {
            duration = 1100
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        val scaleY = ObjectAnimator.ofFloat(splashLogo, View.SCALE_Y, 0.92f, 1.06f).apply {
            duration = 1100
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        val glowAlpha = ObjectAnimator.ofFloat(splashGlow, View.ALPHA, 0.35f, 0.85f).apply {
            duration = 1300
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }

        splashAnimatorSet = AnimatorSet().apply {
            playTogether(scaleX, scaleY, glowAlpha)
            start()
        }
    }

    private fun dismissSplash() {
        if (isSplashDismissed) return
        isSplashDismissed = true
        splashHandler.removeCallbacks(splashTimeoutRunnable)

        splashOverlay.animate()
            .alpha(0f)
            .setDuration(420)
            .withEndAction {
                splashAnimatorSet?.cancel()
                splashOverlay.visibility = View.GONE
            }
            .start()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        // Hardware acceleration
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView.overScrollMode = View.OVER_SCROLL_NEVER
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false

        // Cookie Manager
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.userAgentString = "${settings.userAgentString} NewbiesTeamAndroid/1.2.1"

        applyCacheMode()

        webView.webChromeClient = WebChromeClient()

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                swipeRefresh.isRefreshing = false
                dismissSplash()
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    swipeRefresh.isRefreshing = false
                    dismissSplash()
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                return handleUrlNavigation(url)
            }

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url == null) return false
                return handleUrlNavigation(url)
            }
        }

        webView.setDownloadListener { url, _, _, _, _ ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Cannot open download link", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleUrlNavigation(url: String): Boolean {
        // Delegate routing logic to KMP core router
        return when (val route = UrlRouter.resolveRoute(url)) {
            is UrlRoute.InternalWeb -> {
                // Stay inside embedded WebView
                false
            }
            is UrlRoute.DirectDownload -> {
                openExternal(Uri.parse(route.url))
                true
            }
            is UrlRoute.ExternalBrowser -> {
                openExternal(Uri.parse(route.url))
                true
            }
            is UrlRoute.SpecialIntent -> {
                handleSpecialScheme(route.uriString, route.scheme)
                true
            }
        }
    }

    private fun handleSpecialScheme(uriString: String, scheme: String) {
        if (scheme == "intent") {
            try {
                val parsedIntent = Intent.parseUri(uriString, Intent.URI_INTENT_SCHEME)
                if (parsedIntent != null) {
                    val packageManager = packageManager
                    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        packageManager.resolveActivity(
                            parsedIntent,
                            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        packageManager.resolveActivity(parsedIntent, PackageManager.MATCH_DEFAULT_ONLY)
                    }
                    if (info != null) {
                        startActivity(parsedIntent)
                        return
                    }
                    val fallbackUrl = parsedIntent.getStringExtra("browser_fallback_url")
                    if (!fallbackUrl.isNullOrEmpty()) {
                        handleUrlNavigation(fallbackUrl)
                        return
                    }
                }
            } catch (_: Exception) {}
            return
        }

        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uriString)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "No app available to handle this link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openExternal(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No browser found to open link", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to open link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(
            R.color.m3_primary,
            R.color.accent_crimson,
            R.color.text_ink
        )
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.black_card)

        swipeRefresh.setOnRefreshListener {
            applyCacheMode()
            webView.reload()
        }

        // Prevent pull-to-refresh while scrolled down inside the page
        swipeRefresh.setOnChildScrollUpCallback { _, _ ->
            webView.scrollY > 0
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun applyCacheMode() {
        if (isNetworkConnected()) {
            webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
        } else {
            webView.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        }
    }

    private fun isNetworkConnected(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities =
            connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun loadWebPage(url: String) {
        applyCacheMode()
        webView.loadUrl(url)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permission != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onDestroy() {
        splashHandler.removeCallbacks(splashTimeoutRunnable)
        splashAnimatorSet?.cancel()
        webView.apply {
            stopLoading()
            loadUrl("about:blank")
            clearHistory()
            removeAllViews()
            destroy()
        }
        super.onDestroy()
    }
}
