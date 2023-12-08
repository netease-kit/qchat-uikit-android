// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.message.model;

import com.netease.nimlib.sdk.qchat.enums.QChatQuickCommentOperateType;
import com.netease.nimlib.sdk.qchat.model.QChatQuickComment;

public class QChatQuickCommentImpl implements QChatQuickComment {

  public Long serverId;
  public Long channelId;
  public String msgSenderAccid;
  public Long msgIdServer;
  public Long msgTime;
  public Integer type;
  public String opeAccid;
  public QChatQuickCommentOperateType operateType;

  public QChatQuickCommentImpl(
      Long serverId,
      Long channelId,
      String msgSenderAccid,
      Long msgIdServer,
      Long msgTime,
      Integer type,
      String opeAccid,
      QChatQuickCommentOperateType operateType) {
    this.serverId = serverId;
    this.channelId = channelId;
    this.msgSenderAccid = msgSenderAccid;
    this.msgIdServer = msgIdServer;
    this.msgTime = msgTime;
    this.type = type;
    this.opeAccid = opeAccid;
    this.operateType = operateType;
  }

  @Override
  public Long getServerId() {
    return serverId;
  }

  @Override
  public Long getChannelId() {
    return channelId;
  }

  @Override
  public String getMsgSenderAccid() {
    return msgSenderAccid;
  }

  @Override
  public Long getMsgIdServer() {
    return msgIdServer;
  }

  @Override
  public Long getMsgTime() {
    return msgTime;
  }

  @Override
  public Integer getType() {
    return type;
  }

  @Override
  public String getOpeAccid() {
    return opeAccid;
  }

  @Override
  public QChatQuickCommentOperateType getOperateType() {
    return operateType;
  }
}
