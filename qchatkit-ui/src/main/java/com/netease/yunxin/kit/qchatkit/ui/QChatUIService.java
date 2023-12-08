// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui;

import android.content.Context;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import com.netease.yunxin.kit.corekit.im.IMKitClient;
import com.netease.yunxin.kit.qchatkit.QChatService;
import com.netease.yunxin.kit.qchatkit.observer.ObserverUnreadInfoResultHelper;
import com.netease.yunxin.kit.qchatkit.ui.message.emoji.EmojiManager;

/** 圈组启动服务，由外部的provider调用，初始化时机比较超前 */
@Keep
public class QChatUIService extends QChatService {

  @NonNull
  @Override
  public String getServiceName() {
    return "QChatUIKit";
  }

  @NonNull
  @Override
  public QChatService create(@NonNull Context context) {
    // 表情库管理初始化
    EmojiManager.init(context);
    // 监听 IMKitClient 初始化完成通知，收到通知后，初始化圈组未读数管理工具
    IMKitClient.registerInitService(context1 -> ObserverUnreadInfoResultHelper.init());
    return this;
  }
}
