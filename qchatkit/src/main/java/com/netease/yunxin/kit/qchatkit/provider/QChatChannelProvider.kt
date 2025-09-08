/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.provider

import com.netease.nimlib.sdk.NIMClient
import com.netease.nimlib.sdk.RequestCallback
import com.netease.nimlib.sdk.qchat.QChatChannelService
import com.netease.nimlib.sdk.qchat.enums.QChatChannelBlackWhiteOperateType
import com.netease.nimlib.sdk.qchat.enums.QChatChannelBlackWhiteType
import com.netease.nimlib.sdk.qchat.enums.QChatChannelMode
import com.netease.nimlib.sdk.qchat.enums.QChatChannelType
import com.netease.nimlib.sdk.qchat.enums.QChatSubscribeOperateType
import com.netease.nimlib.sdk.qchat.enums.QChatSubscribeType
import com.netease.nimlib.sdk.qchat.model.QChatChannelIdInfo
import com.netease.nimlib.sdk.qchat.param.QChatCreateChannelParam
import com.netease.nimlib.sdk.qchat.param.QChatDeleteChannelParam
import com.netease.nimlib.sdk.qchat.param.QChatGetChannelBlackWhiteMembersByPageParam
import com.netease.nimlib.sdk.qchat.param.QChatGetChannelMembersByPageParam
import com.netease.nimlib.sdk.qchat.param.QChatGetChannelUnreadInfosParam
import com.netease.nimlib.sdk.qchat.param.QChatGetChannelsByPageParam
import com.netease.nimlib.sdk.qchat.param.QChatGetChannelsParam
import com.netease.nimlib.sdk.qchat.param.QChatGetExistingChannelBlackWhiteMembersParam
import com.netease.nimlib.sdk.qchat.param.QChatSubscribeChannelAsVisitorParam
import com.netease.nimlib.sdk.qchat.param.QChatSubscribeChannelParam
import com.netease.nimlib.sdk.qchat.param.QChatUpdateChannelBlackWhiteMembersParam
import com.netease.nimlib.sdk.qchat.param.QChatUpdateChannelParam
import com.netease.nimlib.sdk.qchat.result.QChatCreateChannelResult
import com.netease.nimlib.sdk.qchat.result.QChatGetChannelBlackWhiteMembersByPageResult
import com.netease.nimlib.sdk.qchat.result.QChatGetChannelMembersByPageResult
import com.netease.nimlib.sdk.qchat.result.QChatGetChannelUnreadInfosResult
import com.netease.nimlib.sdk.qchat.result.QChatGetChannelsByPageResult
import com.netease.nimlib.sdk.qchat.result.QChatGetChannelsResult
import com.netease.nimlib.sdk.qchat.result.QChatGetExistingChannelBlackWhiteMembersResult
import com.netease.nimlib.sdk.qchat.result.QChatSubscribeChannelAsVisitorResult
import com.netease.nimlib.sdk.qchat.result.QChatSubscribeChannelResult
import com.netease.nimlib.sdk.qchat.result.QChatUpdateChannelResult
import com.netease.yunxin.kit.corekit.model.ErrorMsg
import com.netease.yunxin.kit.corekit.model.ResultInfo
import com.netease.yunxin.kit.qchatkit.onResult
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object QChatChannelProvider {
    private val qChatChannelService =
        NIMClient.getService(QChatChannelService::class.java)

    /**
     * create channel
     */
    suspend fun createChannel(
        serverId: Long,
        name: String,
        topic: String,
        type: QChatChannelType,
        mode: QChatChannelMode?
    ): ResultInfo<QChatCreateChannelResult> {
        return suspendCoroutine {
            val param = QChatCreateChannelParam(serverId, name, type)
            param.topic = topic
            param.viewMode = mode
            qChatChannelService.createChannel(param).onResult(it)
        }
    }

    /**
     * delete channel
     */
    suspend fun deleteChannel(channelId: Long): ResultInfo<Void> {
        return suspendCoroutine {
            val channelParam = QChatDeleteChannelParam(channelId)
            qChatChannelService.deleteChannel(channelParam)
                .setCallback(object : RequestCallback<Void> {
                    override fun onSuccess(result: Void?) {
                        it.resume(ResultInfo(result))
                    }

                    override fun onFailed(code: Int) {
                        it.resume(ResultInfo(success = false, msg = ErrorMsg(code)))
                    }

                    override fun onException(throwable: Throwable?) {
                        it.resume(
                            ResultInfo(
                                success = false,
                                msg = ErrorMsg(
                                    -1,
                                    message = "deleteChannel-${throwable?.stackTraceToString()}",
                                    exception = throwable
                                )
                            )
                        )
                    }
                })
        }
    }

    /**
     * update channel
     */
    suspend fun updateChannel(
        channelId: Long,
        name: String,
        topic: String?
    ): ResultInfo<QChatUpdateChannelResult> {
        return suspendCoroutine {
            val channelParam = QChatUpdateChannelParam(channelId)
            channelParam.name = name
            channelParam.topic = topic
            qChatChannelService.updateChannel(channelParam).onResult(it)
        }
    }

    @JvmStatic
    suspend fun fetchChannelsByPage(
        serverId: Long,
        timeTag: Long,
        limit: Int
    ): ResultInfo<QChatGetChannelsByPageResult> {
        return suspendCoroutine {
            val param = QChatGetChannelsByPageParam(serverId, timeTag, limit)
            qChatChannelService.getChannelsByPage(param).onResult(it)
        }
    }

    suspend fun fetchChannelInfo(channelId: Long): ResultInfo<QChatGetChannelsResult> {
        return fetchChannelInfoList(mutableListOf(channelId))
    }

    suspend fun fetchChannelInfoList(channelIdList: List<Long>): ResultInfo<QChatGetChannelsResult> {
        return suspendCoroutine {
            val channelParam = QChatGetChannelsParam(channelIdList)
            qChatChannelService.getChannels(channelParam).onResult(it)
        }
    }

    @JvmStatic
    suspend fun fetchChannelMembersByPage(
        serverId: Long,
        channelId: Long,
        timeTag: Long,
        limit: Int
    ): ResultInfo<QChatGetChannelMembersByPageResult> {
        return suspendCoroutine {
            val param = QChatGetChannelMembersByPageParam(serverId, channelId, timeTag)
            param.limit = limit
            qChatChannelService.getChannelMembersByPage(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun queryChannelBlackWhiteMembers(
        serverId: Long,
        channelId: Long,
        memberType: QChatChannelBlackWhiteType,
        accIdList: List<String>
    ): ResultInfo<QChatGetExistingChannelBlackWhiteMembersResult> {
        return suspendCoroutine {
            val param = QChatGetExistingChannelBlackWhiteMembersParam(
                serverId,
                channelId,
                memberType,
                accIdList
            )
            qChatChannelService.getExistingChannelBlackWhiteMembers(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun fetchChannelBlackWhiteMembersByPage(
        serverId: Long,
        channelId: Long,
        timeTag: Long,
        type: QChatChannelBlackWhiteType,
        limit: Int
    ): ResultInfo<QChatGetChannelBlackWhiteMembersByPageResult> {
        return suspendCoroutine {
            val param =
                QChatGetChannelBlackWhiteMembersByPageParam(serverId, channelId, type, timeTag)
            param.limit = limit
            qChatChannelService.getChannelBlackWhiteMembersByPage(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun subscribeChannel(
        type: QChatSubscribeType,
        channelIdInfoList: List<QChatChannelIdInfo>,
        register: Boolean
    ): ResultInfo<QChatSubscribeChannelResult> {
        return suspendCoroutine {
            val param = QChatSubscribeChannelParam(
                type,
                if (register) QChatSubscribeOperateType.SUB else QChatSubscribeOperateType.UN_SUB,
                channelIdInfoList
            )
            qChatChannelService.subscribeChannel(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun addRemoveChannelBlackWhiteMembers(
        serverId: Long,
        channelId: Long,
        accIdList: List<String>,
        type: QChatChannelBlackWhiteType,
        opType: QChatChannelBlackWhiteOperateType
    ): ResultInfo<Void> {
        return suspendCoroutine {
            val param = QChatUpdateChannelBlackWhiteMembersParam(
                serverId,
                channelId,
                type,
                opType,
                accIdList
            )
            qChatChannelService.updateChannelBlackWhiteMembers(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun getChannelUnreadInfos(channelIdInfoList: List<QChatChannelIdInfo>): ResultInfo<QChatGetChannelUnreadInfosResult> {
        return suspendCoroutine {
            val param = QChatGetChannelUnreadInfosParam(channelIdInfoList)
            qChatChannelService.getChannelUnreadInfos(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun subscribeAsVisitor(
        channelIdInfoList: List<QChatChannelIdInfo>,
        register: Boolean
    ): ResultInfo<QChatSubscribeChannelAsVisitorResult> {
        return suspendCoroutine {
            val param = QChatSubscribeChannelAsVisitorParam(
                if (register) QChatSubscribeOperateType.SUB else QChatSubscribeOperateType.UN_SUB,
                channelIdInfoList
            )
            qChatChannelService.subscribeAsVisitor(param).onResult(it)
        }
    }
}
