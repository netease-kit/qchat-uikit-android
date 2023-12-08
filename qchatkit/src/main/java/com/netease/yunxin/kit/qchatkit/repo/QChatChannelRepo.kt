/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.repo

import com.netease.nimlib.sdk.qchat.enums.QChatChannelBlackWhiteOperateType
import com.netease.nimlib.sdk.qchat.enums.QChatChannelBlackWhiteType
import com.netease.nimlib.sdk.qchat.model.QChatChannelIdInfo
import com.netease.nimlib.sdk.qchat.model.QChatMessage
import com.netease.yunxin.kit.alog.ALog
import com.netease.yunxin.kit.corekit.im.provider.FetchCallback
import com.netease.yunxin.kit.corekit.im.utils.toInform
import com.netease.yunxin.kit.corekit.qchat.provider.QChatChannelProvider
import com.netease.yunxin.kit.corekit.qchat.provider.QChatMessageProvider
import com.netease.yunxin.kit.corekit.qchat.provider.QChatRoleProvider
import com.netease.yunxin.kit.qchatkit.repo.model.NextInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelInfoWithLastMessage
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelMember
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelModeEnum
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelRoleInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelTypeEnum
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerChannelIdPair
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerMemberInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerRoleInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSubscribeTypeInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatUnreadInfoItem
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * qchat channel repo
 * you can user to create channel ,set channel info ,subscribe channel info and so on
 */
object QChatChannelRepo {
    private const val TAG = "QChatChannelRepo"
    private const val LIB_TAG = "QChatKit"

    /**
     * used by [fetchServerMemberInfoWithRolesList], the default count that role tags of 2 lines.
     */
    private const val DEFAULT_LOAD_CHANNEL_MEMBER_ROLE_WITH_ACC_ID_COUNT = 30

