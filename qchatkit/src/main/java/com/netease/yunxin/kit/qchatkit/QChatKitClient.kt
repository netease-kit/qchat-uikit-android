/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit

import android.content.Context
import com.netease.nimlib.sdk.ModeCode
import com.netease.nimlib.sdk.NIMClient
import com.netease.nimlib.sdk.NimStrings
import com.netease.nimlib.sdk.NosTokenSceneConfig
import com.netease.nimlib.sdk.SDKOptions
import com.netease.nimlib.sdk.StatusBarNotificationConfig
import com.netease.nimlib.sdk.StatusCode
import com.netease.nimlib.sdk.auth.LoginInfo
import com.netease.nimlib.sdk.mixpush.MixPushMessageHandler
import com.netease.nimlib.sdk.mixpush.NIMPushClient
import com.netease.nimlib.sdk.msg.model.CaptureDeviceInfoConfig
import com.netease.nimlib.sdk.v2.auth.V2NIMLoginDetailListener
import com.netease.nimlib.sdk.v2.auth.V2NIMLoginListener
import com.netease.nimlib.sdk.v2.auth.V2NIMLoginService
import com.netease.nimlib.sdk.v2.auth.enums.V2NIMDataSyncState
import com.netease.nimlib.sdk.v2.auth.enums.V2NIMDataSyncType
import com.netease.nimlib.sdk.v2.auth.enums.V2NIMLoginStatus
import com.netease.nimlib.sdk.v2.auth.option.V2NIMLoginOption
import com.netease.nimlib.sdk.v2.user.V2NIMUser
import com.netease.yunxin.kit.corekit.im2.IMKitClient
import com.netease.yunxin.kit.corekit.im2.extend.FetchCallback
import com.netease.yunxin.kit.corekit.im2.provider.LoginProvider

object QChatKitClient {

    @JvmStatic
    fun init(context: Context, loginInfo: LoginInfo?, appKey: String?) {
        IMKitClient.init(context, appKey)
    }

    @JvmStatic
    fun init(context: Context, loginInfo: LoginInfo?, options: SDKOptions) {
        IMKitClient.init(context, options, null)
    }

    /**
     * 添加IM SDK V2版本的登录监听
     * @param listener V2NIMLoginListener
     */
    @JvmStatic
    fun addLoginListener(listener: V2NIMLoginListener) {
        NIMClient.getService(V2NIMLoginService::class.java).addLoginListener(listener)
    }

    /**
     * 移除IM SDK V2版本的登录监听
     * @param listener V2NIMLoginListener
     */
    @JvmStatic
    fun removeLoginListener(listener: V2NIMLoginListener) {
        NIMClient.getService(V2NIMLoginService::class.java).removeLoginListener(listener)
    }

    /**
     * 主数据是否同步完成
     *
     * @return
     */
    @JvmStatic
    fun isDataSyncComplete(): Boolean {
        val dataSync = NIMClient.getService(V2NIMLoginService::class.java).dataSync
        for (sync in dataSync) {
            if (sync.type == V2NIMDataSyncType.V2NIM_DATA_SYNC_MAIN && sync.state == V2NIMDataSyncState.V2NIM_DATA_SYNC_STATE_COMPLETED) {
                return true
            }
        }
        return false
    }

    /**
     * 添加IM SDK V2版本的登录详情监听
     * @param listener V2NIMLoginDetailListener
     */
    @JvmStatic
    fun addLoginDetailListener(listener: V2NIMLoginDetailListener) {
        NIMClient.getService(V2NIMLoginService::class.java).addLoginDetailListener(listener)
    }

    /**
     * 移除IM SDK V2版本的登录详情监听
     * @param listener V2NIMLoginDetailListener
     */
    @JvmStatic
    fun removeLoginDetailListener(listener: V2NIMLoginDetailListener) {
        NIMClient.getService(V2NIMLoginService::class.java).removeLoginDetailListener(listener)
    }

    @JvmStatic
    fun setContext(context: Context) {
        IMKitClient.setContext(context)
    }

    @JvmStatic
    fun getApplicationContext(): Context {
        return IMKitClient.getApplicationContext()!!
    }

    /**
     * 获取当前用户的状态。
     */
    @JvmStatic
    fun getStatus(): StatusCode {
        return NIMClient.getStatus()
    }

    /**
     * 获取 SDK 当前的登录模式。
     */
    @JvmStatic
    fun getLoginMode(): ModeCode {
        return NIMClient.getMode()
    }

