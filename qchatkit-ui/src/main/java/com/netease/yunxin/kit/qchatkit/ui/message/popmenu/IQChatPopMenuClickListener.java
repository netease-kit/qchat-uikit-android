// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.message.popmenu;

import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageInfo;

/** message long click menu event listener */
public interface IQChatPopMenuClickListener {
  default boolean onCopy(QChatMessageInfo messageInfo) {
    return false;
  }

  default boolean onDelete(QChatMessageInfo messageInfo) {
    return false;
  }

  default boolean onRecall(QChatMessageInfo messageInfo) {
    return false;
  }

  default boolean onEmojiClick(QChatMessageInfo messageInfo, int emoji) {
    return false;
  }
}
