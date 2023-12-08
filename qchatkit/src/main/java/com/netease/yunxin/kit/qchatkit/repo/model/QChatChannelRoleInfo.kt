/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.repo.model

class QChatChannelRoleInfo(
    serverId: Long,
    roleId: Long,
    val parentRoleId: Long,
    val channelId: Long,
    name: String,
    icon: String?,
    ext: String?,
    auths: Map<QChatRoleResourceEnum, QChatRoleOptionEnum>,
    type: Int,
    createTime: Long,
    val updateTime: Long
) : QChatServerRoleInfo(serverId, roleId, name, type, icon, auths, ext, 1, 0, createTime) {

    override fun hashCode(): Int {
        var result = serverId.hashCode()
        result = 31 * result + roleId.hashCode()
        return result
    }
}
