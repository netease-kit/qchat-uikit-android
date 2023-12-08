// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.message.viewholder;

import android.text.TextUtils;
import android.view.View;
import androidx.annotation.NonNull;
import com.netease.yunxin.kit.common.ui.utils.AvatarColor;
import com.netease.yunxin.kit.qchatkit.ui.common.CommonViewHolder;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatChannelMemberStatusViewHolderBinding;
import com.netease.yunxin.kit.qchatkit.ui.message.model.ChannelMemberStatusBean;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatBaseBean;

/** 话题成员列表ViewHolder */
public class QChannelMemberViewHolder extends CommonViewHolder<QChatBaseBean> {

  private QChatChannelMemberStatusViewHolderBinding binding;

  public QChannelMemberViewHolder(@NonNull View itemView) {
    super(itemView);
  }

  public QChannelMemberViewHolder(QChatChannelMemberStatusViewHolderBinding viewBinding) {
    this(viewBinding.getRoot());
    binding = viewBinding;
    binding
        .getRoot()
        .setOnClickListener(
            v -> {
              if (itemListener != null) {
                itemListener.onClick(this.data, this.position);
              }
            });
  }

  @Override
  protected void onBindData(QChatBaseBean data, int position) {
    this.data = data;
    this.position = position;
    if (data instanceof ChannelMemberStatusBean) {
      ChannelMemberStatusBean bean = (ChannelMemberStatusBean) data;
      binding.qChatMemberNameTv.setText(bean.channelMember.getNickName());
      if (!TextUtils.equals(bean.channelMember.getNickName(), bean.channelMember.getAccId())) {
        binding.qChatMemberAccTv.setText(bean.channelMember.getAccId());
        binding.qChatMemberAccTv.setVisibility(View.VISIBLE);
      } else {
        binding.qChatMemberAccTv.setVisibility(View.GONE);
      }
      binding.qChatMemberAvatarIv.setData(
          bean.channelMember.getAvatarUrl(),
          bean.channelMember.getNickName(),
          AvatarColor.avatarColor(bean.channelMember.getAccId()));
      binding.qChatMemberStatusIv.setVisibility(View.GONE);
      binding.qChatMemberCoverView.setVisibility(View.GONE);
    }
  }
}
