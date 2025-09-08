/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

@file:JvmName("RepoExtends")

package com.netease.yunxin.kit.qchatkit.repo

import com.netease.nimlib.sdk.Observer
import com.netease.nimlib.sdk.qchat.enums.QChatChannelMode
import com.netease.nimlib.sdk.qchat.enums.QChatChannelType
import com.netease.nimlib.sdk.qchat.enums.QChatSubscribeType
import com.netease.nimlib.sdk.qchat.enums.QChatSystemMessageToType
import com.netease.nimlib.sdk.qchat.enums.QChatSystemNotificationType
import com.netease.nimlib.sdk.qchat.event.QChatMessageDeleteEvent
import com.netease.nimlib.sdk.qchat.event.QChatMessageRevokeEvent
import com.netease.nimlib.sdk.qchat.event.QChatMessageUpdateEvent
import com.netease.nimlib.sdk.qchat.event.QChatServerUnreadInfoChangedEvent
import com.netease.nimlib.sdk.qchat.event.QChatUnreadInfoChangedEvent
import com.netease.nimlib.sdk.qchat.model.QChatChannel
import com.netease.nimlib.sdk.qchat.model.QChatChannelIdInfo
import com.netease.nimlib.sdk.qchat.model.QChatChannelRole
import com.netease.nimlib.sdk.qchat.model.QChatMemberRole
import com.netease.nimlib.sdk.qchat.model.QChatMessage
import com.netease.nimlib.sdk.qchat.model.QChatMessageQuickCommentDetail
import com.netease.nimlib.sdk.qchat.model.QChatMsgUpdateInfo
import com.netease.nimlib.sdk.qchat.model.QChatQuickCommentDetail
import com.netease.nimlib.sdk.qchat.model.QChatServer
import com.netease.nimlib.sdk.qchat.model.QChatServerMember
import com.netease.nimlib.sdk.qchat.model.QChatServerRole
import com.netease.nimlib.sdk.qchat.model.QChatServerRoleMember
import com.netease.nimlib.sdk.qchat.model.QChatServerUnreadInfo
import com.netease.nimlib.sdk.qchat.model.QChatSystemNotification
import com.netease.nimlib.sdk.qchat.model.QChatUnreadInfo
import com.netease.nimlib.sdk.qchat.result.QChatGetQuickCommentsResult
import com.netease.yunxin.kit.corekit.model.ResultInfo
import com.netease.yunxin.kit.corekit.model.ResultObserver
import com.netease.yunxin.kit.qchatkit.repo.model.AccIds
import com.netease.yunxin.kit.qchatkit.repo.model.Channel
import com.netease.yunxin.kit.qchatkit.repo.model.ChannelAccIds
import com.netease.yunxin.kit.qchatkit.repo.model.ChannelCategoryCreate
import com.netease.yunxin.kit.qchatkit.repo.model.ChannelCategoryRemove
import com.netease.yunxin.kit.qchatkit.repo.model.ChannelCategoryUpdate
import com.netease.yunxin.kit.qchatkit.repo.model.ChannelCategoryUpdateWhiteBlackMember
import com.netease.yunxin.kit.qchatkit.repo.model.ChannelCategoryUpdateWhiteBlackRole
import com.netease.yunxin.kit.qchatkit.repo.model.ChannelCreate
import com.netease.yunxin.kit.qchatkit.repo.model.ChannelMsg
import com.netease.yunxin.kit.qchatkit.repo.model.ChannelMsgTyping
import com.netease.yunxin.kit.qchatkit.repo.model.ChannelMsgUnreadCount
import com.netease.yunxin.kit.qchatkit.repo.model.ChannelMsgUnreadStatus
import com.netease.yunxin.kit.qchatkit.repo.model.ChannelRemove
import com.netease.yunxin.kit.qchatkit.repo.model.ChannelRoleAuthUpdate
import com.netease.yunxin.kit.qchatkit.repo.model.ChannelUpdate
import com.netease.yunxin.kit.qchatkit.repo.model.ChannelUpdateQuickComment
import com.netease.yunxin.kit.qchatkit.repo.model.ChannelUpdateWhiteBlackRole
import com.netease.yunxin.kit.qchatkit.repo.model.ChannelUpdateWhiteBlackRoleMember
import com.netease.yunxin.kit.qchatkit.repo.model.ChannelVisibilityUpdate
import com.netease.yunxin.kit.qchatkit.repo.model.Custom
import com.netease.yunxin.kit.qchatkit.repo.model.MemberRoleAuthUpdate
import com.netease.yunxin.kit.qchatkit.repo.model.MuteORRelease
import com.netease.yunxin.kit.qchatkit.repo.model.MyMemberInfoUpdate
import com.netease.yunxin.kit.qchatkit.repo.model.NextInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatAnnounceMemberInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelInfoWithLastMessage
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelMember
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelModeEnum
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelRoleInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelTypeEnum
import com.netease.yunxin.kit.qchatkit.repo.model.QChatGetQuickCommentsResultInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageDeleteEventInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageQuickCommentDetailInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageRevokeEventInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageUpdateEventInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMsgUpdateInfoItem
import com.netease.yunxin.kit.qchatkit.repo.model.QChatQuickCommentDetailInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerChannelIdPair
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerMemberInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerMemberWithRoleInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerRoleInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerRoleMemberInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerUnreadInfoChangedEventInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerUnreadInfoItem
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSubscribeTypeInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSystemMessageToTypeInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSystemNotificationInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSystemNotificationTypeInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatUnreadInfoChangedEventInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatUnreadInfoItem
import com.netease.yunxin.kit.qchatkit.repo.model.Server
import com.netease.yunxin.kit.qchatkit.repo.model.ServerAccIds
import com.netease.yunxin.kit.qchatkit.repo.model.ServerCreate
import com.netease.yunxin.kit.qchatkit.repo.model.ServerEnterLeave
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberApply
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberApplyAccept
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberApplyDone
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberApplyReject
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberInvite
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberInviteAccept
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberInviteDone
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberInviteReject
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberJoinByInviteCode
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberKick
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberLeave
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberUpdate
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMsg
import com.netease.yunxin.kit.qchatkit.repo.model.ServerRemove
import com.netease.yunxin.kit.qchatkit.repo.model.ServerRoleAuthUpdate
import com.netease.yunxin.kit.qchatkit.repo.model.ServerRoleMemberAdd
import com.netease.yunxin.kit.qchatkit.repo.model.ServerRoleMemberDelete
import com.netease.yunxin.kit.qchatkit.repo.model.ServerUpdate
import com.netease.yunxin.kit.qchatkit.repo.model.VisitorChannelVisibilityUpdate

