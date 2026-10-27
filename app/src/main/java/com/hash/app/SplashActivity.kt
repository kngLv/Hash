package com.hash.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.hash.main.MainActivity

class SplashActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private val openMain = Runnable {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        overridePendingTransition(0, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        window.statusBarColor = getColor(R.color.splash_background)
        window.navigationBarColor = getColor(R.color.splash_background)
        handler.postDelayed(openMain, SPLASH_DURATION_MILLIS)
    }

    override fun onDestroy() {
        handler.removeCallbacks(openMain)
        super.onDestroy()
    }

    private companion object {
        const val SPLASH_DURATION_MILLIS = 600L
    }
}
