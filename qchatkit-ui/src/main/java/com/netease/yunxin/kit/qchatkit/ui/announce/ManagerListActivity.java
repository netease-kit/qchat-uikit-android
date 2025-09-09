// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.announce;

import static com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant.REQUEST_MEMBER_SELECTOR_KEY;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.netease.yunxin.kit.common.ui.activities.BaseActivity;
import com.netease.yunxin.kit.common.ui.dialog.ChoiceListener;
import com.netease.yunxin.kit.common.ui.dialog.CommonChoiceDialog;
import com.netease.yunxin.kit.common.utils.NetworkUtils;
import com.netease.yunxin.kit.corekit.im2.IMKitClient;
import com.netease.yunxin.kit.corekit.model.ErrorMsg;
import com.netease.yunxin.kit.corekit.model.ResultInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.NextInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatAnnounceMemberInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerRoleMemberInfo;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.announce.adapter.ServerMemberAdapter;
import com.netease.yunxin.kit.qchatkit.ui.announce.viewmodel.RoleMemberViewModel;
import com.netease.yunxin.kit.qchatkit.ui.common.LoadMoreRecyclerViewDecorator;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatAnnounceMemberListActivityBinding;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant;
import com.netease.yunxin.kit.qchatkit.ui.server.QChatMemberSelectorActivity;
import com.netease.yunxin.kit.qchatkit.ui.utils.QChatUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 公告频道管理员页面（包含公告频道创建者及管理员，不包含普通订阅者） */
public class ManagerListActivity extends BaseActivity {

  private static final String PARAM_KEY_SERVER_ID = "key_server_id";
  private static final String PARAM_KEY_CHANNEL_ID = "key_channel_id";
  private static final String PARAM_KEY_ROLE_ID = "key_role_id";
  private static final String PARAM_KEY_OWNER_ID = "key_owner_id";

  private final RoleMemberViewModel viewModel = new RoleMemberViewModel();
  private QChatAnnounceMemberListActivityBinding binding;
  private ServerMemberAdapter adapter;
  private LoadMoreRecyclerViewDecorator<QChatAnnounceMemberInfo> loadMoreRecyclerViewDecorator;

  /** 监听获取成员列表初始化结果通知 */
  private final Observer<ResultInfo<List<QChatAnnounceMemberInfo>>> observerForInit =
      new ObserverWrapper(true);
  /** 监听获取成员列表更多数据结果通知 */
  private final Observer<ResultInfo<List<QChatAnnounceMemberInfo>>> observerForLoadMore =
      new ObserverWrapper(false);

  /** 监听成员被踢出结果通知 */
  private final Observer<ResultInfo<String>> observerForKickMember =
      result -> {
        if (isFinishing()) {
          return;
        }
        if (result.getSuccess() && result.getValue() != null) {
          adapter.removeData(new QChatAnnounceMemberInfo(result.getValue()));
        }
      };

  /** 监听成员邀请结果通知 */
  private final Observer<ResultInfo<List<String>>> observerForInviteMembers =
      result -> {
        if (isFinishing()) {
          return;
        }
        if (result.getSuccess()) {
          if (result.getValue() == null || result.getValue().isEmpty()) {
            Toast.makeText(
                    ManagerListActivity.this,
                    getString(R.string.qchat_server_request_fail),
                    Toast.LENGTH_SHORT)
                .show();
            return;
          }
          viewModel.init();
        } else if (NetworkUtils.isConnected()) {
          ErrorMsg msg = result.getMsg();
          Toast.makeText(
                  ManagerListActivity.this,
                  getString(R.string.qchat_server_request_fail)
                      + (msg != null ? msg.getCode() : ""),
                  Toast.LENGTH_SHORT)
              .show();
        } else {
          Toast.makeText(
                  ManagerListActivity.this,
                  getString(R.string.qchat_network_error_tip),
                  Toast.LENGTH_SHORT)
              .show();
        }
      };

