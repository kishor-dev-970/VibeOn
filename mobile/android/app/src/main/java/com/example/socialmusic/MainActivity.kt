package com.example.socialmusic

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.graphics.Color
import android.util.Rational
import java.lang.ref.WeakReference

import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.ReactRootView
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.fabricEnabled
import com.facebook.react.defaults.DefaultReactActivityDelegate

import expo.modules.ReactActivityDelegateWrapper

class MainActivity : ReactActivity() {

    companion object {
        @Volatile
        private var activeInstance: WeakReference<MainActivity>? = null

        fun enterPipGlobal(): Boolean {
            val act = activeInstance?.get() ?: return false
            return act.enterPipNow()
        }

        fun setPipAutoEnterGlobal(enabled: Boolean) {
            val act = activeInstance?.get() ?: return
            act.setPipAutoEnter(enabled)
        }
    }

    override fun onBackPressed() {
        if (BraveliteWebView.activeFullscreen?.exitFullscreen() == true) {
            return
        }
        super.onBackPressed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Set the theme to AppTheme BEFORE onCreate to support
        // coloring the background, status bar, and navigation bar.
        // This is required for expo-splash-screen.
        setTheme(R.style.AppTheme);
        super.onCreate(null)
        activeInstance = WeakReference(this)
    }

    override fun onResume() {
        super.onResume()
        activeInstance = WeakReference(this)
    }

    override fun onDestroy() {
        if (activeInstance?.get() === this) {
            activeInstance = null
        }
        super.onDestroy()
    }

    fun setPipAutoEnter(enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                    val builder = PictureInPictureParams.Builder()
                        .setAspectRatio(Rational(4, 5))
                        .setAutoEnterEnabled(enabled)
                        .setActions(mediaActions())
                    setPictureInPictureParams(builder.build())
                }
            } catch (_: Exception) {
            }
        }
    }

    fun enterPipNow(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) && !isInPictureInPictureMode) {
                    return enterPictureInPictureMode(pipParams())
                }
            } catch (_: Exception) {
            }
        }
        return false
    }

    // Auto-PiP: if audio is playing, turning the app into a floating mini
    // player (with media controls) instead of leaving to the background.
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isInPictureInPictureMode) {
            try {
                if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                    val snap = PlaybackService.snapshot()
                    if (snap?.playing == true) {
                        enterPictureInPictureMode(pipParams())
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        notifyPipState(isInPictureInPictureMode)
    }

    @Suppress("DEPRECATION")
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        notifyPipState(isInPictureInPictureMode)
    }

    private fun notifyPipState(isInPictureInPictureMode: Boolean) {
        VibEvents.emit(this, "onPipModeChanged") { map ->
            map.putBoolean("isPip", isInPictureInPictureMode)
        }
    }

    private fun mediaActions(): List<RemoteAction> {
        val actions = mutableListOf<RemoteAction>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            actions.add(
                remoteAction(
                    android.R.drawable.ic_media_previous,
                    "Previous",
                    "Previous song",
                    servicePi(PlaybackService.ACTION_PIP_PREV, 30)
                )
            )
            actions.add(
                remoteAction(
                    android.R.drawable.ic_media_next,
                    "Next",
                    "Next song",
                    servicePi(PlaybackService.ACTION_PIP_NEXT, 31)
                )
            )
            actions.add(
                remoteAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Stop",
                    "Stop playback",
                    servicePi(PlaybackService.ACTION_STOP_AUDIO, 32)
                )
            )
        }
        return actions
    }

    private fun pipParams(): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(4, 5))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setActions(mediaActions())
        }
        return builder.build()
    }

    private fun remoteAction(iconRes: Int, title: String, description: String, pi: PendingIntent): RemoteAction =
        RemoteAction(Icon.createWithResource(this, iconRes), title, description, pi)

    private fun servicePi(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this,
            requestCode,
            Intent(this, PlaybackService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

  override fun getMainComponentName(): String = "main"

  override fun createReactActivityDelegate(): ReactActivityDelegate {
    return ReactActivityDelegateWrapper(
          this,
          BuildConfig.IS_NEW_ARCHITECTURE_ENABLED,
          object : DefaultReactActivityDelegate(
              this,
              mainComponentName,
              fabricEnabled
          ){
            override fun createRootView(): ReactRootView {
              val rootView = super.createRootView() ?: ReactRootView(this@MainActivity)
              rootView.setBackgroundColor(0xFF0B0818.toInt())
              return rootView
            }
          })
  }

  override fun invokeDefaultOnBackPressed() {
      if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
          if (!moveTaskToBack(false)) {
              super.invokeDefaultOnBackPressed()
          }
          return
      }

      super.invokeDefaultOnBackPressed()
  }
}