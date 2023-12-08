// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.message;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.netease.yunxin.kit.alog.ALog;
import com.netease.yunxin.kit.common.ui.viewmodel.FetchResult;
import com.netease.yunxin.kit.common.ui.viewmodel.LoadStatus;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerInfo;
import com.netease.yunxin.kit.qchatkit.ui.announce.SettingActivity;
import com.netease.yunxin.kit.qchatkit.ui.common.QChatChannelBaseActivity;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatChannelMessageActivityBinding;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant;

/** 话题聊天页面 */
public class ChannelMessageActivity extends QChatChannelBaseActivity {

  private static final String TAG = "QChatChannelMessageActivity";
  private QChatChannelMessageActivityBinding viewBinding;
  private MessageFragment messageFragment;
  private ChannelMessageViewModel viewModel;
  private QChatChannelInfo channelInfo;
  private QChatServerInfo serverInfo;
  private long serverId;
  private long channelId;
  private String channelName;
  private String serverOwnerId;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewBinding = QChatChannelMessageActivityBinding.inflate(getLayoutInflater());
    setContentView(viewBinding.getRoot());
    serverId = getIntent().getLongExtra(QChatConstant.SERVER_ID, 0);
    channelId = getIntent().getLongExtra(QChatConstant.CHANNEL_ID, 0);
    serverOwnerId = getIntent().getStringExtra(QChatConstant.SERVER_OWNER_ID);
    channelName = getIntent().getStringExtra(QChatConstant.CHANNEL_NAME);
    configServerIdAndChannelId(serverId, channelId);
    serverInfo = (QChatServerInfo) getIntent().getSerializableExtra(QChatConstant.SERVER_INFO);
    ALog.d(TAG, "initData", "info:" + channelId + "'" + channelName);
    initView();
    initData();
    refreshView();
  }

  protected void initView() {
    ALog.d(TAG, "initView");
    viewBinding.qChatMessageTitleActionLayout.setOnClickListener(
        view -> {
          if (serverInfo != null
              && serverInfo.getAnnouncementInfo() != null
              && serverInfo.getAnnouncementInfo().isValid()) {
            SettingActivity.launch(this, serverInfo, serverId);
          } else {
            ChannelMemberActivity.launch(
                this,
                serverId,
                channelId,
                serverOwnerId,
                String.valueOf(viewBinding.qChatMessageTitleTv.getText()));
          }
        });
    viewBinding.qChatMessageTitleLeftIv.setOnClickListener(
        view -> {
          ALog.d(TAG, "OnClickListener", "qChatMessageTitleLeftIv");
          finish();
        });
  }

  protected void initData() {
    LinearLayoutManager layoutManager = new LinearLayoutManager(this);
    layoutManager.setOrientation(RecyclerView.VERTICAL);
    //加载聊天页面Fragment，逻辑都在Fragment中
    messageFragment = new MessageFragment();
    messageFragment.init(serverInfo, serverId, channelId);
    getSupportFragmentManager()
        .beginTransaction()
        .add(viewBinding.qChatMessageBodyLayout.getId(), messageFragment)
        .commit();

    viewModel = new ViewModelProvider(this).get(ChannelMessageViewModel.class);
    viewModel
        .getChannelNotifyLiveData()
        .observeForever(
            result -> {
              if (result.getType() == FetchResult.FetchType.Remove) {
                ALog.d(TAG, "ChannelLiveData", "Remove");
                finish();
              }
            });

    //监听话题信息变化
    viewModel
        .getChannelInfoLiveData()
        .observeForever(
            result -> {
              if (result.getLoadStatus() == LoadStatus.Success && result.getData() != null) {
                channelInfo = result.getData();
                ALog.d(TAG, "ChannelInfoLiveData", "Success");
                refreshView();
                if (messageFragment != null) {
                  messageFragment.updateChannelInfo(channelInfo);
                }
              }
            });
    // 监听服务器信息变化
    viewModel
        .getServerUpdateLiveData()
        .observeForever(
            result -> {
              if (result.getLoadStatus() == LoadStatus.Success && result.getData() != null) {
                serverInfo = result.getData();
                refreshView();
                ALog.d(TAG, "ServerUpdateLiveData", "Success");
                if (messageFragment != null) {
                  messageFragment.updateServerInfo(serverInfo);
                }
              }
            });
    viewModel.init(serverId, channelId);
  }

  @Override
  protected void onResume() {
    super.onResume();
    ALog.d(TAG, "onResume");
    viewModel.fetchChannelInfo(channelId);
  }

  /** 刷新页面，根据社区或者频道信息变更，刷新标题中社区名称 */
  public void refreshView() {
    if (channelInfo != null) {
      ALog.d(TAG, "loadChannelInfo");
      String titleText = channelName;
      if (serverInfo != null && serverInfo.getAnnouncementInfo() != null) {
        titleText = serverInfo.getName();
      } else if (channelInfo != null) {
        titleText = channelInfo.getName();
      }
      viewBinding.qChatMessageTitleTv.setText(titleText);
    } else {
      viewBinding.qChatMessageTitleTv.setText(channelName);
    }
  }

  /**
   * 跳转到话题成员列表页面
   *
   * @param activity 上下文
   * @param serverId 服务器id
   * @param channelId 话题id
   * @param channelName 话题名称
   */
  public static void launch(
      Activity activity,
      QChatServerInfo serverInfo,
      long serverId,
      long channelId,
      String ownerId,
      String channelName) {
    Intent intent = new Intent(activity, ChannelMessageActivity.class);
    intent.putExtra(QChatConstant.SERVER_INFO, serverInfo);
    intent.putExtra(QChatConstant.SERVER_ID, serverId);
    intent.putExtra(QChatConstant.SERVER_OWNER_ID, ownerId);
    intent.putExtra(QChatConstant.CHANNEL_ID, channelId);
    intent.putExtra(QChatConstant.CHANNEL_NAME, channelName);
    activity.startActivity(intent);
  }
}
