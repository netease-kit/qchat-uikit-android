// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.message;

import static com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant.LIB_TAG;
import static com.netease.yunxin.kit.qchatkit.ui.server.viewmodel.QChatServerListViewModel.ServerVisitorInfoMgr;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import com.netease.nimlib.sdk.msg.constant.MsgStatusEnum;
import com.netease.nimlib.sdk.msg.constant.MsgTypeEnum;
import com.netease.yunxin.kit.alog.ALog;
import com.netease.yunxin.kit.common.ui.dialog.ChoiceListener;
import com.netease.yunxin.kit.common.ui.dialog.CommonAlertDialog;
import com.netease.yunxin.kit.common.ui.dialog.CommonChoiceDialog;
import com.netease.yunxin.kit.common.ui.fragments.BaseFragment;
import com.netease.yunxin.kit.common.ui.utils.ToastX;
import com.netease.yunxin.kit.common.ui.viewmodel.FetchResult;
import com.netease.yunxin.kit.common.ui.viewmodel.LoadStatus;
import com.netease.yunxin.kit.common.utils.CommonFileProvider;
import com.netease.yunxin.kit.common.utils.KeyboardUtils;
import com.netease.yunxin.kit.common.utils.NetworkUtils;
import com.netease.yunxin.kit.common.utils.PermissionUtils;
import com.netease.yunxin.kit.corekit.im.IMKitClient;
import com.netease.yunxin.kit.corekit.im.provider.FetchCallback;
import com.netease.yunxin.kit.corekit.im.utils.RouterConstant;
import com.netease.yunxin.kit.corekit.qchat.QChatKitClient;
import com.netease.yunxin.kit.corekit.route.XKitRouter;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageQuickCommentDetailInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatQuickCommentDetailInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerRoleInfo;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.announce.QuickEmojiManager;
import com.netease.yunxin.kit.qchatkit.ui.common.QChatCache;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatChannelMessageFragmentBinding;
import com.netease.yunxin.kit.qchatkit.ui.message.audio.QChatMessageAudioControl;
import com.netease.yunxin.kit.qchatkit.ui.message.interfaces.IMessageLoadHandler;
import com.netease.yunxin.kit.qchatkit.ui.message.interfaces.IMessageProxy;
import com.netease.yunxin.kit.qchatkit.ui.message.interfaces.IQChatMsgClickListener;
import com.netease.yunxin.kit.qchatkit.ui.message.popmenu.IQChatPopMenuClickListener;
import com.netease.yunxin.kit.qchatkit.ui.message.popmenu.QChatPopMenu;
import com.netease.yunxin.kit.qchatkit.ui.message.view.PhotoPickerDialog;
import com.netease.yunxin.kit.qchatkit.ui.message.view.QChatMessageAdapter;
import com.netease.yunxin.kit.qchatkit.ui.message.view.QChatMessageListView;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant;
import com.netease.yunxin.kit.qchatkit.ui.server.viewmodel.QChatServerListViewModel;
import com.netease.yunxin.kit.qchatkit.ui.utils.FileUtils;
import com.netease.yunxin.kit.qchatkit.ui.utils.MessageUtil;
import com.netease.yunxin.kit.qchatkit.ui.utils.QChatUtils;
import com.netease.yunxin.kit.qchatkit.ui.utils.SendImageHelper;
import java.io.File;
import java.util.List;
import java.util.Map;

/** 聊天页面Fragment */
public class MessageFragment extends BaseFragment {

  public static final String TAG = "QChatChannelMessageFragment";
  private final int REQUEST_PERMISSION = 0;
  private int currentRequest = 0;

  private final int MAX_QUICK_COMMENT_COUNT = 50;

  private QChatChannelMessageFragmentBinding viewBinding;
  private MessageViewModel viewModel;
  private ActivityResultLauncher<Intent> activityResultLauncher;
  private File tempFile;
  private PhotoPickerDialog photoPickerDialog;
  protected QChatPopMenu popMenu;

