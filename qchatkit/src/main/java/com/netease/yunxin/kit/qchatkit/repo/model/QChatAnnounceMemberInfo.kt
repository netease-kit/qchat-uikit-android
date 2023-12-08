/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.repo.model

import android.text.TextUtils
import java.io.Serializable

class QChatAnnounceMemberInfo @JvmOverloads constructor(
    val accId: String,
    val nick: String? = null,
    val avatarUrl: String? = null,
    val userType: Int = USER_TYPE_NORMAL,
    val createTime: Long = 0L,
    val joinTime: Long = 0L,
    val nextInfo: NextInfo? = null
) : Serializable {

    fun getNickName(): String = if (TextUtils.isEmpty(nick)) accId else nick!!

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QChatAnnounceMemberInfo

        if (accId != other.accId) return false

        return true
    }

    override fun hashCode(): Int {
        return accId.hashCode()
    }

    override fun toString(): String {
        return "QChatAnnounceMemberInfo(accId='$accId', nick=$nick, avatarUrl=$avatarUrl, createTime=$createTime, joinTime=$joinTime, userType=$userType, nextInfo=$nextInfo)"
    }

    companion object {

        const val USER_TYPE_OWNER = 3

        const val USER_TYPE_MANAGER = 2

        const val USER_TYPE_NORMAL = 1
    }
}
