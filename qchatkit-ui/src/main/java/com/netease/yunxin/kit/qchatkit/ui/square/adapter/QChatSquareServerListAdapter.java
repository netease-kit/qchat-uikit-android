// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.square.adapter;

import android.content.Context;
import android.view.View;
import com.bumptech.glide.Glide;
import com.netease.yunxin.kit.common.utils.SizeUtils;
import com.netease.yunxin.kit.qchatkit.ui.common.QChatCommonAdapter;
import com.netease.yunxin.kit.qchatkit.ui.common.image.GranularRoundedCornersWithCenterCrop;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatSquareSubViewHolderItemBinding;
import com.netease.yunxin.kit.qchatkit.ui.square.model.QChatServerInfoWithJoinState;
import java.util.ArrayList;
import java.util.List;

public class QChatSquareServerListAdapter
    extends QChatCommonAdapter<QChatServerInfoWithJoinState, QChatSquareSubViewHolderItemBinding> {
  private static final String PAYLOADS_UPDATE = "join_state";
  private final float cornerRadius = SizeUtils.dp2px(6);

  private final List<Long> bindedServerIdList = new ArrayList<>();

  public QChatSquareServerListAdapter(Context context) {
    super(context, QChatSquareSubViewHolderItemBinding.class);
  }

  public void updateServerInfo(QChatServerInfoWithJoinState newInfo) {
    if (newInfo == null) {
      return;
    }
    int index = dataSource.indexOf(newInfo);
    if (index < 0) {
      return;
    }
    QChatServerInfoWithJoinState info = dataSource.get(index);
    info.serverInfo = newInfo.serverInfo;
    if (bindedServerIdList.contains(newInfo.serverInfo.getServerId())) {
      notifyItemChanged(index, PAYLOADS_UPDATE);
    }
  }

  public void updateServerJoinedState(QChatServerInfoWithJoinState newInfo) {
    if (newInfo == null) {
      return;
    }
    int index = dataSource.indexOf(newInfo);
    if (index < 0) {
      return;
    }
    QChatServerInfoWithJoinState info = dataSource.get(index);
    info.joined = newInfo.joined;
    if (bindedServerIdList.contains(newInfo.serverInfo.getServerId())) {
      notifyItemChanged(index, PAYLOADS_UPDATE);
    }
  }

  @Override
  public void onBindViewHolder(
      QChatSquareSubViewHolderItemBinding binding,
      int position,
      QChatServerInfoWithJoinState data,
      int bingingAdapterPosition) {
    bindedServerIdList.add(data.serverInfo.getServerId());
    super.onBindViewHolder(binding, position, data, bingingAdapterPosition);
    // 名称
    binding.tvTitle.setText(data.serverInfo.getName());
    // 大图
    Glide.with(context.getApplicationContext())
        .load(data.serverInfo.getIconUrl())
        .transform(new GranularRoundedCornersWithCenterCrop(cornerRadius, cornerRadius, 0, 0))
        .into(binding.ivBigIcon);
    // 小图
    Glide.with(context.getApplicationContext())
        .load(data.serverInfo.getIconUrl())
        .transform(
            new GranularRoundedCornersWithCenterCrop(
                cornerRadius, cornerRadius, cornerRadius, cornerRadius))
        .into(binding.ivSmallIcon);
    // 描述
    binding.tvDesc.setText(data.getDesc());
    // 加入标记
    binding.tvEnterFlag.setVisibility(data.joined ? View.VISIBLE : View.INVISIBLE);
  }
}