    /**
     * 订阅话题 大服务器下，只有订阅话题后才能收到该话题的订阅内容（消息、未读数、未读状态）；
     * 与你相关的消息不需要订阅话题就可以收到，比如@你的消息（的消息不属于与你相关的消息） 小服务器下，
     * 不需要订阅话题就可以收到所有该服务器下所有话题的消息 订阅正在输入事件不区分大服务器和小服务器，只有订阅了才会收到，默认最多订阅100个话题
     */
    @JvmStatic
    fun subscribeChannel(
        type: QChatSubscribeTypeInfo,
        idPairList: List<QChatServerChannelIdPair>,
        register: Boolean,
        callback: FetchCallback<List<QChatUnreadInfoItem>>?
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatChannelProvider.subscribeChannel(
                type.toParam(),
                idPairList.map {
                    it.toInfo()
                },
                register
            ).toInform(callback) {
                it?.unreadInfoList?.map { item ->
                    item.toItem()
                }
            }
        }
    }

    /**
     * 以游客身份订阅话题
     */
    @JvmStatic
    fun subscribeAsVisitor(
        serverId: Long,
        channelIdList: List<Long>,
        register: Boolean,
        callback: FetchCallback<List<QChatServerChannelIdPair>>?
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            channelIdList.map {
                QChatChannelIdInfo(serverId, it)
            }
            QChatChannelProvider.subscribeAsVisitor(
                channelIdList.map {
                    QChatChannelIdInfo(serverId, it)
                },
                register
            )
                .toInform(callback) {
                    it?.failedList?.map { item ->
                        item.toInfo()
                    }
                }
        }
    }

    /**
     * 查询未读信息
     */
    @JvmStatic
    fun fetchChannelUnreadInfoList(
        idPairList: List<QChatServerChannelIdPair>,
        callback: FetchCallback<List<QChatUnreadInfoItem>>?
    ) {
        ALog.d(LIB_TAG, TAG, "fetchChannelUnreadInfoList:${idPairList.size}")
        CoroutineScope(Dispatchers.Main).launch {
            QChatChannelProvider.getChannelUnreadInfos(idPairList.map { it.toInfo() })
                .toInform(callback) {
                    it?.unreadInfoList?.map { item ->
                        item.toItem()
                    }
                }
        }
    }

    /**
     * 通过分页接口查询话题
     */
    @JvmStatic
    fun fetchChannelsByServerId(
        serverId: Long,
        timeTag: Long,
        limit: Int,
        callBack: FetchCallback<List<QChatChannelInfo>>?
    ) {
        ALog.d(LIB_TAG, TAG, "fetchChannelsByServerId:$serverId,$timeTag,$limit")
        CoroutineScope(Dispatchers.Main).launch {
            QChatChannelProvider.fetchChannelsByPage(serverId, timeTag, limit)
                .toInform(callBack) { pageResult ->
                    val lastIndex = (pageResult?.channels?.size ?: 0) - 1
                    pageResult?.channels?.mapIndexed { index, qChatChannel ->
                        qChatChannel.toInfo(
                            if (index == lastIndex) {
                                NextInfo(
                                    pageResult.isHasMore,
                                    pageResult.nextTimeTag
                                )
                            } else {
                                null
                            }
                        )
                    }
                }
        }
    }

    /**
     * 查询100个话题（分页限制最多100个）
     */
    @JvmStatic
    fun fetchMaxChannelIdsByServerIdForVisitor(serverId: Long, callback: FetchCallback<List<Long>>?) {
        ALog.d(LIB_TAG, TAG, "fetchAllChannelIdsByServerId:$serverId")
        val maxCount = 100
        CoroutineScope(Dispatchers.Main).launch {
            QChatChannelProvider.fetchChannelsByPage(serverId, 0L, maxCount)
                .toInform(callback) { pageResult ->
                    pageResult?.channels?.map {
                        it.channelId
                    }
                }
        }
    }

    /**
     * 通过分页接口查询话题和话题里面最后的会话消息数据
     */
    @JvmStatic
    fun fetchChannelsByServerIdWithLastMessage(
        serverId: Long,
        timeTag: Long,
        limit: Int,
        callBack: FetchCallback<List<QChatChannelInfoWithLastMessage>>?
    ) {
        ALog.d(LIB_TAG, TAG, "fetchChannelsByServerIdWithLastMessage:$serverId,$timeTag,$limit")
        CoroutineScope(Dispatchers.Main).launch {
            QChatChannelProvider.fetchChannelsByPage(serverId, timeTag, limit)
                .toInform(callBack) { pageResult ->
                    val lastIndex = (pageResult?.channels?.size ?: 0) - 1
                    val channelIds = pageResult?.channels?.map {
                        it.channelId
                    }
                    val channelMsgMap = mutableMapOf<Long, QChatMessage>()
                    if (channelIds?.isNotEmpty() == true) {
                        val maxSizeForRequest = 20
                        val totalCount = channelIds.size
                        var startIndex = 0
                        var endIndex = min(totalCount, maxSizeForRequest)
                        while (startIndex < endIndex) {
                            QChatMessageProvider.getLastMessageOfChannels(
                                serverId,
                                channelIds.subList(startIndex, endIndex)
                            ).value?.channelMsgMap?.run {
                                channelMsgMap.putAll(this)
                            }
                            startIndex = endIndex
                            endIndex = min(maxSizeForRequest + endIndex, totalCount)
                        }
                    }
                    pageResult?.channels?.mapIndexed { index, qChatChannel ->
                        qChatChannel.toInfoWithLastMessage(
                            nextInfo = if (index == lastIndex) {
                                NextInfo(
                                    pageResult.isHasMore,
                                    pageResult.nextTimeTag
                                )
                            } else {
                                null
                            },
                            lastMessage = channelMsgMap[qChatChannel.channelId]
                        )
                    }
                }
        }
    }

    /**
     * 通过accid查询该accid所属的服务器身份组列表，结果只有自定义身份组，不包含everyone身份组
     */
    @JvmStatic
    fun fetchServerRolesByAccId(
        serverId: Long,
        accId: String,
        timeTag: Long,
        limit: Int,
        callBack: FetchCallback<List<QChatServerRoleInfo>>?
    ) {
        ALog.d(LIB_TAG, TAG, "fetchServerRolesByAccId:$serverId,$timeTag,$accId")
        CoroutineScope(Dispatchers.Main).launch {
            QChatRoleProvider.getServerRolesByAccId(serverId, accId, timeTag, limit)
                .toInform(callBack) { result ->
                    result?.roleList?.map {
                        it.toInfo()
                    }
                }
        }
    }

    /**
     * 创建话题
     */
    @JvmStatic
    fun createChannel(
        serverId: Long,
        channelName: String,
        topic: String,
        mode: QChatChannelModeEnum,
        type: QChatChannelTypeEnum,
        callback: FetchCallback<QChatChannelInfo>?
    ) {
        ALog.d(LIB_TAG, TAG, "createChannel:$serverId,$topic,${mode.name},${type.name}")
        CoroutineScope(Dispatchers.Main).launch {
            val result = QChatChannelProvider.createChannel(
                serverId,
                channelName,
                topic,
                QChatConvert.convertToChannelType(type),
                QChatConvert.convertToChannelMode(mode)
            )
            callback?.run {
                result.toInform(this) {
                    it?.channel?.toInfo()
                }
            }
        }
    }

    /**
     * 通过话题Id查询话题
     */
    @JvmStatic
    fun fetchChannelInfo(channelId: Long, callback: FetchCallback<QChatChannelInfo>) {
        ALog.d(LIB_TAG, TAG, "fetchChannelInfo:$channelId")
        CoroutineScope(Dispatchers.Main).launch {
            val result = QChatChannelProvider.fetchChannelInfo(channelId)
            callback.run {
                result.toInform(this) {
                    val channels = it?.channels
                    if (channels != null && channels.size > 0) {
                        channels[0]?.toInfo()
                    } else {
                        null
                    }
                }
            }
        }
    }

    /**
     * 通过话题Id删除话题
     */
    @JvmStatic
    fun deleteChannel(channelId: Long, callback: FetchCallback<Void>) {
        ALog.d(LIB_TAG, TAG, "deleteChannel:$channelId")
        CoroutineScope(Dispatchers.Main).launch {
            val result = QChatChannelProvider.deleteChannel(channelId)
            callback.run {
                result.toInform(this) {
                    null
                }
            }
        }
    }

    /**
     * 修改话题信息
     */
    @JvmStatic
    fun updateChannel(
        channelId: Long,
        name: String,
        topic: String?,
        callback: FetchCallback<QChatChannelInfo>
    ) {
        ALog.d(LIB_TAG, TAG, "updateChannel::$channelId")
        CoroutineScope(Dispatchers.Main).launch {
            val result = QChatChannelProvider.updateChannel(channelId, name, topic)
            callback.run {
                result.toInform(this) {
                    it?.channel?.toInfo()
                }
            }
        }
    }

    /**
     * 查询某话题下的身份组信息列表
     */
    @JvmStatic
    fun fetchChannelRoles(
        serverId: Long,
        channelId: Long,
        timeTag: Long,
        limit: Int,
        callback: FetchCallback<List<QChatChannelRoleInfo>>
    ) {
        ALog.d(LIB_TAG, TAG, "fetchChannelRoles:$serverId,$channelId")
        CoroutineScope(Dispatchers.Main).launch {
            val result = QChatRoleProvider.fetchChannelRoles(serverId, channelId, timeTag, limit)
            callback.run {
                result.toInform(this) {
                    it?.roleList?.map { role ->
                        role.toInfo()
                    }
                }
            }
        }
    }

    /**
     * 查询channel下某人的定制权限
     */
    @JvmStatic
    fun fetchChannelRoleMembers(
        serverId: Long,
        channelId: Long,
        timeTag: Long,
        limit: Int,
        callback: FetchCallback<List<QChatChannelMember>>
    ) {
        ALog.d(LIB_TAG, TAG, "fetchChannelRoleMembers:$serverId,$channelId")
        CoroutineScope(Dispatchers.Main).launch {
            val result = QChatRoleProvider.fetchChannelMemberRole(
                serverId,
                channelId,
                timeTag,
                limit
            )
            callback.run {
                result.toInform(this) {
                    it?.roleList?.map { member ->
                        member.toInfo()
                    }
                }
            }
        }
    }

    /**
     * 通过分页接口查询话题成员
     */
    @JvmStatic
    fun fetchChannelMembers(
        serverId: Long,
        channelId: Long,
        timeTag: Long,
        limit: Int,
        callback: FetchCallback<List<QChatServerMemberInfo>>
    ) {
        ALog.d(LIB_TAG, TAG, "fetchChannelMembers:$serverId,$channelId")
        CoroutineScope(Dispatchers.Main).launch {
            val result = QChatChannelProvider.fetchChannelMembersByPage(
                serverId,
                channelId,
                timeTag,
                limit
            )
            callback.run {
                result.toInform(this) {
                    it?.members?.map { member ->
                        member.toInfo()
                    }
                }
            }
        }
    }

    /**
     * 分页查询话题黑白名单成员列表，公开话题查询黑名单，私有话题查询白名单
     */
    @JvmStatic
    fun fetchChannelBlackWhiteMembers(
        serverId: Long,
        channelId: Long,
        timeTag: Long,
        type: QChatChannelModeEnum,
        limit: Int,
        callback: FetchCallback<List<QChatServerMemberInfo>>
    ) {
        ALog.d(LIB_TAG, TAG, "fetchChannelBlackWhiteMembers:$serverId,$channelId")
        CoroutineScope(Dispatchers.Main).launch {
            val memberType = if (type == QChatChannelModeEnum.Public) QChatChannelBlackWhiteType.BLACK else QChatChannelBlackWhiteType.WHITE
            val result = QChatChannelProvider.fetchChannelBlackWhiteMembersByPage(
                serverId,
                channelId,
                timeTag,
                memberType,
                limit
            )
            callback.run {
                result.toInform(this) {
                    it?.memberList?.map { item ->
                        item.toInfo()
                    }
                }
            }
        }
    }

    /**
     * 添加话题黑白名单成员
     */
    @JvmStatic
    fun addChannelBlackWhiteMembers(
        serverId: Long,
        channelId: Long,
        accIdList: List<String>,
        type: QChatChannelModeEnum,
        callback: FetchCallback<Void>
    ) {
        ALog.d(LIB_TAG, TAG, "addChannelBlackWhiteMembers:$serverId,$channelId")
        CoroutineScope(Dispatchers.Main).launch {
            val memberType = if (type == QChatChannelModeEnum.Public) QChatChannelBlackWhiteType.BLACK else QChatChannelBlackWhiteType.WHITE
            val result = QChatChannelProvider.addRemoveChannelBlackWhiteMembers(
                serverId,
                channelId,
                accIdList,
                memberType,
                QChatChannelBlackWhiteOperateType.ADD
            )
            callback.run {
                result.toInform(this) {
                    it
                }
            }
        }
    }

    /**
     * 移除话题黑白名单成员
     */
    @JvmStatic
    fun removeChannelBlackWhiteMembers(
        serverId: Long,
        channelId: Long,
        channelType: Int,
        accIdList: List<String>,
        callback: FetchCallback<Void>
    ) {
        ALog.d(LIB_TAG, TAG, "removeChannelBlackWhiteMembers:$serverId,$channelId")
        CoroutineScope(Dispatchers.Main).launch {
            val memberType = if (channelType == 0) QChatChannelBlackWhiteType.BLACK else QChatChannelBlackWhiteType.WHITE
            val result = QChatChannelProvider.addRemoveChannelBlackWhiteMembers(
                serverId,
                channelId,
                accIdList,
                memberType,
                QChatChannelBlackWhiteOperateType.REMOVE
            )
            callback.run {
                result.toInform(this) {
                    it
                }
            }
        }
    }
}
