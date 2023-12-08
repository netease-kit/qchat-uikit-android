/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.repo.model

enum class QChatRoleResourceEnum(value: Int) {
    NONE(0),
    MANAGE_SERVER(1),
    MANAGE_CHANNEL(2),
    MANAGE_ROLE(3),
    SEND_MSG(4),
    ACCOUNT_INFO_SELF(5),
    INVITE_SERVER(6),
    KICK_SERVER(7),
    ACCOUNT_INFO_OTHER(8),
    RECALL_MSG(9),
    DELETE_MSG(10),
    REMIND_OTHER(11),
    REMIND_EVERYONE(12),
    MANAGE_BLACK_WHITE_LIST(13)
}
