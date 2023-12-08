// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.message.viewholder;

import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.NonNull;
import com.netease.nimlib.sdk.msg.constant.MsgTypeEnum;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageInfo;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatBaseMessageViewHolderBinding;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatTextMessageViewHolderBinding;
import com.netease.yunxin.kit.qchatkit.ui.utils.MessageUtil;

/** 圈组文本消息ViewHolder */
public class QChatTextMessageViewHolder extends QChatBaseMessageViewHolder {

  private QChatTextMessageViewHolderBinding textBinding;

  public QChatTextMessageViewHolder(@NonNull QChatBaseMessageViewHolderBinding parent) {
    super(parent);
  }

  @Override
  public void addContainer() {
    textBinding =
        QChatTextMessageViewHolderBinding.inflate(
            LayoutInflater.from(getParent().getContext()), getContainer(), true);
  }

  @Override
  public void bindData(QChatMessageInfo data, int position, QChatMessageInfo lastMessage) {
    super.bindData(data, position, lastMessage);
    if (data.getMessage().getMsgType() == MsgTypeEnum.text) {
      MessageUtil.identifyFaceExpression(
          textBinding.getRoot().getContext(),
          textBinding.messageText,
          data.getMessage().getContent(),
          ImageSpan.ALIGN_BOTTOM);
    } else {
      //文件消息暂不支持所以展示提示信息
      textBinding.messageText.setText(
          textBinding
              .getRoot()
              .getContext()
              .getResources()
              .getString(R.string.qchat_message_not_support_tips));
    }
  }

  @Override
  public void onMessageRevokeStatus(QChatMessageInfo data) {
    super.onMessageRevokeStatus(data);
    if (revokedViewBinding != null) {
      if (!MessageUtil.revokeMsgIsEdit(data)) {
        revokedViewBinding.tvAction.setVisibility(View.GONE);
      }
    }
  }
}
