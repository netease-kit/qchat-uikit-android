// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.message.model;

import androidx.annotation.NonNull;
import com.netease.yunxin.kit.corekit.event.BaseEvent;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageInfo;

public class QChatMsgEvent extends BaseEvent {

  public EventType eventType;

  public QChatMessageInfo msgInfo;

  public QChatMsgEvent(EventType eventType, QChatMessageInfo qChatMessageInfo) {
    this.eventType = eventType;
    this.msgInfo = qChatMessageInfo;
  }

  @NonNull
  @Override
  public String getType() {
    return "QChatMsgEvent";
  }

  public enum EventType {
    Delete,
    Revoke
  }
}
