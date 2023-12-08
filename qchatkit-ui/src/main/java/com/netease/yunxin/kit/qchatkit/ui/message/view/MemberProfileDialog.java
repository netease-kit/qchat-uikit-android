// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.message.view;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.netease.yunxin.kit.alog.ALog;
import com.netease.yunxin.kit.common.ui.dialog.BaseBottomDialog;
import com.netease.yunxin.kit.common.ui.utils.AvatarColor;
import com.netease.yunxin.kit.corekit.im.provider.FetchCallback;
import com.netease.yunxin.kit.corekit.im.utils.RouterConstant;
import com.netease.yunxin.kit.corekit.qchat.QChatKitClient;
import com.netease.yunxin.kit.corekit.route.XKitRouter;
import com.netease.yunxin.kit.qchatkit.repo.QChatChannelRepo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerMemberInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerRoleInfo;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatMemberProfileLayoutBinding;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant;
import java.util.List;

/** 个人信息弹窗 */
public class MemberProfileDialog extends BaseBottomDialog {

  private static final String TAG = MemberProfileDialog.class.getSimpleName();
  private QChatMemberProfileLayoutBinding viewBinding;
  private QChatServerMemberInfo memberInfo;
  private List<QChatServerRoleInfo> roleInfoList;

  @Nullable
  @Override
  protected View getRootView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
    viewBinding = QChatMemberProfileLayoutBinding.inflate(inflater, container, false);
    return viewBinding.getRoot();
  }

  public void setData(QChatServerMemberInfo member, List<QChatServerRoleInfo> roleInfoList) {
    ALog.d(TAG, "setData");
    memberInfo = member;
    this.roleInfoList = roleInfoList;
  }

  public void setData(QChatServerMemberInfo member, boolean requestRole) {
    ALog.d(TAG, "setData,requestRole:" + requestRole);
    memberInfo = member;
    if (requestRole) {
      requestRoleInfo();
    }
  }

  private void requestRoleInfo() {
    ALog.d(TAG, "requestRoleInfo");
    if (memberInfo == null) {
      return;
    }
    QChatChannelRepo.fetchServerRolesByAccId(
        memberInfo.getServerId(),
        memberInfo.getAccId(),
        0,
        QChatConstant.MEMBER_PAGE_SIZE,
        new FetchCallback<List<QChatServerRoleInfo>>() {
          @Override
          public void onSuccess(@Nullable List<QChatServerRoleInfo> param) {
            ALog.d(TAG, "fetchMemberRoleList", "onSuccess");
            if (param != null) {
              updateData(param);
            }
          }

          @Override
          public void onFailed(int code) {
            ALog.d(TAG, "fetchMemberRoleList", "onFailed:" + code);
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            String errorMsg = exception != null ? exception.getMessage() : "";
            ALog.d(TAG, "fetchMemberRoleList", "onException:" + errorMsg);
          }
        });
  }

  @Override
  protected void initData() {
    super.initData();
    ALog.d(TAG, "initData");
    loadData();
  }

  public void updateData(List<QChatServerRoleInfo> roleInfoList) {
    ALog.d(TAG, "updateData");
    this.roleInfoList = roleInfoList;
    loadData();
  }

  private void loadData() {

    ALog.d(TAG, "loadData");
    if (!TextUtils.isEmpty(memberInfo.getNick())
        && !TextUtils.equals(memberInfo.getAccId(), memberInfo.getNick())) {
      viewBinding.qChatMemberProfileName.setText(memberInfo.getNick());
      viewBinding.qChatMemberProfileNick.setVisibility(View.VISIBLE);
      viewBinding.qChatMemberProfileNick.setText(memberInfo.getAccId());
    } else {
      viewBinding.qChatMemberProfileName.setText(memberInfo.getAccId());
      viewBinding.qChatMemberProfileNick.setVisibility(View.GONE);
    }

    viewBinding.qChatMemberProfileAvatar.setData(
        memberInfo.getAvatarUrl(),
        memberInfo.getNickName(),
        AvatarColor.avatarColor(memberInfo.getAccId()));
    if (roleInfoList != null && roleInfoList.size() >= 1) {
      viewBinding.qChatMemberProfileFlGroup.setData(roleInfoList);
    }
    viewBinding.qChatMemberProfileAvatar.setOnClickListener(
        v -> {
          if (TextUtils.equals(memberInfo.getAccId(), QChatKitClient.account())) {
            XKitRouter.withKey(RouterConstant.PATH_MINE_INFO_PAGE)
                .withContext(v.getContext())
                .navigate();
          } else {
            XKitRouter.withKey(RouterConstant.PATH_USER_INFO_PAGE)
                .withContext(v.getContext())
                .withParam(RouterConstant.KEY_ACCOUNT_ID_KEY, memberInfo.getAccId())
                .navigate();
          }
        });
  }
}
