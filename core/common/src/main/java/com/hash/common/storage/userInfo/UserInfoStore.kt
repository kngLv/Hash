package com.hash.common.storage.userInfo

import com.google.gson.Gson
import com.hash.bean.mine.UserInfoBean
import com.tencent.mmkv.MMKV

object UserInfoStore {

    private val kv by lazy { MMKV.mmkvWithID("userInfo") }

    private const val KEY_USER_INFO = "key_user_info"
    private const val KEY_COOKIE = "key_cookie"

    private val gson = Gson()

    /** Save user info to MMKV. If userInfo is null, clear stored value. */
    fun saveUserInfo(userInfo: UserInfoBean) {
        try {
            val json = gson.toJson(userInfo)
            kv.encode(KEY_USER_INFO, json)
        } catch (_: Throwable) { /* ignore storage failures */ }
    }

    /** Get currently cached user info synchronously (may be null) */
    fun getUserInfoCached(): UserInfoBean? {
        return try {
            val json = kv.decodeString(KEY_USER_INFO, null) ?: return null
            gson.fromJson(json, UserInfoBean::class.java)
        } catch (_: Throwable) { null }
    }

    /** Clear user info from storage */
    fun clearUserInfo() {
        try { try { kv.removeValueForKey(KEY_USER_INFO) } catch (_: NoSuchMethodError) { kv.remove(KEY_USER_INFO) } } catch (_: Throwable) {}
    }

    // Cookie helpers (examples of other small pieces of user data)
    fun saveCookie(cookie: String?) {
        try {
            if (cookie == null) {
                try { kv.removeValueForKey(KEY_COOKIE) } catch (_: NoSuchMethodError) { kv.remove(KEY_COOKIE) }
                return
            }
            kv.encode(KEY_COOKIE, cookie)
        } catch (_: Throwable) { }
    }

    fun getCookie(): String? = try { kv.decodeString(KEY_COOKIE, null) } catch (_: Throwable) { null }
}