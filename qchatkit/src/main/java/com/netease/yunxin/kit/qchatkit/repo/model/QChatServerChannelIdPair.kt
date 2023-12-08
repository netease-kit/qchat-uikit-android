/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.repo.model

class QChatServerChannelIdPair(val serverId: Long, val channelId: Long) {
    override fun toString(): String {
        return "QChatServerChannelIdPair(serverId=$serverId, channelId=$channelId)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QChatServerChannelIdPair

        if (serverId != other.serverId) return false
        if (channelId != other.channelId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = serverId.hashCode()
        result = 31 * result + channelId.hashCode()
        return result
    }
}
