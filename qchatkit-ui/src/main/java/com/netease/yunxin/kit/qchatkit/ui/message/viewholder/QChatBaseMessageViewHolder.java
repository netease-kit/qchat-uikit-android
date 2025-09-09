// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.message.viewholder;

import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import com.netease.nimlib.sdk.msg.constant.MsgDirectionEnum;
import com.netease.nimlib.sdk.msg.constant.MsgStatusEnum;
import com.netease.nimlib.sdk.v2.user.V2NIMUser;
import com.netease.yunxin.kit.common.ui.utils.AvatarColor;
import com.netease.yunxin.kit.common.ui.utils.TimeFormatUtils;
import com.netease.yunxin.kit.common.utils.SizeUtils;
import com.netease.yunxin.kit.corekit.im2.IMKitClient;
import com.netease.yunxin.kit.qchatkit.repo.QChatUserRepo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageQuickCommentDetailInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatQuickCommentDetailInfo;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.common.QChatCache;
import com.netease.yunxin.kit.qchatkit.ui.common.QChatCallback;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatBaseMessageViewHolderBinding;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatMessageRevokedViewBinding;
import com.netease.yunxin.kit.qchatkit.ui.message.interfaces.IMessageOptionCallBack;
import com.netease.yunxin.kit.qchatkit.ui.message.interfaces.IQChatMsgClickListener;
import com.netease.yunxin.kit.qchatkit.ui.message.view.QChatMessageAdapter;
import com.netease.yunxin.kit.qchatkit.ui.utils.MessageUtil;
import java.util.List;

/** base message view holder for qchat */
public abstract class QChatBaseMessageViewHolder extends RecyclerView.ViewHolder {

  private static final int SHOW_TIME_INTERVAL = 5 * 60 * 1000;

  boolean isMine = false;

  IMessageOptionCallBack optionCallBack;

  public QChatBaseMessageViewHolderBinding baseViewBinding;

  public QChatMessageRevokedViewBinding revokedViewBinding;
  public QChatMessageInfo currentMessage;

  protected IQChatMsgClickListener msgClickListener;

  protected int position = 0;

  public QChatBaseMessageViewHolder(@NonNull QChatBaseMessageViewHolderBinding viewBiding) {
    super(viewBiding.baseRoot);
    baseViewBinding = viewBiding;
    addContainer();
  }

  public void setOptionCallBack(IMessageOptionCallBack callBack) {
    optionCallBack = callBack;
  }

  public void setQChatMsgClickListener(IQChatMsgClickListener msgClickListener) {
    this.msgClickListener = msgClickListener;
  }

  public void bindData(QChatMessageInfo data, int position, @NonNull List<?> payload) {
    if (!payload.isEmpty()) {
      for (int i = 0; i < payload.size(); ++i) {
        String payloadItem = payload.get(i).toString();
        if (TextUtils.equals(payloadItem, QChatMessageAdapter.STATUS_PAYLOAD)) {
          setStatus(data);
          currentMessage = data;
        } else if (TextUtils.equals(payloadItem, QChatMessageAdapter.REVOKE_PAYLOAD)) {
          if (data.isRevoke()) {
            currentMessage = data;
            onMessageRevoke(data);
            setQuickComment(data);
          }
        } else if (TextUtils.equals(payloadItem, QChatMessageAdapter.REVOKE_STATUS_PAYLOAD)) {
          onMessageRevokeStatus(data);
        } else if (TextUtils.equals(
            payloadItem, QChatMessageAdapter.QUICK_COMMENT_STATUS_PAYLOAD)) {
          setQuickComment(data);
        }
      }
    }
    this.position = position;
  }

  public void bindData(QChatMessageInfo data, int position, QChatMessageInfo lastMessage) {
    currentMessage = data;
    // 清空消息内容，初始化
    baseViewBinding.messageContainer.removeAllViews();
    addContainer();
    onMessageBackgroundConfig(data);
    setUserInfo(data);
    setTime(data, lastMessage);
    if (data.isRevoke()) {
      onMessageRevoke(data);
    }
    setQuickComment(data);
    setMsgClickListener();
    onLayoutConfig(currentMessage);
    setStatus(data);
    this.position = position;
  }

  protected void setTime(QChatMessageInfo messageBean, QChatMessageInfo lastMessage) {
    long createTime =
        messageBean.getTime() == 0 ? System.currentTimeMillis() : messageBean.getTime();
    if (lastMessage != null && createTime - lastMessage.getTime() < SHOW_TIME_INTERVAL) {
      baseViewBinding.tvTime.setVisibility(View.GONE);
    } else {
      baseViewBinding.tvTime.setVisibility(View.VISIBLE);
      baseViewBinding.tvTime.setText(
          TimeFormatUtils.formatMillisecond(itemView.getContext(), createTime));
    }
  }

