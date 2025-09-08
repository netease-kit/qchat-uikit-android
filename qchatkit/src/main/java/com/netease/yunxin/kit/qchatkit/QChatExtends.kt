/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

@file:JvmName("ProviderExtends")

package com.netease.yunxin.kit.qchatkit

import com.netease.nimlib.sdk.AbortableFuture
import com.netease.nimlib.sdk.InvocationFuture
import com.netease.nimlib.sdk.RequestCallback
import com.netease.yunxin.kit.alog.ALog
import com.netease.yunxin.kit.corekit.im2.extend.FetchCallback
import com.netease.yunxin.kit.corekit.model.ErrorMsg
import com.netease.yunxin.kit.corekit.model.ResultInfo
import com.netease.yunxin.kit.corekit.model.ResultObserver
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun <T> AbortableFuture<T>.onResult(
    continuation: Continuation<ResultInfo<T>>,
    flag: String? = null
) {
    this.setCallback(object : RequestCallback<T> {
        override fun onSuccess(param: T) {
            ALog.d("ChatKit", "onResult", "success")
            continuation.resume(ResultInfo(param))
        }

        override fun onFailed(code: Int) {
            ALog.d("ChatKit", "onResult", "onFailed$code")
            continuation.resume(
                ResultInfo(
                    success = false,
                    msg = ErrorMsg(code)
                )
            )
        }

        override fun onException(exception: Throwable?) {
            ALog.d("ChatKit", "onResult", "onException${exception?.message}")
            continuation.resume(
                ResultInfo(
                    success = false,
                    msg = ErrorMsg(
                        -1,
                        "$flag-${exception?.stackTraceToString()}",
                        exception
                    )
                )
            )
        }
    })
}

fun <T> InvocationFuture<T>.onResult(
    continuation: Continuation<ResultInfo<T>>,
    flag: String? = null
) {
    this.setCallback(object : RequestCallback<T> {
        override fun onSuccess(param: T) {
            ALog.d("ChatKit", "onResult", "success")
            continuation.resume(ResultInfo(param))
        }

        override fun onFailed(code: Int) {
            ALog.d("ChatKit", "onResult", "onFailed$code")
            continuation.resume(
                ResultInfo(
                    success = false,
                    msg = ErrorMsg(code)
                )
            )
        }

        override fun onException(exception: Throwable?) {
            ALog.d("ChatKit", "onResult", "onException${exception?.message}")
            continuation.resume(
                ResultInfo(
                    success = false,
                    msg = ErrorMsg(
                        -1,
                        "$flag-${exception?.stackTraceToString()}",
                        exception
                    )
                )
            )
        }
    })
}

suspend fun <Source, Dest> ResultInfo<Source>.toInform(
    callback: FetchCallback<Dest>?,
    convert: suspend (Source?) -> Dest?
) {
    callback ?: return
    when {
        success -> {
            ALog.d("ChatKit", "toInform", "success")
            callback.onSuccess(convert.invoke(value))
        }
        msg?.exception == null -> {
            ALog.d("ChatKit", "toInform", "onFailed${msg?.code ?: -1}")
            callback.onError(msg?.code ?: -1, msg?.exception?.message ?: "")
        }
        else -> {
            ALog.d("ChatKit", "toInform", "onException${msg?.exception?.message}")
            callback.onError(msg?.code ?: -1, msg?.exception?.message ?: "")
        }
    }
}

suspend fun <Source, Dest> ResultInfo<Source>.toDispatchInform(
    callback: FetchCallback<Dest>?,
    convertDispatcher: CoroutineContext = Dispatchers.IO,
    callbackDispatcher: CoroutineContext = Dispatchers.Main,
    convert: suspend (Source?) -> Dest?
) {
    callback ?: return
    when {
        success -> {
            ALog.d("ChatKit", "toInform", "success")
            withContext(convertDispatcher) {
                val result = convert.invoke(value)
                withContext(callbackDispatcher) {
                    callback.onSuccess(result)
                }
            }
        }
        msg?.exception == null -> {
            ALog.d("ChatKit", "toInform", "onFailed${msg?.code ?: -1}")
            withContext(callbackDispatcher) {
                callback.onError(msg?.code ?: -1, msg?.exception?.message ?: "")
            }
        }
        else -> {
            ALog.d("ChatKit", "toInform", "onException${msg?.exception?.message}")
            withContext(callbackDispatcher) {
                callback.onError(msg?.code ?: -1, msg?.exception?.message ?: "")
            }
        }
    }
}

fun <T, R> ResultObserver<R>.toFetchCallback(convert: ((T?) -> R?)): FetchCallback<T> {
    return object : FetchCallback<T> {
        override fun onSuccess(param: T?) {
            ALog.d("ChatKit", "FetchCallback", "success")
            onResult(ResultInfo(convert(param), true))
        }

        override fun onError(errorCode: Int, errorMsg: String?) {
            ALog.d("ChatKit", "FetchCallback", "onError$errorCode")
            onResult(ResultInfo(success = false, msg = ErrorMsg(errorCode, errorMsg)))
        }
    }
}
