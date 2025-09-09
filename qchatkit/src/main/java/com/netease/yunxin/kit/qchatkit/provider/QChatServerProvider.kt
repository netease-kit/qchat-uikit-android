/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.provider

import android.util.Pair
import com.netease.nimlib.sdk.NIMClient
import com.netease.nimlib.sdk.qchat.QChatServerService
import com.netease.nimlib.sdk.qchat.enums.QChatApplyJoinMode
import com.netease.nimlib.sdk.qchat.enums.QChatInviteMode
import com.netease.nimlib.sdk.qchat.enums.QChatPushMsgType
import com.netease.nimlib.sdk.qchat.enums.QChatSearchServerTypeEnum
import com.netease.nimlib.sdk.qchat.enums.QChatServerSearchSortEnum
import com.netease.nimlib.sdk.qchat.enums.QChatSubscribeOperateType
import com.netease.nimlib.sdk.qchat.enums.QChatSubscribeType
import com.netease.nimlib.sdk.qchat.param.QChatApplyServerJoinParam
import com.netease.nimlib.sdk.qchat.param.QChatCreateServerParam
import com.netease.nimlib.sdk.qchat.param.QChatDeleteServerParam
import com.netease.nimlib.sdk.qchat.param.QChatEnterServerAsVisitorParam
import com.netease.nimlib.sdk.qchat.param.QChatGetServerMembersByPageParam
import com.netease.nimlib.sdk.qchat.param.QChatGetServerMembersParam
import com.netease.nimlib.sdk.qchat.param.QChatGetServersByPageParam
import com.netease.nimlib.sdk.qchat.param.QChatGetServersParam
import com.netease.nimlib.sdk.qchat.param.QChatGetUserServerPushConfigsParam
import com.netease.nimlib.sdk.qchat.param.QChatInviteServerMembersParam
import com.netease.nimlib.sdk.qchat.param.QChatKickServerMembersParam
import com.netease.nimlib.sdk.qchat.param.QChatLeaveServerAsVisitorParam
import com.netease.nimlib.sdk.qchat.param.QChatLeaveServerParam
import com.netease.nimlib.sdk.qchat.param.QChatSearchServerByPageParam
import com.netease.nimlib.sdk.qchat.param.QChatSubscribeServerAsVisitorParam
import com.netease.nimlib.sdk.qchat.param.QChatSubscribeServerParam
import com.netease.nimlib.sdk.qchat.param.QChatUpdateMyMemberInfoParam
import com.netease.nimlib.sdk.qchat.param.QChatUpdateServerMemberInfoParam
import com.netease.nimlib.sdk.qchat.param.QChatUpdateServerParam
import com.netease.nimlib.sdk.qchat.param.QChatUpdateUserServerPushConfigParam
import com.netease.nimlib.sdk.qchat.result.QChatApplyServerJoinResult
import com.netease.nimlib.sdk.qchat.result.QChatCreateServerResult
import com.netease.nimlib.sdk.qchat.result.QChatEnterServerAsVisitorResult
import com.netease.nimlib.sdk.qchat.result.QChatGetServerMembersByPageResult
import com.netease.nimlib.sdk.qchat.result.QChatGetServerMembersResult
import com.netease.nimlib.sdk.qchat.result.QChatGetServersByPageResult
import com.netease.nimlib.sdk.qchat.result.QChatGetServersResult
import com.netease.nimlib.sdk.qchat.result.QChatGetUserPushConfigsResult
import com.netease.nimlib.sdk.qchat.result.QChatInviteServerMembersResult
import com.netease.nimlib.sdk.qchat.result.QChatLeaveServerAsVisitorResult
import com.netease.nimlib.sdk.qchat.result.QChatSearchServerByPageResult
import com.netease.nimlib.sdk.qchat.result.QChatSubscribeServerAsVisitorResult
import com.netease.nimlib.sdk.qchat.result.QChatSubscribeServerResult
import com.netease.nimlib.sdk.qchat.result.QChatUpdateMyMemberInfoResult
import com.netease.nimlib.sdk.qchat.result.QChatUpdateServerMemberInfoResult
import com.netease.nimlib.sdk.qchat.result.QChatUpdateServerResult
import com.netease.yunxin.kit.corekit.model.ResultInfo
import com.netease.yunxin.kit.qchatkit.onResult
import kotlin.coroutines.suspendCoroutine

