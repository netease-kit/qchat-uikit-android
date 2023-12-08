// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.message.view;

import android.text.TextUtils;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.netease.nimlib.sdk.msg.constant.MsgTypeEnum;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageInfo;
import com.netease.yunxin.kit.qchatkit.ui.message.interfaces.IMessageOptionCallBack;
import com.netease.yunxin.kit.qchatkit.ui.message.interfaces.IQChatMsgClickListener;
import com.netease.yunxin.kit.qchatkit.ui.message.viewholder.QChatBaseMessageViewHolder;
import com.netease.yunxin.kit.qchatkit.ui.message.viewholder.QChatMessageViewHolderFactory;
import java.util.ArrayList;
import java.util.List;

public class QChatMessageAdapter extends RecyclerView.Adapter<QChatBaseMessageViewHolder> {

  public static final String STATUS_PAYLOAD = "messageStatus";
  public static final String REVOKE_PAYLOAD = "messageRevoke";
  public static final String REVOKE_STATUS_PAYLOAD = "messageRevokeStatus";
  public static final String QUICK_COMMENT_STATUS_PAYLOAD = "messageQuickCommentStatus";

  QChatMessageViewHolderFactory viewHolderFactory;

  private List<QChatMessageInfo> messageList = new ArrayList<>();

  IMessageOptionCallBack optionCallBack;

  private IQChatMsgClickListener msgClickListener;

  public QChatMessageAdapter(QChatMessageViewHolderFactory viewHolderFactory) {
    this.viewHolderFactory = viewHolderFactory;
  }

  public void setQChatMsgClickListener(IQChatMsgClickListener msgClickListener) {
    this.msgClickListener = msgClickListener;
  }

  @NonNull
  @Override
  public QChatBaseMessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return viewHolderFactory.getViewHolder(parent, viewType);
  }

  @Override
  public void onBindViewHolder(
      @NonNull QChatBaseMessageViewHolder holder, int position, @NonNull List<Object> payloads) {
    if (payloads.isEmpty()) {
      super.onBindViewHolder(holder, position, payloads);
    } else {
      QChatMessageInfo data = messageList.get(position);
      holder.bindData(data, position, payloads);
    }
  }

  @Override
  public void onBindViewHolder(@NonNull QChatBaseMessageViewHolder holder, int position) {
    QChatMessageInfo data = messageList.get(position);
    QChatMessageInfo lastMessage = null;
    if (position - 1 >= 0) {
      lastMessage = messageList.get(position - 1);
    }
    holder.setOptionCallBack(optionCallBack);
    holder.setQChatMsgClickListener(msgClickListener);
    holder.bindData(data, position, lastMessage);
  }

  @Override
  public int getItemCount() {
    return messageList.size();
  }

  public List<QChatMessageInfo> getMessageList() {
    return messageList;
  }

  public List<QChatMessageInfo> getQChatMessageListByType(MsgTypeEnum typeEnum) {
    List<QChatMessageInfo> msgList = new ArrayList<>();
    for (QChatMessageInfo messageInfo : this.messageList) {
      if (messageInfo.getMsgType() == typeEnum && !messageInfo.isRevoke()) {
        msgList.add(messageInfo);
      }
    }
    return msgList;
  }

  public void setViewHolderFactory(QChatMessageViewHolderFactory viewHolderFactory) {
    this.viewHolderFactory = viewHolderFactory;
  }

  @Override
  public int getItemViewType(int position) {
    return messageList.get(position).getMsgType().getValue();
  }

  @Override
  public void onViewAttachedToWindow(@NonNull QChatBaseMessageViewHolder holder) {
    holder.onAttachedToWindow();
    super.onViewAttachedToWindow(holder);
  }

  @Override
  public void onViewDetachedFromWindow(@NonNull QChatBaseMessageViewHolder holder) {
    holder.onDetachedFromWindow();
    super.onViewDetachedFromWindow(holder);
  }

  public void setOptionCallBack(IMessageOptionCallBack optionCallBack) {
    this.optionCallBack = optionCallBack;
  }

  public void appendMessages(List<QChatMessageInfo> message) {
    int pos = messageList.size();
    messageList.addAll(message);
    notifyItemRangeInserted(pos, message.size());
  }

  public void appendMessages(QChatMessageInfo message) {
    int pos = messageList.size();
    messageList.add(message);
    notifyItemInserted(pos);
  }

  public void updateMessageStatus(QChatMessageInfo message) {
    int pos = getMessageIndex(message);
    if (pos >= 0) {
      messageList.remove(pos);
      messageList.add(pos, message);
      notifyItemChanged(pos, STATUS_PAYLOAD);
    }
  }

  public void updateMessage(QChatMessageInfo message, String playLoad) {
    int pos = getMessageIndex(message);
    if (pos >= 0) {
      messageList.remove(pos);
      messageList.add(pos, message);
      notifyItemChanged(pos, playLoad);
    }
  }

  public void updateMessage(List<Long> msgIdList, String playLoad) {
    if (msgIdList == null || msgIdList.isEmpty()) {
      return;
    }
    for (Long msgId : msgIdList) {
      int pos = getMessageIndex(msgId);
      if (pos >= 0) {
        notifyItemChanged(pos, playLoad);
      }
    }
  }

  public void updateMessagePlayLoad(String playLoad) {
    for (int index = messageList.size() - 1; index >= 0; index--) {
      notifyItemChanged(index, playLoad);
    }
  }

  private int getMessageIndex(QChatMessageInfo message) {
    if (message != null) {
      for (int index = 0; index < messageList.size(); index++) {
        if (TextUtils.equals(message.getUuid(), messageList.get(index).getUuid())) {
          return index;
        }
      }
    }
    return -1;
  }

  private int getMessageIndex(long messageId) {
    for (int index = 0; index < messageList.size(); index++) {
      if (messageId == messageList.get(index).getMsgIdServer()) {
        return index;
      }
    }
    return -1;
  }

  public void forwardMessages(List<QChatMessageInfo> message) {
    messageList.addAll(0, message);
    notifyItemRangeInserted(0, message.size());
  }

  public QChatMessageInfo getFirstMessage() {
    if (messageList.isEmpty()) {
      return null;
    }
    return messageList.get(0);
  }

  public QChatMessageInfo getLastMessage() {
    if (messageList.isEmpty()) {
      return null;
    }
    return messageList.get(messageList.size() - 1);
  }

  public void removeMessage(QChatMessageInfo message) {
    int pos = messageList.indexOf(message);
    if (pos >= 0) {
      messageList.remove(message);
      notifyItemRemoved(pos);
    }
  }

  public void revokeMessage(QChatMessageInfo message) {
    updateMessage(message, REVOKE_PAYLOAD);
  }

  public void updateMessage(QChatMessageInfo message, Object payload) {
    int pos = getMessageIndex(message);
    if (pos >= 0) {
      messageList.set(pos, message);
      notifyItemChanged(pos, payload);
    }
  }

  public interface EndItemBindingListener {
    void onEndItemBinding();
  }
}
