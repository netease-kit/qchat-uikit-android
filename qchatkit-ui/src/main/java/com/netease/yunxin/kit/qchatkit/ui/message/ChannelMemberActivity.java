// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.message;

import static com.netease.yunxin.kit.qchatkit.ui.server.viewmodel.QChatServerListViewModel.ServerVisitorInfoMgr;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.netease.yunxin.kit.alog.ALog;
import com.netease.yunxin.kit.common.ui.viewmodel.FetchResult;
import com.netease.yunxin.kit.common.ui.viewmodel.LoadStatus;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerMemberInfo;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.channel.setting.ChannelSettingActivity;
import com.netease.yunxin.kit.qchatkit.ui.common.CommonRecyclerViewAdapter;
import com.netease.yunxin.kit.qchatkit.ui.common.QChatChannelBaseActivity;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatChannelMemberActivityBinding;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatChannelMemberStatusViewHolderBinding;
import com.netease.yunxin.kit.qchatkit.ui.message.model.ChannelMemberStatusBean;
import com.netease.yunxin.kit.qchatkit.ui.message.view.MemberProfileDialog;
import com.netease.yunxin.kit.qchatkit.ui.message.viewholder.QChannelMemberViewHolder;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatViewType;
import com.netease.yunxin.kit.qchatkit.ui.server.viewmodel.QChatServerListViewModel;

/** 话题成员列表，普通话题聊天页面右上角人员按钮点击进入 */
public class ChannelMemberActivity extends QChatChannelBaseActivity {

  private static final String TAG = "QChatChannelMessageActivity";
  private QChatChannelMemberActivityBinding viewBinding;
  protected CommonRecyclerViewAdapter recyclerViewAdapter;
  private ChannelMemberViewModel viewModel;
  private MemberProfileDialog profileDialog;
  private long serverId;
  private long channelId;
  private String serverOwnerId;
  private String channelName;
  private final int LOAD_MORE_DIFF = 4;

  /**
   * 话题访客更新观察者
   *
   * <p>用于更新成员列表
   */
  private final QChatServerListViewModel.QChatServerVisitorDataChangeObserver
      serverVisitorDataChangeObserver =
          serverInfoForVisitor -> {
            if (viewBinding != null
                && !QChatServerListViewModel.ServerVisitorInfoMgr.getInstance()
                    .isVisitor(serverId)) {
              viewBinding.qChatMessageMemberTitleSetting.setVisibility(View.VISIBLE);
            }
          };

