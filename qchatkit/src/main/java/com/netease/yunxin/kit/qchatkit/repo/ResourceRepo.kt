/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.repo

import com.netease.yunxin.kit.common.utils.FileIOUtils
import com.netease.yunxin.kit.corekit.im2.extend.FetchCallback
import com.netease.yunxin.kit.corekit.im2.extend.ProgressFetchCallback
import com.netease.yunxin.kit.corekit.im2.provider.StorageProvider
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 资源相关业务逻辑接口
 */
object ResourceRepo {

    /**
     * 上传文件到NOS
     * @param file 文件
     * @param callBack 上传结果回调
     */
    @JvmStatic
    fun uploadFile(file: File, callBack: FetchCallback<String>?) {
        StorageProvider.uploadFile(file, callBack)
    }

    /**
     * 写文件
     * @param file 文件
     * @param content 内容
     * @param callBack 写文件地址回调
     */
    @JvmStatic
    fun writeFile(file: File, content: String, callBack: FetchCallback<String>?) {
        CoroutineScope(Dispatchers.Default).launch {
            FileIOUtils.writeFileFromString(file, content)
            callBack?.onSuccess(file.absolutePath)
        }
    }

    /**
     * 写文件并上传到NOS
     * @param file 文件
     * @param content 内容
     * @param callBack 上传结果回调
     */
    @JvmStatic
    fun writeLocalFileAndUploadNOS(
        file: File,
        content: String,
        callBack: FetchCallback<String>?
    ) {
        CoroutineScope(Dispatchers.Default).launch {
            FileIOUtils.writeFileFromString(file, content)
            StorageProvider.uploadFile(file, callBack)
        }
    }

    /**
     * 下载文件
     * @param url 下载地址
     * @param path 下载路径
     */
    @JvmStatic
    fun downloadFile(url: String, path: String, callBack: ProgressFetchCallback<String>?) {
        StorageProvider.downloadFile(url, path, callBack)
    }
}
