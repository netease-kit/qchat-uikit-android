/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.repo.model

class QChatMsgUpdateInfoItem(
    val operatorAccount: String?,
    val operatorClientType: Int = 0,
    val msg: String?,
    val ext: String?,
    val pushContent: String?,
    val pushPayload: String?
) {
    override fun toString(): String {
        return "QChatMsgUpdateInfoItem(operatorAccount=$operatorAccount, operatorClientType=$operatorClientType, msg=$msg, ext=$ext, pushContent=$pushContent, pushPayload=$pushPayload)"
    }
}
