// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.announce;

import static com.netease.yunxin.kit.corekit.im.utils.RouterConstant.REQUEST_CONTACT_SELECTOR_KEY;

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
import com.netease.yunxin.kit.corekit.im.utils.RouterConstant;
import com.netease.yunxin.kit.corekit.model.ErrorMsg;
import com.netease.yunxin.kit.corekit.model.ResultInfo;
import com.netease.yunxin.kit.corekit.route.XKitRouter;
import com.netease.yunxin.kit.qchatkit.repo.model.NextInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatAnnounceMemberInfo;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.announce.adapter.ServerMemberAdapter;
import com.netease.yunxin.kit.qchatkit.ui.announce.viewmodel.ServerMemberViewModel;
import com.netease.yunxin.kit.qchatkit.ui.common.LoadMoreRecyclerViewDecorator;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatAnnounceMemberListActivityBinding;
import com.netease.yunxin.kit.qchatkit.ui.utils.QChatUtils;
import java.util.ArrayList;
import java.util.List;

/** 公告频道成员列表（仅展示订阅者，不展示管理员及创建者） */
public class ServerMemberListActivity extends BaseActivity {
  private static final String PARAM_KEY_SERVER_ID = "key_server_id";
  private static final String PARAM_KEY_ROLE_ID = "key_role_id";
  private static final String PARAM_KEY_OWNER_ID = "key_owner_id";
  private static final String PARAM_KEY_CHANNEL_ID = "key_channel_id";

  private boolean hasPermission;

  private QChatAnnounceMemberListActivityBinding binding;
  private final ServerMemberViewModel viewModel = new ServerMemberViewModel();

  private final ServerMemberAdapter adapter = new ServerMemberAdapter(this);
  private final Observer<ResultInfo<List<QChatAnnounceMemberInfo>>> observerForInit =
      new ObserverWrapper(true);

  private final Observer<ResultInfo<List<QChatAnnounceMemberInfo>>> observerForLoadMore =
      new ObserverWrapper(false);

  /** 监听权限校验结果通知，更具是否拥有权限展示ui */
  private final Observer<Boolean> observerForPermission =
      hasPermission -> {
        this.hasPermission = hasPermission;
        if (!isFinishing()) {
          adapter.updatePermissionStatus(hasPermission);
          binding.tvAddMember.setVisibility(hasPermission ? View.VISIBLE : View.GONE);
        }
      };

  /** 监听订阅者成员踢出结果通知，此通知为当前用户主动踢出 */
  private final Observer<ResultInfo<String>> observerForKickMember =
      result -> {
        if (isFinishing()) {
          return;
        }
        if (result.getSuccess() && result.getValue() != null) {
          adapter.removeData(new QChatAnnounceMemberInfo(result.getValue()));
        }
        if (adapter.getItemCount() == 0) {
          binding.emptyGroup.setVisibility(View.VISIBLE);
        }
      };
  /** 监听邀请订阅者成员结果通知 */
  private final Observer<ResultInfo<List<String>>> observerForInviteMembers =
      result -> {
        if (isFinishing()) {
          return;
        }
        if (result.getSuccess()) {
          viewModel.init();
        } else if (NetworkUtils.isConnected()) {
          ErrorMsg msg = result.getMsg();
          Toast.makeText(
                  ServerMemberListActivity.this,
                  getString(R.string.qchat_server_request_fail)
                      + (msg != null ? msg.getCode() : ""),
                  Toast.LENGTH_SHORT)
              .show();
        } else {
          Toast.makeText(
                  ServerMemberListActivity.this,
                  getString(R.string.common_network_error),
                  Toast.LENGTH_SHORT)
              .show();
        }
      };

  /** 监听订阅者成员移除通知，此通知为非当前用户自己移除 */
  private final Observer<List<String>> observerForRemoveMembers = adapter::removeDataList;

  /** 监听页面关闭通知 */
  private final Observer<Object> observerForFinish = o -> finish();

