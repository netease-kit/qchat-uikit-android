// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.server.adapter;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.netease.yunxin.kit.common.ui.utils.AvatarColor;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSearchResultInfo;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.common.QChatCommonAdapter;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatServerSearchResultItemBinding;

/** Adapter for searching server. */
public class QChatSearchResultAdapter
    extends QChatCommonAdapter<QChatSearchResultInfo, QChatServerSearchResultItemBinding> {
  private final boolean isAnnouncement;

  private OnClickListener<QChatSearchResultInfo, QChatServerSearchResultItemBinding>
      tipClickListener;

  public QChatSearchResultAdapter(Context context, boolean isAnnouncement) {
    super(context, QChatServerSearchResultItemBinding.class);
    this.isAnnouncement = isAnnouncement;
  }

  public void setTipClickListener(
      OnClickListener<QChatSearchResultInfo, QChatServerSearchResultItemBinding> tipClickListener) {
    this.tipClickListener = tipClickListener;
  }

  @Override
  public void onBindViewHolder(
      @NonNull ItemViewHolder<QChatServerSearchResultItemBinding> holder,
      int position,
      @NonNull QChatSearchResultInfo data) {
    QChatServerSearchResultItemBinding binding = holder.binding;
    binding.cavIcon.setData(
        data.serverInfo.getIconUrl(),
        data.serverInfo.getName(),
        AvatarColor.avatarColor(data.serverInfo.getServerId()));

    binding.tvName.setText(data.serverInfo.getName());

    binding.tvServerId.setText(String.valueOf(data.serverInfo.getServerId()));

    TextView tvActionAndTip = binding.tvActionAndTip;
    if (isAnnouncement) {
      tvActionAndTip.setVisibility(View.VISIBLE);
      if (data.state == QChatSearchResultInfo.STATE_NOT_JOIN) {
        tvActionAndTip.setText(R.string.qchat_server_state_join);
        tvActionAndTip.setEnabled(true);
      } else if (data.state == QChatSearchResultInfo.STATE_JOINED) {
        tvActionAndTip.setText(R.string.qchat_server_state_joined);
        tvActionAndTip.setEnabled(false);
      }
      tvActionAndTip.setOnClickListener(
          v -> {
            if (tipClickListener != null) {
              tipClickListener.onClick(data, holder);
            }
          });
    } else {
      tvActionAndTip.setVisibility(View.GONE);
    }
  }

  public void updateItemState() {
    notifyItemRangeChanged(0, getItemCount());
  }
}
