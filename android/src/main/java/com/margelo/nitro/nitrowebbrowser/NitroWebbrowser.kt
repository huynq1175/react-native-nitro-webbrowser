package com.margelo.nitro.nitrowebbrowser

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Browser
import android.text.TextUtils
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsService
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.core.graphics.ColorUtils
import com.facebook.proguard.annotations.DoNotStrip
import com.facebook.react.bridge.ReactApplicationContext
import com.margelo.nitro.NitroModules
import com.margelo.nitro.core.Promise
import java.util.regex.Pattern
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@DoNotStrip
class NitroWebbrowser : HybridNitroWebbrowserSpec() {

  companion object {
    private const val CHROME_PACKAGE_STABLE = "com.android.chrome"
    private const val CHROME_PACKAGE_BETA = "com.chrome.beta"
    private const val CHROME_PACKAGE_DEV = "com.chrome.dev"
    private const val LOCAL_PACKAGE = "com.google.android.apps.chrome"
    private const val ACTION_CUSTOM_TABS_CONNECTION =
      "android.support.customtabs.action.CustomTabsService"

    private val animationIdentifierPattern = Pattern.compile("^.+:.+/")

    var onBrowserDismissed: ((type: String, message: String?, isError: Boolean) -> Unit)? = null
  }

  private var openBrowserResolve: ((BrowserResult) -> Unit)? = null
  private var isLightTheme: Boolean = false
  private var customTabsClient: CustomTabsClient? = null

  private val appContext: Context
    get() = NitroModules.applicationContext
      ?: throw IllegalStateException("No application context")

  private val currentActivity: Activity?
    get() {
      val ctx = appContext
      if (ctx is ReactApplicationContext) {
        return ctx.currentActivity
      }
      return null
    }

