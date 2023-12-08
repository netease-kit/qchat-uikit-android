/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.repo.model

class QChatChannelInfo(
    val channelId: Long,
    val serverId: Long,
    val name: String,
    val topic: String?,
    val type: QChatChannelTypeEnum,
    val valid: Boolean,
    val createTime: Long,
    val updateTime: Long,
    val owner: String,
    val viewMode: QChatChannelModeEnum,
    val custom: String? = null,
    val nextInfo: NextInfo? = null
) {
    override fun toString(): String {
        return "QChatChannelInfo(channelId=$channelId, serverId=$serverId, name='$name', topic=$topic, type=$type, valid=$valid, createTime=$createTime, updateTime=$updateTime, owner='$owner', viewMode=$viewMode, custom=$custom)"
    }
}