object QChatServerProvider {
    private val qChatServerService =
        NIMClient.getService(QChatServerService::class.java)

    @JvmStatic
    @JvmOverloads
    suspend fun createServer(
        name: String,
        icon: String? = null,
        custom: String? = null,
        inviteMode: QChatInviteMode = QChatInviteMode.AGREE_NEED_NOT,
        applyJoinMode: QChatApplyJoinMode = QChatApplyJoinMode.AGREE_NEED_NOT
    ): ResultInfo<QChatCreateServerResult> {
        return suspendCoroutine {
            val param = QChatCreateServerParam(name).apply {
                icon?.run {
                    setIcon(this)
                }
                custom?.run {
                    setCustom(this)
                }
                setApplyJoinMode(applyJoinMode)
                setInviteMode(inviteMode)
            }
            qChatServerService.createServer(param)
                .onResult(it, "createServer")
        }
    }

    @JvmStatic
    suspend fun getServersByPage(
        timeTag: Long,
        limit: Int
    ): ResultInfo<QChatGetServersByPageResult> {
        val param = QChatGetServersByPageParam(timeTag, limit)
        return suspendCoroutine {
            qChatServerService.getServersByPage(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun getServers(serverIdList: List<Long>): ResultInfo<QChatGetServersResult> {
        return suspendCoroutine {
            val param = QChatGetServersParam(serverIdList)
            qChatServerService.getServers(param).onResult(it)
        }
    }

    @JvmStatic
    @JvmOverloads
    suspend fun applyServerJoin(
        serverId: Long,
        postscript: String? = null
    ): ResultInfo<QChatApplyServerJoinResult> {
        return suspendCoroutine {
            val param = QChatApplyServerJoinParam(serverId)
            postscript?.run {
                param.postscript = this
            }
            qChatServerService.applyServerJoin(param).onResult(it)
        }
    }

    @JvmStatic
    @JvmOverloads
    suspend fun updateServer(
        serverId: Long,
        name: String? = null,
        icon: String? = null,
        custom: String? = null
    ): ResultInfo<QChatUpdateServerResult> {
        val param = QChatUpdateServerParam(serverId)
        name?.let { param.name = it }
        icon?.let { param.icon = it }
        custom?.let { param.custom = it }
        return suspendCoroutine {
            qChatServerService.updateServer(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun deleteServer(
        serverId: Long
    ): ResultInfo<Void> {
        val param = QChatDeleteServerParam(serverId)
        return suspendCoroutine {
            qChatServerService.deleteServer(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun leaveServer(
        serverId: Long
    ): ResultInfo<Void> {
        val param = QChatLeaveServerParam(serverId)
        return suspendCoroutine {
            qChatServerService.leaveServer(param).onResult(it)
        }
    }

    @JvmStatic
    @JvmOverloads
    suspend fun updateServerMember(
        serverId: Long,
        accid: String,
        nick: String? = null,
        avatar: String? = null
    ): ResultInfo<QChatUpdateServerMemberInfoResult> {
        val param = QChatUpdateServerMemberInfoParam(serverId, accid)
        nick?.let { param.nick = it }
        avatar?.let { param.avatar = it }
        return suspendCoroutine {
            qChatServerService.updateServerMemberInfo(param).onResult(it)
        }
    }

    @JvmStatic
    @JvmOverloads
    suspend fun updateMyServerMember(
        serverId: Long,
        nick: String? = null,
        avatar: String? = null,
        custom: String? = null
    ): ResultInfo<QChatUpdateMyMemberInfoResult> {
        val param = QChatUpdateMyMemberInfoParam(serverId)
        nick?.let { param.nick = it }
        avatar?.let { param.avatar = it }
        custom?.let { param.custom = it }
        return suspendCoroutine {
            qChatServerService.updateMyMemberInfo(param).onResult(it)
        }
    }

    suspend fun kickOutMember(
        serverId: Long,
        accid: String
    ): ResultInfo<Void> {
        val param = QChatKickServerMembersParam(serverId, listOf(accid))
        return suspendCoroutine {
            qChatServerService.kickServerMembers(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun getServerMembers(list: List<Pair<Long, String>>): ResultInfo<QChatGetServerMembersResult> {
        return suspendCoroutine {
            val param = QChatGetServerMembersParam(list)
            qChatServerService.getServerMembers(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun getServerMembersByPage(
        serverId: Long,
        timeTag: Long,
        limit: Int
    ): ResultInfo<QChatGetServerMembersByPageResult> {
        return suspendCoroutine {
            val param = QChatGetServerMembersByPageParam(serverId, timeTag, limit)
            qChatServerService.getServerMembersByPage(param).onResult(it)
        }
    }

    @JvmStatic
    @JvmOverloads
    suspend fun inviteServerMembers(
        serverId: Long,
        accIdList: List<String>,
        postscript: String? = null
    ): ResultInfo<QChatInviteServerMembersResult> {
        return suspendCoroutine {
            val param = QChatInviteServerMembersParam(serverId, accIdList).apply {
                postscript?.also { script ->
                    this.postscript = script
                }
            }
            qChatServerService.inviteServerMembers(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun subscribeServer(
        serverIdList: List<Long>,
        register: Boolean
    ): ResultInfo<QChatSubscribeServerResult> {
        return suspendCoroutine {
            val param = QChatSubscribeServerParam(
                QChatSubscribeType.SERVER_MSG,
                if (register) QChatSubscribeOperateType.SUB else QChatSubscribeOperateType.UN_SUB,
                serverIdList
            )
            qChatServerService.subscribeServer(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun searchServerByPage(
        keyword: String,
        asc: Boolean,
        searchServerTypeEnum: QChatSearchServerTypeEnum,
        startTime: Long? = null,
        endTime: Long? = null,
        limit: Int? = null,
        serverTypes: List<Int>? = null,
        sortEnum: QChatServerSearchSortEnum? = null,
        cursor: String? = null
    ): ResultInfo<QChatSearchServerByPageResult> {
        return suspendCoroutine {
            val param = QChatSearchServerByPageParam(
                keyword,
                asc,
                searchServerTypeEnum,
                startTime,
                endTime,
                limit,
                serverTypes,
                sortEnum,
                cursor
            )
            qChatServerService.searchServerByPage(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun enterAsVisitor(serverIdList: List<Long>): ResultInfo<QChatEnterServerAsVisitorResult> {
        return suspendCoroutine {
            val param = QChatEnterServerAsVisitorParam(serverIdList)
            qChatServerService.enterAsVisitor(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun leaveAsVisitor(serverIdList: List<Long>): ResultInfo<QChatLeaveServerAsVisitorResult> {
        return suspendCoroutine {
            val param = QChatLeaveServerAsVisitorParam(serverIdList)
            qChatServerService.leaveAsVisitor(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun subscribeAsVisitor(
        serverIdList: List<Long>,
        register: Boolean
    ): ResultInfo<QChatSubscribeServerAsVisitorResult> {
        return suspendCoroutine {
            val param = QChatSubscribeServerAsVisitorParam(
                if (register) QChatSubscribeOperateType.SUB else QChatSubscribeOperateType.UN_SUB,
                serverIdList
            )
            qChatServerService.subscribeAsVisitor(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun updateServerPushConfig(
        serverId: Long,
        type: QChatPushMsgType
    ): ResultInfo<Void> {
        return suspendCoroutine {
            val param = QChatUpdateUserServerPushConfigParam(serverId, type)
            qChatServerService.updateUserServerPushConfig(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun getServerPushConfig(
        serverIdList: List<Long>
    ): ResultInfo<QChatGetUserPushConfigsResult> {
        return suspendCoroutine {
            val param = QChatGetUserServerPushConfigsParam(serverIdList)
            qChatServerService.getUserServerPushConfigs(param).onResult(it)
        }
    }
}
