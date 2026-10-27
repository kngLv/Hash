package com.hash.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.startup.AppInitializer
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.hash.common.IApp
import com.hash.common.startup.MMKVInitializer
import com.hash.common.startup.NetInitializer
import com.hash.main.MainActivity
import com.hash.router.AInitializer

class SplashActivity : Activity() {

    private lateinit var decorView: View
    private var hasDrawnSplash = false
    private val initializeAndOpenMain = Runnable {
        initializeApplication()
        if (isFinishing || isDestroyed) return@Runnable
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
        decorView = window.decorView
        decorView.viewTreeObserver.addOnDrawListener(object : ViewTreeObserver.OnDrawListener {
            override fun onDraw() {
                if (hasDrawnSplash) return
                hasDrawnSplash = true
                decorView.post {
                    decorView.viewTreeObserver.takeIf { it.isAlive }
                        ?.removeOnDrawListener(this)
                    decorView.post(initializeAndOpenMain)
                }
            }
        })
    }

    override fun onDestroy() {
        decorView.removeCallbacks(initializeAndOpenMain)
        super.onDestroy()
    }

    private fun initializeApplication() {
        val initializer = AppInitializer.getInstance(applicationContext)
        initializer.initializeComponent(MMKVInitializer::class.java)
        initializer.initializeComponent(NetInitializer::class.java)
        initializer.initializeComponent(AInitializer::class.java)
        IApp.initializeAfterFirstFrame()
    }
}
