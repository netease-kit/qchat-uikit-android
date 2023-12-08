// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.server.adapter;

import android.content.Context;
import android.view.View;
import androidx.annotation.NonNull;
import com.netease.yunxin.kit.common.ui.utils.AvatarColor;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerRoleMemberInfo;
import com.netease.yunxin.kit.qchatkit.ui.common.QChatCommonAdapter;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatRoleMemberViewHolderBinding;

public class QChatServerMemberListAdapter
    extends QChatCommonAdapter<QChatServerRoleMemberInfo, QChatRoleMemberViewHolderBinding> {

  private final DeleteListener deleteListener;

  private final SelectListener selectListener;

  private boolean isDelete;

  private boolean isSelect;

  public QChatServerMemberListAdapter(Context context, DeleteListener deleteListener) {
    super(context, QChatRoleMemberViewHolderBinding.class);
    this.deleteListener = deleteListener;
    this.selectListener = null;
    isDelete = true;
  }

  public QChatServerMemberListAdapter(Context context, SelectListener selectListener) {
    super(context, QChatRoleMemberViewHolderBinding.class);
    this.selectListener = selectListener;
    this.deleteListener = null;
    isSelect = true;
  }

  @Override
  public void onBindViewHolder(
      @NonNull ItemViewHolder<QChatRoleMemberViewHolderBinding> holder,
      int position,
      @NonNull QChatServerRoleMemberInfo data) {
    QChatRoleMemberViewHolderBinding binding = holder.binding;
    if (isSelect) {
      binding.rbCheck.setOnCheckedChangeListener(
          (buttonView, isChecked) -> {
            data.setSelected(isChecked);
            if (selectListener != null) {
              selectListener.onSelected(data, isChecked);
            }
          });
      binding.rbCheck.setChecked(data.getSelected());
      binding.rbCheck.setVisibility(View.VISIBLE);
      binding
          .getRoot()
          .setOnClickListener(
              v -> {
                if (clickListener != null) {
                  clickListener.onClick(data, holder);
                }
              });
    } else {
      binding.rbCheck.setVisibility(View.GONE);
    }
    if (isDelete) {
      binding.ivDelete.setVisibility(View.VISIBLE);
      binding.ivDelete.setOnClickListener(
          v -> {
            if (deleteListener != null) {
              deleteListener.deleteClick(data);
            }
          });
    } else {
      binding.ivDelete.setVisibility(View.GONE);
    }
    binding.avatar.setData(
        data.getAvatarUrl(), data.getNickName(), AvatarColor.avatarColor(data.getAccId()));
    binding.tvName.setText(data.getNickName());
  }

  public interface DeleteListener {
    void deleteClick(QChatServerRoleMemberInfo item);
  }

  public interface SelectListener {
    void onSelected(QChatServerRoleMemberInfo item, boolean selected);
  }
}
