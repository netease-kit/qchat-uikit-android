// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.server;

import static com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant.REQUEST_MEMBER_SELECTOR_KEY;
import static com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant.REQUEST_MEMBER_SIZE_KEY;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.netease.nimlib.sdk.qchat.result.QChatRemoveMembersFromServerRoleResult;
import com.netease.yunxin.kit.common.ui.dialog.ChoiceListener;
import com.netease.yunxin.kit.common.ui.dialog.CommonChoiceDialog;
import com.netease.yunxin.kit.corekit.im.provider.FetchCallback;
import com.netease.yunxin.kit.qchatkit.repo.QChatRoleRepo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerRoleInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerRoleMemberInfo;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.common.LoadMoreRecyclerViewDecorator;
import com.netease.yunxin.kit.qchatkit.ui.common.QChatCallback;
import com.netease.yunxin.kit.qchatkit.ui.common.QChatServerCommonBaseActivity;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatRoleMemberActivityLayoutBinding;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant;
import com.netease.yunxin.kit.qchatkit.ui.server.adapter.QChatServerMemberListAdapter;
import com.netease.yunxin.kit.qchatkit.ui.utils.QChatUtils;
import java.util.ArrayList;
import java.util.List;

/** 圈组社区身份组成员列表页面 */
public class QChatRoleMemberActivity extends QChatServerCommonBaseActivity {

  private static final int PAGE_SIZE = 20;

  private QChatRoleMemberActivityLayoutBinding binding;

  private QChatServerRoleInfo roleInfo;

  private QChatServerMemberListAdapter memberAdapter;

  private ActivityResultLauncher<Intent> selectorListLauncher;

  private LoadMoreRecyclerViewDecorator<QChatServerRoleMemberInfo> decorator;

  @NonNull
  @Override
  public View getContentView() {
    binding = QChatRoleMemberActivityLayoutBinding.inflate(getLayoutInflater());
    return binding.getRoot();
  }

  @Override
  public void initView() {
    registerResult();
    binding
        .title
        .setOnBackIconClickListener(v -> onBackPressed())
        .setTitle(R.string.qchat_member_manager);
    binding.rlyMemberModify.setOnClickListener(
        v -> {
          // 为身份组添加成员
          Intent intent =
              new Intent(QChatRoleMemberActivity.this, QChatMemberSelectorActivity.class);
          intent.putExtra(QChatConstant.SERVER_ID, roleInfo.getServerId());
          intent.putExtra(
              QChatConstant.REQUEST_MEMBER_FILTER_KEY, QChatConstant.REQUEST_MEMBER_FILTER_ROLE);
          intent.putExtra(QChatConstant.SERVER_ROLE_ID, roleInfo.getRoleId());
          selectorListLauncher.launch(intent);
        });
  }

