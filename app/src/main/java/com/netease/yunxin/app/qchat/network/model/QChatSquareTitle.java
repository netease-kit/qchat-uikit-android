// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.app.qchat.network.model;

import com.google.gson.annotations.SerializedName;

public class QChatSquareTitle {
  @SerializedName("type")
  public int type;

  @SerializedName("title")
  public String title;
}
