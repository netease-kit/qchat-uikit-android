/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.repo

import com.netease.nimlib.sdk.v2.user.V2NIMUser
import com.netease.yunxin.kit.corekit.im2.extend.FetchCallback
import com.netease.yunxin.kit.corekit.im2.provider.V2UserInfoProvider

/**
 * Q chat user repo
 *
 * @constructor Create empty Qchat user repo
 */
object QChatUserRepo {

    /**
     * 根据ACCID 查询用户头像URL
     */
    @JvmStatic
    fun fetchUserAvatar(
        accId: String,
        callBack: FetchCallback<String>
    ) {
        val list = listOf(accId)
        V2UserInfoProvider.getUserInfo(
            list,
            object : FetchCallback<List<V2NIMUser>> {

                override fun onError(errorCode: Int, errorMsg: String?) {
                    callBack.onError(errorCode, errorMsg)
                }

                override fun onSuccess(data: List<V2NIMUser>?) {
                    if (!data.isNullOrEmpty()) {
                        val user = data[0]
                        callBack.onSuccess(user.avatar)
                    } else {
                        callBack.onSuccess(null)
                    }
                }
            }
        )
    }
}
