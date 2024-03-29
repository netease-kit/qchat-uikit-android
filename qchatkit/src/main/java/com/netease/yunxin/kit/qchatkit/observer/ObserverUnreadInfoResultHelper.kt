/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.observer

import com.netease.nimlib.sdk.StatusCode
import com.netease.yunxin.kit.corekit.im.model.EventObserver
import com.netease.yunxin.kit.corekit.qchat.QChatKitClient
import com.netease.yunxin.kit.qchatkit.repo.QChatServiceObserverRepo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatUnreadInfoChangedEventInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatUnreadInfoItem

/**
 * The helper utils to handle unread info result.
 */
object ObserverUnreadInfoResultHelper {
    /**
     * record unread info, format is [serverId-(channelId-UnreadInfo)]
     */
    private val serverChannelMap = mutableMapOf<Long, MutableMap<Long, QChatUnreadInfoItem>>()
    private var lastAccount: String? = null

    // init for observer
    @JvmStatic
    fun init() {
        // unread info notification register observer
        QChatServiceObserverRepo.observeUnreadInfoChanged(
            object : EventObserver<QChatUnreadInfoChangedEventInfo>() {
                override fun onEvent(event: QChatUnreadInfoChangedEventInfo?) {
                    event?.run {
                        appendUnreadInfoList(event.UnreadInfos)
                    }
                }
            },
            true
        )
        // observer unread notification to update unread info.
        QChatKitClient.getAuthServiceObserver().observeOnlineStatus({ code ->
            val currentAccount = QChatKitClient.account()
            if (code == StatusCode.LOGINED && currentAccount != lastAccount) {
                clear()
                lastAccount = currentAccount
            }
        }, true)
    }

    /**
     * clear all record
     */
    @JvmStatic
    fun clear() {
        serverChannelMap.clear()
    }

    /**
     * clear the record of the specified serverId.
     */
    @JvmStatic
    fun clear(serverId: Long) {
        serverChannelMap[serverId]?.clear()
    }

    /**
     * clear the record of the specified serverId and channelId
     */
    @JvmStatic
    fun clear(serverId: Long, channelId: Long) {
        serverChannelMap[serverId]?.remove(channelId)
    }

    /**
     * get unreadInfo from local record by serverId, channelId.
     */
    @JvmStatic
    fun getUnreadInfoItem(serverId: Long, channelId: Long): QChatUnreadInfoItem? {
        return serverChannelMap[serverId]?.get(channelId)
    }

    /**
     * get unreadInfo from local record by serverId
     */
    @JvmStatic
    fun getUnreadInfoMap(serverId: Long): Map<Long, QChatUnreadInfoItem>? {
        return serverChannelMap[serverId]
    }

    /**
     * The local record has unread msg by serverId.
     */
    @JvmStatic
    fun hasUnreadMsg(serverId: Long): Boolean {
        val map = serverChannelMap[serverId] ?: return false
        for (info in map.values) {
            if (info.unreadCount > 0) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun getTotalUnreadCountForServer(): Int {
        var count = 0
        for (serverId in serverChannelMap.keys) {
            count += getUnreadCountForServer(serverId)
        }
        return count
    }

    /**
     * get unread count for server.
     */
    @JvmStatic
    fun getUnreadCountForServer(serverId: Long): Int {
        val map = serverChannelMap[serverId] ?: return 0
        var count = 0
        for (info in map.values) {
            count += info.unreadCount
        }
        return count
    }

    /**
     * add the list of [QChatUnreadInfoItem] to local map and return the list of serverId.
     */
    @JvmStatic
    fun appendUnreadInfoList(infoList: List<QChatUnreadInfoItem>?): List<Long> {
        return serverChannelMap.append(infoList)
    }

    private fun MutableMap<Long, MutableMap<Long, QChatUnreadInfoItem>>.append(
        list: List<QChatUnreadInfoItem>?
    ): List<Long> {
        list ?: return listOf()
        val result = mutableListOf<Long>()
        list.forEach {
            append(it)
            result.add(it.serverId)
        }
        return result
    }

    private fun MutableMap<Long, MutableMap<Long, QChatUnreadInfoItem>>.append(
        item: QChatUnreadInfoItem
    ) {
        val itemValue = get(item.serverId)
        if (itemValue == null) {
            put(item.serverId, mutableMapOf(item.channelId to item))
        } else {
            itemValue[item.channelId] = item
        }
    }
}
