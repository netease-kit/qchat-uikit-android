// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.announce;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.netease.nimlib.sdk.qchat.enums.QChatRoleOption;
import com.netease.nimlib.sdk.qchat.enums.QChatRoleResource;
import com.netease.yunxin.kit.common.ui.activities.BaseActivity;
import com.netease.yunxin.kit.common.ui.dialog.ChoiceListener;
import com.netease.yunxin.kit.common.ui.dialog.CommonChoiceDialog;
import com.netease.yunxin.kit.common.ui.photo.PhotoChoiceDialog;
import com.netease.yunxin.kit.common.ui.utils.AvatarColor;
import com.netease.yunxin.kit.common.ui.utils.CommonCallback;
import com.netease.yunxin.kit.common.ui.utils.ToastX;
import com.netease.yunxin.kit.common.ui.viewmodel.FetchResult;
import com.netease.yunxin.kit.common.ui.viewmodel.LoadStatus;
import com.netease.yunxin.kit.common.utils.NetworkUtils;
import com.netease.yunxin.kit.corekit.im2.extend.FetchCallback;
import com.netease.yunxin.kit.qchatkit.QChatKitClient;
import com.netease.yunxin.kit.qchatkit.repo.ResourceRepo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatRoleOptionEnum;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatRoleResourceEnum;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerRoleInfo;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.announce.viewmodel.SettingViewModel;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatAnncSettingBinding;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant;
import java.io.File;
import java.util.List;
import java.util.Map;

/** 公告频道设置页面 */
public class SettingActivity extends BaseActivity {
  private QChatAnncSettingBinding viewBinding;
  private SettingViewModel viewModel;
  private long serverId;
  private boolean isManager = false;
  private boolean isCreator = false;
  private List<QChatServerRoleInfo> roleInfoList;
  private InputMethodManager manager;
  private QChatServerInfo serverInfo;
  private QChatServerRoleInfo managerRoleInfo;

