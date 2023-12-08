/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.utils

import android.text.TextUtils
import android.util.LongSparseArray
import com.netease.yunxin.kit.common.utils.SPUtils

/** The better way is db than sp.  */
object MessageRevokeHelper {

    @JvmStatic
    fun getContent(msgId: Long): String? {
        var content = getContentFromMemory(msgId)
        if (TextUtils.isEmpty(content)) {
            content = getContentFromLocal(msgId)
        }
        return content
    }

    @JvmStatic
    fun recordContent(msgId: Long, content: String) {
        recordContentToMemory(msgId, content)
        recordContentToLocal(msgId, content)
    }

    // -----------
    private val msgIdContentSparse = LongSparseArray<String>()
    private fun getContentFromMemory(msgId: Long): String? {
        return msgIdContentSparse[msgId]
    }

    private fun recordContentToMemory(msgId: Long, content: String) {
        msgIdContentSparse.put(msgId, content)
    }

    private const val SP_NAME_REVOKE = "self_revoke_info"
    private const val EXTRA_SUFFIX = "_revoke_content"

    private fun getContentFromLocal(msgId: Long): String? {
        return SPUtils.getInstance(SP_NAME_REVOKE).getString(getSPKey(msgId))
    }

    private fun recordContentToLocal(msgId: Long, content: String) {
        SPUtils.getInstance(SP_NAME_REVOKE).put(getSPKey(msgId), content)
    }

    private fun getSPKey(msgId: Long): String {
        return msgId.toString() + EXTRA_SUFFIX
    }
}
