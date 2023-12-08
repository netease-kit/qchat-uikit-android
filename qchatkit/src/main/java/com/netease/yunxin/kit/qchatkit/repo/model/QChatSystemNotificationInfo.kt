/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.repo.model

sealed class QChatSystemMessageToTypeInfo
object Server : QChatSystemMessageToTypeInfo()
object Channel : QChatSystemMessageToTypeInfo()
object ServerAccIds : QChatSystemMessageToTypeInfo()
object ChannelAccIds : QChatSystemMessageToTypeInfo()
object AccIds : QChatSystemMessageToTypeInfo()

sealed class QChatSystemNotificationTypeInfo
object ServerMemberInvite : QChatSystemNotificationTypeInfo()
object ServerMemberInviteReject : QChatSystemNotificationTypeInfo()
object ServerMemberApply : QChatSystemNotificationTypeInfo()
object ServerMemberApplyReject : QChatSystemNotificationTypeInfo()
object ServerCreate : QChatSystemNotificationTypeInfo()
object ServerRemove : QChatSystemNotificationTypeInfo()
object ServerUpdate : QChatSystemNotificationTypeInfo()
object ServerMemberInviteDone : QChatSystemNotificationTypeInfo()
object ServerMemberInviteAccept : QChatSystemNotificationTypeInfo()
object ServerMemberApplyDone : QChatSystemNotificationTypeInfo()
object ServerMemberApplyAccept : QChatSystemNotificationTypeInfo()
object ServerMemberKick : QChatSystemNotificationTypeInfo()
object ServerMemberLeave : QChatSystemNotificationTypeInfo()
object ServerMemberUpdate : QChatSystemNotificationTypeInfo()
object ChannelCreate : QChatSystemNotificationTypeInfo()
object ChannelRemove : QChatSystemNotificationTypeInfo()
object ChannelUpdate : QChatSystemNotificationTypeInfo()
object ChannelUpdateWhiteBlackRole : QChatSystemNotificationTypeInfo()
object ChannelUpdateWhiteBlackRoleMember : QChatSystemNotificationTypeInfo()
object ChannelUpdateQuickComment : QChatSystemNotificationTypeInfo()
object ChannelCategoryCreate : QChatSystemNotificationTypeInfo()
object ChannelCategoryRemove : QChatSystemNotificationTypeInfo()
object ChannelCategoryUpdate : QChatSystemNotificationTypeInfo()
object ChannelCategoryUpdateWhiteBlackRole : QChatSystemNotificationTypeInfo()
object ChannelCategoryUpdateWhiteBlackMember : QChatSystemNotificationTypeInfo()
object ServerRoleMemberAdd : QChatSystemNotificationTypeInfo()
object ServerRoleMemberDelete : QChatSystemNotificationTypeInfo()
object ServerRoleAuthUpdate : QChatSystemNotificationTypeInfo()
object ChannelRoleAuthUpdate : QChatSystemNotificationTypeInfo()
object MemberRoleAuthUpdate : QChatSystemNotificationTypeInfo()
object ChannelVisibilityUpdate : QChatSystemNotificationTypeInfo()
object ServerEnterLeave : QChatSystemNotificationTypeInfo()
object ServerMemberJoinByInviteCode : QChatSystemNotificationTypeInfo()
object VisitorChannelVisibilityUpdate : QChatSystemNotificationTypeInfo()
object MyMemberInfoUpdate : QChatSystemNotificationTypeInfo()
object Custom : QChatSystemNotificationTypeInfo()

class QChatSystemNotificationInfo(
    val serverId: Long?,
    val channelId: Long?,
    val toAccIds: List<String>?,
    val fromAccount: String?,
    val toType: QChatSystemMessageToTypeInfo?,
    val fromClientType: Int?,
    val fromDeviceId: String?,
    val fromNick: String?,
    val time: Long?,
    val updateTime: Long?,
    val type: QChatSystemNotificationTypeInfo?,
    val msgIdClient: String?,
    val msgIdServer: String?,
    val body: String?,
    val attach: String?,
    val attachment: Any?,
    val extension: String?,
    val status: Int?,
    val pushPayload: String?,
    val pushContent: String?,
    val isPersistEnable: Boolean?
) {
    override fun toString(): String {
        return "QChatSystemNotificationInfo(serverId=$serverId, channelId=$channelId, toAccIds=$toAccIds, fromAccount=$fromAccount, toType=$toType, fromClientType=$fromClientType, fromDeviceId=$fromDeviceId, fromNick=$fromNick, time=$time, updateTime=$updateTime, type=$type, msgIdClient=$msgIdClient, msgIdServer=$msgIdServer, body=$body, attach=$attach, attachment=$attachment, extension=$extension, status=$status, pushPayload=$pushPayload, pushContent=$pushContent, isPersistEnable=$isPersistEnable)"
    }
}
