/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.login

import com.netease.nimlib.sdk.NIMClient
import com.netease.nimlib.sdk.RequestCallback
import com.netease.nimlib.sdk.auth.AuthService
import com.netease.nimlib.sdk.auth.LoginInfo
import com.netease.nimlib.sdk.qchat.QChatService
import com.netease.nimlib.sdk.qchat.param.QChatLoginParam
import com.netease.nimlib.sdk.qchat.result.QChatLoginResult
import com.netease.yunxin.kit.corekit.im2.extend.FetchCallback
import com.netease.yunxin.kit.corekit.im2.provider.LoginProvider
import com.netease.yunxin.kit.corekit.im2.utils.toFetchCallback
import com.netease.yunxin.kit.corekit.model.ErrorMsg
import com.netease.yunxin.kit.corekit.model.ResultInfo
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val loginFailMsg: String = "login im onFailed"
const val logoutFailMsg: String = "logout im onFailed"

fun LoginProvider.loginIMWithQChat(
    info: LoginInfo,
    callback: FetchCallback<QChatLoginResult>?
) {
    CoroutineScope(Dispatchers.Main).launch {
        val loginIMResult = LoginProvider.loginIMInner(info.account, info.token)
        if (!loginIMResult.success) {
            val errorMsg =
                loginIMResult.msg ?: ErrorMsg(errorCode, "loginIMWithQChat-loginIMInner-error")
            callback?.onError(errorMsg.code, errorMsg.message)
            return@launch
        }
        LoginProvider.loginQChat(callback)
    }
}

suspend fun LoginProvider.loginIMInner(account: String, token: String): ResultInfo<Void> {
    return suspendCoroutine {
        LoginProvider.login(
            account,
            token,
            null,
            object : FetchCallback<Void> {
                override fun onError(errorCode: Int, errorMsg: String?) {
                    it.resume(ResultInfo(success = false, msg = ErrorMsg(errorCode, errorMsg)))
                }

                override fun onSuccess(data: Void?) {
                    it.resume(ResultInfo(data))
                }
            }
        )
    }
}

fun LoginProvider.loginQChat(callback: FetchCallback<QChatLoginResult>?) {
    val param = QChatLoginParam()
    NIMClient.getService(QChatService::class.java).login(param)
        .setCallback(
            object : RequestCallback<QChatLoginResult> {
                override fun onSuccess(param: QChatLoginResult) {
                    callback?.onSuccess(param)
                }

                override fun onFailed(code: Int) {
                    callback?.onError(code, loginFailMsg)
                }

                override fun onException(exception: Throwable?) {
                    callback?.onError(errorCode, exception?.stackTraceToString() ?: "Error")
                }
            }
        )
}

fun LoginProvider.logoutQChat(callback: FetchCallback<Void>?) {
    NIMClient.getService(QChatService::class.java).logout()
        .setCallback(
            object : RequestCallback<Void> {
                override fun onSuccess(param: Void?) {
                    callback?.onSuccess(param)
                }

                override fun onFailed(code: Int) {
                    callback?.onError(code, logoutFailMsg)
                }

                override fun onException(exception: Throwable?) {
                    callback?.onError(errorCode, exception?.stackTraceToString() ?: "Error")
                }
            }
        )
}

fun LoginProvider.logoutIMWithQChat(callback: FetchCallback<Void>?) {
    CoroutineScope(Dispatchers.Main).launch {
        val result =
            suspendCoroutine<ResultInfo<Void>> {
                NIMClient.getService(QChatService::class.java).logout().toFetchCallback(callback)
            }
        if (result.success) {
            NIMClient.getService(AuthService::class.java).logout()
            callback?.onSuccess(null)
        } else {
            val code = result.msg?.code ?: errorCode
            val message =
                result.msg?.message ?: (result.msg?.exception?.stackTraceToString() ?: "Error")
            callback?.onError(code, message)
        }
    }
}
