package com.hash.common.utils

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import com.hash.common.IApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

/**
 * @name GlobalUtil
 * @package com.hash.common.utils
 * @author 345 QQ:1831712732
 * @time 2024/12/26 22:38
 * @description
 */
object GlobalUtil {

    private var TAG = "GlobalUtil"

    /**
     * 获取当前应用程序的包名。
     *
     * @return 当前应用程序的包名。
     */
    val appPackage = IApp.instant.packageName

    /**
     * 获取当前应用程序的名称。
     * @return 当前应用程序的名称。
     */
    val appName: String = IApp.instant.resources.getString(IApp.instant.applicationInfo.labelRes)

    /**
     * 获取当前应用程序的版本名。
     * @return 当前应用程序的版本名。
     */
    val appVersionName: String? =
        IApp.instant.packageManager.getPackageInfo(appPackage, 0).versionName

    /**
     * 获取当前应用程序的版本号。
     * @return 当前应用程序的版本号。
     */
    val appVersionCode: Long
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            IApp.instant.packageManager.getPackageInfo(appPackage, 0).longVersionCode
        } else {
            IApp.instant.packageManager.getPackageInfo(appPackage, 0).versionCode.toLong()
        }

    /**
     * 获取开眼应用程序的版本名。
     * @return 开眼当前应用程序的版本名。
     */
    val eyepetizerVersionName: String
        get() = "6.3.1"

    /**
     * 获取开眼应用程序的版本号。
     * @return 开眼当前应用程序的版本号。
     */
    val eyepetizerVersionCode: Long
        get() = 6030012

    /**
     * 获取设备的的型号，如果无法获取到，则返回Unknown。
     * @return 设备型号。
     */
    val deviceModel: String
        get() {
            var deviceModel = Build.MODEL
            if (TextUtils.isEmpty(deviceModel)) {
                deviceModel = "unknown"
            }
            return deviceModel
        }

    /**
     * 获取设备的品牌，如果无法获取到，则返回Unknown。
     * @return 设备品牌，全部转换为小写格式。
     */
    val deviceBrand: String
        get() {
            var deviceBrand = Build.BRAND
            if (TextUtils.isEmpty(deviceBrand)) {
                deviceBrand = "unknown"
            }
            return deviceBrand.lowercase(Locale.getDefault())
        }

    private var deviceSerial: String? = null

    /**
     * 获取设备的序列号。如果无法获取到设备的序列号，则会生成一个随机的UUID来作为设备的序列号，UUID生成之后会存入缓存，
     * 下次获取设备序列号的时候会优先从缓存中读取。
     * @return 设备的序列号。
     */
    @SuppressLint("HardwareIds")
    fun getDeviceSerial(): String {
        if (deviceSerial == null) {
            deviceSerial =
                UUID.randomUUID().toString().replace("-", "").uppercase(Locale.getDefault())
            return deviceSerial.toString()
        } else {
            return deviceSerial.toString()
        }
    }

    // Added: cached deviceId that prefers ANDROID_ID and falls back to getDeviceSerial()
    private var deviceId: String? = null

    /**
     * 获取设备唯一ID，优先使用 Settings.Secure.ANDROID_ID（做已知无效值过滤），
     * 若无法获取则回退到 getDeviceSerial()。结果会缓存，避免重复读取。
     */
    @SuppressLint("HardwareIds")
    fun getDeviceId(): String {
        if (!deviceId.isNullOrEmpty()) return deviceId!!

        var androidId: String? = null
        try {
            androidId = Settings.Secure.getString(IApp.instant.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            Log.e(TAG, "getDeviceId: failed to read ANDROID_ID", e)
        }

        // 过滤已知的无效 ANDROID_ID 值
        val invalidIds = setOf("9774d56d682e549c", "0123456789abcdef", "000000000000000")
        deviceId = if (!androidId.isNullOrEmpty() && !invalidIds.contains(androidId.lowercase(Locale.getDefault()))) {
            androidId.replace("-", "").uppercase(Locale.getDefault())
        } else {
            getDeviceSerial()
        }

        return deviceId!!
    }


    /**
     * 获取AndroidManifest.xml文件中，<application>标签下的meta-data值。
     *
     * @param key
     *  <application>标签下的meta-data健
     */
    fun getApplicationMetaData(key: String): String? {
        var applicationInfo: ApplicationInfo? = null
        try {
            applicationInfo = IApp.instant.packageManager.getApplicationInfo(
                appPackage,
                PackageManager.GET_META_DATA
            )
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, e.message, e)
        }
        return applicationInfo?.metaData?.getString(key)
    }

    /**
     * 判断某个应用是否安装。
     * @param packageName
     * 要检查是否安装的应用包名
     * @return 安装返回true，否则返回false。
     */
    fun isInstalled(packageName: String): Boolean {
        val packageInfo: PackageInfo? = try {
            IApp.instant.packageManager.getPackageInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        return packageInfo != null
    }

    /**
     * 获取当前应用程序的图标。
     */
    fun getAppIcon(): Drawable {
        val packageManager = IApp.instant.packageManager
        val applicationInfo = packageManager.getApplicationInfo(appPackage, 0)
        return packageManager.getApplicationIcon(applicationInfo)
    }

    /**
     * 判断手机是否安装了QQ。
     */
    fun isQQInstalled() = isInstalled("com.tencent.mobileqq")

    /**
     * 判断手机是否安装了微信。
     */
    fun isWechatInstalled() = isInstalled("com.tencent.mm")

    /**
     * 判断手机是否安装了微博。
     * */
    fun isWeiboInstalled() = isInstalled("com.sina.weibo")
}