package com.hash.app

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hash.main.MainActivity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LaunchActivityInstrumentedTest {

    @Test
    fun launcherIntent_includesCustomSplashActivityInsteadOfMainActivity() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val launcherIntent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setPackage(context.packageName)

        val activityNames = context.packageManager
            .queryIntentActivities(launcherIntent, 0)
            .map { it.activityInfo.name }

        assertTrue(activityNames.contains("com.hash.app.SplashActivity"))
        assertFalse(activityNames.contains(MainActivity::class.java.name))
        assertFalse(activityNames.contains("com.hash.main.splash.SplashActivity"))
    }

    @Test
    fun splashBranding_usesTheSystemRecommendedDpSize() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val branding = context.getDrawable(R.drawable.splash_branding) as BitmapDrawable
        val density = context.resources.displayMetrics.density

        assertEquals(200f, branding.intrinsicWidth / density, 0.1f)
        assertEquals(80f, branding.intrinsicHeight / density, 0.1f)
    }

    @Test
    fun startupProvider_doesNotInitializeAppComponentsBeforeSplashIsDrawn() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val provider = context.packageManager.resolveContentProvider(
            "${context.packageName}.androidx-startup",
            PackageManager.GET_META_DATA,
        )
        val metadata = provider?.metaData

        assertFalse(metadata?.containsKey("com.hash.common.startup.NetInitializer") == true)
        assertFalse(metadata?.containsKey("com.hash.common.startup.MMKVInitializer") == true)
        assertFalse(metadata?.containsKey("com.hash.router.AInitializer") == true)
    }
}
