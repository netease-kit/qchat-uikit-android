// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.square.model;

import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerInfo;
import java.util.Objects;
import org.json.JSONException;
import org.json.JSONObject;

public class QChatServerInfoWithJoinState {
  public QChatServerInfo serverInfo;
  public boolean joined = false;

  public QChatServerInfoWithJoinState(QChatServerInfo serverInfo) {
    this.serverInfo = serverInfo;
  }

  public QChatServerInfoWithJoinState(QChatServerInfo serverInfo, boolean joined) {
    this.serverInfo = serverInfo;
    this.joined = joined;
  }

  public String getDesc() {
    if (serverInfo == null || serverInfo.getCustom() == null) {
      return null;
    }
    String result;
    try {
      JSONObject object = new JSONObject(serverInfo.getCustom());
      result = object.optString("topic");
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    QChatServerInfoWithJoinState that = (QChatServerInfoWithJoinState) o;

    return Objects.equals(serverInfo, that.serverInfo);
  }

  @Override
  public int hashCode() {
    return serverInfo != null ? serverInfo.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "QChatServerInfoWithJoinState{"
        + "serverInfo="
        + serverInfo
        + ", joined="
        + joined
        + '}';
  }
}