    /**
     * 通知栏消息提醒开关。
     */
    @JvmStatic
    fun toggleNotification(on: Boolean) {
        NIMClient.toggleNotification(on)
    }

    /**
     * 撤回消息需要通知栏提醒的开关。
     */
    @JvmStatic
    fun toggleRevokeMessageNotification(on: Boolean) {
        NIMClient.toggleRevokeMessageNotification(on)
    }

    /**
     * 更新通知栏消息提醒配置，包括是否需要振动提醒、是否需要响铃提醒等。
     */
    @JvmStatic
    fun updateStatusBarNotificationConfig(config: StatusBarNotificationConfig) {
        NIMClient.updateStatusBarNotificationConfig(config)
    }

    /**
     * 更新系统文案。
     * 使用场景： 当系统语言发生变化时，可以调用该接口更新文案配置，实现 SDK 对多语言的支持。也可以用于定制通知栏消息提醒的文案
     */
    @JvmStatic
    fun updateStrings(content: NimStrings) {
        NIMClient.updateStrings(content)
    }

    /**
     * 获取 SDK 数据缓存目录路径。
     * 调用时机： 建议在初始化 SDK 之后调用
     */
    @JvmStatic
    fun getSDKStorageDirPath(): String {
        return NIMClient.getSdkStorageDirPath()
    }

    /**
     * 运行时获取当前 SDK 版本号
     */
    @JvmStatic
    fun getSDKVersion(): String {
        return NIMClient.getSDKVersion()
    }

    /**
     * 更新 SDK NOS Token 场景配置。对于 SDK NOS Token，云信 SDK 有默认值 ，若用户不单独配置，则直接采用默认值
     */
    @JvmStatic
    fun updateTokenSceneConfig(config: NosTokenSceneConfig) {
        NIMClient.updateTokenSceneConfig(config)
    }

    /**
     * 更新获取设备信息的相关配置。包括配置是否获取产品型号、是否获取制造商信息、是否获取品牌信息，null 表示都可以获取，没有限制
     */
    @JvmStatic
    fun updateCaptureDeviceInfoOption(config: CaptureDeviceInfoConfig) {
        NIMClient.updateCaptureDeviceInfoOption(config)
    }

    /**
     * 注册第三方推送消息接收handler 在云信SDK初始化 NimClient.init 接口前调用
     */
    @JvmStatic
    fun registerMixPushMessageHandler(handler: MixPushMessageHandler) {
        NIMPushClient.registerMixPushMessageHandler(handler)
    }

    /**
     * 登录IM
     * @param account 账号
     * @param token token
     * @param option 登录配置项
     * @param callback 登录回调
     */
    @JvmStatic
    fun login(
        account: String, token: String, option: V2NIMLoginOption?,
        callback:
        FetchCallback<Void>?
    ) {
        var loginOption = option
        if (loginOption == null) {
            loginOption = V2NIMLoginOption()
        }
        LoginProvider.login(account, token, loginOption, callback)
    }

    /**
     * 登出IM
     */
    @JvmStatic
    fun logout(callback: FetchCallback<Void>?) {
        LoginProvider.logout(callback)
    }

    /**
     * 获取当前登录账号
     * @return 当前登录账号
     */
    @JvmStatic
    fun account(): String? {
        return LoginProvider.currentAccount()
    }

    /**
     * 获取当前登录用户信息
     * @return 当前登录用户信息
     */
    @JvmStatic
    fun currentUser(): V2NIMUser? {
        return LoginProvider.currentUser()
    }

    /**
     * 更新当前登录用户信息，重新从SDK查询新的用户信息
     * @return 当前登录用户信息
     */
    @JvmStatic
    fun updateCurrentUser() {
        LoginProvider.updateCurrentUser()
    }

    /**
     * 设置当前登录用户信息
     * @return 当前登录用户信息
     */
    @JvmStatic
    fun setCurrentUser(user: V2NIMUser) {
        LoginProvider.setCurrentUser(user)
    }

    /**
     * 获取当前IM 是否完成登录
     * @return true 已经登录，false 未登录
     */
    @JvmStatic
    fun hasLogin(): Boolean {
        return NIMClient.getService(V2NIMLoginService::class.java).loginStatus ==
            V2NIMLoginStatus.V2NIM_LOGIN_STATUS_LOGINED
    }
}
