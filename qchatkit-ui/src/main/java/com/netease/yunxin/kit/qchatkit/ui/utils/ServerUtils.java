// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.utils;

import com.netease.nimlib.sdk.qchat.model.QChatServer;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerInfo;

public class ServerUtils {

  public static QChatServerInfo convertServerInfo(QChatServer server) {
    if (server == null) {
      return null;
    }
    QChatServerInfo info =
        new QChatServerInfo(
            server.getServerId(),
            server.getName(),
            server.getIcon(),
            server.getOwner(),
            server.getMemberNumber(),
            server.getInviteMode().getValue(),
            server.getApplyMode().getValue(),
            server.isValid(),
            server.getCreateTime(),
            server.getUpdateTime(),
            server.getChannelNum(),
            server.getCustom(),
            server.getSearchType(),
            null);

    return info;
  }
}
