// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.server.model;

import androidx.annotation.NonNull;
import com.netease.yunxin.kit.corekit.event.BaseEvent;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerInfo;

public class QChatEnterServerEvent extends BaseEvent {
  public static final String TYPE = "QChatEnterServerEvent";

  public final QChatServerInfo serverInfo;

  public QChatEnterServerEvent(QChatServerInfo serverInfo) {
    this.serverInfo = serverInfo;
  }

  @NonNull
  @Override
  public String getType() {
    return TYPE;
  }
}
