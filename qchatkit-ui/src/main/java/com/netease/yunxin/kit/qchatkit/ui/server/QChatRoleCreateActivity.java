// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.server;

import static com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant.REQUEST_MEMBER_SELECTOR_KEY;

import android.app.Activity;
import android.content.Intent;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerRoleMemberInfo;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.common.QChatCallback;
import com.netease.yunxin.kit.qchatkit.ui.common.QChatServerCommonBaseActivity;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatRoleGroupCreatorActivityLayoutBinding;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant;
import com.netease.yunxin.kit.qchatkit.ui.server.adapter.QChatServerMemberListAdapter;
import com.netease.yunxin.kit.qchatkit.ui.server.viewmodel.QChatRoleCreateViewModel;
import com.netease.yunxin.kit.qchatkit.ui.utils.QChatUtils;
import java.util.ArrayList;
import java.util.List;

/** 社区身份组创建页面 */
public class QChatRoleCreateActivity extends QChatServerCommonBaseActivity {

  private QChatRoleGroupCreatorActivityLayoutBinding binding;

  private QChatRoleCreateViewModel viewModel;

  private long serverId;

  private ActivityResultLauncher<Intent> selectorListLauncher;

  private QChatServerMemberListAdapter memberListAdapter;

  @NonNull
  @Override
  public View getContentView() {
    binding = QChatRoleGroupCreatorActivityLayoutBinding.inflate(getLayoutInflater());
    return binding.getRoot();
  }

  @Override
  public void initView() {
    changeStatusBarColor(R.color.color_eef1f4);
    registerResult();
    binding
        .title
        .setOnBackIconClickListener(v -> onBackPressed())
        .setTitle(R.string.qchat_create_new_role_group)
        .setActionText(R.string.qchat_create)
        .setActionEnable(false)
        .setActionTextColor(getResources().getColor(R.color.color_337eff))
        .setActionListener(
            v ->
                QChatUtils.isConnectedToastAndRun(
                    this,
                    () -> {
                      String name = binding.chatRoleName.getText().toString().trim();
                      if (TextUtils.isEmpty(name)) {
                        Toast.makeText(
                                this, R.string.qchat_create_role_empty_name_tip, Toast.LENGTH_SHORT)
                            .show();
                        return;
                      }
                      binding.title.setActionEnable(false);
                      viewModel.createRole(serverId, name);
                    }));

    binding.rlyMemberAdd.setOnClickListener(
        v -> {
          // 跳转进入成员选择页面
          Intent intent =
              new Intent(QChatRoleCreateActivity.this, QChatMemberSelectorActivity.class);
          intent.putExtra(QChatConstant.SERVER_ID, serverId);
          // 过滤已经在列表中的成员
          intent.putExtra(
              QChatConstant.REQUEST_MEMBER_FILTER_LIST,
              new ArrayList<>(viewModel.getSelectedUsers()));
          selectorListLauncher.launch(intent);
        });

    binding.rvMember.setLayoutManager(new LinearLayoutManager(this));
    memberListAdapter =
        new QChatServerMemberListAdapter(
            this,
            item -> {
              // 移除目标成员
              memberListAdapter.removeData(item);
              viewModel.deleteSelectMember(item.getAccId());
            });
    binding.rvMember.setAdapter(memberListAdapter);
    binding.chatRoleName.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {
            binding.title.setActionEnable(!TextUtils.isEmpty(s));
          }

          @Override
          public void afterTextChanged(Editable s) {}
        });
  }

  private void registerResult() {
    selectorListLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
              // 监听成员选择页面结果，将结果添加到列表中
              if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                ArrayList<QChatServerRoleMemberInfo> members =
                    result.getData().getParcelableArrayListExtra(REQUEST_MEMBER_SELECTOR_KEY);
                if (members != null && members.size() > 0) {
                  List<String> accIds = new ArrayList<>();
                  for (QChatServerRoleMemberInfo member : members) {
                    accIds.add(member.getAccId());
                  }
                  viewModel.addSelectMember(accIds);
                  memberListAdapter.addData(members);
                }
              }
            });
  }

  @Override
  public void initData() {
    serverId = getIntent().getLongExtra(QChatConstant.SERVER_ID, 0);
    configServerId(serverId);
    viewModel
        .getCreateResult()
        .observe(
            this,
            aBoolean -> {
              if (aBoolean) {
                finish();
              }
            });

    viewModel
        .getErrorLiveData()
        .observe(
            this, errorMsg -> QChatCallback.showToast(errorMsg.getCode(), getApplicationContext()));
  }

  @Override
  protected void initViewModel() {
    viewModel = new ViewModelProvider(this).get(QChatRoleCreateViewModel.class);
  }

  /**
   * 页面启动方法
   *
   * @param activity 启动页面Activity
   * @param serverId 创建身份组的社区id
   */
  public static void launch(Activity activity, long serverId) {
    Intent intent = new Intent(activity, QChatRoleCreateActivity.class);
    intent.putExtra(QChatConstant.SERVER_ID, serverId);
    activity.startActivity(intent);
  }
}