  private final Observer<List<String>> observerForRemoveMembers =
      accIds -> adapter.removeDataList(accIds);

  private final Observer<Object> observerForFinish = o -> finish();

  private ActivityResultLauncher<Intent> launcher;
  private long serverId;
  private long roleId;
  private String ownerId;

  private long channelId;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = QChatAnnounceMemberListActivityBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    serverId = getIntent().getLongExtra(PARAM_KEY_SERVER_ID, 0);
    roleId = getIntent().getLongExtra(PARAM_KEY_ROLE_ID, 0);
    ownerId = getIntent().getStringExtra(PARAM_KEY_OWNER_ID);
    channelId = getIntent().getLongExtra(PARAM_KEY_CHANNEL_ID, 0);
    if (serverId <= 0 || channelId <= 0 || roleId <= 0 || TextUtils.isEmpty(ownerId)) {
      return;
    }
    // 处理人员选择页面返回成员列表批量添加管理员
    launcher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
              if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                ArrayList<QChatServerRoleMemberInfo> members =
                    result.getData().getParcelableArrayListExtra(REQUEST_MEMBER_SELECTOR_KEY);
                if (members != null) {
                  List<String> accIdList = new ArrayList<>();
                  for (QChatServerRoleMemberInfo item : members) {
                    accIdList.add(item.getAccId());
                  }
                  if (!accIdList.isEmpty()) {
                    QChatUtils.isConnectedToastAndRun(this, () -> viewModel.addManagers(accIdList));
                  }
                }
              }
            });
    viewModel.configType(serverId, channelId, roleId, ownerId);
    viewModel.getInitManagersResult().observeForever(observerForInit);
    viewModel.getLoadMoreManagersResult().observeForever(observerForLoadMore);
    viewModel.getAddManagersResult().observeForever(observerForInviteMembers);
    viewModel.getKickManagerResult().observeForever(observerForKickMember);
    viewModel.getMemberRemoveResult().observeForever(observerForRemoveMembers);
    viewModel.getFinishResult().observeForever(observerForFinish);
    initView();
  }

  private void initView() {
    binding.tvTitle.setText(R.string.qchat_announce_manager_title);
    binding.ivBack.setOnClickListener(v -> finish());

    boolean isOwner = Objects.equals(ownerId, IMKitClient.account());
    binding.tvAddMember.setOnClickListener(
        v -> {
          // 跳转成员选择列表页面
          ArrayList<String> filterAccIdList = new ArrayList<>();
          filterAccIdList.add(ownerId);
          Intent intent = new Intent(this, QChatMemberSelectorActivity.class);
          intent.putExtra(QChatConstant.SERVER_ID, serverId);
          intent.putExtra(QChatConstant.REQUEST_MEMBER_MAX_SIZE, 1);
          intent.putExtra(
              QChatConstant.REQUEST_MEMBER_FILTER_KEY, QChatConstant.REQUEST_MEMBER_FILTER_ROLE);
          intent.putExtra(QChatConstant.SERVER_ROLE_ID, roleId);
          intent.putExtra(QChatConstant.REQUEST_MEMBER_FILTER_LIST, filterAccIdList);
          launcher.launch(intent);
        });

    binding.tvAddMember.setVisibility(isOwner ? View.VISIBLE : View.GONE);
    adapter = new ServerMemberAdapter(this, isOwner);
    adapter.setItemClickListener(
        (data, holder) -> {
          // 跳转管理员权限编辑页面
          if (TextUtils.equals(ownerId, IMKitClient.account())
              && !TextUtils.equals(data.getAccId(), ownerId)) {
            ManagerEditActivity.launch(this, data, serverId, roleId, channelId);
          }
        });

    adapter.setActionBtnClickListener(this::showDeleteConfirmDialog);

    LinearLayoutManager layoutManager =
        new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
    binding.ryMemberList.setLayoutManager(layoutManager);
    binding.ryMemberList.setAdapter(adapter);

    loadMoreRecyclerViewDecorator =
        new LoadMoreRecyclerViewDecorator<>(binding.ryMemberList, layoutManager, adapter);
    loadMoreRecyclerViewDecorator.setLoadMoreListener(
        data -> {
          NextInfo info;
          if (data == null || data.getNextInfo() == null) {
            info = null;
          } else {
            info = data.getNextInfo();
          }
          if (info != null && !info.getHasMore()) {
            return;
          }
          long timeTag = info != null ? info.getNextTimeTag() : 0;
          if (timeTag == 0) {
            return;
          }
          viewModel.loadMore(timeTag, data.getAccId());
        });

    QChatUtils.isConnectedToastAndRun(this, viewModel::init);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    viewModel.getInitManagersResult().removeObserver(observerForInit);
    viewModel.getLoadMoreManagersResult().removeObserver(observerForLoadMore);
    viewModel.getAddManagersResult().removeObserver(observerForInviteMembers);
    viewModel.getKickManagerResult().removeObserver(observerForKickMember);
    viewModel.getMemberRemoveResult().removeObserver(observerForRemoveMembers);
    viewModel.getFinishResult().removeObserver(observerForFinish);
  }

  /**
   * 展示管理员删除确认弹窗
   *
   * @param item 待删除管理员
   */
  private void showDeleteConfirmDialog(QChatAnnounceMemberInfo item) {
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
                QChatUtils.isConnectedToastAndRun(
                    ManagerListActivity.this, () -> viewModel.kickManager(item.getAccId()));
              }

              @Override
              public void onNegative() {}
            })
        .show(getSupportFragmentManager());
  }

  /**
   * 页面启动方法
   *
   * @param context 上下文
   * @param serverId 公告频道对应的社区id
   * @param roleId 管理员身份组 id
   * @param channelId 公告频道对应的话题id
   * @param owner 公告频道创建者
   */
  public static void launch(
      Context context, long serverId, long channelId, long roleId, String owner) {
    Intent intent = new Intent(context, ManagerListActivity.class);
    intent.putExtra(PARAM_KEY_SERVER_ID, serverId);
    intent.putExtra(PARAM_KEY_CHANNEL_ID, channelId);
    intent.putExtra(PARAM_KEY_ROLE_ID, roleId);
    intent.putExtra(PARAM_KEY_OWNER_ID, owner);
    context.startActivity(intent);
  }

  /** Handling the change of initialization or loading more data. */
  private class ObserverWrapper implements Observer<ResultInfo<List<QChatAnnounceMemberInfo>>> {
    public final boolean clear;

    public ObserverWrapper(boolean clear) {
      this.clear = clear;
    }

    @Override
    public void onChanged(ResultInfo<List<QChatAnnounceMemberInfo>> resultInfo) {
      if (resultInfo.getSuccess()) {
        List<QChatAnnounceMemberInfo> data = resultInfo.getValue();
        adapter.addDataList(data, clear);
        if (clear && (data == null || data.isEmpty())) {
          binding.emptyGroup.setVisibility(View.VISIBLE);
        } else {
          binding.emptyGroup.setVisibility(View.GONE);
        }
      } else if (NetworkUtils.isConnected()) {
        ErrorMsg msg = resultInfo.getMsg();
        Toast.makeText(
                ManagerListActivity.this,
                getString(R.string.qchat_server_request_fail) + (msg != null ? msg.getCode() : ""),
                Toast.LENGTH_SHORT)
            .show();
      } else {
        Toast.makeText(
                ManagerListActivity.this,
                getString(R.string.qchat_network_error_tip),
                Toast.LENGTH_SHORT)
            .show();
      }
      if (loadMoreRecyclerViewDecorator != null) {
        if (clear) {
          loadMoreRecyclerViewDecorator.init();
        }
        loadMoreRecyclerViewDecorator.notifyResult(resultInfo.getSuccess());
      }
    }
  }
}
