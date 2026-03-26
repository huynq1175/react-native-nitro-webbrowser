package com.margelo.nitro.nitrowebbrowser

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.core.view.WindowCompat

class ChromeTabsManagerActivity : Activity() {

  companion object {
    private const val KEY_BROWSER_INTENT = "browserIntent"
    private const val KEY_ANIMATION_BUNDLE = "animationBundle"
    private const val BROWSER_RESULT_TYPE = "browserResultType"
    private const val DEFAULT_RESULT_TYPE = "dismiss"

    fun createStartIntent(context: Context, authIntent: Intent, animationBundle: Bundle? = null): Intent {
      val intent = Intent(context, ChromeTabsManagerActivity::class.java)
      intent.putExtra(KEY_BROWSER_INTENT, authIntent)
      animationBundle?.let { intent.putExtra(KEY_ANIMATION_BUNDLE, it) }
      return intent
    }

    fun createDismissIntent(context: Context): Intent {
      val intent = Intent(context, ChromeTabsManagerActivity::class.java)
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
      return intent
    }
  }

  private var opened = false
  private var resultType: String? = null
  private var isError = false

  override fun onCreate(savedInstanceState: Bundle?) {
    try {
      super.onCreate(savedInstanceState)

      @Suppress("DEPRECATION")
      overridePendingTransition(0, 0)

      // Edge-to-edge: transparent system bars
      WindowCompat.setDecorFitsSystemWindows(window, false)
      window.statusBarColor = Color.TRANSPARENT
      window.navigationBarColor = Color.TRANSPARENT

      if (intent.hasExtra(KEY_BROWSER_INTENT) &&
        (savedInstanceState == null || savedInstanceState.getString(BROWSER_RESULT_TYPE) == null)
      ) {
        @Suppress("DEPRECATION")
        val browserIntent = intent.getParcelableExtra<Intent>(KEY_BROWSER_INTENT)
        @Suppress("DEPRECATION")
        val animBundle = intent.getBundleExtra(KEY_ANIMATION_BUNDLE)
        browserIntent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        browserIntent?.let { startActivity(it, animBundle) }
        resultType = DEFAULT_RESULT_TYPE
      } else {
        finish()
      }
    } catch (e: Exception) {
      isError = true
      NitroWebbrowser.onBrowserDismissed?.invoke(
        resultType ?: DEFAULT_RESULT_TYPE,
        "Unable to open url.",
        true
      )
      finish()
      e.printStackTrace()
    }
  }

  override fun onResume() {
    super.onResume()
    if (!opened) {
      opened = true
    } else {
      resultType = "cancel"
      finish()
      @Suppress("DEPRECATION")
      overridePendingTransition(0, 0)
    }
  }

  override fun onDestroy() {
    resultType?.let { type ->
      when (type) {
        "cancel" -> NitroWebbrowser.onBrowserDismissed?.invoke(
          "cancel",
          "chrome tabs activity closed",
          isError
        )

        else -> NitroWebbrowser.onBrowserDismissed?.invoke(
          DEFAULT_RESULT_TYPE,
          "chrome tabs activity destroyed",
          isError
        )
      }
      resultType = null
    }
    super.onDestroy()
  }

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    setIntent(intent)
  }

  override fun onRestoreInstanceState(savedInstanceState: Bundle) {
    super.onRestoreInstanceState(savedInstanceState)
    resultType = savedInstanceState.getString(BROWSER_RESULT_TYPE)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    outState.putString(BROWSER_RESULT_TYPE, DEFAULT_RESULT_TYPE)
    super.onSaveInstanceState(outState)
  }
}