  private ActivityResultLauncher<Intent> launcher;
  private LoadMoreRecyclerViewDecorator<QChatAnnounceMemberInfo> decorator;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = QChatAnnounceMemberListActivityBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    long serverId = getIntent().getLongExtra(PARAM_KEY_SERVER_ID, 0);
    long channelId = getIntent().getLongExtra(PARAM_KEY_CHANNEL_ID, 0);
    long roleId = getIntent().getLongExtra(PARAM_KEY_ROLE_ID, 0);
    String ownerId = getIntent().getStringExtra(PARAM_KEY_OWNER_ID);
    if (serverId <= 0 || roleId <= 0 || channelId < 0 || TextUtils.isEmpty(ownerId)) {
      return;
    }
    // 处理成员选择页面返回的成员accId列表，通过此列表邀请成员
    launcher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
              if (result.getResultCode() != RESULT_OK) {
                return;
              }
              Intent data = result.getData();
              if (data == null) {
                return;
              }
              ArrayList<String> friends =
                  data.getStringArrayListExtra(REQUEST_CONTACT_SELECTOR_KEY);
              if (friends == null || friends.isEmpty()) {
                return;
              }
              QChatUtils.isConnectedToastAndRun(
                  this,
                  () -> {
                    if (!hasPermission) {
                      Toast.makeText(this, R.string.qchat_no_permission, Toast.LENGTH_SHORT).show();
                      return;
                    }
                    viewModel.inviteMember(friends);
                  });
            });
    viewModel.configType(serverId, roleId, channelId, ownerId);
    viewModel.getInitServerMembersResult().observeForever(observerForInit);
    viewModel.getLoadMoreServerMembersResult().observeForever(observerForLoadMore);
    viewModel.getPermissionResult().observeForever(observerForPermission);
    viewModel.getInviteMembersResult().observeForever(observerForInviteMembers);
    viewModel.getKickMemberResult().observeForever(observerForKickMember);
    viewModel.getMemberRemoveResult().observeForever(observerForRemoveMembers);
    viewModel.getFinishResult().observeForever(observerForFinish);
    viewModel.checkPermission();
    initView();
  }

  private void initView() {
    binding.tvTitle.setText(R.string.qchat_announce_member_title);
    binding.ivBack.setOnClickListener(v -> finish());

    binding.tvAddMember.setOnClickListener(
        v -> {
          // 添加公告频道订阅者，跳转到通讯录人员选择页面
          List<String> filterList = new ArrayList<>();
          filterList.addAll(viewModel.getFilterAccIdList());
          filterList.addAll(adapter.getAccIdList());
          XKitRouter.withKey(RouterConstant.PATH_CONTACT_SELECTOR_PAGE)
              .withContext(this)
              .withParam(RouterConstant.SELECTOR_CONTACT_FILTER_KEY, filterList)
              .navigate(launcher);
        });

    adapter.setItemClickListener(
        (data, holder) -> {
          // 订阅者点击跳转到人员名片页面（好友名片/陌生人名片）
          XKitRouter.withKey(RouterConstant.PATH_USER_INFO_PAGE)
              .withContext(this)
              .withParam(RouterConstant.KEY_ACCOUNT_ID_KEY, data.getAccId())
              .navigate();
        });

    adapter.setActionBtnClickListener(this::showDeleteConfirmDialog);

    LinearLayoutManager layoutManager =
        new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
    binding.ryMemberList.setLayoutManager(layoutManager);
    binding.ryMemberList.setAdapter(adapter);

    decorator = new LoadMoreRecyclerViewDecorator<>(binding.ryMemberList, layoutManager, adapter);
    decorator.setLoadMoreListener(
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
          viewModel.loadMore(timeTag);
        });

    QChatUtils.isConnectedToastAndRun(this, viewModel::init);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    viewModel.getInitServerMembersResult().removeObserver(observerForInit);
    viewModel.getLoadMoreServerMembersResult().removeObserver(observerForLoadMore);
    viewModel.getPermissionResult().removeObserver(observerForPermission);
    viewModel.getInviteMembersResult().removeObserver(observerForInviteMembers);
    viewModel.getKickMemberResult().removeObserver(observerForKickMember);
    viewModel.getMemberRemoveResult().removeObserver(observerForRemoveMembers);
    viewModel.getFinishResult().removeObserver(observerForFinish);
  }

  /**
   * 展示成员删除确认弹窗
   *
   * @param item 待删除用户
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
                    ServerMemberListActivity.this,
                    () -> {
                      if (!hasPermission) {
                        Toast.makeText(
                                ServerMemberListActivity.this,
                                R.string.qchat_no_permission,
                                Toast.LENGTH_SHORT)
                            .show();
                        return;
                      }
                      viewModel.kickMember(item.getAccId());
                    });
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
      Context context, long serverId, long roleId, long channelId, String owner) {
    Intent intent = new Intent(context, ServerMemberListActivity.class);
    intent.putExtra(PARAM_KEY_SERVER_ID, serverId);
    intent.putExtra(PARAM_KEY_ROLE_ID, roleId);
    intent.putExtra(PARAM_KEY_CHANNEL_ID, channelId);
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
                ServerMemberListActivity.this,
                getString(R.string.qchat_server_request_fail) + (msg != null ? msg.getCode() : ""),
                Toast.LENGTH_SHORT)
            .show();
      } else {
        Toast.makeText(
                ServerMemberListActivity.this,
                getString(R.string.common_network_error),
                Toast.LENGTH_SHORT)
            .show();
      }
      if (decorator != null) {
        if (clear) {
          decorator.init();
        }
        decorator.notifyResult(resultInfo.getSuccess());
      }
    }
  }
}
