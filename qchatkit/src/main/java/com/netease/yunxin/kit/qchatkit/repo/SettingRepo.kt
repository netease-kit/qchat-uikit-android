/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.repo

import com.netease.nimlib.sdk.StatusBarNotificationConfig
import com.netease.nimlib.sdk.v2.setting.enums.V2NIMP2PMessageMuteMode
import com.netease.yunxin.kit.corekit.im2.extend.FetchCallback
import com.netease.yunxin.kit.corekit.im2.provider.ConfigProvider
import com.netease.yunxin.kit.corekit.im2.provider.SettingProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 设置相关业务逻辑接口
 * <p> 提供一些全局设置的获取、设置等操作，内容保存到本地SharePreference中 </p>
 */
object SettingRepo {

    const val recentForward: String = "recent_forward"

    const val maxRecentForward = 5

    /**
     * 获取群组聊天页面是否展示已读未读状态
     * @param callback 回调 true展示已读未读，false不展示
     */
    @JvmStatic
    fun getShowReadStatus(callback: FetchCallback<Boolean>?) {
        CoroutineScope(Dispatchers.Default).launch {
            val result = ConfigProvider.getShowReadStatus()
            withContext(Dispatchers.Main) {
                callback?.onSuccess(result)
            }
        }
    }

    /**
     * 获取群组聊天页面是否展示已读未读状态，同步方法，从SharePreference中读取
     * @return true展示已读未读，false不展示
     */
    @JvmStatic
    fun getShowReadStatus(): Boolean {
        return ConfigProvider.getShowReadStatus()
    }

    /**
     * 设置群组聊天页面是否展示已读未读状态
     * @param show true展示已读未读，false不展示
     */
    @JvmStatic
    fun setShowReadStatus(show: Boolean) {
        CoroutineScope(Dispatchers.Default).launch {
            ConfigProvider.updateShowReadStatus(show)
        }
    }

    /**
     * 获取音频播放模式
     * @return true扬声器模式，false听筒模式
     */
    @JvmStatic
    fun getHandsetMode(): Boolean {
        return ConfigProvider.getAudioPlayModel() == 0
    }

    /**
     * 获取音频播放模式
     * @param callback 回调 true扬声器模式，false听筒模式
     */
    @JvmStatic
    fun getHandsetMode(callback: FetchCallback<Boolean>?) {
        CoroutineScope(Dispatchers.Default).launch {
            val result = ConfigProvider.getAudioPlayModel() == 0
            withContext(Dispatchers.Main) {
                callback?.onSuccess(result)
            }
        }
    }

    /**
     * 设置音频播放模式
     * @param value true扬声器模式，false听筒模式
     */
    @JvmStatic
    fun setHandsetMode(value: Boolean) {
        CoroutineScope(Dispatchers.Default).launch {
            var mode = 0
            if (!value) {
                mode = 1
            }
            ConfigProvider.updateAudioPlayMode(mode)
        }
    }

    /**
     * 新消息通知时，是否响铃
     * @return true响铃，false不响铃
     */
    @JvmStatic
    fun getRingMode(): Boolean {
        return ConfigProvider.getRingToggle()
    }

    /**
     * 新消息通知时，是否响铃
     * @param callback 回调 true响铃，false不响铃
     */
    @JvmStatic
    fun getRingMode(callback: FetchCallback<Boolean>?) {
        CoroutineScope(Dispatchers.Default).launch {
            val result = ConfigProvider.getRingToggle()
            withContext(Dispatchers.Main) {
                callback?.onSuccess(result)
            }
        }
    }

    /**
     * 设置新消息通知时，是否响铃
     * @param mode true响铃，false不响铃
     */
    @JvmStatic
    fun setRingMode(mode: Boolean) {
        CoroutineScope(Dispatchers.Default).launch {
            var config: StatusBarNotificationConfig? = ConfigProvider.getStatusConfig()
            if (config == null) {
                config = StatusBarNotificationConfig()
            }
            config.ring = mode
            ConfigProvider.updateRingToggle(mode)
            ConfigProvider.saveStatusBarNotificationConfig(config)
            SettingProvider.updateStatusBarNotificationConfig(config)
        }
    }

    /**
     * 新消息通知时，是否震动
     * @return true震动，false不震动
     */
    @JvmStatic
    fun getVibrateMode(): Boolean {
        return ConfigProvider.getVibrateToggle()
    }

    /**
     * 新消息通知时，是否震动
     * @param callback 回调 true震动，false不震动
     */
    @JvmStatic
    fun getVibrateMode(callback: FetchCallback<Boolean>?) {
        CoroutineScope(Dispatchers.Default).launch {
            val result = ConfigProvider.getVibrateToggle()
            withContext(Dispatchers.Main) {
                callback?.onSuccess(result)
            }
        }
    }

    /**
     * 设置新消息通知时，是否震动
     * @param mode true震动，false不震动
     */
    @JvmStatic
    fun setVibrateMode(mode: Boolean) {
        CoroutineScope(Dispatchers.Default).launch {
            var config: StatusBarNotificationConfig? = ConfigProvider.getStatusConfig()
            if (config == null) {
                config = StatusBarNotificationConfig()
            }
            config.vibrate = mode
            ConfigProvider.updateVibrateToggle(mode)
            ConfigProvider.saveStatusBarNotificationConfig(config)
            SettingProvider.updateStatusBarNotificationConfig(config)
        }
    }

    /**
     * 设置群消息免打扰
     * @param accountId 账号ID
     * @param mute 是否免打扰，true 免打扰，false 开启消息提醒
     * @param callback 结果回调
     */
    @JvmStatic
    fun setP2PMessageMuteMode(
        accountId: String,
        mute: Boolean,
        callback: FetchCallback<Void>? = null
    ) {
        val muteMode =
            if (mute) {
                V2NIMP2PMessageMuteMode.V2NIM_P2P_MESSAGE_MUTE_MODE_ON
            } else {
                V2NIMP2PMessageMuteMode.V2NIM_P2P_MESSAGE_MUTE_MODE_OFF
            }
        SettingProvider.setP2PMessageMuteMode(
            accountId,
            muteMode,
            callback
        )
    }

    /**
     * 设置会话免打扰
     * @param conversationId 会话ID
     * @param callback 结果回调
     */
    @JvmStatic
    fun getP2PMessageMuteMode(
        conversationId: String,
        callback: FetchCallback<Boolean>? = null
    ) {
        CoroutineScope(Dispatchers.Default).launch {
            val muteMode = SettingProvider.getP2PMessageMuteMode(
                conversationId
            )
            CoroutineScope(Dispatchers.Main).launch {
                callback?.onSuccess(
                    muteMode == V2NIMP2PMessageMuteMode.V2NIM_P2P_MESSAGE_MUTE_MODE_ON
                )
            }
        }
    }
}
