/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.repo

import com.netease.nimlib.sdk.qchat.param.QChatDeleteMessageParam
import com.netease.nimlib.sdk.qchat.param.QChatDownloadAttachmentParam
import com.netease.nimlib.sdk.qchat.param.QChatRevokeMessageParam
import com.netease.nimlib.sdk.qchat.param.QChatSendMessageParam
import com.netease.nimlib.sdk.qchat.param.QChatUpdateParam
import com.netease.yunxin.kit.corekit.im.provider.FetchCallback
import com.netease.yunxin.kit.corekit.im.utils.toInform
import com.netease.yunxin.kit.corekit.qchat.provider.QChatMessageProvider
import com.netease.yunxin.kit.qchatkit.repo.model.QChatGetQuickCommentsResultInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageQuickCommentDetailInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSendMessageInfo
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * message repo provider send message,query message ,message status and so on
 */
object QChatMessageRepo {

    /**
     * 发送消息
     */
    @JvmStatic
    fun sendMessage(
        msg: QChatSendMessageInfo,
        callback: FetchCallback<QChatMessageInfo>
    ): QChatMessageInfo {
        val sendMessage = QChatConvert.convertToSendMessage(msg)
        val messageInfo = QChatMessageInfo(sendMessage.toQChatMessage())
        msg.messageInfo = messageInfo
        CoroutineScope(Dispatchers.Main).launch {
            QChatMessageProvider.sendMessage(sendMessage).toInform(callback) {
                it?.sentMessage?.let { msg ->
                    QChatMessageInfo(msg)
                }
            }
        }
        return messageInfo
    }

    /**
     * 发送消息
     */
    @JvmStatic
    fun sendMessage(
        msg: QChatSendMessageParam,
        callback: FetchCallback<QChatMessageInfo>
    ): QChatMessageInfo {
        val messageInfo = QChatMessageInfo(msg.toQChatMessage())
        CoroutineScope(Dispatchers.Main).launch {
            QChatMessageProvider.sendMessage(msg).toInform(callback) {
                it?.sentMessage?.let { msg ->
                    QChatMessageInfo(msg)
                }
            }
        }
        return messageInfo
    }

