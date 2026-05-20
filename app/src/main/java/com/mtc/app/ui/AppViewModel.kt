package com.mtc.app.ui

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * App 级状态管理
 */
@HiltViewModel
class AppViewModel @Inject constructor() : androidx.lifecycle.ViewModel() {

    // 始终显示服务器配置页，不保存/不自动跳转
    val isServerConfigured: javax.inject.Provider<Boolean?> =
        object : javax.inject.Provider<Boolean?> {
            override fun get(): Boolean? = false
        }
}