fun QChatServer.toInfo(nextInfo: NextInfo? = null): QChatServerInfo {
    return QChatServerInfo(
        serverId,
        name,
        icon,
        owner,
        memberNumber,
        inviteMode.value,
        applyMode.value,
        isValid,
        createTime,
        updateTime,
        channelNum,
        custom,
        searchType,
        nextInfo
    )
}

fun QChatChannel.toInfo(nextInfo: NextInfo? = null): QChatChannelInfo {
    return QChatChannelInfo(
        channelId,
        serverId,
        name,
        topic,
        type.toInfo(),
        isValid,
        createTime,
        updateTime,
        owner,
        viewMode.toInfo(),
        custom,
        nextInfo
    )
}

fun QChatChannel.toInfoWithLastMessage(
    nextInfo: NextInfo? = null,
    lastMessage: QChatMessage? = null
): QChatChannelInfoWithLastMessage {
    return QChatChannelInfoWithLastMessage(
        QChatChannelInfo(
            channelId,
            serverId,
            name,
            topic,
            type.toInfo(),
            isValid,
            createTime,
            updateTime,
            owner,
            viewMode.toInfo(),
            custom,
            nextInfo
        ),
        lastMessage = lastMessage?.toInfo(),
        nextInfo = nextInfo
    )
}

fun QChatChannelMode.toInfo(): QChatChannelModeEnum {
    return when (this) {
        QChatChannelMode.PUBLIC -> QChatChannelModeEnum.Public
        QChatChannelMode.PRIVATE -> QChatChannelModeEnum.Private
    }
}

fun QChatServerMember.toInfo(
    roleList: List<QChatServerRoleInfo>?,
    nextInfo: NextInfo? = null
): QChatServerMemberWithRoleInfo {
    return QChatServerMemberWithRoleInfo(
        serverId,
        accid,
        nick,
        avatar,
        type.value,
        joinTime,
        inviter,
        createTime,
        updateTime,
        isValid,
        custom,
        nextInfo,
        roleList
    )
}

