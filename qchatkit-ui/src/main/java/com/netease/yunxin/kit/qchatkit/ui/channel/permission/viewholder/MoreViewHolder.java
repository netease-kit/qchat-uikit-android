// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.channel.permission.viewholder;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import com.netease.yunxin.kit.common.utils.SizeUtils;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.channel.permission.QChatChannelPermissionExpandableHelper;
import com.netease.yunxin.kit.qchatkit.ui.common.CommonViewHolder;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatMoreViewholderLayoutBinding;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatBaseBean;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatMoreBean;

public class MoreViewHolder extends CommonViewHolder<QChatBaseBean> {

  private QChatMoreViewholderLayoutBinding viewBinding;

  public MoreViewHolder(@NonNull View itemView) {
    super(itemView);
  }

  public MoreViewHolder(QChatMoreViewholderLayoutBinding viewBinding) {
    this(viewBinding.getRoot());
    this.viewBinding = viewBinding;
    this.viewBinding
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
    ViewGroup.LayoutParams layoutParams = viewBinding.getRoot().getLayoutParams();
    if (position > QChatChannelPermissionExpandableHelper.NUM_MAX_EXPANDABLE_ITEM) {
      layoutParams.height = SizeUtils.dp2px(50);
    } else {
      layoutParams.height = 0;
    }
    viewBinding.getRoot().setLayoutParams(layoutParams);
    if (data instanceof QChatMoreBean) {
      this.data = data;
      this.position = position;
      QChatMoreBean bean = (QChatMoreBean) data;
      Context context = viewBinding.getRoot().getContext();
      Drawable icon;
      String title;
      if (QChatChannelPermissionExpandableHelper.isExpandable()) {
        title = context.getString(R.string.qchat_pack_up_with_all, bean.extend);
        icon = AppCompatResources.getDrawable(context, R.drawable.ic_up);
      } else {
        title = context.getString(R.string.qchat_more_title, String.valueOf(bean.extend));
        icon = AppCompatResources.getDrawable(context, R.drawable.ic_down);
      }
      viewBinding.qChatVhMoreTv.setText(title);
      icon.setBounds(0, 0, icon.getMinimumWidth(), icon.getMinimumHeight());
      viewBinding.qChatVhMoreTv.setCompoundDrawables(null, null, icon, null);
    }
  }
}
