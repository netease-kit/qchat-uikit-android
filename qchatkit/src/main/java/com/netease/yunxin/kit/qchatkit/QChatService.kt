/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit

import android.content.Context
import androidx.annotation.Keep
import com.netease.yunxin.kit.corekit.XKitService
import com.netease.yunxin.kit.corekit.startup.Initializer

@Keep
open class QChatService : XKitService {

    override val serviceName: String
        get() = "QChatKit"

    override val versionName: String
        get() = BuildConfig.versionName

    override val appKey: String?
        get() = null

    override fun onMethodCall(method: String, param: Map<String, Any?>?): Any? {
        return null
    }

    override fun create(context: Context): QChatService {
        return this
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
