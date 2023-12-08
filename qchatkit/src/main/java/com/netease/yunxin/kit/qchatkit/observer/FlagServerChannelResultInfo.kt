/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.observer

import com.netease.yunxin.kit.corekit.model.ResultInfo

/**
 * The result with serverId and channelId with flag.
 */
class FlagServerChannelResultInfo<T>(
    val serverId: Long,
    val channelId: Long?,
    val flag: String? = null,
    val resultInfo: ResultInfo<T>
) {
    override fun toString(): String {
        return "FlagServerChannelResultInfo(serverId=$serverId, channelId=$channelId, flag=$flag, resultInfo=$resultInfo)"
    }
}
