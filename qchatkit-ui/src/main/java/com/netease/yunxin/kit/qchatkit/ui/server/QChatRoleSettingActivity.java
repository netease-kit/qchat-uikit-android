// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.server;

import static com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant.ROLE_EVERYONE_TYPE;
import static com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant.SERVER_ROLE_INFO;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.CompoundButton;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.netease.yunxin.kit.common.ui.utils.ToastX;
import com.netease.yunxin.kit.common.utils.NetworkUtils;
import com.netease.yunxin.kit.qchatkit.repo.QChatRoleRepo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatRoleOptionEnum;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatRoleResourceEnum;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerRoleInfo;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.common.QChatCallback;
import com.netease.yunxin.kit.qchatkit.ui.common.QChatServerCommonBaseActivity;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatRolesSettingActivityLayoutBinding;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant;
import java.util.HashMap;
import java.util.Map;

/** 身份组设置页面 */
public class QChatRoleSettingActivity extends QChatServerCommonBaseActivity {

  private QChatRolesSettingActivityLayoutBinding binding;

  /** 身份组信息 */
  private QChatServerRoleInfo role;

  private ActivityResultLauncher<Intent> memberLauncher;

  @NonNull
  @Override
  public View getContentView() {
    binding = QChatRolesSettingActivityLayoutBinding.inflate(getLayoutInflater());
    return binding.getRoot();
  }

  @Override
  public void initView() {
    registerResult();
    changeStatusBarColor(R.color.color_eef1f4);
    binding.title.setOnBackIconClickListener(v -> onBackPressed());
    binding.title.getTitleTextView().setEllipsize(TextUtils.TruncateAt.MIDDLE);
    binding.rlyMemberModify.setOnClickListener(
        v -> {
          // 跳转身份组成员列表页面
          Intent intent = new Intent(QChatRoleSettingActivity.this, QChatRoleMemberActivity.class);
          intent.putExtra(SERVER_ROLE_INFO, role);
          memberLauncher.launch(intent);
        });
  }

