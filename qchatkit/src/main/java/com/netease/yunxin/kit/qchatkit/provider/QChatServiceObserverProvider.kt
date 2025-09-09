/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.provider

import com.netease.nimlib.sdk.NIMClient
import com.netease.nimlib.sdk.Observer
import com.netease.nimlib.sdk.qchat.QChatServiceObserver
import com.netease.nimlib.sdk.qchat.event.QChatMessageDeleteEvent
import com.netease.nimlib.sdk.qchat.event.QChatMessageRevokeEvent
import com.netease.nimlib.sdk.qchat.event.QChatMessageUpdateEvent
import com.netease.nimlib.sdk.qchat.event.QChatServerUnreadInfoChangedEvent
import com.netease.nimlib.sdk.qchat.event.QChatUnreadInfoChangedEvent
import com.netease.nimlib.sdk.qchat.model.QChatMessage
import com.netease.nimlib.sdk.qchat.model.QChatSystemNotification

object QChatServiceObserverProvider {
    private val qChatServiceObserver =
        NIMClient.getService(QChatServiceObserver::class.java)

    @JvmStatic
    fun observeReceiveSystemNotification(
        observer: Observer<List<QChatSystemNotification?>>,
        register: Boolean
    ) {
        qChatServiceObserver.observeReceiveSystemNotification(observer, register)
    }

    @JvmStatic
    fun observeReceiveMessage(observer: Observer<List<QChatMessage>>?, register: Boolean) {
        qChatServiceObserver.observeReceiveMessage(observer, register)
    }

    @JvmStatic
    fun observeMessageUpdate(observer: Observer<QChatMessageUpdateEvent>?, register: Boolean) {
        qChatServiceObserver.observeMessageUpdate(observer, register)
    }

    @JvmStatic
    fun observeMessageRevoke(observer: Observer<QChatMessageRevokeEvent>?, register: Boolean) {
        qChatServiceObserver.observeMessageRevoke(observer, register)
    }

    @JvmStatic
    fun observeMessageDelete(observer: Observer<QChatMessageDeleteEvent?>?, register: Boolean) {
        qChatServiceObserver.observeMessageDelete(observer, register)
    }

    @JvmStatic
    fun observeUnreadInfoChanged(
        observer: Observer<QChatUnreadInfoChangedEvent>?,
        register: Boolean
    ) {
        qChatServiceObserver.observeUnreadInfoChanged(observer, register)
    }

    @JvmStatic
    fun observeMessageStatusChange(observer: Observer<QChatMessage>?, register: Boolean) {
        qChatServiceObserver.observeMessageStatusChange(observer, register)
    }

    @JvmStatic
    fun observeServerUnreadInfoChanged(
        observer: Observer<QChatServerUnreadInfoChangedEvent>?,
        register: Boolean
    ) {
        qChatServiceObserver.observeServerUnreadInfoChanged(observer, register)
    }
}
