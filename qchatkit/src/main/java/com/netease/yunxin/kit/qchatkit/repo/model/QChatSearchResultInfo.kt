/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */
package com.netease.yunxin.kit.qchatkit.repo.model

class QChatSearchResultInfo(
    @JvmField val serverInfo: QChatServerInfo,
    @JvmField var state: Int = STATE_NOT_JOIN,
    @JvmField var channelInfo: QChatChannelInfo?
) {

    companion object {
        /**
         * user didn't join the server.
         */
        const val STATE_NOT_JOIN = 0

        /**
         * user had joined the server.
         */
        const val STATE_JOINED = 1
    }
}