  private void registerResult() {
    // 处理在成员列表页面添加或删除成员后，身份组人数变化
    memberLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
              if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                int memberSize =
                    result
                        .getData()
                        .getIntExtra(
                            QChatConstant.REQUEST_MEMBER_SIZE_KEY, (int) role.getMemberCount());
                binding.tvNumber.setText(String.valueOf(memberSize));
              }
            });
  }

  /**
   * 更新身份组权限
   *
   * @param switchCompat 身份组对应的权限开关控件
   * @param key 对应的权限
   */
  private void updateAuths(CompoundButton switchCompat, QChatRoleResourceEnum key) {
    binding.flyProcess.setVisibility(View.VISIBLE);
    Map<QChatRoleResourceEnum, QChatRoleOptionEnum> auths = new HashMap<>();
    auths.put(key, switchCompat.isChecked() ? QChatRoleOptionEnum.DENY : QChatRoleOptionEnum.ALLOW);
    QChatRoleRepo.updateRole(
        role.getServerId(),
        role.getRoleId(),
        null,
        null,
        null,
        auths,
        new QChatCallback<Void>(getApplicationContext()) {
          @Override
          public void onSuccess(@Nullable Void param) {
            switchCompat.toggle();
            super.onSuccess(param);
          }
        }.setProcess(binding.flyProcess));
  }

  @Override
  public void initData() {
    role = (QChatServerRoleInfo) getIntent().getSerializableExtra(SERVER_ROLE_INFO);
    if (role != null) {
      configServerId(role.getServerId());
    }
    String title = getResources().getString(R.string.qchat_role_permission_title);
    binding.title.setTitle(String.format(title, role.getName()));
    binding.chatRoleName.setText(role.getName());

    if (role.getType() == ROLE_EVERYONE_TYPE) {
      binding.rlyMemberModify.setVisibility(View.GONE);
      binding.tvMemberManager.setVisibility(View.GONE);
    } else {
      binding
          .title
          .setActionText(R.string.qchat_save)
          .setActionTextColor(getResources().getColor(R.color.color_337eff))
          .setActionListener(
              v -> {
                if (!NetworkUtils.isConnected()) {
                  ToastX.showShortToast(R.string.qchat_network_error_tip);
                  return;
                }
                // 更新身份组
                String name = binding.chatRoleName.getText().toString().trim();
                QChatRoleRepo.updateRole(
                    role.getServerId(),
                    role.getRoleId(),
                    name,
                    null,
                    null,
                    null,
                    new QChatCallback<Void>(getApplicationContext()) {
                      @Override
                      public void onSuccess(@Nullable Void param) {
                        finish();
                      }
                    });
              });
    }

    binding.tvNumber.setText(String.valueOf(role.getMemberCount()));

    //------------------ 根据具体权限设置控件状态 ---------------------

    binding.scManagerServer.setChecked(
        role.getAuths().get(QChatRoleResourceEnum.MANAGE_SERVER) == QChatRoleOptionEnum.ALLOW);

    binding.scManageChannelPro.setChecked(
        role.getAuths().get(QChatRoleResourceEnum.MANAGE_CHANNEL) == QChatRoleOptionEnum.ALLOW);

    binding.scManagerRole.setChecked(
        role.getAuths().get(QChatRoleResourceEnum.MANAGE_ROLE) == QChatRoleOptionEnum.ALLOW);

    binding.scSendMessage.setChecked(
        role.getAuths().get(QChatRoleResourceEnum.SEND_MSG) == QChatRoleOptionEnum.ALLOW);

    binding.scDeleteMessage.setChecked(
        role.getAuths().get(QChatRoleResourceEnum.DELETE_MSG) == QChatRoleOptionEnum.ALLOW);

    binding.scRevokeMessage.setChecked(
        role.getAuths().get(QChatRoleResourceEnum.RECALL_MSG) == QChatRoleOptionEnum.ALLOW);

    binding.scModifySelfMember.setChecked(
        role.getAuths().get(QChatRoleResourceEnum.ACCOUNT_INFO_SELF) == QChatRoleOptionEnum.ALLOW);

    binding.scKickOut.setChecked(
        role.getAuths().get(QChatRoleResourceEnum.KICK_SERVER) == QChatRoleOptionEnum.ALLOW);

    binding.scModifyOtherMember.setChecked(
        role.getAuths().get(QChatRoleResourceEnum.ACCOUNT_INFO_OTHER) == QChatRoleOptionEnum.ALLOW);

    binding.scInviteMember.setChecked(
        role.getAuths().get(QChatRoleResourceEnum.INVITE_SERVER) == QChatRoleOptionEnum.ALLOW);

    binding.scManagerMember.setChecked(
        role.getAuths().get(QChatRoleResourceEnum.MANAGE_BLACK_WHITE_LIST)
            == QChatRoleOptionEnum.ALLOW);

    setListener();
  }

  /** 监听身份组权限按钮点击，并执行权限更新 */
  private void setListener() {
    binding.rlyManagerServer.setOnClickListener(
        v -> updateAuths(binding.scManagerServer, QChatRoleResourceEnum.MANAGE_SERVER));

    binding.rlyManageChannelPro.setOnClickListener(
        v -> updateAuths(binding.scManageChannelPro, QChatRoleResourceEnum.MANAGE_CHANNEL));

    binding.rlyManagerRole.setOnClickListener(
        v -> updateAuths(binding.scManagerRole, QChatRoleResourceEnum.MANAGE_ROLE));

    binding.rlySendMessage.setOnClickListener(
        v -> updateAuths(binding.scSendMessage, QChatRoleResourceEnum.SEND_MSG));

    binding.rlyDeleteMessage.setOnClickListener(
        v -> updateAuths(binding.scDeleteMessage, QChatRoleResourceEnum.DELETE_MSG));

    binding.rlyRevokeMessage.setOnClickListener(
        v -> updateAuths(binding.scRevokeMessage, QChatRoleResourceEnum.RECALL_MSG));

    binding.rlyModifySelfMember.setOnClickListener(
        v -> updateAuths(binding.scModifySelfMember, QChatRoleResourceEnum.ACCOUNT_INFO_SELF));

    binding.rlyKickOut.setOnClickListener(
        v -> updateAuths(binding.scKickOut, QChatRoleResourceEnum.KICK_SERVER));

    binding.rlyModifyOtherMember.setOnClickListener(
        v -> updateAuths(binding.scModifyOtherMember, QChatRoleResourceEnum.ACCOUNT_INFO_OTHER));

    binding.rlyInviteMember.setOnClickListener(
        v -> updateAuths(binding.scInviteMember, QChatRoleResourceEnum.INVITE_SERVER));

    binding.rlyManagerMember.setOnClickListener(
        v -> updateAuths(binding.scManagerMember, QChatRoleResourceEnum.MANAGE_BLACK_WHITE_LIST));
  }

  @Override
  protected void initViewModel() {}

  /**
   * 页面启动方法
   *
   * @param activity 页面启动activity
   * @param data 社区身份组信息
   */
  public static void launch(Activity activity, QChatServerRoleInfo data) {
    Intent intent = new Intent(activity, QChatRoleSettingActivity.class);
    intent.putExtra(QChatConstant.SERVER_ROLE_INFO, data);
    activity.startActivity(intent);
  }
}
