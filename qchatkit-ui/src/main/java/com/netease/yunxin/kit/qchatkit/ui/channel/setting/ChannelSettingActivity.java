// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.channel.setting;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.netease.yunxin.kit.alog.ALog;
import com.netease.yunxin.kit.common.ui.dialog.ChoiceListener;
import com.netease.yunxin.kit.common.ui.dialog.CommonChoiceDialog;
import com.netease.yunxin.kit.common.ui.viewmodel.FetchResult;
import com.netease.yunxin.kit.common.ui.viewmodel.LoadStatus;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelModeEnum;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.channel.blackwhite.QChatBlackWhiteActivity;
import com.netease.yunxin.kit.qchatkit.ui.channel.permission.QChatChannelPermissionActivity;
import com.netease.yunxin.kit.qchatkit.ui.common.QChatChannelBaseActivity;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatChannelSettingActivityBinding;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant;
import com.netease.yunxin.kit.qchatkit.ui.utils.QChatUtils;

/** 话题设置页面，可以修改名称和主题，提供权限和黑名单或白名单入口 */
public class ChannelSettingActivity extends QChatChannelBaseActivity {

  private static final String TAG = "ChannelSettingActivity";
  private QChatChannelSettingActivityBinding viewBinding;
  private ChannelSettingViewModel viewModel;
  private QChatChannelInfo channelInfo;
  private long channelId;
  private String serverOwnerId;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ALog.d(TAG, "onCreate");
    changeStatusBarColor(R.color.color_eff1f4);
    viewBinding = QChatChannelSettingActivityBinding.inflate(LayoutInflater.from(this));
    viewModel = new ViewModelProvider(this).get(ChannelSettingViewModel.class);
    setContentView(viewBinding.getRoot());
    initView();
    initData();
  }

  private void initView() {

    viewBinding.channelSettingTitleLeftTv.setOnClickListener(view -> finish());

    viewBinding.channelSettingTitleRightTv.setOnClickListener(
        view -> QChatUtils.isConnectedToastAndRun(this, this::updateChannelSetting));

    viewBinding.channelSettingPermissionRtv.setOnClickListener(
        view -> {
          ALog.d(TAG, "OnClickListener", "channelSettingPermissionRtv" + checkParamValid());
          QChatUtils.isConnectedToastAndRun(
              this,
              () -> {
                if (checkParamValid()) {
                  QChatChannelPermissionActivity.launch(this, channelInfo.getServerId(), channelId);
                } else {
                  Toast.makeText(
                          this,
                          getResources().getString(R.string.qchat_channel_empty_error),
                          Toast.LENGTH_SHORT)
                      .show();
                }
              });
        });

    viewBinding.channelSettingWhiteNameListRtv.setOnClickListener(
        view -> {
          ALog.d(TAG, "OnClickListener", "channelSettingWhiteNameListRtv" + checkParamValid());
          QChatUtils.isConnectedToastAndRun(
              this,
              () -> {
                if (checkParamValid()) {
                  QChatBlackWhiteActivity.launch(
                      this,
                      channelInfo.getServerId(),
                      channelId,
                      serverOwnerId,
                      this.channelInfo.getViewMode().ordinal());
                } else {
                  Toast.makeText(
                          this,
                          getResources().getString(R.string.qchat_channel_empty_error),
                          Toast.LENGTH_SHORT)
                      .show();
                }
              });
        });

    viewBinding.channelSettingDeleteRtv.setOnClickListener(view -> showDeleteDialog());
  }

  /** 删除话题弹窗 */
  private void showDeleteDialog() {
    ALog.d(TAG, "showDeleteDialog");
    CommonChoiceDialog commonConfirmDialog = new CommonChoiceDialog();
    commonConfirmDialog
        .setTitleStr(getString(R.string.qchat_channel_delete))
        .setContentStr(
            getString(
                R.string.qchat_delete_topic_content,
                channelInfo != null ? channelInfo.getName() : ""))
        .setPositiveStr(getString(R.string.qchat_sure))
        .setNegativeStr(getString(R.string.qchat_cancel))
        .setConfirmListener(
            new ChoiceListener() {
              @Override
              public void onNegative() {}

              @Override
              public void onPositive() {
                ALog.d(TAG, "showDeleteDialog", "onPositive:" + channelId);
                QChatUtils.isConnectedToastAndRun(
                    ChannelSettingActivity.this, () -> viewModel.deleteChannel(channelId));
              }
            })
        .show(getSupportFragmentManager());
  }

  private void initData() {

    channelId = getIntent().getLongExtra(QChatConstant.CHANNEL_ID, 0);
    serverOwnerId = getIntent().getStringExtra(QChatConstant.SERVER_OWNER_ID);

    //监听获取话题信息
    viewModel
        .getFetchResult()
        .observe(
            this,
            result -> {
              if (result.getLoadStatus() == LoadStatus.Success) {
                loadData(result.getData());

              } else if (result.getLoadStatus() == LoadStatus.Error) {
                Toast.makeText(this, result.getErrorMsg(this), Toast.LENGTH_SHORT).show();
              } else if (result.getLoadStatus() == LoadStatus.Finish) {
                if (result.getType() == FetchResult.FetchType.Remove) {
                  finish();
                }
              }
            });
    //获取话题信息
    viewModel.fetchChannelInfo(channelId);
    ALog.d(TAG, "initData", "channelId:" + channelId);
  }

  /** 加载话题信息 */
  private void loadData(QChatChannelInfo channelInfo) {
    if (channelInfo == null) {
      return;
    }
    configServerIdAndChannelId(channelInfo.getServerId(), channelInfo.getChannelId());
    ALog.d(TAG, "loadData", "channelInfo:" + channelInfo.getChannelId());
    this.channelInfo = channelInfo;
    viewBinding.channelSettingNameEt.setText(channelInfo.getName());
    viewBinding.channelSettingTopicEt.setText(channelInfo.getTopic());
    if (this.channelInfo.getViewMode() == QChatChannelModeEnum.Public) {
      viewBinding.channelSettingWhiteNameListRtv.setText(R.string.qchat_channel_black_name_list);
    } else {
      viewBinding.channelSettingWhiteNameListRtv.setText(R.string.qchat_channel_white_name_list);
    }
  }

  /** 更新话题信息 */
  private void updateChannelSetting() {
    if (channelInfo != null) {
      ALog.d(TAG, "updateChannelSetting", "channelInfo:" + channelInfo.getChannelId());
      String channelName = viewBinding.channelSettingNameEt.getText();
      String topic = viewBinding.channelSettingTopicEt.getText();
      if (!TextUtils.isEmpty(channelName)) {
        viewModel.updateChannel(channelId, channelName, topic);
      } else {
        Toast.makeText(
                this, getString(R.string.qchat_channel_update_empty_error), Toast.LENGTH_SHORT)
            .show();
      }
    }
  }

  /** 检查参数是否有效 */
  private boolean checkParamValid() {
    return channelInfo != null && channelId > 0;
  }

  /** 启动话题设置页面 */
  public static void launch(
      Activity activity, long serverId, long channelId, String ownerId, String channelName) {
    Intent intent = new Intent(activity, ChannelSettingActivity.class);
    intent.putExtra(QChatConstant.SERVER_ID, serverId);
    intent.putExtra(QChatConstant.SERVER_OWNER_ID, ownerId);
    intent.putExtra(QChatConstant.CHANNEL_ID, channelId);
    intent.putExtra(QChatConstant.CHANNEL_NAME, channelName);
    activity.startActivity(intent);
  }
}
