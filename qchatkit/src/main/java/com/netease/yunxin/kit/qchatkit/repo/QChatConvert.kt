/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.repo

import com.netease.nimlib.sdk.qchat.enums.QChatChannelBlackWhiteType
import com.netease.nimlib.sdk.qchat.enums.QChatChannelMode
import com.netease.nimlib.sdk.qchat.enums.QChatChannelType
import com.netease.nimlib.sdk.qchat.enums.QChatRoleOption
import com.netease.nimlib.sdk.qchat.enums.QChatRoleResource
import com.netease.nimlib.sdk.qchat.param.QChatSendMessageParam
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelMemberType
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelModeEnum
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelTypeEnum
import com.netease.yunxin.kit.qchatkit.repo.model.QChatRoleOptionEnum
import com.netease.yunxin.kit.qchatkit.repo.model.QChatRoleResourceEnum
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSendMessageInfo

object QChatConvert {

    fun convertAuth(auth: MutableMap<QChatRoleResource, QChatRoleOption>?): Map<QChatRoleResourceEnum, QChatRoleOptionEnum> {
        val result = mutableMapOf<QChatRoleResourceEnum, QChatRoleOptionEnum>()
        auth?.map {
            result.put(convertToRoleResourceEnum(it.key), convertToRoleOptionEnum(it.value))
        }
        return result
    }

    private fun convertToRoleResourceEnum(res: QChatRoleResource): QChatRoleResourceEnum {
        return when (res.value()) {
            1 -> QChatRoleResourceEnum.MANAGE_SERVER
            2 -> QChatRoleResourceEnum.MANAGE_CHANNEL
            3 -> QChatRoleResourceEnum.MANAGE_ROLE
            4 -> QChatRoleResourceEnum.SEND_MSG
            5 -> QChatRoleResourceEnum.ACCOUNT_INFO_SELF
            6 -> QChatRoleResourceEnum.INVITE_SERVER
            7 -> QChatRoleResourceEnum.KICK_SERVER
            8 -> QChatRoleResourceEnum.ACCOUNT_INFO_OTHER
            9 -> QChatRoleResourceEnum.RECALL_MSG
            10 -> QChatRoleResourceEnum.DELETE_MSG
            11 -> QChatRoleResourceEnum.REMIND_OTHER
            12 -> QChatRoleResourceEnum.REMIND_EVERYONE
            13 -> QChatRoleResourceEnum.MANAGE_BLACK_WHITE_LIST
            else -> QChatRoleResourceEnum.NONE
        }
    }

    fun convertToRoleResource(res: QChatRoleResourceEnum): QChatRoleResource {
        return QChatRoleResource.getByValue(res.ordinal)
    }

    fun convertToRoleOption(res: QChatRoleOptionEnum): QChatRoleOption {
        return when (res) {
            QChatRoleOptionEnum.ALLOW -> QChatRoleOption.ALLOW
            QChatRoleOptionEnum.DENY -> QChatRoleOption.DENY
            QChatRoleOptionEnum.INHERIT -> QChatRoleOption.INHERIT
        }
    }

    private fun convertToRoleOptionEnum(res: QChatRoleOption): QChatRoleOptionEnum {
        return when (res.value) {
            1 -> QChatRoleOptionEnum.ALLOW
            -1 -> QChatRoleOptionEnum.DENY
            0 -> QChatRoleOptionEnum.INHERIT
            else -> QChatRoleOptionEnum.INHERIT
        }
    }

    fun roleOptionEnum(value: Int): QChatRoleOptionEnum {
        return when (value) {
            0 -> QChatRoleOptionEnum.ALLOW
            1 -> QChatRoleOptionEnum.DENY
            2 -> QChatRoleOptionEnum.INHERIT
            else -> QChatRoleOptionEnum.INHERIT
        }
    }

    fun convertToMemberType(value: QChatChannelMemberType): QChatChannelBlackWhiteType {
        return when (value) {
            QChatChannelMemberType.BLACK -> QChatChannelBlackWhiteType.BLACK
            QChatChannelMemberType.WHITE -> QChatChannelBlackWhiteType.WHITE
        }
    }

    fun convertToChannelMode(value: QChatChannelModeEnum): QChatChannelMode {
        return when (value) {
            QChatChannelModeEnum.Public -> QChatChannelMode.PUBLIC
            QChatChannelModeEnum.Private -> QChatChannelMode.PRIVATE
        }
    }

    fun convertToChannelType(value: QChatChannelTypeEnum): QChatChannelType {
        return when (value) {
            QChatChannelTypeEnum.Message -> QChatChannelType.MessageChannel
            QChatChannelTypeEnum.Custom -> QChatChannelType.CustomChannel
            QChatChannelTypeEnum.Rtc -> QChatChannelType.RTCChannel
        }
    }

    fun convertToSendMessage(msg: QChatSendMessageInfo): QChatSendMessageParam {
        val param = QChatSendMessageParam(
            msg.serverId,
            msg.channelId,
            msg.type
        )
        param.body = msg.body
        param.attach = msg.attach
        param.extension = msg.extension
        param.pushPayload = msg.pushPayload
        param.mentionedAccidList = msg.mentionedAccidList
        param.isMentionedAll = msg.mentionedAll
        param.isHistoryEnable = msg.historyEnable
        param.isPushEnable = msg.pushEnable
        param.isNeedBadge = msg.needBadge
        param.isNeedPushNick = msg.needPushNick
        param.serverStatus = msg.serverStatus
        return param
    }
}
