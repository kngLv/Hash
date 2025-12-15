package com.hash.common.storage

import android.content.Context
import androidx.startup.Initializer
import com.tencent.mmkv.MMKV

/**
 * Auto-initialize MMKV at app startup (via androidx.startup).
 */
class MMKVInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        // Initialize MMKV directly; keep simple and robust for module use
        MMKV.initialize(context.applicationContext)
        // force default instance creation
        MMKV.defaultMMKV()
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
