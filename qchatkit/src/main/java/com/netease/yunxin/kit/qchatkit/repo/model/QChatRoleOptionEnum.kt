/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.repo.model

enum class QChatRoleOptionEnum {
    ALLOW, DENY, INHERIT;

    companion object {
        fun typeOfValue(type: Int): QChatRoleOptionEnum {
            return when (type) {
                0 -> ALLOW
                1 -> DENY
                2 -> INHERIT
                else -> INHERIT
            }
        }
    }
}
