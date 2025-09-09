/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.repo

import com.netease.yunxin.kit.qchatkit.EventObserver
import com.netease.yunxin.kit.qchatkit.provider.QChatServiceObserverProvider
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageDeleteEventInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageRevokeEventInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageUpdateEventInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerUnreadInfoChangedEventInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSystemNotificationInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSystemNotificationTypeInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatUnreadInfoChangedEventInfo

/**
 * qchat observer repo
 */
object QChatServiceObserverRepo {

    /**
     * observer system notification
     */
    @JvmStatic
    fun observerSystemNotification(
        observer: EventObserver<List<QChatSystemNotificationInfo?>>,
        register: Boolean
    ) {
        QChatServiceObserverProvider.observeReceiveSystemNotification(
            if (register) {
                observer.getObserverInner {
                    it?.map { item ->
                        item?.toInfo()
                    }
                }
            } else {
                observer.getObserverInner()
            },
            register
        )
    }

    /**
     * observer system notification with type
     */
    @JvmStatic
    fun observerSystemNotificationWithType(
        observer: EventObserver<List<QChatSystemNotificationInfo?>>,
        typeList: List<QChatSystemNotificationTypeInfo>
    ) {
        QChatServiceObserverProvider.observeReceiveSystemNotification(
            observer.getObserverInner {
                it?.asSequence()?.map { item ->
                    item?.toInfo()
                }?.filter { item ->
                    typeList.contains(item?.type)
                }?.toList()
            },
            true
        )
    }

    /**
     * observer receive message
     */
    @JvmStatic
    fun observeReceiveMessage(observer: EventObserver<List<QChatMessageInfo>>, register: Boolean) {
        QChatServiceObserverProvider.observeReceiveMessage(
            if (register) {
                observer.getObserverInner {
                    it?.map { item ->
                        item.toInfo()
                    }
                }
            } else {
                observer.getObserverInner()
            },
            register
        )
    }

    /**
     * observer receive message with server id and channel id
     */
    @JvmStatic
    fun observeReceiveMessage(
        serverId: Long,
        channelId: Long,
        observer: EventObserver<List<QChatMessageInfo>>,
        register: Boolean
    ) {
        QChatServiceObserverProvider.observeReceiveMessage(
            if (register) {
                observer.getObserverInner {
                    it?.filter { item ->
                        item.qChatServerId == serverId && item.qChatChannelId == channelId
                    }?.map { item ->
                        item.toInfo()
                    }
                }
            } else {
                observer.getObserverInner()
            },
            register
        )
    }

    /**
     * observe message update notification
     */
    @JvmStatic
    fun observeMessageUpdate(
        observer: EventObserver<QChatMessageUpdateEventInfo>,
        register: Boolean
    ) {
        QChatServiceObserverProvider.observeMessageUpdate(
            if (register) {
                observer.getObserverInner {
                    it?.toInfo()
                }
            } else {
                observer.getObserverInner()
            },
            register
        )
    }

    /**
     * observe message revoke notification
     */
    @JvmStatic
    fun observeMessageRevoke(
        observer: EventObserver<QChatMessageRevokeEventInfo>,
        register: Boolean
    ) {
        QChatServiceObserverProvider.observeMessageRevoke(
            if (register) {
                observer.getObserverInner {
                    it?.toInfo()
                }
            } else {
                observer.getObserverInner()
            },
            register
        )
    }

    /**
     * observe message delete notification
     */
    @JvmStatic
    fun observeMessageDelete(
        observer: EventObserver<QChatMessageDeleteEventInfo>,
        register: Boolean
    ) {
        QChatServiceObserverProvider.observeMessageDelete(
            if (register) {
                observer.getObserverInner {
                    it?.toInfo()
                }
            } else {
                observer.getObserverInner()
            },
            register
        )
    }

    /**
     * observe message unread change notification
     */
    @JvmStatic
    fun observeUnreadInfoChanged(
        observer: EventObserver<QChatUnreadInfoChangedEventInfo>,
        register: Boolean
    ) {
        QChatServiceObserverProvider.observeUnreadInfoChanged(
            if (register) {
                observer.getObserverInner {
                    it?.toInfo()
                }
            } else {
                observer.getObserverInner()
            },
            register
        )
    }

    /**
     * observe message status change notification
     */
    @JvmStatic
    fun observeMessageStatusChange(observer: EventObserver<QChatMessageInfo>, register: Boolean) {
        QChatServiceObserverProvider.observeMessageStatusChange(
            if (register) {
                observer.getObserverInner {
                    it?.toInfo()
                } } else {
                observer.getObserverInner()
            },
            register
        )
    }

    /**
     * observe message unread change notification for server
     */
    @JvmStatic
    fun observeServerUnreadInfoChanged(
        observer: EventObserver<QChatServerUnreadInfoChangedEventInfo>,
        register: Boolean
    ) {
        QChatServiceObserverProvider.observeServerUnreadInfoChanged(
            if (register) {
                observer.getObserverInner {
                    it?.toInfo()
                }
            } else {
                observer.getObserverInner()
            },
            register
        )
    }
}
