/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit

import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 当断网时，接收方收到的消息时间戳可能不按照顺序，因此导致消息乱序，设置已读错误；
 * 将给定时间[millisInFuture]内的消息数据累积合并成一个新消息列表；
 */
class TimerCacheWithQChatMsg {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var currentJob: Job? = null
    private val cache = mutableListOf<QChatMessageInfo>()
    private var millisInFuture = 0L
    private var notify: ((List<QChatMessageInfo>) -> Unit)? = null

    fun init(millisInFuture: Long, notify: ((List<QChatMessageInfo>) -> Unit)?) {
        this.millisInFuture = millisInFuture
        this.notify = notify
    }

    fun handle(msgList: List<QChatMessageInfo>?) {
        if (msgList?.isNotEmpty() == true) {
            cache.addAll(msgList)
        }
        if (currentJob?.isCompleted == false) {
            return
        }
        currentJob = coroutineScope.launch {
            delay(millisInFuture)
            withContext(Dispatchers.Main) {
                notify?.invoke(cache)
                cache.clear()
            }
        }
    }

    fun unInit() {
        currentJob?.cancel()
        this.notify = null
    }
}
