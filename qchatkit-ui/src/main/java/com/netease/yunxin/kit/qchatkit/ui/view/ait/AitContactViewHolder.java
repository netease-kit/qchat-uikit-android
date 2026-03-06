// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.view.ait;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.netease.yunxin.kit.common.ui.utils.AvatarColor;
import com.netease.yunxin.kit.qchatkit.ui.databinding.ItemAitContactBinding;
import com.netease.yunxin.kit.qchatkit.ui.model.ait.AitUserInfo;

/** @成员选人列表 ViewHolder */
public class AitContactViewHolder extends RecyclerView.ViewHolder {

  private final ItemAitContactBinding binding;

  public AitContactViewHolder(@NonNull View itemView) {
    super(itemView);
    binding = ItemAitContactBinding.bind(itemView);
  }

  public void bind(AitUserInfo item) {
    if (item == null) return;
    binding.tvName.setText(item.getAitName());
    binding.ivAvatar.setData(
        item.getAvatar(),
        item.getAitName(),
        AvatarColor.avatarColor(item.getAccount()));
  }
}