  protected void setUserInfo(QChatMessageInfo messageBean) {
    boolean isReceive = MessageUtil.isReceivedMessage(messageBean);
    if (isReceive) {
      baseViewBinding.otherUserAvatar.setVisibility(View.VISIBLE);
      String name = messageBean.getName();
      if (messageBean.getFromAccount() != null) {
        QChatUserRepo.fetchUserAvatar(
            messageBean.getFromAccount(),
            new QChatCallback<String>(itemView.getContext()) {
              @Override
              public void onSuccess(@Nullable String param) {
                baseViewBinding.otherUserAvatar.setData(
                    param, name, AvatarColor.avatarColor(messageBean.getFromAccount()));
              }
            });
      }
      baseViewBinding.myAvatar.setVisibility(View.GONE);
      baseViewBinding.messageStatus.setVisibility(View.GONE);
    } else {
      baseViewBinding.myAvatar.setVisibility(View.VISIBLE);
      V2NIMUser userInfo = IMKitClient.currentUser();
      if (userInfo != null) {
        String nickname =
            TextUtils.isEmpty(userInfo.getName()) ? userInfo.getAccountId() : userInfo.getName();
        baseViewBinding.myAvatar.setData(
            userInfo.getAvatar(), nickname, AvatarColor.avatarColor(userInfo.getAccountId()));
      }
      baseViewBinding.otherUserAvatar.setVisibility(View.GONE);
      baseViewBinding.messageStatus.setVisibility(View.VISIBLE);
    }
  }

  protected void onMessageBackgroundConfig(QChatMessageInfo messageBean) {
    boolean isReceive = MessageUtil.isReceivedMessage(messageBean);
    if (isReceive) {
      baseViewBinding.contentWithBottomLayer.setBackgroundResource(
          R.drawable.bg_qchat_message_other);
    } else {
      baseViewBinding.contentWithBottomLayer.setBackgroundResource(
          R.drawable.bg_qchat_message_self);
    }
  }

  /**
   * 设置消息布局 layout 相关设置，由于收/发消息都在同一个布局文件中展示，需要通过此方法来修改 不同消息方向下的布局内容
   *
   * @param messageBean 待展示消息
   */
  protected void onLayoutConfig(QChatMessageInfo messageBean) {
    ConstraintLayout.LayoutParams messageContainerLayoutParams =
        (ConstraintLayout.LayoutParams) baseViewBinding.messageContainer.getLayoutParams();
    ConstraintLayout.LayoutParams messageTopLayoutParams =
        (ConstraintLayout.LayoutParams) baseViewBinding.messageTopGroup.getLayoutParams();
    ConstraintLayout.LayoutParams messageBottomLayoutParams =
        (ConstraintLayout.LayoutParams) baseViewBinding.messageBottomGroup.getLayoutParams();
    ConstraintLayout.LayoutParams signalLayoutParams =
        (ConstraintLayout.LayoutParams) baseViewBinding.llSignal.getLayoutParams();
    ConstraintLayout.LayoutParams statusLayoutParams =
        (ConstraintLayout.LayoutParams) baseViewBinding.messageStatus.getLayoutParams();
    if (MessageUtil.isReceivedMessage(messageBean)) {
      // 收到的消息设置消息体展示居左
      messageContainerLayoutParams.horizontalBias = 0;
      messageTopLayoutParams.horizontalBias = 0;
      messageBottomLayoutParams.horizontalBias = 0;
      baseViewBinding.llSignal.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
    } else {
      // 发送的消息设置消息体展示居右
      messageContainerLayoutParams.horizontalBias = 1;
      messageTopLayoutParams.horizontalBias = 1;
      messageBottomLayoutParams.horizontalBias = 1;
      baseViewBinding.llSignal.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
    }

    int size = SizeUtils.dp2px(10);
    // 设置标记
    signalLayoutParams.rightMargin = size;
    signalLayoutParams.leftMargin = size;
    statusLayoutParams.rightMargin = size;
    // 设置消息体
    messageContainerLayoutParams.rightMargin = size;
    messageContainerLayoutParams.leftMargin = size;
    // 设置消息体上部（回复内容）
    messageTopLayoutParams.rightMargin = size;
    messageTopLayoutParams.leftMargin = size;
    // 设置消息体下部
    messageBottomLayoutParams.rightMargin = size;
    messageBottomLayoutParams.leftMargin = size;
    // 非回复消息修改布局文件默认大小
    if (!messageBean.hasReply()) {
      messageContainerLayoutParams.width = 0;
      messageTopLayoutParams.width = 0;
      messageBottomLayoutParams.width = 0;
    }

    baseViewBinding.llSignal.setLayoutParams(signalLayoutParams);
    baseViewBinding.messageContainer.setLayoutParams(messageContainerLayoutParams);
    baseViewBinding.messageBottomGroup.setLayoutParams(messageBottomLayoutParams);
    baseViewBinding.messageContainer.setLayoutParams(messageContainerLayoutParams);
    baseViewBinding.messageTopGroup.setLayoutParams(messageTopLayoutParams);
  }

