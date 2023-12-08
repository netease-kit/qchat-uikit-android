/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.repo.model

class QChatMessageQuickCommentDetailInfo(
    val serverId: Long,
    val channelId: Long,
    val msgIdServer: Long,
    var totalCount: Int,
    var lastUpdateTime: Long,
    val details: List<QChatQuickCommentDetailInfo>?
)
