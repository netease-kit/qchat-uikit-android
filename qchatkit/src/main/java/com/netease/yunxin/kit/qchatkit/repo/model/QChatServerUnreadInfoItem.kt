/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.repo.model

class QChatServerUnreadInfoItem(
    val serverId: Long,
    val unreadCount: Int,
    val mentionedCount: Int,
    val maxCount: Int
) {
    override fun toString(): String {
        return "QChatServerUnreadInfoItem(serverId=$serverId, unreadCount=$unreadCount, mentionedCount=$mentionedCount, maxCount=$maxCount)"
    }
}
