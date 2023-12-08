/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.repo.model

import java.io.Serializable

class QChatUnreadInfoItem(
    val serverId: Long,
    val channelId: Long,
    val ackTimeTag: Long,
    val unreadCount: Int,
    val mentionedCount: Int,
    val maxCount: Int,
    val lastMsgTime: Long
) : Serializable {
    override fun toString(): String {
        return "QChatUnreadInfoItem(serverId=$serverId, channelId=$channelId, ackTimeTag=$ackTimeTag, unreadCount=$unreadCount, mentionedCount=$mentionedCount, maxCount=$maxCount, lastMsgTime=$lastMsgTime)"
    }
}
