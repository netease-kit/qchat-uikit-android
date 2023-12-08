// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.channel.adapter;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelInfoWithLastMessage;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatUnreadInfoItem;
import com.netease.yunxin.kit.qchatkit.ui.common.QChatCommonAdapter;
import com.netease.yunxin.kit.qchatkit.ui.common.QChatMessageTypeTipUtils;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatFragmentChannelListItemBinding;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QChatFragmentChannelAdapter
    extends QChatCommonAdapter<
        QChatChannelInfoWithLastMessage, QChatFragmentChannelListItemBinding> {

  private static final String PAYLOADS_LAST_MESSAGE = "payloads_last_message";

  private Map<Long, QChatUnreadInfoItem> unreadInfoItemMap;

  private final Map<Long, Integer> channelInfoIndexMap = new HashMap<>();

  public QChatFragmentChannelAdapter(Context context) {
    super(context, QChatFragmentChannelListItemBinding.class);
  }

  public void updateLastMessageInfo(long channelId, QChatMessageInfo messageInfo) {
    Integer index = channelInfoIndexMap.get(channelId);
    if (index == null) {
      return;
    }
    if (index >= 0 && getItemCount() > index) {
      QChatChannelInfoWithLastMessage data = getItemData(index);
      if (data != null) {
        data.setLastMessage(messageInfo);
        notifyItemChanged(index, PAYLOADS_LAST_MESSAGE);
      }
    }
  }

  public void updateUnreadCount(Map<Long, QChatUnreadInfoItem> map) {
    this.unreadInfoItemMap = map;
  }

  @Override
  public void onBindViewHolder(
      @NonNull ItemViewHolder<QChatFragmentChannelListItemBinding> holder,
      int position,
      @NonNull List<Object> payloads) {
    super.onBindViewHolder(holder, position, payloads);
    if (payloads.contains(PAYLOADS_LAST_MESSAGE)) {
      updateForLastMessage(holder.binding.tvChannelMessage, getItemData(position));
    }
  }

  @Override
  public void onBindViewHolder(
      QChatFragmentChannelListItemBinding binding,
      int position,
      QChatChannelInfoWithLastMessage data,
      int bingingAdapterPosition) {
    super.onBindViewHolder(binding, position, data, bingingAdapterPosition);

    channelInfoIndexMap.put(data.getChannelInfo().getChannelId(), bingingAdapterPosition);

    binding.tvChannelTitle.setText(data.getChannelInfo().getName());

    updateForLastMessage(binding.tvChannelMessage, data);

    int count = getUnreadCount(data.getChannelInfo().getChannelId());
    if (count > 0) {
      String content;
      if (count >= 100) {
        content = "99+";
      } else {
        content = String.valueOf(count);
      }
      binding.tvUnReadCount.setText(content);
      binding.tvUnReadCount.setVisibility(View.VISIBLE);
    } else {
      binding.tvUnReadCount.setVisibility(View.GONE);
    }
  }

  private int getUnreadCount(long channelId) {
    if (unreadInfoItemMap == null) {
      return 0;
    }
    QChatUnreadInfoItem item = unreadInfoItemMap.get(channelId);
    if (item == null) {
      return 0;
    }
    return item.getUnreadCount();
  }

  private void updateForLastMessage(TextView view, QChatChannelInfoWithLastMessage info) {
    if (view == null || info == null) {
      return;
    }
    String messageContent =
        QChatMessageTypeTipUtils.getMessageContent(context, info.getLastMessage());
    if (messageContent != null) {
      view.setText(messageContent);
      view.setVisibility(View.VISIBLE);
    } else {
      view.setVisibility(View.GONE);
    }
  }
}
