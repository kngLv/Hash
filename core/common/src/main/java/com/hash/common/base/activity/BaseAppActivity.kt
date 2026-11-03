package com.hash.common.base.activity

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.core.widget.addTextChangedListener
import androidx.core.view.WindowCompat
import com.hash.common.R
import com.hash.common.base.dialog.BaseDialog
import com.hash.common.base.dialog.LoadingDialog
import com.hash.common.ui.loading.LoadingConfig
import com.hash.common.ui.loading.LoadingStateOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * @name BaseAppActivity
 * @package com.hash.common.base.activity
 * @author 345 QQ:1831712732
 * @time 2024/11/26 22:56
 * @description
 */
abstract class BaseAppActivity : BaseActivity() {

    private var dialog: BaseDialog? = null
    private var showDialogRunnable: Runnable? = null

    /** 页面自行创建 ViewModel 后按需覆写；未覆写时不观察 Loading 状态。 */
    protected open val loadingStateOwner: LoadingStateOwner? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadingStateOwner?.let(::observeLoadingState)
    }

    private fun observeLoadingState(owner: LoadingStateOwner) {
        lifecycleScope.launch {
            owner.loadingState.config.collect { config ->
                if (config == null) {
                    dismissLoading()
                } else {
                    showLoading(
                        delayMillis = config.delayMillis,
                        cancelOnTouchOutside = config.cancelOnTouchOutside,
                    )
                }
            }
        }
    }

    private fun isShowDialog(): Boolean = dialog != null && dialog!!.isShowing

    /**
     * 显示当前 Activity 唯一持有的 Loading Dialog。
     *
     * [delayMillis] 用于避免短请求造成闪烁；[cancelOnTouchOutside] 为 true 时，
     * 用户可点击弹窗外部关闭弹窗。
     */
    fun showLoading(
        delayMillis: Long = LoadingConfig.DEFAULT_DELAY_MILLIS,
        cancelOnTouchOutside: Boolean = true,
    ) {
        if (isFinishing || isDestroyed) {
            return
        }
        if (isShowDialog()) {
            dialog?.setCancelable(cancelOnTouchOutside)
            dialog?.setCanceledOnTouchOutside(cancelOnTouchOutside)
            return
        }
        if (showDialogRunnable != null) return
        showDialogRunnable = Runnable {
            showDialogRunnable = null
            if (isFinishing || isDestroyed) {
                return@Runnable
            }
            if (dialog == null) {
                dialog = LoadingDialog.Builder(this)
                    .create()
            }
            dialog?.setCancelable(cancelOnTouchOutside)
            dialog?.setCanceledOnTouchOutside(cancelOnTouchOutside)
            if (!dialog!!.isShowing) {
                dialog!!.show()
            }
        }
        showDialogRunnable?.let { postDelayed(it, delayMillis.coerceAtLeast(0L)) }
    }

    /**
     * 隐藏加载对话框
     */
    fun dismissLoading() {
        showDialogRunnable?.let(::removeCallbacks)
        showDialogRunnable = null
        if (isShowDialog()) {
            dialog?.dismiss()
        }
    }

    override fun initLayout() {
        super.initLayout()
        configureSystemBarIcons()
        setBack()
    }

    private fun setBack() {
        findViewById<View>(R.id.back)?.setOnClickListener {
            finish()
        }
    }

    private fun configureSystemBarIcons() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = useDarkStatusBarIcons()
            isAppearanceLightNavigationBars = useDarkNavigationBarIcons()
        }
    }

    /**
     * 状态栏和导航栏图标是否统一使用深色，默认随应用深色模式自动切换。
     *
     * 若两个系统栏需要不同图标颜色，分别覆写 [useDarkStatusBarIcons] 和
     * [useDarkNavigationBarIcons]。
     */
    open fun useDarkSystemBarIcons(): Boolean =
        resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK !=
            Configuration.UI_MODE_NIGHT_YES

    /** 状态栏图标是否使用深色。 */
    open fun useDarkStatusBarIcons(): Boolean = useDarkSystemBarIcons()

    /** 导航栏图标是否使用深色。 */
    open fun useDarkNavigationBarIcons(): Boolean = useDarkSystemBarIcons()

    override fun onDestroy() {
        super.onDestroy()
        getContentView()?.run {
            traverse(this)
        }
        dismissLoading()
        dialog = null
    }

    private fun traverse(root: ViewGroup) {
        val childCount = root.childCount
        for (i in 0 until childCount) {
            val child = root.getChildAt(i)
            if (child is ViewGroup) {
                child.background = null
                traverse(child)
            } else {
                EditText(this).addTextChangedListener { }
                child.background = null
                when {
                    child is ImageView -> child.setImageDrawable(null)
                }
            }
        }
    }

    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        super.startActivityForResult(intent, requestCode, options)
        overridePendingTransition(R.anim.right_in_activity, R.anim.right_out_activity)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.left_in_activity, R.anim.left_out_activity)
    }

}
