// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.message.viewholder;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.NonNull;
import com.netease.nimlib.sdk.msg.constant.MsgTypeEnum;
import com.netease.yunxin.kit.alog.ALog;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageInfo;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatBaseMessageViewHolderBinding;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatTextMessageViewHolderBinding;
import com.netease.yunxin.kit.qchatkit.ui.utils.MessageUtil;
import java.util.List;
import java.util.Map;

/** 圈组文本消息ViewHolder */
public class QChatTextMessageViewHolder extends QChatBaseMessageViewHolder {

  private static final String TAG = "QChatTextMsgViewHolder";

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
      String content = data.getMessage().getContent();
      Map<String, Object> remoteExtension = data.getMessage().getRemoteExtension();
      // 先按普通表情渲染
      MessageUtil.identifyFaceExpression(
          textBinding.getRoot().getContext(),
          textBinding.messageText,
          content,
          ImageSpan.ALIGN_BOTTOM);
      // 若消息包含 yxAitMsg 扩展，在已渲染文本上叠加@蓝色高亮
      if (remoteExtension != null && remoteExtension.containsKey("yxAitMsg")) {
        // 先获取当前含 Emoji span 的文本，再叠加 Ait 高亮
        CharSequence current = textBinding.messageText.getText();
        SpannableString spannableWithAit =
            MessageUtil.applyAitHighlight(
                content, remoteExtension, Color.parseColor("#337EFF"));
        // 把 Emoji Span 从已渲染的 text 复制到 spannableWithAit
        if (current instanceof android.text.Spanned) {
          android.text.Spanned spanned = (android.text.Spanned) current;
          Object[] spans = spanned.getSpans(0, spanned.length(), Object.class);
          for (Object span : spans) {
            int spanStart = spanned.getSpanStart(span);
            int spanEnd = spanned.getSpanEnd(span);
            int flags = spanned.getSpanFlags(span);
            if (spanStart >= 0 && spanEnd <= spannableWithAit.length()) {
              spannableWithAit.setSpan(span, spanStart, spanEnd, flags);
            }
          }
        }
        textBinding.messageText.setText(spannableWithAit);
      }
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