  /**
   * 话题信息更新观察者
   *
   * <p>用于更新标题栏的话题名称
   */
  private final Observer<FetchResult<QChatChannelInfo>> observerForChannelInfoUpdate =
      result -> {
        if (result.getLoadStatus() == LoadStatus.Success && result.getData() != null) {
          channelName = result.getData().getName();
          if (viewBinding != null) {
            viewBinding.qChatMessageMemberTitleTv.setText(channelName);
          }
        }
      };

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    changeStatusBarColor(R.color.color_white);
    viewBinding = QChatChannelMemberActivityBinding.inflate(getLayoutInflater());
    setContentView(viewBinding.getRoot());
    initView();
    initViewModel();
    loadData();
  }

  protected void initViewModel() {
    ALog.d(TAG, "initViewModel");
    viewModel = new ViewModelProvider(this).get(ChannelMemberViewModel.class);
    viewModel
        .getMembersLiveData()
        .observe(
            this,
            result -> {
              if (result.getLoadStatus() == LoadStatus.Success) {
                recyclerViewAdapter.setData(result.getData());
              } else if (result.getLoadStatus() == LoadStatus.Finish) {
                if (result.getType() == FetchResult.FetchType.Add) {
                  recyclerViewAdapter.addData(result.getTypeIndex(), result.getData());
                } else if (result.getType() == FetchResult.FetchType.Remove) {
                  recyclerViewAdapter.removeData(result.getTypeIndex());
                }
              } else if (result.getLoadStatus() == LoadStatus.Error) {
                Toast.makeText(
                        this,
                        getResources().getString(result.errorMsg().getRes()),
                        Toast.LENGTH_SHORT)
                    .show();
              }
            });

    viewModel
        .getChannelNotifyLiveData()
        .observe(
            this,
            result -> {
              if (result.getType() == FetchResult.FetchType.Remove) {
                ALog.d(TAG, "ChannelLiveData", "Remove");
                finish();
              }
            });
    viewModel.getChannelInfoLiveData().observeForever(observerForChannelInfoUpdate);
  }

  protected void initView() {
    ALog.d(TAG, "initView");
    viewBinding.qChatMessageMemberTitleLeftIv.setOnClickListener(v -> finish());
    viewBinding.qChatMessageMemberTitleSetting.setOnClickListener(
        view -> {
          ALog.d(TAG, "OnClickListener", "qChatMessageMemberTitleSetting");
          ChannelSettingActivity.launch(this, serverId, channelId, serverOwnerId, channelName);
        });

    LinearLayoutManager layoutManager = new LinearLayoutManager(this);
    layoutManager.setOrientation(RecyclerView.VERTICAL);
    viewBinding.qChatMessageMemberRecyclerView.setLayoutManager(layoutManager);
    recyclerViewAdapter = new CommonRecyclerViewAdapter();
    recyclerViewAdapter.setViewHolderFactory(
        (parent, viewType) -> {
          if (viewType == QChatViewType.CHANNEL_MEMBER_VIEW_TYPE) {
            QChatChannelMemberStatusViewHolderBinding viewHolderBinding =
                QChatChannelMemberStatusViewHolderBinding.inflate(
                    getLayoutInflater(), parent, false);
            QChannelMemberViewHolder viewHolder = new QChannelMemberViewHolder(viewHolderBinding);
            viewHolder.setItemOnClickListener(
                (data, position) -> {
                  if (profileDialog == null) {
                    profileDialog = new MemberProfileDialog();
                  }
                  if (profileDialog.isAdded()) {
                    profileDialog.dismiss();
                  }
                  QChatServerMemberInfo member = ((ChannelMemberStatusBean) data).channelMember;
                  profileDialog.setData(member, true);
                  profileDialog.show(getSupportFragmentManager(), member.getAccId());
                });
            return viewHolder;
          }
          return null;
        });
    viewBinding.qChatMessageMemberRecyclerView.addOnScrollListener(
        new RecyclerView.OnScrollListener() {
          @Override
          public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
              int position = layoutManager.findLastVisibleItemPosition();
              if (isLoadMore() && recyclerViewAdapter.getItemCount() < position + LOAD_MORE_DIFF) {
                loadMoreMember();
              }
            }
          }

          @Override
          public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
          }
        });
    viewBinding.qChatMessageMemberRecyclerView.setAdapter(recyclerViewAdapter);
  }

  protected void loadData() {
    serverId = getIntent().getLongExtra(QChatConstant.SERVER_ID, 0);
    channelId = getIntent().getLongExtra(QChatConstant.CHANNEL_ID, 0);
    serverOwnerId = getIntent().getStringExtra(QChatConstant.SERVER_OWNER_ID);
    configServerIdAndChannelId(serverId, channelId);
    channelName = getIntent().getStringExtra(QChatConstant.CHANNEL_NAME);
    ALog.d(TAG, "initData", "info:" + channelId + "'" + channelName);
    //初始化viewmodel
    viewModel.init(serverId, channelId);

    //监听访客信息变更
    ServerVisitorInfoMgr.getInstance().addObserver(serverVisitorDataChangeObserver);
    if (ServerVisitorInfoMgr.getInstance().isVisitor(serverId)) {
      viewBinding.qChatMessageMemberTitleSetting.setVisibility(View.GONE);
    }
    viewBinding.qChatMessageMemberTitleTv.setText(channelName);
  }

  private boolean isLoadMore() {
    return viewModel.hasMore();
  }

  private void loadMoreMember() {
    viewModel.loadMore(serverId, channelId);
  }

  @Override
  protected void onResume() {
    super.onResume();
    ALog.d(TAG, "onResume");
    viewModel.fetchMemberList(serverId, channelId);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    ServerVisitorInfoMgr.getInstance().removeObserver(serverVisitorDataChangeObserver);
    if (viewModel != null) {
      viewModel.getChannelInfoLiveData().removeObserver(observerForChannelInfoUpdate);
    }
  }

  /**
   * 启动话题成员列表页面
   *
   * @param activity activity
   * @param serverId 话题所属服务器id
   * @param channelId 话题id
   * @param channelName 话题名称
   */
  public static void launch(
      Activity activity, long serverId, long channelId, String ownerId, String channelName) {
    Intent intent = new Intent(activity, ChannelMemberActivity.class);
    intent.putExtra(QChatConstant.SERVER_ID, serverId);
    intent.putExtra(QChatConstant.SERVER_OWNER_ID, ownerId);
    intent.putExtra(QChatConstant.CHANNEL_ID, channelId);
    intent.putExtra(QChatConstant.CHANNEL_NAME, channelName);
    activity.startActivity(intent);
  }
}
