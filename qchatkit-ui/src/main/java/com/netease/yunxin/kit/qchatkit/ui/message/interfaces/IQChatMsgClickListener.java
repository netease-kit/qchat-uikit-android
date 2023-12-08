// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.message.interfaces;

import android.view.View;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatQuickCommentDetailInfo;

public interface IQChatMsgClickListener {

  default boolean onMessageLongClick(View view, int position, QChatMessageInfo message) {
    return false;
  }

  default boolean onMessageClick(View view, int position, QChatMessageInfo message) {
    return false;
  }

  default boolean onUserIconClick(View view, int position, QChatMessageInfo messageInfo) {
    return false;
  }

  default boolean onSelfIconClick(View view, int position, QChatMessageInfo messageInfo) {
    return false;
  }

  default boolean onSendFailBtnClick(View view, int position, QChatMessageInfo messageInfo) {
    return false;
  }

  default boolean onReEditRevokeMessage(View view, int position, QChatMessageInfo messageInfo) {
    return false;
  }

  default boolean onQuickCommentMessage(
      View view,
      int position,
      int type,
      QChatQuickCommentDetailInfo quickCommentInfo,
      QChatMessageInfo messageInfo) {
    return false;
  }
}