  protected QChatServerInfo serverInfo;
  private long serverId;
  private long channelId;
  private boolean isAnnounceManager = false;
  private String hint;
  private ActivityResultLauncher<String[]> permissionLauncher;
  private static final int COPY_SHOW_TIME = 1000;
  private static final int AUDIO_MIN_TIME = 1000;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    ALog.d(TAG, "onCreateView");
    viewBinding = QChatChannelMessageFragmentBinding.inflate(inflater, container, false);
    return viewBinding.getRoot();
  }

  public void init(QChatServerInfo serverInfo, long serverId, long channelId) {
    ALog.d(TAG, "init", "info:" + serverId + "," + channelId);
    this.serverId = serverId;
    this.channelId = channelId;
    updateServerInfo(serverInfo);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    viewModel = new ViewModelProvider(this).get(MessageViewModel.class);
    viewModel.init(serverId, channelId);
    initView();
    initLauncher();
    //初始化底部输入框，输入框的操作时间通过IMessageProxy回调到Fragment中
    viewBinding.qChatMessageBottomLayout.init(messageProxy);
    //监听列表滑动，加载更多历史消息
    viewBinding.qChatMessageListRecyclerView.setLoadHandler(
        new IMessageLoadHandler() {
          @Override
          public boolean loadMoreForward(QChatMessageInfo messageInfo) {
            viewModel.fetchForwardMessage(messageInfo);
            return true;
          }

          @Override
          public boolean loadMoreBackground(QChatMessageInfo messageInfo) {
            viewModel.fetchBackwardMessage(messageInfo);
            return true;
          }
        });

    viewModel.getQueryMessageLiveData().observeForever(queryMessageObserver);
    viewModel.getSendMessageLiveData().observeForever(sendMessageObserver);
    viewModel.getDeleteMessageLiveData().observeForever(deleteMessageObserver);
    viewModel.getRevokeMessageLiveData().observeForever(revokeMessageObserver);
    viewModel.getQuickCommentLiveData().observeForever(quickCommentObserver);
    viewModel.getServerRoleLiveData().observeForever(serverRoleObserver);
    viewModel.getEnterLeaveServerLiveData().observeForever(enterLiveObserver);

    NetworkUtils.registerNetworkStatusChangedListener(networkStateListener);
    //网络连接去远程查询消息，否则加载本地缓存
    if (NetworkUtils.isConnected()) {
      viewModel.fetchMessageList();
      if (serverInfo != null
          && serverInfo.getAnnouncementInfo() != null
          && serverInfo.getAnnouncementInfo().getManagerRoleId() != null
          && !TextUtils.equals(serverInfo.getOwner(), QChatKitClient.account())) {
        viewModel.queryManager(
            serverId,
            QChatKitClient.account(),
            serverInfo.getAnnouncementInfo().getManagerRoleId());
      }
    } else {
      viewModel.loadMessageCache();
    }

    //监听游客模式
    ServerVisitorInfoMgr.getInstance().addObserver(serverVisitorDataChangeObserver);
  }

  private void initView() {
    if (ServerVisitorInfoMgr.getInstance().isVisitor(serverId)) {
      viewBinding.viewQChatVisitor.setVisibility(View.VISIBLE);
      viewBinding.viewQChatVisitor.configToJoinServerInfo(serverId, null);
      viewBinding.viewQChatVisitor.configBackgroundResource(R.color.color_transparent);
    } else {
      viewBinding.viewQChatVisitor.setVisibility(View.GONE);
    }

    viewBinding.qChatMessageBottomLayout.setVisibility(View.VISIBLE);
    viewBinding.qChatMessageBottomTips.setVisibility(View.GONE);

    //列表有新消息，默认滚动到底部
    viewBinding.qChatMessageListRecyclerView.addOnLayoutChangeListener(
        (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
          if (bottom < oldBottom) {
            viewBinding.qChatMessageListRecyclerView.scrollBy(0, oldBottom - bottom);
          }
        });
    //滚动消息列表，收起输入框中输入法和操作按钮
    viewBinding.qChatMessageListRecyclerView.setOnListViewEventListener(
        new QChatMessageListView.OnListViewEventListener() {
          @Override
          public void onListViewStartScroll() {
            viewBinding.qChatMessageBottomLayout.collapse(true);
          }

          @Override
          public void onListViewTouched() {
            viewBinding.qChatMessageBottomLayout.collapse(true);
          }
        });

    viewBinding.qChatMessageListRecyclerView.setOnTouchListener(
        (v, event) -> {
          viewBinding.qChatMessageBottomLayout.collapse(true);
          return false;
        });

    // 设置消息列表中 消息点击时间，包括消息点击、头像点击、发送失败按钮点击、撤回消息点击
    viewBinding.qChatMessageListRecyclerView.setQChatMsgClickListener(
        new IQChatMsgClickListener() {

          // 消息点击事件
          @Override
          public boolean onMessageClick(View view, int position, QChatMessageInfo message) {
            clickMessage(message);
            return true;
          }

          // 消息长按
          @Override
          public boolean onMessageLongClick(View view, int position, QChatMessageInfo message) {
            if (ServerVisitorInfoMgr.getInstance().isVisitor(serverId)) {
              return true;
            }
            if (message.isRevoke()) {
              return false;
            }
            showPopMenu(view, message, false);
            return true;
          }

          // 头像点击
          @Override
          public boolean onUserIconClick(View view, int position, QChatMessageInfo messageInfo) {
            if (TextUtils.equals(IMKitClient.account(), messageInfo.getFromAccount())) {
              XKitRouter.withKey(RouterConstant.PATH_MINE_INFO_PAGE)
                  .withContext(requireContext())
                  .navigate();
            } else {
              XKitRouter.withKey(RouterConstant.PATH_USER_INFO_PAGE)
                  .withContext(requireContext())
                  .withParam(RouterConstant.KEY_ACCOUNT_ID_KEY, messageInfo.getFromAccount())
                  .navigate();
            }
            return true;
          }

          // 发送失败按钮点击
          public boolean onSendFailBtnClick(View view, int position, QChatMessageInfo messageInfo) {
            return true;
          }

          // 撤回消息点击重新编辑
          @Override
          public boolean onReEditRevokeMessage(
              View view, int position, QChatMessageInfo messageInfo) {
            if (messageInfo.isRevoke()) {
              if (viewBinding.viewQChatVisitor.getVisibility() == View.VISIBLE
                  || viewBinding.qChatMessageBottomTips.getVisibility() == View.VISIBLE) {
                Toast.makeText(
                        requireContext(), R.string.qchat_message_no_permission, Toast.LENGTH_SHORT)
                    .show();
                return true;
              }

              if (MessageUtil.revokeMsgIsEdit(messageInfo)) {
                KeyboardUtils.showKeyboard(viewBinding.qChatMessageBottomLayout.getEditText());
                viewBinding.qChatMessageBottomLayout.setInputText(messageInfo.getRevokeText());
              } else {
                Toast.makeText(
                        requireContext(),
                        R.string.qchat_message_revoke_edit_error,
                        Toast.LENGTH_SHORT)
                    .show();
                viewBinding.qChatMessageListRecyclerView.updateMessage(
                    messageInfo, QChatMessageAdapter.REVOKE_STATUS_PAYLOAD);
              }
            }
            return true;
          }

          // 消息中快捷表情添加按钮点击
          @Override
          public boolean onQuickCommentMessage(
              View view,
              int position,
              int type,
              QChatQuickCommentDetailInfo quickCommentInfo,
              QChatMessageInfo messageInfo) {
            if (allowOperateQuickComment()) {
              if (type == 0) {
                showPopMenu(view, messageInfo, true);
              } else if (quickCommentInfo != null && QChatUtils.checkNetworkAndToast()) {
                if (quickCommentInfo.getHasSelf()) {
                  viewModel.removeQuickComment(messageInfo, quickCommentInfo.getType());
                } else {
                  viewModel.addQuickComment(messageInfo, quickCommentInfo.getType());
                }
              }
            }
            return true;
          }
        });

    // 设置消息列表中 消息长按事件
    viewBinding.qChatMessageListRecyclerView.setMsgPopMenuActionListener(
        new IQChatPopMenuClickListener() {
          // 复制消息
          @Override
          public boolean onCopy(QChatMessageInfo data) {
            ClipboardManager cmb =
                (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText(null, data.getContent());
            cmb.setPrimaryClip(clipData);

            showCopyTip();
            return true;
          }

          // 删除消息
          @Override
          public boolean onDelete(QChatMessageInfo messageInfo) {
            showDeleteConfirmDialog(messageInfo);
            return true;
          }

          // 撤回消息
          @Override
          public boolean onRecall(QChatMessageInfo messageInfo) {
            showRevokeConfirmDialog(messageInfo);
            return true;
          }

          // emoji表情点击
          @Override
          public boolean onEmojiClick(QChatMessageInfo messageInfo, int emoji) {
            // 发送emoji表情在快捷评论中，约定索引从1开始，所以此处需要+1
            addQuickCommentEmoji(messageInfo, emoji + 1);
            return true;
          }
        });

    // 消息已读标记
    viewBinding.qChatMessageListRecyclerView.setOptionCallback(
        message -> {
          if (!ServerVisitorInfoMgr.getInstance().isVisitor(serverId)) {
            viewModel.makeMessageRead(message);
          }
        });
  }

  /** 是否允许操作表情快捷评论 访客模式不允许 快捷评论关闭不允许 */
  private boolean allowOperateQuickComment() {
    if (ServerVisitorInfoMgr.getInstance().isVisitor(serverId)) {
      return false;
    }
    // add quick comment
    if (!QuickEmojiManager.isAllowEmojiReply()) {
      Toast.makeText(
              requireContext(), R.string.qchat_message_quick_comment_close_tips, Toast.LENGTH_SHORT)
          .show();
      return false;
    }

    return true;
  }

  /**
   * 消息中添加快捷评论
   *
   * @param messageInfo 消息
   */
  private void addQuickCommentEmoji(QChatMessageInfo messageInfo, int emoji) {
    if (!allowOperateQuickComment() || !QChatUtils.checkNetworkAndToast()) {
      return;
    }
    if (QChatCache.hasSelfQuickComment(messageInfo.getMsgIdServer(), emoji)) {
      viewModel.removeQuickComment(messageInfo, emoji);
    } else {
      QChatMessageQuickCommentDetailInfo detailInfo =
          QChatCache.getQuickComment(messageInfo.getMsgIdServer());
      if (detailInfo != null
          && detailInfo.getTotalCount() >= MAX_QUICK_COMMENT_COUNT
          && !QChatCache.hasCommentType(messageInfo.getMsgIdServer(), emoji)) {
        Toast.makeText(
                requireContext(),
                R.string.qchat_message_quick_comment_max_count_tips,
                Toast.LENGTH_SHORT)
            .show();
      } else {
        viewModel.addQuickComment(messageInfo, emoji);
      }
    }
  }

  @Override
  public void onStop() {
    super.onStop();
    QChatMessageAudioControl.getInstance().stopAudio();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    NetworkUtils.unregisterNetworkStatusChangedListener(networkStateListener);
    viewModel.getSendMessageLiveData().removeObserver(sendMessageObserver);
    viewModel.getQueryMessageLiveData().removeObserver(queryMessageObserver);
    viewModel.getDeleteMessageLiveData().removeObserver(deleteMessageObserver);
    viewModel.getRevokeMessageLiveData().removeObserver(revokeMessageObserver);
    viewModel.getQuickCommentLiveData().removeObserver(quickCommentObserver);
    viewModel.getServerRoleLiveData().removeObserver(serverRoleObserver);
    viewModel.getEnterLeaveServerLiveData().removeObserver(enterLiveObserver);
    ServerVisitorInfoMgr.getInstance().removeObserver(serverVisitorDataChangeObserver);
    QChatCache.clear();
  }

  /** 话题信息变更，更新UI */
  public void updateChannelInfo(QChatChannelInfo channelInfo) {
    if (channelInfo != null && getContext() != null) {
      ALog.d(TAG, "updateChannelInfo", "info");

      if (ServerVisitorInfoMgr.getInstance().isVisitor(serverId)) {
        viewBinding.qChatMessageBottomLayout.configEnable(
            false,
            viewBinding.getRoot().getContext().getString(R.string.qchat_visitor_chat_join_tip));
      } else {
        if (serverInfo != null
            && serverInfo.getAnnouncementInfo() != null
            && serverInfo.getAnnouncementInfo().isValid()) {
          hint =
              String.format(
                  getResources().getString(R.string.qchat_channel_message_send_hint),
                  serverInfo.getName());
        } else {
          hint =
              String.format(
                  getResources().getString(R.string.qchat_channel_message_send_hint),
                  channelInfo.getName());
        }
        viewBinding.qChatMessageBottomLayout.configEnable(true, hint);
      }
    }
  }

  /** 社区信息变更，更新UI */
  public void updateServerInfo(QChatServerInfo server) {
    if (server != null
        && server.getAnnouncementInfo() != null
        && server.getAnnouncementInfo().isValid()) {
      QuickEmojiManager.setAllowEmojiReply(
          Boolean.TRUE.equals(server.getAnnouncementInfo().getEmojiReplay()));

      if (TextUtils.equals(server.getOwner(), QChatKitClient.account())) {
        isAnnounceManager = true;
      }
      if (isAdded()) {
        hint =
            String.format(
                getResources().getString(R.string.qchat_channel_message_send_hint),
                serverInfo.getName());
        viewBinding.qChatMessageBottomLayout.configEnable(true, hint);
      }

    } else {
      QuickEmojiManager.setAllowEmojiReply(true);
    }
    if (serverInfo != null
        && server != null
        && serverInfo.getAnnouncementInfo() != null
        && server.getAnnouncementInfo() != null
        && server.getAnnouncementInfo().isValid()
        && serverInfo.getAnnouncementInfo().getEmojiReplay()
            != server.getAnnouncementInfo().getEmojiReplay()) {
      viewBinding.qChatMessageListRecyclerView.updateMessagePlayLoad(
          QChatMessageAdapter.QUICK_COMMENT_STATUS_PAYLOAD);
    }
    this.serverInfo = server;
  }

  /** 消息查询监听 */
  private final Observer<FetchResult<List<QChatMessageInfo>>> queryMessageObserver =
      result -> {
        if (result.getLoadStatus() == LoadStatus.Success) {
          viewBinding.qChatMessageListRecyclerView.appendMessages(result.getData());
        } else if (result.getLoadStatus() == LoadStatus.Finish) {
          if (result.getType() == FetchResult.FetchType.Add) {
            if (result.getTypeIndex() == -1) {
              viewBinding.qChatMessageListRecyclerView.appendMessages(result.getData());
            } else {
              viewBinding.qChatMessageListRecyclerView.addMessagesForward(result.getData());
              viewBinding.qChatMessageListRecyclerView.setHasMoreForwardMessages(
                  viewModel.isHasForward());
            }
          }
        }
        hidePopMenu();
      };

  /** 发送消息结果监听 */
  private final Observer<FetchResult<QChatMessageInfo>> sendMessageObserver =
      result -> {
        if (result.getLoadStatus() == LoadStatus.Success) {
          ALog.d(TAG, "SendMessageLiveData", "Success");
          viewBinding.qChatMessageListRecyclerView.updateMessageStatus(result.getData());
        } else if (result.getLoadStatus() == LoadStatus.Error) {
          if (result.getError() != null
              && result.getError().getCode() == QChatConstant.ERROR_CODE_IM_NO_PERMISSION) {
            ALog.d(TAG, "SendMessageLiveData", "Error 403");
            showPermissionErrorDialog();
            if (result.getData() != null) {
              viewBinding.qChatMessageListRecyclerView.deleteMessage(result.getData());
            }
          } else {
            if (result.getData() != null) {
              viewBinding.qChatMessageListRecyclerView.updateMessageStatus(result.getData());
            }
          }
          ALog.d(TAG, "SendMessageLiveData", "Error");
        }
      };

  /** 删除消息结果监听 */
  private final Observer<FetchResult<QChatMessageInfo>> deleteMessageObserver =
      new Observer<FetchResult<QChatMessageInfo>>() {
        @Override
        public void onChanged(FetchResult<QChatMessageInfo> result) {
          if (result.getLoadStatus() == LoadStatus.Success) {
            ALog.d(LIB_TAG, TAG, "deleteMessageObserver," + "Success");
            viewBinding.qChatMessageListRecyclerView.deleteMessage(result.getData());
            hidePopMenu();
          }
        }
      };

  /** 撤回消息结果监听 */
  private final Observer<FetchResult<QChatMessageInfo>> revokeMessageObserver =
      new Observer<FetchResult<QChatMessageInfo>>() {
        @Override
        public void onChanged(FetchResult<QChatMessageInfo> result) {
          if (result.getLoadStatus() == LoadStatus.Success) {
            ALog.d(LIB_TAG, TAG, "deleteMessageObserver," + "Success");
            viewBinding.qChatMessageListRecyclerView.revokeMessage(result.getData());
            hidePopMenu();
          }
        }
      };

  /** 快捷评论结果监听 */
  private final Observer<FetchResult<List<Long>>> quickCommentObserver =
      result -> {
        if (result.getLoadStatus() == LoadStatus.Success) {
          ALog.d(LIB_TAG, TAG, "forwardMessageObserver," + "Success");
          List<Long> msgSerIds = result.getData();
          viewBinding.qChatMessageListRecyclerView.updateMessage(
              msgSerIds, QChatMessageAdapter.QUICK_COMMENT_STATUS_PAYLOAD);
        }
      };

  /** 服务器角色信息监听 */
  private final Observer<FetchResult<QChatServerRoleInfo>> serverRoleObserver =
      result -> {
        if (result.getLoadStatus() == LoadStatus.Success) {
          ALog.d(LIB_TAG, TAG, "serverRoleObserver," + "Success");
          QChatServerRoleInfo serverRoleInfo = result.getData();
          if (serverRoleInfo != null
              || TextUtils.equals(serverInfo.getOwner(), QChatKitClient.account())) {
            viewBinding.qChatMessageBottomLayout.setVisibility(View.VISIBLE);
            viewBinding.qChatMessageBottomTips.setVisibility(View.GONE);
            isAnnounceManager = true;
            hidePopMenu();
          } else {
            viewBinding.qChatMessageBottomLayout.setVisibility(View.INVISIBLE);
            viewBinding.qChatMessageBottomTips.setVisibility(View.VISIBLE);
            isAnnounceManager = false;
            hidePopMenu();
          }
        }
      };

  /** 进入离开话题监听，主要用于游客模式 */
  private final Observer<FetchResult<Boolean>> enterLiveObserver =
      result -> {
        if (result.getLoadStatus() == LoadStatus.Success) {
          ALog.d(LIB_TAG, TAG, "enterLiveObserver," + "Success");
          viewBinding.qChatMessageListRecyclerView.updateMessagePlayLoad(
              QChatMessageAdapter.QUICK_COMMENT_STATUS_PAYLOAD);
        }
      };

  private final NetworkUtils.NetworkStateListener networkStateListener =
      new NetworkUtils.NetworkStateListener() {

        @Override
        public void onConnected(NetworkUtils.NetworkType networkType) {
          if (viewBinding == null) {
            return;
          }
          viewBinding.networkTip.setVisibility(View.GONE);
        }

        @Override
        public void onDisconnected() {
          if (viewBinding == null) {
            return;
          }
          viewBinding.networkTip.setVisibility(View.VISIBLE);
        }
      };

  /** 游客模式 */
  private final QChatServerListViewModel.QChatServerVisitorDataChangeObserver
      serverVisitorDataChangeObserver =
          serverInfoForVisitor -> {
            if (viewBinding != null && !ServerVisitorInfoMgr.getInstance().isVisitor(serverId)) {
              viewBinding.viewQChatVisitor.setVisibility(View.GONE);
              viewBinding.qChatMessageBottomLayout.configEnable(true, hint);
            }
          };

  /** 消息操作代理实现 */
  private final IMessageProxy messageProxy =
      new IMessageProxy() {
        @Override
        public boolean sendTextMessage(String msg) {
          ALog.d(TAG, "sendTextMessage", "info:" + msg);
          QChatMessageInfo messageInfo = viewModel.sendTextMessage(msg);
          viewBinding.qChatMessageListRecyclerView.appendMessage(messageInfo);
          return true;
        }

        @Override
        public boolean sendImage() {
          ALog.d(TAG, "sendImage");
          photoPicker();
          return true;
        }

        @Override
        public boolean sendFile() {
          Toast.makeText(getActivityContext(), R.string.qchat_develop_text, Toast.LENGTH_SHORT)
              .show();
          return false;
        }

        @Override
        public boolean sendEmoji() {
          Toast.makeText(getActivityContext(), R.string.qchat_develop_text, Toast.LENGTH_SHORT)
              .show();
          return false;
        }

        @Override
        public boolean sendVoice() {
          Toast.makeText(getActivityContext(), R.string.qchat_develop_text, Toast.LENGTH_SHORT)
              .show();
          return false;
        }

        @Override
        public boolean hasPermission(String permission) {
          if (TextUtils.isEmpty(permission)) {
            return false;
          }
          if (PermissionUtils.hasPermissions(MessageFragment.this.getContext(), permission)) {
            return true;
          } else {
            requestCameraPermission(permission, REQUEST_PERMISSION);
            return false;
          }
        }

        @Override
        public boolean pickMedia() {
          photoPicker();
          return false;
        }

        @Override
        public boolean takePicture() {
          return false;
        }

        @Override
        public boolean captureVideo() {
          return false;
        }

        @Override
        public void onInputPanelExpand() {
          Toast.makeText(getActivityContext(), R.string.qchat_develop_text, Toast.LENGTH_SHORT)
              .show();
        }

        @Override
        public boolean sendAudio(File audioFile, long audioLength) {
          if (audioLength < AUDIO_MIN_TIME) {
            Toast.makeText(
                    getActivityContext(),
                    R.string.qchat_pressed_audio_too_short,
                    Toast.LENGTH_SHORT)
                .show();
          } else {
            QChatMessageInfo messageInfo = viewModel.sendVoiceMessage(audioFile, audioLength);
            viewBinding.qChatMessageListRecyclerView.appendMessage(messageInfo);
          }
          return true;
        }

        @Override
        public void shouldCollapseInputPanel() {}

        @Override
        public String getAccount() {
          return IMKitClient.account();
        }

        @Override
        public Context getActivityContext() {
          return MessageFragment.this.getContext();
        }
      };

  private void initLauncher() {
    activityResultLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
              if (result.getResultCode() == Activity.RESULT_OK) {
                String path = null;
                if (result.getData() == null) {
                  path = tempFile.getPath();
                  ALog.d(TAG, "activityResultLauncher", "info:" + path);
                  QChatMessageInfo messageInfo =
                      viewModel.sendImageMessage(MessageUtil.createImageMessage(path));
                  viewBinding.qChatMessageListRecyclerView.appendMessage(messageInfo);
                } else {
                  Uri imageUri = result.getData().getData();
                  ALog.d(TAG, "activityResultLauncher", "intent info");
                  new SendImageHelper.SendImageTask(
                          getActivity(),
                          imageUri,
                          (filePath, isOrig) -> {
                            QChatMessageInfo messageInfo =
                                viewModel.sendImageMessage(
                                    MessageUtil.createImageMessage(filePath));
                            viewBinding.qChatMessageListRecyclerView.appendMessage(messageInfo);
                          })
                      .execute();
                }
              }
            });

    permissionLauncher =
        registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
              if (result != null) {
                for (Map.Entry<String, Boolean> entry : result.entrySet()) {
                  String permission = entry.getKey();
                  boolean grant = entry.getValue();
                  if (!grant) {
                    if (shouldShowRequestPermissionRationale(permission)) {

                      ToastX.showShortToast(
                          getResources().getString(R.string.qchat_permission_deny_tips));
                    } else {

                      ToastX.showShortToast(
                          MessageUtil.getPermissionText(requireContext(), permission));
                    }
                  }
                }
              }
            });
  }

  private void showRevokeConfirmDialog(QChatMessageInfo messageInfo) {
    CommonChoiceDialog dialog = new CommonChoiceDialog();
    dialog
        .setTitleStr(getString(R.string.qchat_message_action_recall))
        .setContentStr(getString(R.string.qchat_message_action_revoke_this_message))
        .setPositiveStr(getString(R.string.qchat_message_positive_recall))
        .setNegativeStr(getString(R.string.cancel))
        .setConfirmListener(
            new ChoiceListener() {
              @Override
              public void onPositive() {
                if (QChatUtils.checkNetworkAndToast()) {
                  viewModel.revokeMessage(messageInfo);
                }
              }

              @Override
              public void onNegative() {}
            })
        .show(getParentFragmentManager());
  }

  private void showDeleteConfirmDialog(QChatMessageInfo messageInfo) {
    CommonChoiceDialog dialog = new CommonChoiceDialog();
    dialog
        .setTitleStr(getString(R.string.qchat_message_action_delete))
        .setContentStr(getString(R.string.qchat_message_action_delete_this_message))
        .setPositiveStr(getString(R.string.qchat_message_action_delete))
        .setNegativeStr(getString(R.string.cancel))
        .setConfirmListener(
            new ChoiceListener() {
              @Override
              public void onPositive() {

                if (messageInfo.getStatus() == MsgStatusEnum.fail
                    || messageInfo.getStatus() == MsgStatusEnum.sending) {
                  viewModel.deleteMsg(messageInfo);
                } else {
                  if (QChatUtils.checkNetworkAndToast()) {
                    viewModel.deleteMsg(messageInfo);
                  }
                }
              }

              @Override
              public void onNegative() {}
            })
        .show(getParentFragmentManager());
  }

  private void checkPopMenu(QChatMessageInfo message) {
    if (popMenu != null && message != null) {
      popMenu.checkPop(message);
    }
  }

  private void hidePopMenu() {
    if (popMenu != null) {
      popMenu.hide();
    }
  }

  private void showPopMenu(View view, QChatMessageInfo message, boolean onlyEmoji) {
    if (popMenu == null) {
      popMenu = new QChatPopMenu();
    }
    if (popMenu.isShowing()) {
      return;
    }
    if (message == null || message.getStatus() == MsgStatusEnum.sending) {
      return;
    }
    int[] location = new int[2];
    viewBinding.qChatMessageListRecyclerView.getLocationOnScreen(location);
    if (serverInfo != null
        && serverInfo.getAnnouncementInfo() != null
        && serverInfo.getAnnouncementInfo().isValid()) {
      // 公告频道，开启表情评论开关
      if (Boolean.TRUE.equals(serverInfo.getAnnouncementInfo().getEmojiReplay())) {
        popMenu.showEmoji(view, message, location[1], onlyEmoji, true, isAnnounceManager);
      } else { // 公告频道，关闭表情评论开关
        popMenu.show(view, message, location[1], true, isAnnounceManager);
      }
    } else { // 非公告频道，正常使用表情评论功能
      popMenu.showEmoji(view, message, location[1], onlyEmoji);
    }
  }

  private void photoPicker() {

    if (photoPickerDialog == null) {
      photoPickerDialog = new PhotoPickerDialog(requireActivity());
    }

    photoPickerDialog.show(
        new FetchCallback<Integer>() {
          @Override
          public void onSuccess(Integer param) {
            if (param == 0) {
              File file = FileUtils.getTempFile(requireActivity(), null);
              Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
              Uri uri;
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                uri = CommonFileProvider.Companion.getUriForFile(requireActivity(), file);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
              } else {
                uri = Uri.fromFile(file);
              }
              tempFile = file;
              ALog.d(TAG, "photoPickerDialog", "info:" + param + "," + uri.getPath());
              intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
              activityResultLauncher.launch(intent);
            } else if (param == 1) {
              ALog.d(TAG, "photoPickerDialog", "info:" + param);
              Intent intent =
                  new Intent(
                      Intent.ACTION_PICK,
                      android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
              intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
              activityResultLauncher.launch(intent);
            }
            photoPickerDialog.dismiss();
          }

          @Override
          public void onFailed(int code) {
            photoPickerDialog.dismiss();
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            photoPickerDialog.dismiss();
          }
        });
  }

  private void showCopyTip() {
    viewBinding.cvCopyTip.setVisibility(View.VISIBLE);
    viewBinding.cvCopyTip.postDelayed(
        () -> viewBinding.cvCopyTip.setVisibility(View.GONE), COPY_SHOW_TIME);
  }

  private void showPermissionErrorDialog() {
    CommonAlertDialog commonDialog = new CommonAlertDialog();
    commonDialog
        .setContentStr(getString(R.string.qchat_no_permission_content))
        .setPositiveStr(getString(R.string.qchat_ensure))
        .setConfirmListener(() -> {})
        .show(requireActivity().getSupportFragmentManager());
  }

  private void requestCameraPermission(String permission, int request) {
    currentRequest = request;
    permissionLauncher.launch(new String[] {permission});
  }

  private void clickMessage(QChatMessageInfo message) {
    if (message.getMsgType() == MsgTypeEnum.image) {
      List<QChatMessageInfo> messageList =
          viewBinding.qChatMessageListRecyclerView.getQChatMessageByType(MsgTypeEnum.image);
      MessageUtil.startWatchImage(requireContext(), message, messageList);
    }
  }
}