fun QChatServerMember.toAnnounceMemberInfo(
    userType: Int,
    nextInfo: NextInfo? = null
): QChatAnnounceMemberInfo {
    return QChatAnnounceMemberInfo(
        accId = accid,
        nick = nick,
        avatarUrl = avatar,
        userType = userType,
        createTime = createTime,
        joinTime = joinTime,
        nextInfo = nextInfo
    )
}

fun QChatServerRoleMember.toAnnounceMemberInfo(userType: Int, nextInfo: NextInfo? = null): QChatAnnounceMemberInfo {
    return QChatAnnounceMemberInfo(
        accId = accid,
        nick = nick,
        avatarUrl = avatar,
        userType = userType,
        createTime = createTime,
        joinTime = jointime,
        nextInfo = nextInfo
    )
}

fun QChatServerMember.toInfo(nextInfo: NextInfo? = null): QChatServerMemberInfo {
    return QChatServerMemberInfo(
        serverId,
        accid,
        nick,
        avatar,
        type.value,
        joinTime,
        inviter,
        createTime,
        updateTime,
        isValid,
        custom,
        nextInfo = nextInfo
    )
}

fun QChatServerRoleMember.toInfo(): QChatServerRoleMemberInfo {
    return QChatServerRoleMemberInfo(
        serverId,
        accid,
        nick,
        avatar,
        type.value,
        jointime,
        inviter,
        createTime,
        updateTime,
        custom
    )
}

fun QChatServerMember.toMember(): QChatServerRoleMemberInfo {
    return QChatServerRoleMemberInfo(
        serverId,
        accid,
        nick,
        avatar,
        type.value,
        joinTime,
        inviter,
        createTime,
        updateTime,
        custom
    )
}

fun QChatChannelRole.toInfo(): QChatChannelRoleInfo {
    return QChatChannelRoleInfo(
        serverId,
        roleId,
        parentRoleId,
        channelId,
        name,
        icon,
        ext,
        QChatConvert.convertAuth(resourceAuths),
        type.value,
        createTime,
        updateTime
    )
}

fun QChatServerRole.toInfo(): QChatServerRoleInfo {
    return QChatServerRoleInfo(
        serverId,
        roleId,
        name,
        type.value,
        icon,
        QChatConvert.convertAuth(resourceAuths),
        extension,
        priority,
        memberCount,
        createTime
    )
}

fun QChatMemberRole.toInfo(): QChatChannelMember {
    return QChatChannelMember(
        serverId,
        id,
        accid,
        channelId,
        QChatConvert.convertAuth(resourceAuths),
        nick,
        avatar,
        type.value,
        jointime,
        inviter,
        createTime,
        updateTime,
        custom
    )
}

