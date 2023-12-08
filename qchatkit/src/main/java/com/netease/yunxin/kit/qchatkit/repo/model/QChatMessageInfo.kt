/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.repo.model

import android.text.TextUtils
import com.netease.nimlib.sdk.qchat.model.QChatMessage
import com.netease.nimlib.sdk.qchat.model.QChatMsgUpdateInfo
import com.netease.yunxin.kit.qchatkit.QChatMsgConstants
import com.netease.yunxin.kit.qchatkit.utils.MessageRevokeHelper
import java.io.Serializable
import org.json.JSONException
import org.json.JSONObject

class QChatMessageInfo(val message: QChatMessage) : QChatMessage by message, Serializable {

    var isRevoke = false
    var revokeText = ""

    init {
        loadRevoke()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QChatMessageInfo

        if (msgIdServer != other.msgIdServer) return false
        if (qChatChannelId != other.qChatChannelId) return false
        if (uuid != other.uuid) return false

        return true
    }

    fun hasReply(): Boolean {
        return false
    }

    fun getName(): String {
        if (TextUtils.isEmpty(fromNick)) {
            return fromAccount
        }
        return fromNick
    }

    override fun hashCode(): Int {
        var result = msgIdServer.hashCode()
        result = 31 * result + qChatChannelId.hashCode()
        result = 31 * result + uuid.hashCode()
        return result
    }

    private fun loadRevoke() {
        val updateInfo: QChatMsgUpdateInfo = message.updateOperatorInfo ?: return

        val extStr = updateInfo.ext
        if (!TextUtils.isEmpty(extStr)) {
            try {
                val jsonObject = JSONObject(extStr)
                if (jsonObject.has(QChatMsgConstants.KEY_QCHAT_REVOKE_MSG)) {
                    isRevoke = jsonObject.getBoolean(QChatMsgConstants.KEY_QCHAT_REVOKE_MSG)
                }
                if (isRevoke) {
                    revokeText = MessageRevokeHelper.getContent(msgIdServer) ?: ""
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }
}
