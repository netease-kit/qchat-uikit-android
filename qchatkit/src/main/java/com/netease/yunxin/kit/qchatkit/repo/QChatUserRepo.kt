/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.repo

import com.netease.yunxin.kit.corekit.im.model.UserInfo
import com.netease.yunxin.kit.corekit.im.provider.FetchCallback
import com.netease.yunxin.kit.corekit.im.provider.UserInfoProvider

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
        UserInfoProvider.fetchUserInfo(
            list,
            object : FetchCallback<List<UserInfo>> {
                override fun onSuccess(param: List<UserInfo>?) {
                    if (param != null && param.isNotEmpty()) {
                        val user = param[0]
                        callBack.onSuccess(user.avatar)
                    } else {
                        callBack.onSuccess(null)
                    }
                }

                override fun onFailed(code: Int) {
                    callBack.onSuccess(null)
                }

                override fun onException(exception: Throwable?) {
                    callBack.onSuccess(null)
                }
            }
        )
    }
}
