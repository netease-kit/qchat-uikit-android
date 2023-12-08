/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.repo.model

import java.io.Serializable

open class QChatServerRoleInfo(
    val serverId: Long,
    val roleId: Long,
    val name: String,
    val type: Int,
    val icon: String?,
    val auths: Map<QChatRoleResourceEnum, QChatRoleOptionEnum>,
    val ext: String?,
    val priority: Long,
    var memberCount: Long,
    val createTime: Long
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) {
            return false
        }

        if (other === this) {
            return true
        }

        val that = other as QChatServerRoleInfo
        return serverId == that.serverId && roleId == that.roleId
    }

    override fun hashCode(): Int {
        var result = serverId.hashCode()
        result = 31 * result + roleId.hashCode()
        return result
    }
}