  private void registerResult() {
    selectorListLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
              // 身份组添加成员后，在本页面加入到目标身份组
              if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                ArrayList<QChatServerRoleMemberInfo> members =
                    result.getData().getParcelableArrayListExtra(REQUEST_MEMBER_SELECTOR_KEY);
                if (members != null && members.size() > 0) {
                  List<String> accIds = new ArrayList<>();
                  for (QChatServerRoleMemberInfo member : members) {
                    accIds.add(member.getAccId());
                  }
                  addMembers(accIds);
                }
              }
            });
  }

  @Override
  public void initData() {
    roleInfo =
        (QChatServerRoleInfo) getIntent().getSerializableExtra(QChatConstant.SERVER_ROLE_INFO);
    if (roleInfo == null) return;
    configServerId(roleInfo.getServerId());
    binding.tvNumber.setText(String.valueOf(roleInfo.getMemberCount()));
    LinearLayoutManager layoutManager = new LinearLayoutManager(this);
    binding.rvMembers.setLayoutManager(layoutManager);
    memberAdapter = new QChatServerMemberListAdapter(this, this::showDeleteConfirmDialog);
    binding.rvMembers.setAdapter(memberAdapter);
    decorator =
        new LoadMoreRecyclerViewDecorator<>(binding.rvMembers, layoutManager, memberAdapter);
    decorator.setLoadMoreListener(
        data ->
            getMembers(
                data == null ? 0 : data.getCreateTime(), data == null ? null : data.getAccId()));
  }

  @Override
  protected void onResume() {
    super.onResume();
    getMembers();
  }

  private void showDeleteConfirmDialog(QChatServerRoleMemberInfo item) {
    String nick = item.getNickName();
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
                deleteMember(item);
              }

              @Override
              public void onNegative() {
                // do nothing
              }
            })
        .show(getSupportFragmentManager());
  }

  /**
   * 删除身份组成员
   *
   * @param item 待删除的身份组成员
   */
  private void deleteMember(QChatServerRoleMemberInfo item) {
    List<String> accIds = new ArrayList<>();
    accIds.add(item.getAccId());
    QChatRoleRepo.removeServerRoleMemberForResult(
        roleInfo.getServerId(),
        roleInfo.getRoleId(),
        accIds,
        new FetchCallback<QChatRemoveMembersFromServerRoleResult>() {
          @Override
          public void onSuccess(@Nullable QChatRemoveMembersFromServerRoleResult param) {
            if (param != null
                && (param.getFailedAccids() == null || param.getFailedAccids().isEmpty())) {
              memberAdapter.removeData(item);
              roleInfo.setMemberCount(roleInfo.getMemberCount() - 1);
              binding.tvNumber.setText(String.valueOf(roleInfo.getMemberCount()));
            } else {
              getMembers();
            }
          }

          @Override
          public void onFailed(int code) {
            QChatUtils.operateError(code);
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            QChatUtils.operateError(-1);
          }
        });
  }

  /**
   * 将用户列表批量添加到当前身份组
   *
   * @param accIds 用户成员 accId 列表
   */
  private void addMembers(List<String> accIds) {
    QChatRoleRepo.addServerRoleMember(
        roleInfo.getServerId(),
        roleInfo.getRoleId(),
        accIds,
        new QChatCallback<List<String>>(getApplicationContext()) {
          @Override
          public void onSuccess(@Nullable List<String> param) {
            if (param != null && !param.isEmpty()) {
              getMembers();
              roleInfo.setMemberCount(roleInfo.getMemberCount() + accIds.size());
              binding.tvNumber.setText(String.valueOf(roleInfo.getMemberCount()));
            }
          }
        });
  }

  /** 获取成员列表 */
  private void getMembers() {
    getMembers(0, null);
  }

  private void getMembers(long timeTag, String accId) {
    QChatRoleRepo.fetchServerRoleMember(
        roleInfo.getServerId(),
        roleInfo.getRoleId(),
        timeTag,
        PAGE_SIZE,
        accId,
        new QChatCallback<List<QChatServerRoleMemberInfo>>(getApplicationContext()) {
          @Override
          public void onSuccess(@Nullable List<QChatServerRoleMemberInfo> param) {
            if (timeTag == 0 && param != null) {
              memberAdapter.addDataList(param, true);
              roleInfo.setMemberCount(memberAdapter.getItemCount());
              binding.tvNumber.setText(String.valueOf(memberAdapter.getItemCount()));
            }
            if (decorator != null) {
              if (timeTag == 0) {
                decorator.init();
              }
              decorator.notifyResult(true);
            }
          }
        });
  }

  @Override
  public void onBackPressed() {
    Intent result = new Intent();
    result.putExtra(REQUEST_MEMBER_SIZE_KEY, memberAdapter.getItemCount());
    setResult(RESULT_OK, result);
    super.onBackPressed();
  }

  @Override
  protected void initViewModel() {}

  /**
   * 页面启动方法
   *
   * @param activity 页面启动 activity
   * @param data 身份组所在社区信息
   */
  public static void launch(Activity activity, QChatServerRoleInfo data) {
    Intent intent = new Intent(activity, QChatRoleMemberActivity.class);
    intent.putExtra(QChatConstant.SERVER_ROLE_INFO, data);
    activity.startActivity(intent);
  }
}