  override fun open(url: String, options: InAppBrowserOptions): Promise<BrowserResult> {
    return Promise.async {
      suspendCoroutine { continuation ->
        val activity = currentActivity
        if (activity == null) {
          continuation.resume(BrowserResult(type = "cancel", message = "No activity", url = null))
          return@suspendCoroutine
        }

        if (openBrowserResolve != null) {
          openBrowserResolve?.invoke(BrowserResult(type = "cancel", message = null, url = null))
          openBrowserResolve = null
        }

        openBrowserResolve = { result ->
          continuation.resume(result)
        }

        val builder = CustomTabsIntent.Builder()
        isLightTheme = false

        val toolbarColor = parseAndSetColor(builder, options.toolbarColor, "setToolbarColor")
        if (toolbarColor != null) {
          isLightTheme = ColorUtils.calculateLuminance(toolbarColor) > 0.5
        }
        parseAndSetColor(builder, options.secondaryToolbarColor, "setSecondaryToolbarColor")
        parseAndSetColor(builder, options.navigationBarColor, "setNavigationBarColor")
        parseAndSetColor(
          builder,
          options.navigationBarDividerColor,
          "setNavigationBarDividerColor"
        )

        if (options.enableDefaultShare == true) {
          builder.addDefaultShareMenuItem()
        }

        options.animations?.let { animations ->
          applyAnimation(appContext, builder, animations)
        }

        if (options.hasBackButton == true) {
          try {
            val iconRes = if (isLightTheme) {
              R.drawable.ic_arrow_back_black
            } else {
              R.drawable.ic_arrow_back_white
            }
            builder.setCloseButtonIcon(
              BitmapFactory.decodeResource(appContext.resources, iconRes)
            )
          } catch (_: Exception) {
          }
        }

        val customTabsIntent = builder.build()
        val intent = customTabsIntent.intent

        options.headers?.let { headers ->
          val bundle = Bundle()
          for ((key, value) in headers) {
            bundle.putString(key, value)
          }
          intent.putExtra(Browser.EXTRA_HEADERS, bundle)
        }

        if (options.forceCloseOnRedirection == true) {
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (options.showInRecents != true) {
          intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
          intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        }

        intent.putExtra(
          CustomTabsIntent.EXTRA_ENABLE_URLBAR_HIDING,
          options.enableUrlBarHiding == true
        )

        try {
          val browserPackage = options.browserPackage
          if (!browserPackage.isNullOrEmpty()) {
            intent.setPackage(browserPackage)
          } else {
            getDefaultBrowser(appContext)?.let { intent.setPackage(it) }
          }
        } catch (_: Exception) {
        }

        if (options.showTitle == true) {
          builder.setShowTitle(true)
        } else {
          intent.putExtra(
            CustomTabsIntent.EXTRA_TITLE_VISIBILITY_STATE,
            CustomTabsIntent.NO_TITLE
          )
        }

        if (options.includeReferrer == true) {
          intent.putExtra(
            Intent.EXTRA_REFERRER,
            Uri.parse("android-app://" + appContext.applicationContext.packageName)
          )
        }

        intent.data = Uri.parse(url)

        onBrowserDismissed = { type, message, isError ->
          onBrowserDismissed = null
          if (isError) {
            openBrowserResolve?.invoke(
              BrowserResult(type = "cancel", message = message ?: "Browser error", url = null)
            )
          } else {
            openBrowserResolve?.invoke(
              BrowserResult(type = type, message = message, url = null)
            )
          }
          openBrowserResolve = null
        }

        activity.startActivity(
          ChromeTabsManagerActivity.createStartIntent(activity, intent),
          customTabsIntent.startAnimationBundle
        )
      }
    }
  }

  override fun close() {
    val activity = currentActivity ?: return
    if (openBrowserResolve == null) return

    onBrowserDismissed = null
    openBrowserResolve?.invoke(BrowserResult(type = "dismiss", message = null, url = null))
    openBrowserResolve = null

    activity.startActivity(ChromeTabsManagerActivity.createDismissIntent(activity))
  }

  override fun openAuth(
    url: String,
    redirectUrl: String,
    options: InAppBrowserOptions
  ): Promise<BrowserResult> {
    val useBottomSheet = options.bottomSheet == true
    if (!useBottomSheet) {
      return open(url, options)
    }

    return Promise.async {
      suspendCoroutine { continuation ->
        val activity = currentActivity
        if (activity == null) {
          continuation.resume(BrowserResult(type = "cancel", message = "No activity", url = null))
          return@suspendCoroutine
        }

        if (openBrowserResolve != null) {
          openBrowserResolve?.invoke(BrowserResult(type = "cancel", message = null, url = null))
          openBrowserResolve = null
        }

        openBrowserResolve = { result ->
          continuation.resume(result)
        }

        val packageName = options.browserPackage?.takeIf { it.isNotEmpty() }
          ?: getDefaultBrowser(appContext)

        if (packageName == null) {
          // Fallback to regular open if no browser supports Custom Tabs
          openBrowserResolve = null
          continuation.resume(BrowserResult(type = "cancel", message = "No browser available", url = null))
          return@suspendCoroutine
        }

        val appCtx = appContext
        val connection = object : CustomTabsServiceConnection() {
          override fun onCustomTabsServiceConnected(
            name: ComponentName,
            client: CustomTabsClient
          ) {
            try {
              val session = client.newSession(CustomTabsCallback())
              if (session == null) {
                appCtx.unbindService(this)
                openBrowserResolve = null
                continuation.resume(BrowserResult(type = "cancel", message = "Failed to create session", url = null))
                return
              }

              val builder = CustomTabsIntent.Builder(session)
              isLightTheme = false

              // Apply color options
              val toolbarColor = parseAndSetColor(builder, options.toolbarColor, "setToolbarColor")
              if (toolbarColor != null) {
                isLightTheme = ColorUtils.calculateLuminance(toolbarColor) > 0.5
              }
              parseAndSetColor(builder, options.secondaryToolbarColor, "setSecondaryToolbarColor")
              parseAndSetColor(builder, options.navigationBarColor, "setNavigationBarColor")
              parseAndSetColor(builder, options.navigationBarDividerColor, "setNavigationBarDividerColor")

              // Calculate bottom sheet height
              val displayMetrics = activity.resources.displayMetrics
              val screenHeightPx = displayMetrics.heightPixels
              val ratio = options.bottomSheetHeightRatio ?: 0.7
              val heightPx = (screenHeightPx * ratio).toInt()
              builder.setInitialActivityHeightPx(heightPx, CustomTabsIntent.ACTIVITY_HEIGHT_ADJUSTABLE)

              // Disable share and minimize toolbar clutter for auth flow
              builder.setShareState(CustomTabsIntent.SHARE_STATE_OFF)
              builder.setBookmarksButtonEnabled(false)
              builder.setDownloadButtonEnabled(false)
              builder.setShowTitle(options.showTitle == true)

              // Set back button
              if (options.hasBackButton != false) {
                try {
                  val iconRes = if (isLightTheme) {
                    R.drawable.ic_arrow_back_black
                  } else {
                    R.drawable.ic_arrow_back_white
                  }
                  builder.setCloseButtonIcon(
                    BitmapFactory.decodeResource(appContext.resources, iconRes)
                  )
                } catch (_: Exception) {
                }
              }

              val customTabsIntent = builder.build()
              val intent = customTabsIntent.intent

              options.headers?.let { headers ->
                val bundle = Bundle()
                for ((key, value) in headers) {
                  bundle.putString(key, value)
                }
                intent.putExtra(Browser.EXTRA_HEADERS, bundle)
              }

              if (options.showInRecents != true) {
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
              }

              intent.setPackage(packageName)
              intent.data = Uri.parse(url)

              onBrowserDismissed = { type, message, isError ->
                onBrowserDismissed = null
                if (isError) {
                  openBrowserResolve?.invoke(
                    BrowserResult(type = "cancel", message = message ?: "Browser error", url = null)
                  )
                } else {
                  openBrowserResolve?.invoke(
                    BrowserResult(type = type, message = message, url = null)
                  )
                }
                openBrowserResolve = null
              }

              activity.startActivity(
                ChromeTabsManagerActivity.createStartIntent(activity, intent),
                customTabsIntent.startAnimationBundle
              )

              // Unbind service after launching to avoid leak
              appCtx.unbindService(this)
            } catch (e: Exception) {
              try { appCtx.unbindService(this) } catch (_: Exception) {}
              openBrowserResolve = null
              continuation.resume(BrowserResult(type = "cancel", message = e.message ?: "Failed to open browser", url = null))
            }
          }

          override fun onServiceDisconnected(name: ComponentName?) {
            // Service disconnected, no action needed
          }
        }

        try {
          CustomTabsClient.bindCustomTabsService(appCtx, packageName, connection)
        } catch (e: Exception) {
          openBrowserResolve = null
          continuation.resume(BrowserResult(type = "cancel", message = e.message ?: "Failed to bind service", url = null))
        }
      }
    }
  }

  override fun closeAuth() {
    close()
  }

  override fun isAvailable(): Promise<Boolean> {
    return Promise.async {
      val resolveInfos = getPreferredPackages(appContext)
      resolveInfos != null && resolveInfos.isNotEmpty()
    }
  }

  override fun warmup(): Promise<Boolean> {
    return Promise.async {
      suspendCoroutine { continuation ->
        val applicationContext = appContext.applicationContext
        val packageName = getDefaultBrowser(applicationContext)
        if (packageName == null) {
          continuation.resume(false)
          return@suspendCoroutine
        }

        val connection = object : CustomTabsServiceConnection() {
          override fun onCustomTabsServiceConnected(
            name: ComponentName,
            client: CustomTabsClient
          ) {
            customTabsClient = client
            val result = client.warmup(0L)
            applicationContext.unbindService(this)
            continuation.resume(result)
          }

          override fun onServiceDisconnected(name: ComponentName?) {
            customTabsClient = null
          }
        }

        try {
          CustomTabsClient.bindCustomTabsService(applicationContext, packageName, connection)
        } catch (_: Exception) {
          continuation.resume(false)
        }
      }
    }
  }

  override fun mayLaunchUrl(mostLikelyUrl: String, otherUrls: Array<String>) {
    val client = customTabsClient ?: return
    val session = client.newSession(CustomTabsCallback()) ?: return

    val otherUrlBundles = ArrayList<Bundle>()
    for (link in otherUrls) {
      if (link.isNotEmpty()) {
        val bundle = Bundle()
        bundle.putParcelable(CustomTabsService.KEY_URL, Uri.parse(link))
        otherUrlBundles.add(bundle)
      }
    }

    session.mayLaunchUrl(Uri.parse(mostLikelyUrl), null, otherUrlBundles)
  }

  // Private helpers

  private fun parseAndSetColor(
    builder: CustomTabsIntent.Builder,
    colorString: String?,
    methodName: String
  ): Int? {
    if (colorString.isNullOrEmpty()) return null
    return try {
      val color = Color.parseColor(colorString)
      val method =
        builder.javaClass.getDeclaredMethod(methodName, Int::class.javaPrimitiveType)
      method.invoke(builder, color)
      color
    } catch (_: Exception) {
      null
    }
  }

  private fun applyAnimation(
    context: Context,
    builder: CustomTabsIntent.Builder,
    animations: BrowserAnimations
  ) {
    val startEnter = resolveAnimationIdentifier(context, animations.startEnter)
    val startExit = resolveAnimationIdentifier(context, animations.startExit)
    val endEnter = resolveAnimationIdentifier(context, animations.endEnter)
    val endExit = resolveAnimationIdentifier(context, animations.endExit)

    if (startEnter != -1 && startExit != -1) {
      builder.setStartAnimations(context, startEnter, startExit)
    }
    if (endEnter != -1 && endExit != -1) {
      builder.setExitAnimations(context, endEnter, endExit)
    }
  }

  private fun resolveAnimationIdentifier(context: Context, identifier: String): Int {
    return if (animationIdentifierPattern.matcher(identifier).find()) {
      context.resources.getIdentifier(identifier, null, null)
    } else {
      context.resources.getIdentifier(identifier, "anim", context.packageName)
    }
  }

  private fun getPreferredPackages(context: Context): List<android.content.pm.ResolveInfo>? {
    val serviceIntent = Intent(ACTION_CUSTOM_TABS_CONNECTION)
    return context.packageManager.queryIntentServices(serviceIntent, 0)
  }

  private fun getDefaultBrowser(context: Context): String? {
    val resolveInfos = getPreferredPackages(context)
    var packageName = CustomTabsClient.getPackageName(
      context,
      listOf(CHROME_PACKAGE_STABLE, CHROME_PACKAGE_BETA, CHROME_PACKAGE_DEV, LOCAL_PACKAGE)
    )
    if (packageName == null && resolveInfos != null && resolveInfos.isNotEmpty()) {
      packageName = resolveInfos[0].serviceInfo.packageName
    }
    return packageName
  }
}
