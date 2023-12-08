// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.server;

import static com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant.ROLE_EVERYONE_TYPE;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.netease.yunxin.kit.common.ui.activities.adapter.CommonMoreRecyclerViewDecorator;
import com.netease.yunxin.kit.common.ui.viewmodel.FetchResult;
import com.netease.yunxin.kit.common.ui.viewmodel.LoadStatus;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerRoleInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerRoleResult;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.common.QChatServerCommonBaseActivity;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatRoleListActivityLayoutBinding;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant;
import com.netease.yunxin.kit.qchatkit.ui.server.adapter.QChatServerRolesAdapter;
import com.netease.yunxin.kit.qchatkit.ui.server.viewmodel.QChatRoleListViewModel;
import java.util.List;

/** 社区身份组列表页面 */
public class QChatRoleListActivity extends QChatServerCommonBaseActivity {

  private QChatRoleListActivityLayoutBinding binding;

  private QChatServerRoleInfo everyOneRole;

  private QChatServerInfo serverInfo;

  private QChatServerRolesAdapter rolesAdapter;

  private QChatRoleListViewModel viewModel;

  @NonNull
  @Override
  public View getContentView() {
    binding = QChatRoleListActivityLayoutBinding.inflate(getLayoutInflater());
    return binding.getRoot();
  }

  @Override
  public void initView() {
    binding
        .title
        .setOnBackIconClickListener(v -> onBackPressed())
        .setTitle(R.string.qchat_roles)
        .setActionImg(R.drawable.ic_add)
        .setActionListener(
            v -> {
              // 进入社区身份组创建页面
              QChatRoleCreateActivity.launch(this, serverInfo.getServerId());
            });
    binding.clEveryone.setOnClickListener(
        v -> {
          if (everyOneRole != null) {
            gotoRoleInfo(everyOneRole);
          }
        });
    binding.tvSort.setOnClickListener(
        v -> {
          if (rolesAdapter.getItemCount() > 0) {
            QChatRoleSortActivity.launch(this, serverInfo);
          }
        });
  }

  @Override
  public void initData() {
    serverInfo = (QChatServerInfo) getIntent().getSerializableExtra(QChatConstant.SERVER_INFO);
    if (serverInfo != null) {
      configServerId(serverInfo.getServerId());
    }
    LinearLayoutManager layoutManager = new LinearLayoutManager(this);
    binding.rvRoles.setLayoutManager(layoutManager);
    rolesAdapter = new QChatServerRolesAdapter();
    rolesAdapter.setItemClickListener((item, position) -> gotoRoleInfo(item));
    binding.rvRoles.setAdapter(rolesAdapter);
    viewModel
        .getRoleListLiveData()
        .observeForever(
            result -> {
              if (result.getLoadStatus() == LoadStatus.Finish && result.getData() != null) {
                ServerRoleResult param = result.getData();
                if (param != null
                    && param.getRoleList() != null
                    && !param.getRoleList().isEmpty()) {
                  List<QChatServerRoleInfo> roleInfoList = param.getRoleList();
                  QChatServerRoleInfo roleInfo = roleInfoList.get(0);
                  if (roleInfo != null && roleInfo.getType() == ROLE_EVERYONE_TYPE) {
                    binding.tvName.setText(roleInfo.getName());
                    everyOneRole = roleInfo;
                    roleInfoList.remove(0);
                  }
                  if (result.getType() == FetchResult.FetchType.Init) {
                    rolesAdapter.refresh(roleInfoList);
                    if (!roleInfoList.isEmpty()) {
                      binding.rlyRoleTitle.setVisibility(View.VISIBLE);
                      binding.tvRoleCount.setText(
                          getString(
                              R.string.qchat_roles_count, String.valueOf(roleInfoList.size())));
                    } else {
                      binding.rlyRoleTitle.setVisibility(View.GONE);
                    }
                  } else {
                    rolesAdapter.append(roleInfoList);
                  }
                }
              } else if (result.getLoadStatus() == LoadStatus.Error) {
                binding.tvRoleCount.setText(getString(R.string.qchat_roles_count, "0"));
              }
            });
    viewModel
        .getRolePermissionLiveData()
        .observeForever(
            result -> {
              if (result.getLoadStatus() == LoadStatus.Success && result.getData() != null) {
                boolean hasPermission = result.getData();
                if (hasPermission) {
                  binding.tvSort.setVisibility(View.VISIBLE);
                  binding.title.getRightImageView().setVisibility(View.VISIBLE);
                } else {
                  binding.title.getRightImageView().setVisibility(View.GONE);
                  binding.tvSort.setVisibility(View.GONE);
                }
              }
            });
    CommonMoreRecyclerViewDecorator<QChatServerRoleInfo> decorator =
        new CommonMoreRecyclerViewDecorator<>(binding.rvRoles, layoutManager, rolesAdapter);
    decorator.setLoadMoreListener(data -> getRolesList(data == null ? 0 : data.getCreateTime()));
  }

  @Override
  protected void onResume() {
    super.onResume();
    getRolesList(0);
    viewModel.checkPermission();
  }

  /**
   * 进入社区身份组设置页面
   *
   * @param info 目标身份组信息
   */
  private void gotoRoleInfo(QChatServerRoleInfo info) {
    QChatRoleSettingActivity.launch(this, info);
  }

  private void getRolesList(long timeTag) {
    viewModel.fetchServerRoles(serverInfo.getServerId(), timeTag);
  }

  @Override
  protected void initViewModel() {
    viewModel = new ViewModelProvider(this).get(QChatRoleListViewModel.class);
  }
}