  public void setQuickComment(QChatMessageInfo data) {
    QChatMessageQuickCommentDetailInfo quickComment =
        QChatCache.getQuickComment(data.getMsgIdServer());
    if (!data.isRevoke()
        && quickComment != null
        && quickComment.getDetails() != null
        && !quickComment.getDetails().isEmpty()) {
      List<QChatQuickCommentDetailInfo> commentDetails = quickComment.getDetails();
      baseViewBinding.emojiGroup.setData(data, commentDetails);
      baseViewBinding.emojiGroup.setVisibility(View.VISIBLE);
      baseViewBinding.emojiGroup.setItemClickListener(
          (view, type, detailInfo) -> {
            if (msgClickListener != null) {
              //弹出的基准是整体的Message 显示区域
              msgClickListener.onQuickCommentMessage(
                  baseViewBinding.contentWithBottomLayer, position, type, detailInfo, data);
            }
          });
    } else {
      baseViewBinding.emojiGroup.removeAllViews();
      baseViewBinding.emojiGroup.setVisibility(View.GONE);
    }
  }

  protected void setMsgClickListener() {
    if (!TextUtils.equals(IMKitClient.account(), currentMessage.getFromAccount())) {
      baseViewBinding.otherUserAvatar.setOnClickListener(
          v -> {
            if (msgClickListener != null) {
              msgClickListener.onUserIconClick(v, position, currentMessage);
            }
          });
    } else {
      baseViewBinding.myAvatar.setOnClickListener(
          v -> {
            if (msgClickListener != null) {
              msgClickListener.onUserIconClick(v, position, currentMessage);
            }
          });
    }
    baseViewBinding.ivStatus.setOnClickListener(
        v -> {
          if (msgClickListener != null && currentMessage.getSendMsgStatus() == MsgStatusEnum.fail) {
            msgClickListener.onSendFailBtnClick(v, position, currentMessage);
          }
        });
    // 设置消息内容区域长按事件
    baseViewBinding.contentWithBottomLayer.setOnLongClickListener(
        v -> msgClickListener.onMessageLongClick(v, position, currentMessage));
    // 设置消息内容区域点击事件
    baseViewBinding.contentWithBottomLayer.setOnClickListener(
        v -> msgClickListener.onMessageClick(v, position, currentMessage));
  }

  protected void setStatus(QChatMessageInfo messageBean) {

    String myAccId = IMKitClient.account();
    isMine = TextUtils.equals(myAccId, messageBean.getFromAccount());
    if (!isMine) {

      baseViewBinding.messageStatus.setVisibility(View.GONE);
    } else {
      baseViewBinding.messageStatus.setVisibility(View.VISIBLE);
      if (messageBean.getSendMsgStatus() == MsgStatusEnum.sending) {
        baseViewBinding.messageSending.setVisibility(View.VISIBLE);
        baseViewBinding.ivStatus.setVisibility(View.GONE);
      } else if ((messageBean.getSendMsgStatus() == MsgStatusEnum.fail)) {
        baseViewBinding.ivStatus.setVisibility(View.VISIBLE);
        baseViewBinding.messageSending.setVisibility(View.GONE);
      } else {
        baseViewBinding.messageSending.setVisibility(View.GONE);
        baseViewBinding.ivStatus.setVisibility(View.GONE);
      }
    }
  }

  public boolean isReceivedMessage(QChatMessageInfo message) {
    return message.getMessage().getDirect() == MsgDirectionEnum.In;
  }

  public ViewGroup getParent() {
    return baseViewBinding.baseRoot;
  }

  public ViewGroup getContainer() {
    return baseViewBinding.messageContainer;
  }

  public void onMessageRevoke(QChatMessageInfo messageInfo) {
    if (!messageInfo.isRevoke()) {
      baseViewBinding.messageContainer.setEnabled(true);
      return;
    }
    baseViewBinding.messageContainer.setEnabled(false);
    baseViewBinding.messageContainer.removeAllViews();
    addRevokeViewToMessageContainer();
    revokedViewBinding.tvAction.setOnClickListener(
        v -> {
          if (msgClickListener != null) {
            msgClickListener.onReEditRevokeMessage(v, position, messageInfo);
          }
        });
    if (MessageUtil.revokeMsgIsEdit(messageInfo)) {
      revokedViewBinding.tvAction.setVisibility(View.VISIBLE);
    } else {
      revokedViewBinding.tvAction.setVisibility(View.GONE);
    }
  }

  public void addContainer() {}

  public void onDetachedFromWindow() {}

  public void onAttachedToWindow() {}

  public void onMessageRevokeStatus(QChatMessageInfo messageInfo) {}

  private void addRevokeViewToMessageContainer() {
    revokedViewBinding =
        QChatMessageRevokedViewBinding.inflate(
            LayoutInflater.from(baseViewBinding.baseRoot.getContext()),
            baseViewBinding.messageContainer,
            true);
  }
}
