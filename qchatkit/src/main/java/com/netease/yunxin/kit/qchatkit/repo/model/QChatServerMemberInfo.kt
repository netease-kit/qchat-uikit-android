/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.repo.model

import android.text.TextUtils
import java.io.Serializable

class QChatServerMemberInfo(
    val serverId: Long,
    val accId: String,
    val nick: String?,
    val avatarUrl: String? = null,
    val type: Int = 0,
    val joinTime: Long,
    val inviter: String,
    val createTime: Long,
    val updateTime: Long,
    val valid: Boolean,
    val custom: String? = null,
    val nextInfo: NextInfo? = null
) : Serializable {
    fun getNickName(): String {
        return if (TextUtils.isEmpty(nick)) accId else nick!!
    }

    override fun toString(): String {
        return "QChatServerMemberInfo(serverId=$serverId, accId='$accId', nick=$nick, avatarUrl=$avatarUrl, type=$type, joinTime=$joinTime, inviter='$inviter', createTime=$createTime, updateTime=$updateTime, valid=$valid, custom=$custom, nextInfo=$nextInfo)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QChatServerMemberWithRoleInfo

        if (serverId != other.serverId) return false
        if (accId != other.accId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = serverId.hashCode()
        result = 31 * result + accId.hashCode()
        return result
    }
}
