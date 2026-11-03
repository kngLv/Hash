package com.hash.common.base.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.SparseArray
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.hash.common.base.InitAction
import com.hash.common.base.action.ActivityAction
import com.hash.common.base.action.BundleAction
import com.hash.common.base.action.HandlerAction
import com.hash.common.base.action.KeyboardAction
import com.hash.common.base.fragment.BaseFragment
import java.util.Random
import kotlin.math.pow

/**
 * @name BaseActivity
 * @package com.hash.common.base
 * @author 345 QQ:1831712732
 * @time 2024/11/26 22:43
 * @description
 */
abstract class BaseActivity : AppCompatActivity(), InitAction, ActivityAction, HandlerAction,
    BundleAction, KeyboardAction {

    val tag: String = this.javaClass.simpleName

    /** Activity 回调集合 */
    private val activityCallbacks: SparseArray<OnActivityCallback?> by lazy { SparseArray(1) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        initContentView()
        initActivity()
        loadData()
    }

    private fun initActivity() {
        initLayout()
        initParam()
        initView()
        listener()
        observer()
    }


    open fun initLayout() {
        initSoftKeyboard()
    }

    open fun initContentView() {
        layoutId().run { setContentView(this) }
    }


    override fun finish() {
        super.finish()
        // 隐藏软键，避免内存泄漏
        hideKeyboard(currentFocus)
    }

    /**
     * 初始化软键盘
     */
    protected open fun initSoftKeyboard() {
        // 点击外部隐藏软键盘，提升用户体验
        getContentView()?.setOnClickListener { _: View? ->
            // 隐藏软键，避免内存泄漏
            hideKeyboard(currentFocus)
        }
    }

    open fun getContentView(): ViewGroup? {
        return findViewById(Window.ID_ANDROID_CONTENT)
    }


    override fun getContext(): Context = this

    override fun startActivity(intent: Intent) = super<AppCompatActivity>.startActivity(intent)

    override fun getBundle(): Bundle? = intent.extras

    /**
     * 如果当前的 Activity（singleTop 启动模式） 被复用时会回调
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 设置为当前的 Intent，避免 Activity 被杀死后重启 Intent 还是最原先的那个
        setIntent(intent)
    }


    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val fragments: MutableList<Fragment?> = supportFragmentManager.fragments
        for (fragment: Fragment? in fragments) {
            // 这个 Fragment 必须是 BaseFragment 的子类，并且处于可见状态
            if (fragment !is BaseFragment || fragment.lifecycle.currentState != Lifecycle.State.RESUMED) {
                continue
            }
            // 将按键事件派发给 Fragment 进行处理
            if (fragment.dispatchKeyEvent(event)) {
                // 如果 Fragment 拦截了这个事件，那么就不交给 Activity 处理
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }


    @Deprecated("Deprecated in Java")
    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        // 隐藏软键，避免内存泄漏
        hideKeyboard(currentFocus)
        // 查看源码得知 startActivity 最终也会调用 startActivityForResult
        super.startActivityForResult(intent, requestCode, options)
    }

    /**
     * startActivityForResult 方法优化
     */
    open fun startActivityForResult(clazz: Class<out Activity>, callback: OnActivityCallback?) {
        startActivityForResult(Intent(this, clazz), null, callback)
    }

    open fun startActivityForResult(intent: Intent, callback: OnActivityCallback?) {
        startActivityForResult(intent, null, callback)
    }


    open fun startActivityForResult(intent: Intent, options: Bundle?, callback: OnActivityCallback?) {
        // 请求码必须在 2 的 16 次方以内
        val requestCode: Int = Random().nextInt(2.0.pow(16.0).toInt())
        activityCallbacks.put(requestCode, callback)
        startActivityForResult(intent, requestCode, options)
    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        var callback: OnActivityCallback?
        if ((activityCallbacks.get(requestCode).also { callback = it }) != null) {
            callback?.onActivityResult(resultCode, data)
            activityCallbacks.remove(requestCode)
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    interface OnActivityCallback {

        /**
         * 结果回调
         *
         * @param resultCode        结果码
         * @param data              数据
         */
        fun onActivityResult(resultCode: Int, data: Intent?)
    }
}
