// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.announce.adapter;

import android.content.Context;
import android.view.View;
import com.netease.yunxin.kit.common.ui.utils.AvatarColor;
import com.netease.yunxin.kit.common.utils.ScreenUtils;
import com.netease.yunxin.kit.common.utils.SizeUtils;
import com.netease.yunxin.kit.corekit.im.IMKitClient;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatAnnounceMemberInfo;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.common.QChatCommonAdapter;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatAnnounceMemberItemBinding;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ServerMemberAdapter
    extends QChatCommonAdapter<QChatAnnounceMemberInfo, QChatAnnounceMemberItemBinding> {
  private boolean hasPermission;
  private OnActionClickListener onActionClickListener;

  public ServerMemberAdapter(Context context, boolean hasPermission) {
    super(context, QChatAnnounceMemberItemBinding.class);
    this.hasPermission = hasPermission;
  }

  public ServerMemberAdapter(Context context) {
    this(context, false);
  }

  public void updatePermissionStatus(boolean hasPermission) {
    if (hasPermission == this.hasPermission) {
      return;
    }
    this.hasPermission = hasPermission;
    notifyItemRangeChanged(0, getItemCount());
  }

  @Override
  public void onBindViewHolder(
      QChatAnnounceMemberItemBinding binding,
      int position,
      QChatAnnounceMemberInfo data,
      int bingingAdapterPosition) {
    super.onBindViewHolder(binding, position, data, bingingAdapterPosition);
    binding.tvName.setText(data.getNickName());
    binding.cavIcon.setData(
        data.getAvatarUrl(), data.getNickName(), AvatarColor.avatarColor(data.getAccId()));
    if (data.getUserType() == QChatAnnounceMemberInfo.USER_TYPE_OWNER) {
      binding.tvRole.setText(R.string.qchat_announce_role_owner);
      binding.tvRole.setVisibility(View.VISIBLE);
    } else if (data.getUserType() == QChatAnnounceMemberInfo.USER_TYPE_MANAGER) {
      binding.tvRole.setText(R.string.qchat_announce_role_manager);
      binding.tvRole.setVisibility(View.VISIBLE);
    } else {
      binding.tvRole.setVisibility(View.GONE);
    }
    if (Objects.equals(IMKitClient.account(), data.getAccId())) {
      binding.tvRemove.setVisibility(View.GONE);
    } else {
      binding.tvRemove.setVisibility(hasPermission ? View.VISIBLE : View.GONE);
      binding.tvRemove.setOnClickListener(
          v -> {
            if (onActionClickListener != null) {
              onActionClickListener.onActionClick(data);
            }
          });
    }
    binding
        .getRoot()
        .post(
            () -> {
              // 用于计算用户名称展示长度问题
              int maxWidth = ScreenUtils.getDisplayWidth();
              if (maxWidth <= 0) {
                return;
              }
              // 屏幕宽度去除头像宽度去除左右 margin
              maxWidth = maxWidth - binding.cavIcon.getWidth() - SizeUtils.dp2px(52);
              // 去除用户角色标签宽度及其 margin
              if (binding.tvRole.getVisibility() == View.VISIBLE) {
                maxWidth = maxWidth - binding.tvRole.getWidth() - SizeUtils.dp2px(16);
              }
              // 去除移除按钮宽度
              if (binding.tvRemove.getVisibility() == View.VISIBLE) {
                maxWidth = maxWidth - binding.tvRemove.getWidth();
              }
              // 设置用户昵称最大长度
              binding.tvName.setMaxWidth(maxWidth);
            });
  }

  public List<String> getAccIdList() {
    List<String> accIdList = new ArrayList<>();
    for (QChatAnnounceMemberInfo item : dataSource) {
      accIdList.add(item.getAccId());
    }
    return accIdList;
  }

  public void setActionBtnClickListener(OnActionClickListener onActionClickListener) {
    this.onActionClickListener = onActionClickListener;
  }

  public void removeDataList(List<String> accIds) {
    if (accIds == null) {
      return;
    }
    for (String accId : accIds) {
      removeData(new QChatAnnounceMemberInfo(accId));
    }
  }

  public interface OnActionClickListener {
    void onActionClick(QChatAnnounceMemberInfo data);
  }
}
