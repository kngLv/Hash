package com.hash.common.base.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.core.widget.addTextChangedListener
import com.gyf.immersionbar.ImmersionBar.setTitleBar
import com.gyf.immersionbar.ktx.immersionBar
import com.hash.common.R
import com.hash.common.base.dialog.BaseDialog
import com.hash.common.base.dialog.WaitDialog
import com.hash.common.ui.loading.LoadingStateOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
        loadingStateOwner?.let { owner ->
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    owner.loadingState.shouldShowLoading.collect { shouldShow ->
                        if (shouldShow) showLoadingDialog() else dismissLoadingDialog()
                    }
                }
            }
        }
    }

    private fun isShowDialog(): Boolean = dialog != null && dialog!!.isShowing

    private fun showLoadingDialog() {
        if (isFinishing || isDestroyed) {
            return
        }
        if (isShowDialog() || showDialogRunnable != null) return
        showDialogRunnable = Runnable {
            showDialogRunnable = null
            if (isFinishing || isDestroyed) {
                return@Runnable
            }
            if (dialog == null) {
                dialog = WaitDialog.Builder(this)
                    .setCancelable(false)
                    .create()
            }
            if (!dialog!!.isShowing) {
                dialog!!.show()
            }
        }
        showDialogRunnable?.let { postDelayed(it, 300) }
    }

    /**
     * 隐藏加载对话框
     */
    private fun dismissLoadingDialog() {
        showDialogRunnable?.let(::removeCallbacks)
        showDialogRunnable = null
        if (!isShowDialog()) return
        dialog?.dismiss()
    }

    /**
     * 将页面级 Loading 状态切换为显示。
     *
     * Dialog 的创建与展示由状态观察者根据生命周期统一处理。
     */
    protected fun showLoading() {
        loadingStateOwner?.loadingState?.show()
    }

    /**
     * 将页面级 Loading 状态切换为隐藏。
     *
     * Dialog 的关闭由状态观察者根据生命周期统一处理。
     */
    protected fun dismissLoading() {
        loadingStateOwner?.loadingState?.dismiss()
    }

    override fun initLayout() {
        super.initLayout()
        initStatusBar()
        setBack()
    }

    private fun setBack() {
        findViewById<View>(R.id.back)?.setOnClickListener {
            finish()
        }
    }

    private fun initStatusBar() {
        if (isStatusBarEnable()) {
            immersionBar {
                statusBarColor(R.color.transparent)
                statusBarDarkFont(isStatusDarkFont())
                getTitleBar()?.let {
                    setTitleBar(this@BaseAppActivity, it)
                } ?: kotlin.run {
                    findViewById<View>(R.id.toolbar)?.let {
                        setTitleBar(this@BaseAppActivity, it)
                    }
                }
            }
        }
    }

    /** 是否启用沉浸式状态栏 */
    open fun isStatusBarEnable(): Boolean = true

    /** 状态栏字体深色模式 */
    open fun isStatusDarkFont(): Boolean = true

    open fun getTitleBar(): View? = null

    override fun onDestroy() {
        super.onDestroy()
        getContentView()?.run {
            traverse(this)
        }
        if (isShowDialog()) {
            dismissLoadingDialog()
        }
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
