package com.hash.login

import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.hash.common.ext.SpanPart
import com.hash.common.ext.setSpannableParts
import com.alibaba.android.arouter.facade.annotation.Route
import com.hash.common.base.activity.BaseBindingActivity
import com.hash.common.impl.TextWatcherImpl
import com.hash.common.ext.selectToggle
import com.hash.common.ext.showToast
import com.hash.login.databinding.ActivityLoginBinding
import com.hash.login.viewmodel.LoginViewmodel
import com.hash.router.RouterActivityPath
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.hash.repository.login.LoginState
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@Route(path = RouterActivityPath.Login.LOGIN)
class LoginActivity : BaseBindingActivity<ActivityLoginBinding>() {

    val viewMode by viewModels<LoginViewmodel>()

    override fun layoutId(): Int = R.layout.activity_login

    override fun initView() {
        setAgreeText()
        setLoginState()
    }

    override fun observer() {
        // Collect StateFlow and SharedFlow from ViewModel while Activity is STARTED
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // collect login state (StateFlow)
                launch {
                    viewMode.loginState.drop(1).collect { state ->
                        when (state) {
                            LoginState.LOGGING -> showLoading()
                            LoginState.LOGGED -> {
                                dismissLoading()
                                showToast(R.string.login_success)
                                finish()
                            }

                            LoginState.UN_LOGIN -> {
                                dismissLoading()
                            }
                        }
                    }
                }
            }
        }
    }


    override fun listener() {
        binding.agreementCheckbox.setOnClickListener {
            binding.agreementCheckbox.selectToggle()
        }
        binding.loginButton.setOnClickListener {
            if (!binding.agreementCheckbox.isSelected) {
                showToast(R.string.login_please_agree)
                return@setOnClickListener
            }
            if (binding.loginButton.isSelected) {
                val number = binding.phoneNumber.text.toString()
                val password = binding.password.text.toString()
                viewMode.login(number, password)
            }
        }
        binding.phoneNumber.addTextChangedListener(
            object : TextWatcherImpl() {
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    binding.loginButton.isSelected =
                        s != null && s.length > 3 && !binding.password.text.isNullOrEmpty()
                }
            }
        )
        binding.password.addTextChangedListener(
            object : TextWatcherImpl() {
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    binding.loginButton.isSelected =
                        s != null && s.length > 3 && !binding.phoneNumber.text.isNullOrEmpty()
                }
            }
        )

    }

    private fun setLoginState() {
        binding.loginButton.isSelected = (binding.phoneNumber.text?.length ?: 0) > 3 &&
                (binding.password.text ?: "").isNotEmpty()
    }

    private fun setAgreeText() {
        val part0 = getString(R.string.login_agree)
        val part1 = getString(R.string.login_user_agreement)
        val part2 = getString(R.string.login_privacy_policy)
        val part3 = getString(R.string.login_minor_protection)

        val linkColor = ContextCompat.getColor(this, com.hash.common.R.color.textLink)
        val secondaryColor = ContextCompat.getColor(this, com.hash.common.R.color.textSecondary)

        val parts = listOf(
            SpanPart(
                part0, color = secondaryColor,
                onClick = { binding.agreementCheckbox.selectToggle() }),
            SpanPart(part1, color = linkColor, onClick = { openUserAgreement() }),
            SpanPart(part2, color = linkColor, onClick = { openPrivacyPolicy() }),
            SpanPart(part3, color = linkColor, onClick = { openMinorProtection() })
        )

        binding.agreementText.setSpannableParts(parts)
    }


    // 以下为点击回调示例，可替换为打开 WebView / 路由跳转等具体实现
    private fun openUserAgreement() {
        Toast.makeText(this, getString(R.string.login_user_agreement), Toast.LENGTH_SHORT).show()
    }

    private fun openPrivacyPolicy() {
        Toast.makeText(this, getString(R.string.login_privacy_policy), Toast.LENGTH_SHORT).show()
    }

    private fun openMinorProtection() {
        Toast.makeText(this, getString(R.string.login_minor_protection), Toast.LENGTH_SHORT).show()
    }
}