    /**
     * 重发消息
     */
    @JvmStatic
    fun resendMessage(msg: QChatMessageInfo, callback: FetchCallback<QChatMessageInfo>) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatMessageProvider.resendMessage(msg).toInform(callback) {
                it?.sentMessage?.let { msg ->
                    QChatMessageInfo(msg)
                }
            }
        }
    }

    /**
     * 添加一条快捷评论
     */
    @JvmStatic
    fun addQuickComment(msg: QChatMessageInfo, comment: Int, callback: FetchCallback<Void>) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatMessageProvider.addMessageQuickComment(msg, comment).toInform(callback) {
                it
            }
        }
    }

    /**
     * 删除一条快捷评论
     */
    @JvmStatic
    fun removeQuickComment(msg: QChatMessageInfo, comment: Int, callback: FetchCallback<Void>) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatMessageProvider.removeMessageQuickComment(msg, comment).toInform(callback) {
                it
            }
        }
    }

    /**
     * 批量查询快捷评论
     */
    @JvmStatic
    fun getQuickComment(
        serverId: Long, channelId: Long, msgList: List<QChatMessageInfo>,
        callback:
        FetchCallback<QChatGetQuickCommentsResultInfo>
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val quickCommentMap = mutableMapOf<Long, QChatMessageQuickCommentDetailInfo>()
            if (msgList?.isNotEmpty()) {
                val maxSizeForRequest = 20
                val totalCount = msgList.size
                var startIndex = 0
                var endIndex = min(totalCount, maxSizeForRequest)
                while (startIndex < endIndex) {
                    QChatMessageProvider.getMessageQuickComment(
                        serverId,
                        channelId,
                        msgList.subList(startIndex, endIndex)
                    ).value?.messageQuickCommentDetailMap?.map {
                        quickCommentMap.put(it.key, it.value.toInfo())
                    }
                    startIndex = endIndex
                    endIndex = min(maxSizeForRequest + endIndex, totalCount)
                }
            }
            callback?.onSuccess(QChatGetQuickCommentsResultInfo(quickCommentMap))
        }
    }

    /**
     * 标记消息已读，该接口存在频控，300ms内只能调用1次
     */
    @JvmStatic
    fun markMessageRead(
        serverId: Long,
        channelId: Long,
        ackTime: Long,
        callback: FetchCallback<Void>
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatMessageProvider.markMessageRead(serverId, channelId, ackTime).toInform(callback) {
                it
            }
        }
    }

    /**
     * 删除消息
     */
    @JvmStatic
    fun deleteMessage(msg: QChatMessageInfo, callback: FetchCallback<QChatMessageInfo>) {
        CoroutineScope(Dispatchers.Main).launch {
            val deleteMessage = QChatDeleteMessageParam(
                QChatUpdateParam(),
                msg.qChatServerId,
                msg.qChatChannelId,
                msg.time,
                msg.msgIdServer
            )
            QChatMessageProvider.deleteMessage(deleteMessage).toInform(callback) {
                it?.message?.let { msg ->
                    QChatMessageInfo(msg)
                }
            }
        }
    }

    /**
     * 撤回消息
     */
    @JvmStatic
    fun revokeMessage(msg: QChatMessageInfo, callback: FetchCallback<QChatMessageInfo>) {
        CoroutineScope(Dispatchers.Main).launch {
            val revokeMessage = QChatRevokeMessageParam(
                QChatUpdateParam(),
                msg.qChatServerId,
                msg.qChatChannelId,
                msg.time,
                msg.msgIdServer
            )
            QChatMessageProvider.revokeMessage(revokeMessage).toInform(callback) {
                it?.message?.let { msg ->
                    QChatMessageInfo(msg)
                }
            }
        }
    }

    /**
     * 撤回消息
     */
    @JvmStatic
    fun revokeMessage(
        revokeParam: QChatRevokeMessageParam,
        callback:
        FetchCallback<QChatMessageInfo>
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatMessageProvider.revokeMessage(revokeParam).toInform(callback) {
                it?.message?.let { msg ->
                    QChatMessageInfo(msg)
                }
            }
        }
    }

    /**
     * 下载消息附件，默认情况下（SDKOPtions::preloadAttach为true），
     * SDK收到多媒体消息后，图片和视频会自动下载缩略图，音频会自动下载文件。
     * 如果下载原图或者原视频等，可调用该接口下载附件
     */
    @JvmStatic
    fun downloadAttachment(msg: QChatMessageInfo, thumb: Boolean, callback: FetchCallback<Void>?) {
        CoroutineScope(Dispatchers.Main).launch {
            val attachmentParam = QChatDownloadAttachmentParam(msg, thumb)
            QChatMessageProvider.downloadAttachment(attachmentParam).toInform(callback) { it }
        }
    }

    /**
     * 查询历史消息
     */
    @JvmStatic
    fun fetchMessageHistory(
        serverId: Long,
        channelId: Long,
        fromTime: Long,
        toTime: Long,
        limit: Int,
        reverse: Boolean,
        callback: FetchCallback<List<QChatMessageInfo>>
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatMessageProvider.getMessageHistory(
                serverId,
                channelId,
                fromTime,
                toTime,
                limit,
                reverse
            ).toInform(callback) {
                val messageList = mutableListOf<QChatMessageInfo>()
                it?.messages?.map { msg ->
                    if (reverse) {
                        messageList.add(QChatMessageInfo(msg))
                    } else {
                        messageList.add(0, QChatMessageInfo(msg))
                    }
                }
                messageList
            }
        }
    }

    /**
     * 查询历史消息，同时查询快捷评论
     */
    @JvmStatic
    fun fetchMessageHistoryWithQuickComment(
        serverId: Long,
        channelId: Long,
        fromTime: Long,
        toTime: Long,
        limit: Int,
        reverse: Boolean,
        callback: FetchCallback<List<QChatMessageInfo>>
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatMessageProvider.getMessageHistory(
                serverId,
                channelId,
                fromTime,
                toTime,
                limit,
                reverse
            ).toInform(callback) {
                val messageList = mutableListOf<QChatMessageInfo>()
                it?.messages?.map { msg ->
                    if (reverse) {
                        messageList.add(QChatMessageInfo(msg))
                    } else {
                        messageList.add(0, QChatMessageInfo(msg))
                    }
                }
                messageList
            }
        }
    }

    /**
     * 指定通道查询消息缓存
     */
    @JvmStatic
    fun getMessageCache(
        serverId: Long,
        channelId: Long,
        reverse: Boolean,
        callback: FetchCallback<List<QChatMessageInfo>>
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatMessageProvider.getMessageHistoryCache(
                serverId,
                channelId
            ).toInform(callback) {
                val messageList = mutableListOf<QChatMessageInfo>()
                it?.map { msg ->
                    if (reverse) {
                        messageList.add(QChatMessageInfo(msg.message))
                    } else {
                        messageList.add(0, QChatMessageInfo(msg.message))
                    }
                }
                messageList
            }
        }
    }

    /**
     * 查询频道的最后一条消息
     */
    @JvmStatic
    fun getLastMessageOfChannels(
        serverId: Long,
        channelIdList: List<Long>,
        callBack: FetchCallback<Map<Long, QChatMessageInfo>>?
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatMessageProvider.getLastMessageOfChannels(serverId, channelIdList).toInform(
                callBack
            ) {
                it?.channelMsgMap?.mapValues { item ->
                    item.value.toInfo()
                }
            }
        }
    }
}
