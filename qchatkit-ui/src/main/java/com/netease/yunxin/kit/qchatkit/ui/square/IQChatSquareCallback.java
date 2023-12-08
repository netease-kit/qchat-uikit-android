// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.square;

import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerInfo;

public interface IQChatSquareCallback {

  /**
   * 进入圈组社区列表
   *
   * @param serverInfo 社区信息
   * @param joined 点击社区时加入服务器的状态，true 已加入；false 未加入
   */
  void onEnterQChatServer(QChatServerInfo serverInfo, boolean joined);
}
