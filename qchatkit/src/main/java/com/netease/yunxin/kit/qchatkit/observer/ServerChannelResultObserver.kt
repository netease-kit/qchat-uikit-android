/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.observer

/**
 * the informer of the ServerChannelResult result.
 */
interface ServerChannelResultObserver<T> {

    /**
     * can call this method to inform user the result.
     *
     * @param result result of execution.
     */
    fun onResult(result: ServerChannelResultInfoSet<T>)
}
