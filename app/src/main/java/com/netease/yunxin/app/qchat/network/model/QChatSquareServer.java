// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.app.qchat.network.model;

import com.google.gson.annotations.SerializedName;

public class QChatSquareServer {

  @SerializedName("serverId")
  public long serverId;

  @SerializedName("serverName")
  public String serverName;

  @SerializedName("custom")
  public String custom;

  @SerializedName("icon")
  public String icon;
}
