package com.hash.common.ext

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hjq.gson.factory.GsonFactory
import com.tencent.mmkv.MMKV
import java.lang.reflect.Type

// Lazily get MMKV instance; if not initialized this will throw with a helpful message.
private fun mmkv(): MMKV = try {
    MMKV.defaultMMKV()
} catch (_: Throwable) {
    throw IllegalStateException("MMKV is not initialized. Make sure to call MMKV.initialize(context) (or use the provided Startup initializer).")
}

// ------------------ Put / Save helpers ------------------

fun String.mmkvPut(value: String?): Boolean = try {
    mmkv().encode(this, value)
} catch (_: Throwable) {
    false
}

fun String.mmkvPut(value: Int): Boolean = try {
    mmkv().encode(this, value)
} catch (_: Throwable) {
    false
}

fun String.mmkvPut(value: Long): Boolean = try {
    mmkv().encode(this, value)
} catch (_: Throwable) {
    false
}

fun String.mmkvPut(value: Float): Boolean = try {
    mmkv().encode(this, value)
} catch (_: Throwable) {
    false
}

fun String.mmkvPut(value: Double): Boolean = try {
    mmkv().encode(this, value)
} catch (_: Throwable) {
    false
}

fun String.mmkvPut(value: Boolean): Boolean = try {
    mmkv().encode(this, value)
} catch (_: Throwable) {
    false
}

fun String.mmkvPut(value: ByteArray?): Boolean = try {
    mmkv().encode(this, value)
} catch (_: Throwable) {
    false
}

fun String.mmkvPutStringSet(value: Set<String>?): Boolean = try {
    mmkv().encode(this, value)
} catch (_: Throwable) {
    false
}

// JSON-based put helper used by reified wrapper
fun String.mmkvPutJson(json: String?): Boolean = try {
    mmkv().encode(this, json)
} catch (_: Throwable) {
    false
}

// Reified inline wrapper that serializes object to JSON and stores it
inline fun <reified T> String.mmkvPutObject(obj: T?): Boolean = try {
    val json = obj?.let { GsonFactory.getSingletonGson().toJson(it) }
    mmkvPutJson(json)
} catch (_: Throwable) {
    false
}

// ------------------ Get / Read helpers ------------------

fun String.mmkvGetString(default: String? = null): String? = try {
    mmkv().decodeString(this, default)
} catch (_: Throwable) {
    default
}

fun String.mmkvGetInt(default: Int = 0): Int = try {
    mmkv().decodeInt(this, default)
} catch (_: Throwable) {
    default
}

fun String.mmkvGetLong(default: Long = 0L): Long = try {
    mmkv().decodeLong(this, default)
} catch (_: Throwable) {
    default
}

fun String.mmkvGetFloat(default: Float = 0f): Float = try {
    mmkv().decodeFloat(this, default)
} catch (_: Throwable) {
    default
}

fun String.mmkvGetDouble(default: Double = 0.0): Double = try {
    mmkv().decodeDouble(this, default)
} catch (_: Throwable) {
    default
}

fun String.mmkvGetBoolean(default: Boolean = false): Boolean = try {
    mmkv().decodeBool(this, default)
} catch (_: Throwable) {
    default
}

fun String.mmkvGetBytes(): ByteArray? = try {
    mmkv().decodeBytes(this)
} catch (_: Throwable) {
    null
}

fun String.mmkvGetStringSet(): Set<String>? = try {
    mmkv().decodeStringSet(this)
} catch (_: Throwable) {
    null
}

// Public non-inline generic get (accepts Class)
fun <T> String.mmkvGetObject(clazz: Class<T>): T? = try {
    val json = mmkv().decodeString(this, null) ?: return null
    GsonFactory.getSingletonGson().fromJson(json, clazz)
} catch (_: Throwable) {
    null
}

fun <T> String.mmkvGetTypeObject(type: Type): T? = try {
    val json = mmkv().decodeString(this, null) ?: return null
    GsonFactory.getSingletonGson().fromJson<T>(json, type)
} catch (_: Throwable) {
    null
}


// Reified inline wrapper
inline fun <reified T> String.mmkvGetObject(): T? = mmkvGetObject(T::class.java)

inline fun <reified T> String.mmkvGetTypeObject(): T? =
    mmkvGetTypeObject(object : TypeToken<T>() {}.type)

// ------------------ Utility helpers ------------------

fun mmkvContains(key: String): Boolean = try {
    mmkv().contains(key)
} catch (_: Throwable) {
    false
}

fun mmkvRemove(key: String): Boolean = try {
    try {
        mmkv().removeValueForKey(key)
    } catch (_: NoSuchMethodError) {
        mmkv().remove(key)
    }
    true
} catch (_: Throwable) {
    false
}

fun mmkvClearAll(): Boolean = try {
    mmkv().clearAll(); true
} catch (_: Throwable) {
    false
}
