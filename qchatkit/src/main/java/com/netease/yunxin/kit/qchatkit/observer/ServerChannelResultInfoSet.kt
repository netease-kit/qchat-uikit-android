/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.observer

import com.netease.yunxin.kit.corekit.model.ResultInfo

/**
 * record a set of result, flag successful result and filed result.
 */
class ServerChannelResultInfoSet<T> {
    private val successfulResult = mutableListOf<FlagServerChannelResultInfo<T>>()
    private val failedResult = mutableListOf<FlagServerChannelResultInfo<T>>()

    fun appendResult(
        serverId: Long,
        channelId: Long? = null,
        flag: String? = null,
        resultInfo: ResultInfo<T>
    ) {
        if (resultInfo.success) {
            appendSuccessfulResult(serverId, channelId, flag, resultInfo)
        } else {
            appendFailedResult(serverId, channelId, flag, resultInfo)
        }
    }

    fun appendSuccessfulResult(
        serverId: Long,
        channelId: Long? = null,
        flag: String? = null,
        resultInfo: ResultInfo<T>
    ) {
        successfulResult.add(FlagServerChannelResultInfo(serverId, channelId, flag, resultInfo))
    }

    fun appendFailedResult(
        serverId: Long,
        channelId: Long? = null,
        flag: String? = null,
        resultInfo: ResultInfo<T>
    ) {
        failedResult.add(FlagServerChannelResultInfo(serverId, channelId, flag, resultInfo))
    }

    fun <R> mergeAllSet(
        set: ServerChannelResultInfoSet<R>,
        convert: (FlagServerChannelResultInfo<R>) -> FlagServerChannelResultInfo<T>
    ) {
        mergeSuccessfulSet(set, convert)
        mergeFailedSet(set, convert)
    }

    fun <R> mergeSuccessfulSet(
        set: ServerChannelResultInfoSet<R>,
        convert: (FlagServerChannelResultInfo<R>) -> FlagServerChannelResultInfo<T>
    ) {
        mergeSet(true, set, convert)
    }

    fun <R> mergeFailedSet(
        set: ServerChannelResultInfoSet<R>,
        convert: (FlagServerChannelResultInfo<R>) -> FlagServerChannelResultInfo<T>
    ) {
        mergeSet(false, set, convert)
    }

    private fun <R> mergeSet(
        success: Boolean,
        set: ServerChannelResultInfoSet<R>,
        convert: (FlagServerChannelResultInfo<R>) -> FlagServerChannelResultInfo<T>
    ) {
        if (success) {
            successfulResult.addAll(
                set.successfulResultList {
                    convert(it)
                }
            )
        } else {
            failedResult.addAll(
                set.failedResultList {
                    convert(it)
                }
            )
        }
    }

    fun <R> getMergedSuccessfulResult(convert: (List<FlagServerChannelResultInfo<T>>) -> R): R {
        return convert(successfulResult.toList())
    }

    fun <R> successfulResultList(convert: (FlagServerChannelResultInfo<T>) -> R): List<R> {
        return successfulResult.mapNotNull(convert)
    }

    fun <R> failedResultList(convert: (FlagServerChannelResultInfo<T>) -> R): List<R> {
        return failedResult.mapNotNull(convert)
    }

    override fun toString(): String {
        return "ServerChannelResultInfoSet(successfulResult=$successfulResult, failedResult=$failedResult)"
    }
}
