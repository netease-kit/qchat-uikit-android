// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.app.qchat.network.model;

import com.google.gson.annotations.SerializedName;

public class QChatSquareResponse<T> {

  @SerializedName("code")
  public int code;

  @SerializedName("data")
  public T data;

  @SerializedName("requestId")
  public String requestId;

  @Override
  public String toString() {
    return "QChatSquareResponse{"
        + "code="
        + code
        + ", data="
        + data
        + ", requestId='"
        + requestId
        + '\''
        + '}';
  }
}
