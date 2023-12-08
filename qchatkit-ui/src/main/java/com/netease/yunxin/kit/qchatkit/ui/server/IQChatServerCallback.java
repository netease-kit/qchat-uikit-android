// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.server;

public interface IQChatServerCallback {

  /**
   * 圈组所有社区未读数变化通知
   *
   * @param count 当前的未读数
   */
  void updateUnreadCount(int count);
}
