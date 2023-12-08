// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.square;

import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerInfo;

public interface IQChatSquareServerItemClickListener {

  /**
   * 具体广场社区点击事件
   *
   * @param serverInfo 社区信息
   * @param joined 当前用户和该社区的点击时的加入状态，true 已加入；false 未加入
   */
  void onItemClick(QChatServerInfo serverInfo, boolean joined);
}