fun QChatSystemNotificationType.toInfo(): QChatSystemNotificationTypeInfo {
    return when (this) {
        QChatSystemNotificationType.SERVER_MEMBER_INVITE -> ServerMemberInvite
        QChatSystemNotificationType.SERVER_MEMBER_INVITE_REJECT -> ServerMemberInviteReject
        QChatSystemNotificationType.SERVER_MEMBER_APPLY -> ServerMemberApply
        QChatSystemNotificationType.SERVER_MEMBER_APPLY_REJECT -> ServerMemberApplyReject
        QChatSystemNotificationType.SERVER_CREATE -> ServerCreate
        QChatSystemNotificationType.SERVER_REMOVE -> ServerRemove
        QChatSystemNotificationType.SERVER_UPDATE -> ServerUpdate
        QChatSystemNotificationType.SERVER_MEMBER_INVITE_DONE -> ServerMemberInviteDone
        QChatSystemNotificationType.SERVER_MEMBER_INVITE_ACCEPT -> ServerMemberInviteAccept
        QChatSystemNotificationType.SERVER_MEMBER_APPLY_DONE -> ServerMemberApplyDone
        QChatSystemNotificationType.SERVER_MEMBER_APPLY_ACCEPT -> ServerMemberApplyAccept
        QChatSystemNotificationType.SERVER_MEMBER_KICK -> ServerMemberKick
        QChatSystemNotificationType.SERVER_MEMBER_LEAVE -> ServerMemberLeave
        QChatSystemNotificationType.SERVER_MEMBER_UPDATE -> ServerMemberUpdate
        QChatSystemNotificationType.CHANNEL_CREATE -> ChannelCreate
        QChatSystemNotificationType.CHANNEL_REMOVE -> ChannelRemove
        QChatSystemNotificationType.CHANNEL_UPDATE -> ChannelUpdate
        QChatSystemNotificationType.CHANNEL_UPDATE_WHITE_BLACK_ROLE -> ChannelUpdateWhiteBlackRole
        QChatSystemNotificationType.CHANNEL_UPDATE_WHITE_BLACK_MEMBER -> ChannelUpdateWhiteBlackRoleMember
        QChatSystemNotificationType.UPDATE_QUICK_COMMENT -> ChannelUpdateQuickComment
        QChatSystemNotificationType.CHANNEL_CATEGORY_CREATE -> ChannelCategoryCreate
        QChatSystemNotificationType.CHANNEL_CATEGORY_REMOVE -> ChannelCategoryRemove
        QChatSystemNotificationType.CHANNEL_CATEGORY_UPDATE -> ChannelCategoryUpdate
        QChatSystemNotificationType.CHANNEL_CATEGORY_UPDATE_WHITE_BLACK_ROLE -> ChannelCategoryUpdateWhiteBlackRole
        QChatSystemNotificationType.CHANNEL_CATEGORY_UPDATE_WHITE_BLACK_MEMBER -> ChannelCategoryUpdateWhiteBlackMember
        QChatSystemNotificationType.SERVER_ROLE_MEMBER_ADD -> ServerRoleMemberAdd
        QChatSystemNotificationType.SERVER_ROLE_MEMBER_DELETE -> ServerRoleMemberDelete
        QChatSystemNotificationType.SERVER_ROLE_AUTH_UPDATE -> ServerRoleAuthUpdate
        QChatSystemNotificationType.CHANNEL_ROLE_AUTH_UPDATE -> ChannelRoleAuthUpdate
        QChatSystemNotificationType.MEMBER_ROLE_AUTH_UPDATE -> MemberRoleAuthUpdate
        QChatSystemNotificationType.CHANNEL_VISIBILITY_UPDATE -> ChannelVisibilityUpdate
        QChatSystemNotificationType.SERVER_ENTER_LEAVE -> ServerEnterLeave
        QChatSystemNotificationType.SERVER_MEMBER_JOIN_BY_INVITE_CODE -> ServerMemberJoinByInviteCode
        QChatSystemNotificationType.VISITOR_CHANNEL_VISIBILITY_UPDATE -> VisitorChannelVisibilityUpdate
        QChatSystemNotificationType.MY_MEMBER_INFO_UPDATED -> MyMemberInfoUpdate
        QChatSystemNotificationType.CUSTOM -> Custom
        QChatSystemNotificationType.MUTE_OR_RELEASE -> MuteORRelease
    }
}

fun QChatSystemMessageToType.toInfo(): QChatSystemMessageToTypeInfo {
    return when (this) {
        QChatSystemMessageToType.SERVER -> Server
        QChatSystemMessageToType.CHANNEL -> Channel
        QChatSystemMessageToType.CHANNEL_ACCIDS -> ChannelAccIds
        QChatSystemMessageToType.SERVER_ACCIDS -> ServerAccIds
        QChatSystemMessageToType.ACCIDS -> AccIds
    }
}

fun QChatChannelType.toInfo(): QChatChannelTypeEnum {
    return when (this) {
        QChatChannelType.CustomChannel -> QChatChannelTypeEnum.Custom
        QChatChannelType.MessageChannel -> QChatChannelTypeEnum.Message
        QChatChannelType.RTCChannel -> QChatChannelTypeEnum.Rtc
    }
}

fun QChatSystemNotification.toInfo(): QChatSystemNotificationInfo {
    return QChatSystemNotificationInfo(
        serverId,
        channelId,
        toAccids,
        fromAccount,
        toType?.toInfo(),
        fromClientType,
        fromDeviceId,
        fromNick,
        time,
        updateTime,
        type?.toInfo(),
        msgIdClient,
        msgIdClient,
        body,
        attach,
        attachment,
        extension,
        status,
        pushPayload,
        pushContent,
        isPersistEnable
    )
}

fun QChatServerChannelIdPair.toInfo(): QChatChannelIdInfo {
    return QChatChannelIdInfo(serverId, channelId)
}

