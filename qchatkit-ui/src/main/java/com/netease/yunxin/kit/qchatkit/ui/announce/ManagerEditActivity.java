// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.announce;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.netease.nimlib.sdk.qchat.enums.QChatRoleOption;
import com.netease.nimlib.sdk.qchat.enums.QChatRoleResource;
import com.netease.nimlib.sdk.qchat.model.QChatMemberRole;
import com.netease.yunxin.kit.common.ui.activities.BaseActivity;
import com.netease.yunxin.kit.common.ui.dialog.ChoiceListener;
import com.netease.yunxin.kit.common.ui.dialog.CommonChoiceDialog;
import com.netease.yunxin.kit.common.ui.utils.AvatarColor;
import com.netease.yunxin.kit.common.ui.viewmodel.LoadStatus;
import com.netease.yunxin.kit.common.utils.NetworkUtils;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatAnnounceMemberInfo;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.announce.viewmodel.ManagerEditViewModel;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatAnncMemberSettingBinding;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant;
import java.util.HashMap;
import java.util.Map;

/** 管理员权限编辑页面 */
public class ManagerEditActivity extends BaseActivity {

  private QChatAnncMemberSettingBinding viewBinding;

  private ManagerEditViewModel viewModel;
  private QChatAnnounceMemberInfo memberInfo;
  private QChatMemberRole memberWithRoleInfo;
  private long serverId;
  private long channelId;
  private long roleId;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    changeStatusBarColor(R.color.color_eff1f4);
    viewBinding = QChatAnncMemberSettingBinding.inflate(getLayoutInflater());
    setContentView(viewBinding.getRoot());
    Intent creatIntent = getIntent();
    memberInfo =
        (QChatAnnounceMemberInfo) creatIntent.getSerializableExtra(QChatConstant.SERVER_MEMBER);
    serverId = creatIntent.getLongExtra(QChatConstant.SERVER_ID, -1);
    channelId = creatIntent.getLongExtra(QChatConstant.CHANNEL_ID, -1);
    roleId = creatIntent.getLongExtra(QChatConstant.SERVER_ROLE_ID, -1);
    if (serverId < 0 || memberInfo == null) {
      finish();
    }
    initView();
    initData();
    loadData();
  }

  private void initView() {
    viewBinding.tvSave.setOnClickListener(
        v -> {
          if (NetworkUtils.isConnected()) {
            // 更新用户个人定制权限
            viewModel.updateMemberRole(
                serverId, channelId, memberInfo.getAccId(), generateOption());
          } else {
            Toast.makeText(
                    getApplicationContext(),
                    getString(R.string.qchat_network_error_tip),
                    Toast.LENGTH_SHORT)
                .show();
          }
        });

    viewBinding.ivBack.setOnClickListener(v -> finish());
    viewBinding.tvQuit.setOnClickListener(v -> showDeleteConfirmDialog());
  }

  private Map<QChatRoleResource, QChatRoleOption> generateOption() {
    Map<QChatRoleResource, QChatRoleOption> opMap = new HashMap<>();
    QChatRoleOption sendOption =
        viewBinding.swSendMessage.isChecked() ? QChatRoleOption.INHERIT : QChatRoleOption.DENY;
    opMap.put(QChatRoleResource.SEND_MSG, sendOption);
    QChatRoleOption editChannelOption =
        viewBinding.swEdit.isChecked() ? QChatRoleOption.INHERIT : QChatRoleOption.DENY;
    opMap.put(QChatRoleResource.MANAGE_CHANNEL, editChannelOption);
    QChatRoleOption deleteOption =
        viewBinding.swDelete.isChecked() ? QChatRoleOption.INHERIT : QChatRoleOption.DENY;
    opMap.put(QChatRoleResource.DELETE_MSG, deleteOption);
    QChatRoleOption managerSub =
        viewBinding.swSubscribe.isChecked() ? QChatRoleOption.INHERIT : QChatRoleOption.DENY;
    opMap.put(QChatRoleResource.MANAGE_ROLE, managerSub);
    QChatRoleOption emojiSub =
        viewBinding.swEmoji.isChecked() ? QChatRoleOption.INHERIT : QChatRoleOption.DENY;
    QChatRoleResource emojiRes =
        new QChatRoleResource(
            QChatConstant.QCHAT_SELF_PERMISSION_EMOJI_REPLAY,
            QChatConstant.QCHAT_PERMISSION_TYPE_ALL);
    opMap.put(emojiRes, emojiSub);
    return opMap;
  }

  private void initData() {
    viewModel = new ViewModelProvider(this).get(ManagerEditViewModel.class);
    viewModel
        .getMemberInfoLiveData()
        .observe(
            this,
            result -> {
              if (result == null) {
                return;
              }
              if (result.getLoadStatus() == LoadStatus.Success) {
                memberWithRoleInfo = result.getData();
                loadData();
              }
            });
    viewModel
        .getKickMemberLiveData()
        .observeForever(
            result -> {
              if (result != null && Boolean.TRUE.equals(result.getData())) {
                finish();
              } else {
                Toast.makeText(
                        getApplicationContext(),
                        getString(R.string.qchat_server_request_fail),
                        Toast.LENGTH_SHORT)
                    .show();
              }
            });
    viewModel
        .getUpdateLiveData()
        .observeForever(
            result -> {
              if (result != null && result.getData()) {
                Toast.makeText(
                        getApplicationContext(),
                        getString(R.string.qchat_announcement_update_success),
                        Toast.LENGTH_SHORT)
                    .show();
                finish();
              } else {
                Toast.makeText(
                        getApplicationContext(),
                        getString(R.string.qchat_announcement_update_fail),
                        Toast.LENGTH_SHORT)
                    .show();
              }
            });
    viewModel.requestMemberInfo(serverId, channelId, memberInfo.getAccId());
  }

  private void loadData() {
    if (memberInfo != null) {
      String titleText =
          String.format(
              getString(R.string.qchat_announcement_member_page_title), memberInfo.getNickName());
      viewBinding.tvTitle.setText(titleText);
      viewBinding.tvName.setText(memberInfo.getNickName());
      viewBinding.ivIcon.setData(
          memberInfo.getAvatarUrl(),
          memberInfo.getNickName(),
          AvatarColor.avatarColor(memberInfo.getNickName()));
      if (memberWithRoleInfo != null && memberWithRoleInfo.getResourceAuths().size() > 0) {
        QChatRoleOption sendOption =
            memberWithRoleInfo.getResourceAuths().get(QChatRoleResource.SEND_MSG);
        viewBinding.swSendMessage.setChecked(sendOption != QChatRoleOption.DENY);
        QChatRoleOption deleteOption =
            memberWithRoleInfo.getResourceAuths().get(QChatRoleResource.DELETE_MSG);
        viewBinding.swDelete.setChecked(deleteOption != QChatRoleOption.DENY);

        QChatRoleOption editOption =
            memberWithRoleInfo.getResourceAuths().get(QChatRoleResource.MANAGE_CHANNEL);
        viewBinding.swEdit.setChecked(editOption != QChatRoleOption.DENY);

        QChatRoleOption subOption =
            memberWithRoleInfo.getResourceAuths().get(QChatRoleResource.MANAGE_ROLE);
        viewBinding.swSubscribe.setChecked(subOption != QChatRoleOption.DENY);

        QChatRoleResource emojiResource =
            new QChatRoleResource(
                QChatConstant.QCHAT_SELF_PERMISSION_EMOJI_REPLAY,
                QChatConstant.QCHAT_PERMISSION_TYPE_ALL);
        QChatRoleOption emojiOption = memberWithRoleInfo.getResourceAuths().get(emojiResource);
        viewBinding.swEmoji.setChecked(emojiOption != QChatRoleOption.DENY);
      }
    }
  }

  private void showDeleteConfirmDialog() {
    String nick = memberInfo.getNickName();
    CommonChoiceDialog dialog = new CommonChoiceDialog();
    dialog
        .setTitleStr(getResources().getString(R.string.qchat_delete_member))
        .setContentStr(
            String.format(getResources().getString(R.string.qchat_delete_some_member), nick))
        .setNegativeStr(getResources().getString(R.string.qchat_cancel))
        .setPositiveStr(getResources().getString(R.string.qchat_sure))
        .setConfirmListener(
            new ChoiceListener() {
              @Override
              public void onPositive() {
                if (NetworkUtils.isConnected()) {
                  viewModel.kickMember(serverId, roleId, channelId, memberInfo.getAccId());
                } else {
                  Toast.makeText(
                          getApplicationContext(),
                          getString(R.string.qchat_network_error_tip),
                          Toast.LENGTH_SHORT)
                      .show();
                }
              }

              @Override
              public void onNegative() {}
            })
        .show(getSupportFragmentManager());
  }

  public static void launch(
      Activity activity,
      QChatAnnounceMemberInfo memberInfo,
      long serverId,
      long roleId,
      long channelId) {
    Intent intent = new Intent(activity, ManagerEditActivity.class);
    intent.putExtra(QChatConstant.SERVER_ID, serverId);
    intent.putExtra(QChatConstant.CHANNEL_ID, channelId);
    intent.putExtra(QChatConstant.SERVER_ROLE_ID, roleId);
    intent.putExtra(QChatConstant.SERVER_MEMBER, memberInfo);
    activity.startActivity(intent);
  }
}
