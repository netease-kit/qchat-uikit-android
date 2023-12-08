/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.repo.model

import com.netease.nimlib.sdk.msg.constant.MsgTypeEnum

data class QChatSendMessageInfo(
    val serverId: Long,
    val channelId: Long,
    val type: MsgTypeEnum,
    val body: String?,
    val uuid: String?,
    val attach: String?
) {
    var messageInfo: QChatMessageInfo? = null
    var extension: MutableMap<String, Any>? = null
    var pushPayload: MutableMap<String, Any>? = null
    var mentionedAccidList: List<String>? = null
    var mentionedAll = false
    var historyEnable = true
    var pushEnable = true
    var needBadge = true
    var needPushNick = true
    var serverStatus: Int? = null

    constructor(serverId: Long, channelId: Long, type: MsgTypeEnum, content: String) : this(
        serverId,
        channelId,
        type,
        content,
        null,
        null
    )
}
