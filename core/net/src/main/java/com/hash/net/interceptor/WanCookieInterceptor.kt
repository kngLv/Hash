package com.hash.net.interceptor

import com.hash.common.storage.userInfo.UserInfoStore
import com.hash.net.NetConstants
import com.hash.net.NetInitializer
import okhttp3.Interceptor
import okhttp3.Response

class WanCookieInterceptor : Interceptor {

    private val loginApi = "user/login"
    private val registerApi = "user/register"
    private val setCookie = "set-cookie"

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestUrl = request.url.toString()
        val newRequest = request.newBuilder()

        if (requestUrl.contains(NetConstants.BASE_URL)) {
            val cookie = UserInfoStore.getCookie()
            if (!cookie.isNullOrEmpty()) {
                newRequest.addHeader("Cookie", cookie)
            }
        }

        val response = chain.proceed(newRequest.build())

        if (requestUrl.contains(loginApi) || requestUrl.contains(registerApi)) {
            if (response.headers(setCookie).isNotEmpty()) {
                val cookie = encodeCookie(response.headers(setCookie))
                UserInfoStore.saveCookie(cookie)
            }
        }

        return response
    }

    fun encodeCookie(cookies: List<String>): String {
        val sb = StringBuilder()
        val set = HashSet<String>()
        cookies
            .map { cookie ->
                cookie.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            }
            .forEach {
                it.filterNot { set.contains(it) }.forEach { set.add(it) }
            }

        val ite = set.iterator()
        while (ite.hasNext()) {
            val cookie = ite.next()
            sb.append(cookie).append(";")
        }

        val last = sb.lastIndexOf(";")
        if (sb.length - 1 == last) {
            sb.deleteCharAt(last)
        }

        return sb.toString()
    }
}