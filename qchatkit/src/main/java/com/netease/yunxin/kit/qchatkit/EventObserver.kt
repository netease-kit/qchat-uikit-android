/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit

import com.netease.nimlib.sdk.Observer
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * wrap the Observer of NIM
 */
abstract class EventObserver<T> {

    private var innerObserver: Observer<*>? = null

    abstract fun onEvent(event: T?)

    fun <M> getObserverInner(convert: ((M?) -> T?)? = null): Observer<M> {
        @Suppress("UNCHECKED_CAST")
        return (innerObserver as? Observer<M>) ?: Observer<M> { event ->
            onEvent(
                convert?.invoke(
                    event
                )
            )
        }.apply {
            innerObserver = this
        }
    }

    fun <M> getObserverInnerSuspend(
        convertCoroutineContext: CoroutineContext = Dispatchers.Main,
        notifyCoroutineContext: CoroutineContext = Dispatchers.Main,
        convert: (suspend (M?) -> T?)? = null
    ): Observer<M> {
        @Suppress("UNCHECKED_CAST")
        return (innerObserver as? Observer<M>) ?: Observer<M> { event ->
            CoroutineScope(notifyCoroutineContext).launch {
                val action = async(convertCoroutineContext) {
                    convert?.invoke(
                        event
                    )
                }
                withContext(notifyCoroutineContext) {
                    onEvent(action.await())
                }
            }
        }.apply {
            innerObserver = this
        }
    }
}
