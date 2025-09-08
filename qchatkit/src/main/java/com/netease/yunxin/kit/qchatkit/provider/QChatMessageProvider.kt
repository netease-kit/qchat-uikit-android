/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.provider

import com.netease.nimlib.sdk.NIMClient
import com.netease.nimlib.sdk.qchat.QChatMessageService
import com.netease.nimlib.sdk.qchat.model.QChatMessage
import com.netease.nimlib.sdk.qchat.model.QChatMessageCache
import com.netease.nimlib.sdk.qchat.param.QChatAddQuickCommentParam
import com.netease.nimlib.sdk.qchat.param.QChatDeleteMessageParam
import com.netease.nimlib.sdk.qchat.param.QChatDownloadAttachmentParam
import com.netease.nimlib.sdk.qchat.param.QChatGetLastMessageOfChannelsParam
import com.netease.nimlib.sdk.qchat.param.QChatGetMessageHistoryParam
import com.netease.nimlib.sdk.qchat.param.QChatGetQuickCommentsParam
import com.netease.nimlib.sdk.qchat.param.QChatMarkMessageReadParam
import com.netease.nimlib.sdk.qchat.param.QChatRemoveQuickCommentParam
import com.netease.nimlib.sdk.qchat.param.QChatResendMessageParam
import com.netease.nimlib.sdk.qchat.param.QChatRevokeMessageParam
import com.netease.nimlib.sdk.qchat.param.QChatSendMessageParam
import com.netease.nimlib.sdk.qchat.result.QChatDeleteMessageResult
import com.netease.nimlib.sdk.qchat.result.QChatGetLastMessageOfChannelsResult
import com.netease.nimlib.sdk.qchat.result.QChatGetMessageHistoryResult
import com.netease.nimlib.sdk.qchat.result.QChatGetQuickCommentsResult
import com.netease.nimlib.sdk.qchat.result.QChatRevokeMessageResult
import com.netease.nimlib.sdk.qchat.result.QChatSendMessageResult
import com.netease.yunxin.kit.corekit.model.ResultInfo
import com.netease.yunxin.kit.qchatkit.onResult
import kotlin.coroutines.suspendCoroutine

object QChatMessageProvider {

    private val qChatMessageService =
        NIMClient.getService(QChatMessageService::class.java)

    @JvmStatic
    suspend fun sendMessage(msg: QChatSendMessageParam): ResultInfo<QChatSendMessageResult> {
        return suspendCoroutine {
            qChatMessageService.sendMessage(msg).onResult(it)
        }
    }

    @JvmStatic
    suspend fun resendMessage(msg: QChatMessage): ResultInfo<QChatSendMessageResult> {
        return suspendCoroutine {
            val resendParam = QChatResendMessageParam(msg)
            qChatMessageService.resendMessage(resendParam).onResult(it)
        }
    }

    @JvmStatic
    suspend fun addMessageQuickComment(msg: QChatMessage, commentParam: Int): ResultInfo<Void> {
        return suspendCoroutine {
            val commentParam = QChatAddQuickCommentParam(msg, commentParam)
            qChatMessageService.addQuickComment(commentParam).onResult(it)
        }
    }

    @JvmStatic
    suspend fun removeMessageQuickComment(msg: QChatMessage, commentParam: Int): ResultInfo<Void> {
        return suspendCoroutine {
            val commentParam = QChatRemoveQuickCommentParam(msg, commentParam)
            qChatMessageService.removeQuickComment(commentParam).onResult(it)
        }
    }

    @JvmStatic
    suspend fun getMessageQuickComment(serverId: Long, channelId: Long, msg: List<QChatMessage>):
        ResultInfo<QChatGetQuickCommentsResult> {
        return suspendCoroutine {
            val commentParam = QChatGetQuickCommentsParam(serverId, channelId, msg)
            qChatMessageService.getQuickComments(commentParam).onResult(it)
        }
    }

    @JvmStatic
    suspend fun markMessageRead(serverId: Long, channelId: Long, ackTime: Long): ResultInfo<Void> {
        return suspendCoroutine {
            val readParam = QChatMarkMessageReadParam(serverId, channelId, ackTime)
            qChatMessageService.markMessageRead(readParam).onResult(it)
        }
    }

    @JvmStatic
    suspend fun deleteMessage(deleteParam: QChatDeleteMessageParam): ResultInfo<QChatDeleteMessageResult> {
        return suspendCoroutine {
            qChatMessageService.deleteMessage(deleteParam).onResult(it)
        }
    }

    @JvmStatic
    suspend fun revokeMessage(revokeParam: QChatRevokeMessageParam):
        ResultInfo<QChatRevokeMessageResult> {
        return suspendCoroutine {
            qChatMessageService.revokeMessage(revokeParam).onResult(it)
        }
    }

    @JvmStatic
    suspend fun getMessageHistory(
        serverId: Long,
        channelId: Long,
        fromTime: Long,
        toTime: Long,
        limit: Int,
        reverse: Boolean
    ): ResultInfo<QChatGetMessageHistoryResult> {
        return suspendCoroutine {
            val historyParam = QChatGetMessageHistoryParam(serverId, channelId)
            historyParam.fromTime = fromTime
            historyParam.toTime = toTime
            historyParam.limit = limit
            historyParam.isReverse = reverse
            qChatMessageService.getMessageHistory(historyParam).onResult(it)
        }
    }

    @JvmStatic
    suspend fun getMessageHistoryCache(
        serverId: Long,
        channelId: Long
    ): ResultInfo<List<QChatMessageCache>> {
        return suspendCoroutine {
            qChatMessageService.getMessageCache(serverId, channelId).onResult(it)
        }
    }

    @JvmStatic
    suspend fun downloadAttachment(downloadParam: QChatDownloadAttachmentParam): ResultInfo<Void> {
        return suspendCoroutine {
            qChatMessageService.downloadAttachment(downloadParam).onResult(it)
        }
    }

    @JvmStatic
    suspend fun getLastMessageOfChannels(
        serverId: Long,
        channelIds: List<Long>
    ): ResultInfo<QChatGetLastMessageOfChannelsResult> {
        return suspendCoroutine {
            qChatMessageService.getLastMessageOfChannels(
                QChatGetLastMessageOfChannelsParam(
                    serverId,
                    channelIds
                )
            ).onResult(it)
        }
    }
}