  private Map<QChatRoleResource, QChatRoleOption> authMap;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    changeStatusBarColor(R.color.color_eff1f4);
    viewBinding = QChatAnncSettingBinding.inflate(getLayoutInflater());
    setContentView(viewBinding.getRoot());
    viewModel = new ViewModelProvider(this).get(SettingViewModel.class);
    manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    Intent dataIntent = getIntent();
    if (dataIntent != null) {
      serverId = dataIntent.getLongExtra(QChatConstant.SERVER_ID, 0);
      serverInfo = (QChatServerInfo) dataIntent.getSerializableExtra(QChatConstant.SERVER_INFO);
    }
    viewModel.init(serverInfo);
    initView();
    initData();
    requestData();
  }

  @Override
  protected void onResume() {
    super.onResume();
  }

  private void initView() {
    viewBinding.ivCopy.setOnClickListener(
        v -> {
          if (serverInfo == null || serverInfo.getAnnouncementInfo() == null) {
            return;
          }
          ClipboardManager cmb =
              (ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);
          ClipData clipData = ClipData.newPlainText(null, viewBinding.tvShare.getText());
          cmb.setPrimaryClip(clipData);
          Toast.makeText(this, R.string.qchat_announcement_server_copy_success, Toast.LENGTH_SHORT)
              .show();
        });

    viewBinding.swAllowEmoji.setOnClickListener(
        v -> {
          if (!NetworkUtils.isConnected()) {
            Toast.makeText(this, R.string.qchat_network_error_tip, Toast.LENGTH_SHORT).show();
            viewBinding.swAllowEmoji.setChecked(!viewBinding.swAllowEmoji.isChecked());
            return;
          }
          if (serverInfo != null && serverInfo.getAnnouncementInfo() != null) {
            viewModel.setEmojiApply(serverInfo, viewBinding.swAllowEmoji.isChecked());
          }
        });
    viewBinding.ivBack.setOnClickListener(v -> finish());
    viewBinding.tvManager.setOnClickListener(
        v -> {
          if (serverInfo == null) {
            return;
          }
          //noinspection DataFlowIssue
          ManagerListActivity.launch(
              this,
              serverId,
              serverInfo.getAnnouncementInfo().getChannelId(),
              serverInfo.getAnnouncementInfo().getManagerRoleId(),
              serverInfo.getOwner());
        });

    viewBinding.tvSubscribe.setOnClickListener(
        v -> {
          if (serverInfo == null) {
            return;
          }
          //noinspection DataFlowIssue
          ServerMemberListActivity.launch(
              this,
              serverId,
              serverInfo.getAnnouncementInfo().getManagerRoleId(),
              serverInfo.getAnnouncementInfo().getChannelId(),
              serverInfo.getOwner());
        });
    viewBinding.tvChannelName.setOnClickListener(
        v -> {
          if (serverInfo == null) {
            return;
          }
          UpdateInfoActivity.launch(this, serverInfo, UpdateInfoActivity.UPDATE_TYPE_NAME, 100);
        });

    viewBinding.tvChannelSubscribe.setOnClickListener(
        v -> {
          if (serverInfo == null) {
            return;
          }
          UpdateInfoActivity.launch(this, serverInfo, UpdateInfoActivity.UPDATE_TYPE_TOPIC, 100);
        });

    viewBinding.tvQuit.setOnClickListener(
        v -> {
          CommonChoiceDialog dialog = new CommonChoiceDialog();
          if (serverInfo == null) {
            return;
          }
          if (isCreator) {
            dialog.setTitleStr(getString(R.string.qchat_announcement_dismiss));
            dialog.setContentStr(
                String.format(
                    getString(R.string.qchat_announcement_dismiss_confirm), serverInfo.getName()));
          } else {
            dialog.setTitleStr(getString(R.string.qchat_announcement_quit));
            dialog.setContentStr(
                String.format(
                    getString(R.string.qchat_announcement_quit_confirm), serverInfo.getName()));
          }
          dialog
              .setNegativeStr(getString(R.string.qchat_announcement_cancel))
              .setPositiveStr(getString(R.string.qchat_announcement_confirm))
              .setConfirmListener(
                  new ChoiceListener() {
                    @Override
                    public void onPositive() {
                      if (!NetworkUtils.isConnected()) {
                        Toast.makeText(
                                SettingActivity.this,
                                R.string.qchat_network_error_tip,
                                Toast.LENGTH_SHORT)
                            .show();
                        return;
                      }
                      if (isCreator) {
                        viewModel.deleteServer(serverId);
                      } else {
                        viewModel.leaveServer(serverId);
                      }
                    }

                    @Override
                    public void onNegative() {}
                  })
              .show(getSupportFragmentManager());
        });

    // 公告频道头像点击事件，更新公告频道头像
    viewBinding.ivAvatar.setOnClickListener(
        v -> {
          if (!NetworkUtils.isConnected()) {
            ToastX.showShortToast(R.string.qchat_network_error_tip);
            return;
          }
          if ((isManager
                  && authMap != null
                  && authMap.get(QChatRoleResource.MANAGE_CHANNEL) != QChatRoleOption.DENY)
              || isCreator) {

            manager.hideSoftInputFromWindow(v.getWindowToken(), 0);
            new PhotoChoiceDialog(this)
                .show(
                    new CommonCallback<File>() {
                      @Override
                      public void onSuccess(@Nullable File param) {
                        if (NetworkUtils.isConnected()) {
                          ResourceRepo.uploadFile(
                              param,
                              new FetchCallback<String>() {
                                @Override
                                public void onError(int code, @Nullable String msg) {
                                  if (code != 0) {
                                    Toast.makeText(
                                            getApplicationContext(),
                                            getString(R.string.qchat_server_request_fail)
                                                + " "
                                                + code,
                                            Toast.LENGTH_SHORT)
                                        .show();
                                  } else if (code == QChatConstant.NO_INTERNET_CODE) {
                                    ToastX.showShortToast(R.string.qchat_network_error_tip);
                                  }
                                }

                                @Override
                                public void onSuccess(@Nullable String param) {
                                  viewModel.updateServer(serverId, param, null);
                                  viewBinding.ivAvatar.setData(
                                      param,
                                      serverInfo.getName(),
                                      AvatarColor.avatarColor(serverInfo.getServerId()));
                                }
                              });
                        } else {
                          ToastX.showShortToast(R.string.qchat_network_error_tip);
                        }
                      }

                      @Override
                      public void onFailed(int code) {
                        if (code != 0) {
                          Toast.makeText(
                                  getApplicationContext(),
                                  getString(R.string.qchat_server_request_fail) + " " + code,
                                  Toast.LENGTH_SHORT)
                              .show();
                        }
                      }

                      @Override
                      public void onException(@Nullable Throwable exception) {
                        Toast.makeText(
                                getApplicationContext(),
                                getString(R.string.qchat_server_request_fail),
                                Toast.LENGTH_SHORT)
                            .show();
                      }
                    });
          } else {
            Toast.makeText(
                    getApplicationContext(),
                    getString(R.string.qchat_message_no_permission),
                    Toast.LENGTH_SHORT)
                .show();
          }
        });
  }

  private void initData() {
    // 监听公告频道信息通知
    viewModel
        .getServerLiveData()
        .observeForever(
            fetchResult -> {
              if (fetchResult.getLoadStatus() == LoadStatus.Success) {
                serverInfo = fetchResult.getData();
                if (serverInfo == null) {
                  return;
                }
                loadData();
              }
            });
    // 监听公告频道管理员身份组信息通知
    viewModel
        .getRoleLiveData()
        .observeForever(
            fetchResult -> {
              if (fetchResult != null && fetchResult.getData() != null) {
                roleInfoList = fetchResult.getData();
                loadData();
              }
            });
    // 监听公告频道信息变更通知
    viewModel
        .getUpdateServerLiveData()
        .observeForever(
            fetchResult -> {
              // 解散或者退出服务
              if (fetchResult != null && fetchResult.getType() == FetchResult.FetchType.Remove) {
                if (Boolean.TRUE.equals(fetchResult.getData())) {
                  finish();
                } else {
                  Toast.makeText(
                          getApplicationContext(),
                          getString(R.string.qchat_server_request_fail),
                          Toast.LENGTH_SHORT)
                      .show();
                }
                // 设置头像
              } else if (fetchResult != null
                  && fetchResult.getType() == FetchResult.FetchType.Add) {
                if (serverInfo != null) {
                  if (Boolean.FALSE.equals(fetchResult.getData())) {
                    viewBinding.ivAvatar.setData(
                        serverInfo.getIconUrl(),
                        serverInfo.getName(),
                        AvatarColor.avatarColor(serverInfo.getServerId()));
                  }
                }
                // 更新Emoji开关
              } else if (fetchResult != null
                  && fetchResult.getType() == FetchResult.FetchType.Update) {
                if (serverInfo != null) {
                  if (Boolean.FALSE.equals(fetchResult.getData())) {
                    Toast.makeText(
                            this, R.string.qchat_announcement_update_fail, Toast.LENGTH_SHORT)
                        .show();
                  }
                  loadData();
                }
              }
            });
    // 监听管理员权限变化通知
    viewModel
        .getAuthLiveData()
        .observeForever(
            result -> {
              if (result != null && result.getData() != null) {
                authMap = result.getData();
                loadData();
              }
            });
  }

  private void requestData() {
    viewModel.getAnnounceServer();
  }

  /** 加载数据（公告频道信息） */
  private void loadData() {
    if (serverInfo != null && serverInfo.getAnnouncementInfo() != null) {
      isManager = false;
      isCreator = false;
      if (roleInfoList != null) {
        QChatServerInfo.AnnouncementInfo announcementInfo = serverInfo.getAnnouncementInfo();
        for (QChatServerRoleInfo roleInfo : roleInfoList) {
          if (announcementInfo.getManagerRoleId() != null
              && roleInfo.getRoleId() == announcementInfo.getManagerRoleId()) {
            isManager = true;
            managerRoleInfo = roleInfo;
            break;
          }
        }
        if (TextUtils.equals(serverInfo.getOwner(), QChatKitClient.account())) {
          isCreator = true;
        }
      }

      viewBinding.tvName.setText(serverInfo.getName());
      viewBinding.ivAvatar.setData(
          serverInfo.getIconUrl(),
          serverInfo.getName(),
          AvatarColor.avatarColor(serverInfo.getServerId()));
      viewBinding.tvShare.setText(String.valueOf(serverInfo.getServerId()));

      if (isManager || isCreator) {
        viewBinding.memberManagerGroup.setVisibility(View.VISIBLE);
        viewBinding.msgManagerGroup.setVisibility(View.VISIBLE);
        viewBinding.ivEditAvatar.setVisibility(View.VISIBLE);
        if (isManager
            && managerRoleInfo != null
            && managerRoleInfo.getAuths().get(QChatRoleResourceEnum.MANAGE_CHANNEL)
                == QChatRoleOptionEnum.DENY) {
          viewBinding.ivEditAvatar.setVisibility(View.GONE);
        }
      } else {
        viewBinding.memberManagerGroup.setVisibility(View.GONE);
        viewBinding.ivEditAvatar.setVisibility(View.GONE);
        viewBinding.msgManagerGroup.setVisibility(View.GONE);
      }

      if (isCreator) {
        viewBinding.tvQuit.setText(R.string.qchat_announcement_dismiss);
      } else {
        viewBinding.tvQuit.setText(R.string.qchat_announcement_quit);
      }

      boolean emojiReply = Boolean.TRUE.equals(serverInfo.getAnnouncementInfo().getEmojiReplay());
      viewBinding.swAllowEmoji.setChecked(emojiReply);
      if (managerRoleInfo != null) {
        long managerCount =
            Math.min(managerRoleInfo.getMemberCount() + 1, serverInfo.getMemberCount());
        viewBinding.tvManagerCount.setText(String.valueOf(managerCount));
        long subCount =
            Math.max(serverInfo.getMemberCount() - managerRoleInfo.getMemberCount() - 1, 0);
        viewBinding.tvSubscribeCount.setText(String.valueOf(subCount));
      }
    } else {
      viewBinding.swAllowEmoji.setChecked(false);
      viewBinding.memberManagerGroup.setVisibility(View.GONE);
      viewBinding.channelInfoGroup.setVisibility(View.GONE);
      viewBinding.ivEditAvatar.setVisibility(View.GONE);
      viewBinding.tvQuit.setText(R.string.qchat_announcement_quit);
    }
  }

  /**
   * 页面启动方法
   *
   * @param activity 页面启动 activity
   * @param serverInfo 公告频道信息
   * @param serverId 公告频道对应的社区id
   */
  public static void launch(Activity activity, QChatServerInfo serverInfo, long serverId) {
    Intent intent = new Intent(activity, SettingActivity.class);
    intent.putExtra(QChatConstant.SERVER_ID, serverId);
    intent.putExtra(QChatConstant.SERVER_INFO, serverInfo);
    activity.startActivity(intent);
  }
}