fun QChatChannelIdInfo.toInfo(): QChatServerChannelIdPair {
    return QChatServerChannelIdPair(serverId, channelId)
}

fun QChatSubscribeType.toInfo(): QChatSubscribeTypeInfo {
    return when (this) {
        QChatSubscribeType.SERVER_MSG -> ServerMsg
        QChatSubscribeType.CHANNEL_MSG_UNREAD_COUNT -> ChannelMsgUnreadCount
        QChatSubscribeType.CHANNEL_MSG_UNREAD_STATUS -> ChannelMsgUnreadStatus
        QChatSubscribeType.CHANNEL_MSG -> ChannelMsg
        QChatSubscribeType.CHANNEL_MSG_TYPING -> ChannelMsgTyping
    }
}

fun QChatSubscribeTypeInfo.toParam(): QChatSubscribeType {
    return when (this) {
        ServerMsg -> QChatSubscribeType.SERVER_MSG
        ChannelMsgUnreadCount -> QChatSubscribeType.CHANNEL_MSG_UNREAD_COUNT
        ChannelMsgUnreadStatus -> QChatSubscribeType.CHANNEL_MSG_UNREAD_STATUS
        ChannelMsg -> QChatSubscribeType.CHANNEL_MSG
        ChannelMsgTyping -> QChatSubscribeType.CHANNEL_MSG_TYPING
    }
}

fun QChatUnreadInfo.toItem(): QChatUnreadInfoItem {
    return QChatUnreadInfoItem(
        serverId,
        channelId,
        ackTimeTag,
        unreadCount,
        mentionedCount,
        maxCount,
        lastMsgTime
    )
}

fun QChatMessage.toInfo(): QChatMessageInfo {
    return QChatMessageInfo(this)
}

fun QChatMsgUpdateInfo.toItem(): QChatMsgUpdateInfoItem {
    return QChatMsgUpdateInfoItem(
        operatorAccount,
        operatorClientType,
        msg,
        ext,
        pushContent,
        pushPayload
    )
}

fun QChatMessageUpdateEvent.toInfo(): QChatMessageUpdateEventInfo {
    return QChatMessageUpdateEventInfo(msgUpdateInfo?.toItem(), message?.toInfo())
}

fun QChatMessageDeleteEvent.toInfo(): QChatMessageDeleteEventInfo {
    return QChatMessageDeleteEventInfo(msgUpdateInfo?.toItem(), message?.toInfo())
}

fun QChatMessageRevokeEvent.toInfo(): QChatMessageRevokeEventInfo {
    return QChatMessageRevokeEventInfo(msgUpdateInfo?.toItem(), message?.toInfo())
}

fun QChatUnreadInfoChangedEvent.toInfo(): QChatUnreadInfoChangedEventInfo {
    return QChatUnreadInfoChangedEventInfo(
        unreadInfos?.map {
            it.toItem()
        },
        lastUnreadInfos?.map { it.toItem() }
    )
}

fun QChatServerUnreadInfo.toItem(): QChatServerUnreadInfoItem {
    return QChatServerUnreadInfoItem(serverId, unreadCount, mentionedCount, maxCount)
}

fun QChatServerUnreadInfoChangedEvent.toInfo(): QChatServerUnreadInfoChangedEventInfo {
    return QChatServerUnreadInfoChangedEventInfo(
        serverUnreadInfos?.map {
            it.toItem()
        }
    )
}

fun QChatQuickCommentDetail.toInfo(): QChatQuickCommentDetailInfo {
    return QChatQuickCommentDetailInfo(
        type,
        count,
        hasSelf(),
        createTime,
        severalAccids
    )
}

fun QChatMessageQuickCommentDetail.toInfo(): QChatMessageQuickCommentDetailInfo {
    return QChatMessageQuickCommentDetailInfo(
        serverId,
        channelId,
        msgIdServer,
        totalCount,
        lastUpdateTime,
        details?.map {
            it.toInfo()
        }
    )
}
fun QChatGetQuickCommentsResult.toInfo(): QChatGetQuickCommentsResultInfo {
    return QChatGetQuickCommentsResultInfo(
        messageQuickCommentDetailMap?.mapValues {
            it.value.toInfo()
        }
    )
}
fun <Source, Dest> ResultObserver<Dest>.toObserver(convert: (Source?) -> Dest?): Observer<Source> {
    return Observer<Source> { event -> onResult(ResultInfo(convert(event))) }
}
