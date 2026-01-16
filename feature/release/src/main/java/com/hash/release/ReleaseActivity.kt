package com.hash.release

import com.alibaba.android.arouter.facade.annotation.Route
import com.hash.common.base.activity.BaseBindingActivity
import com.hash.common.manager.BuglyCrashManager
import com.hash.release.databinding.ActivityReleaseBinding
import com.hash.router.RouterActivityPath
import com.hash.umengsdk.UmengClient

/**
 * @name ReleaseActivity
 * @package com.hash.release
 * @author 345 QQ:1831712732
 * @time 2024/12/15 20:29
 * @description
 */

@Route(path = RouterActivityPath.Release.RELEASE)
class ReleaseActivity : BaseBindingActivity<ActivityReleaseBinding>() {
    override fun layoutId(): Int = R.layout.activity_release

    override fun initView() {
    }

    override fun listener() {
        binding.throwCrashButton.setOnClickListener {
            throw IllegalStateException("are you ok?")
        }
        binding.crashButton.setOnClickListener {
            val s = RuntimeException("Custom Crash?")
            BuglyCrashManager.postCatchException(s)
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(
            0,
//            com.hash.common.R.anim.activity_from_top_to_bottom_in,
            com.hash.common.R.anim.activity_from_top_to_bottom_in
        )
    }
}