// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.square.model;

public class QChatSquarePageInfo {
  public String title;
  public int serverType;

  public QChatSquarePageInfo(String title, int serverType) {
    this.title = title;
    this.serverType = serverType;
  }

  @Override
  public String toString() {
    return "QChatSquarePageInfo{" + "title='" + title + '\'' + ", serverType=" + serverType + '}';
  }
}
