// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.announce.viewmodel;

import static com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant.LIB_TAG;

import android.text.TextUtils;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import com.netease.nimlib.qchat.model.systemnotification.QChatSystemNotificationAttachmentImpl;
import com.netease.nimlib.sdk.qchat.enums.QChatRoleOption;
import com.netease.nimlib.sdk.qchat.enums.QChatRoleResource;
import com.netease.nimlib.sdk.qchat.model.systemnotification.QChatUpdateMemberRoleAuthsAttachment;
import com.netease.nimlib.sdk.qchat.model.systemnotification.QChatUpdateServerAttachment;
import com.netease.yunxin.kit.alog.ALog;
import com.netease.yunxin.kit.common.ui.viewmodel.BaseViewModel;
import com.netease.yunxin.kit.common.ui.viewmodel.FetchResult;
import com.netease.yunxin.kit.common.ui.viewmodel.LoadStatus;
import com.netease.yunxin.kit.corekit.im.IMKitClient;
import com.netease.yunxin.kit.corekit.im.model.EventObserver;
import com.netease.yunxin.kit.corekit.im.provider.FetchCallback;
import com.netease.yunxin.kit.qchatkit.repo.QChatRoleRepo;
import com.netease.yunxin.kit.qchatkit.repo.QChatServerRepo;
import com.netease.yunxin.kit.qchatkit.repo.QChatServiceObserverRepo;
import com.netease.yunxin.kit.qchatkit.repo.model.MemberRoleAuthUpdate;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerRoleInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSystemNotificationInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSystemNotificationTypeInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberApplyDone;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberInviteDone;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberKick;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberLeave;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberUpdate;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerRemove;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerRoleMemberAdd;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerRoleMemberDelete;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerRoleResult;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerUpdate;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant;
import com.netease.yunxin.kit.qchatkit.ui.utils.QChatUtils;
import com.netease.yunxin.kit.qchatkit.ui.utils.ServerUtils;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** 公告频道设置管理 */
public class SettingViewModel extends BaseViewModel {

  public final String TAG = SettingViewModel.class.getSimpleName();
  private QChatServerInfo observerServerInfo;
  private final MutableLiveData<FetchResult<QChatServerInfo>> serverLiveData =
      new MutableLiveData<>();
  private final MutableLiveData<FetchResult<Boolean>> updateServerLiveData =
      new MutableLiveData<>();

  private final MutableLiveData<FetchResult<List<QChatServerRoleInfo>>> roleLiveData =
      new MutableLiveData<>();

  private final MutableLiveData<FetchResult<Map<QChatRoleResource, QChatRoleOption>>> authLiveData =
      new MutableLiveData<>();

  public MutableLiveData<FetchResult<QChatServerInfo>> getServerLiveData() {
    return serverLiveData;
  }

  public MutableLiveData<FetchResult<Boolean>> getUpdateServerLiveData() {
    return updateServerLiveData;
  }

  public MutableLiveData<FetchResult<List<QChatServerRoleInfo>>> getRoleLiveData() {
    return roleLiveData;
  }

  public MutableLiveData<FetchResult<Map<QChatRoleResource, QChatRoleOption>>> getAuthLiveData() {
    return authLiveData;
  }

  public void init(QChatServerInfo serverInfo) {
    observerServerInfo = serverInfo;
    ALog.d(TAG, "registerDeleteChannelObserver");
    QChatServiceObserverRepo.observerSystemNotification(notificationObserver, true);
  }

  /** 获取公告频道信息 */
  public void getAnnounceServer() {
    if (observerServerInfo == null || observerServerInfo.getAnnouncementInfo() == null) {
      return;
    }
    ALog.d(LIB_TAG, TAG, "getAnnounceServer serverId = " + observerServerInfo.getServerId());
    QChatServerRepo.getServer(
        observerServerInfo.getServerId(),
        new FetchCallback<QChatServerInfo>() {
          @Override
          public void onSuccess(@Nullable QChatServerInfo server) {
            ALog.d(LIB_TAG, TAG, "getServer result success");
            FetchResult<QChatServerInfo> fetchResult = new FetchResult<>(LoadStatus.Success);
            fetchResult.setData(server);
            serverLiveData.setValue(fetchResult);
          }

          @Override
          public void onFailed(int code) {
            QChatUtils.operateError(code);
            ALog.d(LIB_TAG, TAG, "getServer result onFailed:" + code);
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            ALog.d(LIB_TAG, TAG, "getServer result onException");
            QChatUtils.operateError(-1);
          }
        });
    requestRoleInfo();
    checkPermission();
  }

  /** 请求公告频道管理身份组信息 */
  public void requestRoleInfo() {
    QChatRoleRepo.fetchServerRoles(
        observerServerInfo.getServerId(),
        0,
        200,
        new FetchCallback<ServerRoleResult>() {
          @Override
          public void onSuccess(@Nullable ServerRoleResult param) {
            ALog.d(LIB_TAG, TAG, "fetchMemberJoinedRoles result success");
            if (param != null && param.getRoleList() != null) {
              FetchResult<List<QChatServerRoleInfo>> fetchResult =
                  new FetchResult<>(LoadStatus.Success);
              fetchResult.setData(param.getRoleList());
              roleLiveData.setValue(fetchResult);
            }
          }

          @Override
          public void onFailed(int code) {
            QChatUtils.operateError(code);
            ALog.d(LIB_TAG, TAG, "fetchMemberJoinedRoles result onFailed:" + code);
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            ALog.d(LIB_TAG, TAG, "fetchMemberJoinedRoles result onException");
          }
        });
  }

  /** 权限检查 */
  public void checkPermission() {
    QChatRoleRepo.checkPermissions(
        observerServerInfo.getServerId(),
        observerServerInfo.getAnnouncementInfo().getChannelId(),
        Collections.singletonList(QChatRoleResource.MANAGE_CHANNEL),
        new FetchCallback<Map<QChatRoleResource, QChatRoleOption>>() {
          @Override
          public void onSuccess(@Nullable Map<QChatRoleResource, QChatRoleOption> param) {
            ALog.d(LIB_TAG, TAG, "checkPermission result success");
            if (param != null) {
              authLiveData.setValue(new FetchResult<>(LoadStatus.Success, param));
            }
          }

          @Override
          public void onFailed(int code) {
            ALog.d(LIB_TAG, TAG, "checkPermission result onFailed:" + code);
            QChatUtils.operateError(-1);
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            ALog.d(LIB_TAG, TAG, "checkPermission result onException");
            QChatUtils.operateError(-1);
          }
        });
  }

  /**
   * 更新公告频道名称及头像
   *
   * @param serverId 公告频道对应社区id
   * @param icon 公告频道头像
   * @param name 公告频道名称
   */
  public void updateServer(long serverId, String icon, String name) {
    QChatServerRepo.updateServer(
        serverId,
        name,
        icon,
        new FetchCallback<Void>() {
          @Override
          public void onSuccess(@Nullable Void param) {
            updateServerLiveData.setValue(new FetchResult<>(FetchResult.FetchType.Add, true));
          }

          @Override
          public void onFailed(int code) {
            QChatUtils.operateError(code);
            updateServerLiveData.setValue(new FetchResult<>(FetchResult.FetchType.Add, false));
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            QChatUtils.operateError(-1);
            updateServerLiveData.setValue(new FetchResult<>(FetchResult.FetchType.Add, false));
          }
        });
  }

  /**
   * 设置当前公告频道是否支持表情回复
   *
   * @param server 公告频道信息
   * @param enable true 允许表情回复；false 不允许表情回复
   */
  public void setEmojiApply(QChatServerInfo server, boolean enable) {
    if (server == null || server.getAnnouncementInfo() == null) {
      return;
    }
    QChatRoleResource roleResource =
        new QChatRoleResource(
            QChatConstant.QCHAT_SELF_PERMISSION_EMOJI_REPLAY,
            QChatConstant.QCHAT_PERMISSION_TYPE_ALL);
    QChatRoleRepo.checkPermissions(
        server.getServerId(),
        server.getAnnouncementInfo().getChannelId(),
        Collections.singletonList(roleResource),
        new FetchCallback<Map<QChatRoleResource, QChatRoleOption>>() {
          @Override
          public void onSuccess(@Nullable Map<QChatRoleResource, QChatRoleOption> param) {
            ALog.d(LIB_TAG, TAG, "setEmojiApply checkPermission result:success");
            if (param != null && param.get(roleResource) != QChatRoleOption.DENY) {
              server.getAnnouncementInfo().setEmojiReplay(enable);
              String custom = QChatServerInfo.generateCustom(server.getAnnouncementInfo(), null);
              QChatServerRepo.updateServer(
                  server.getServerId(),
                  null,
                  null,
                  custom,
                  new FetchCallback<Void>() {
                    @Override
                    public void onSuccess(@Nullable Void param) {
                      ALog.d(LIB_TAG, TAG, "setEmojiApply result:success");
                      updateServerLiveData.setValue(
                          new FetchResult<>(FetchResult.FetchType.Update, true));
                    }

                    @Override
                    public void onFailed(int code) {
                      ALog.d(LIB_TAG, TAG, "setEmojiApply result:onFailed=" + code);
                      QChatUtils.operateError(code);
                      updateServerLiveData.setValue(
                          new FetchResult<>(FetchResult.FetchType.Update, false));
                    }

                    @Override
                    public void onException(@Nullable Throwable exception) {
                      ALog.d(LIB_TAG, TAG, "setEmojiApply result:onException");
                      QChatUtils.operateError(-1);
                      updateServerLiveData.setValue(
                          new FetchResult<>(FetchResult.FetchType.Update, false));
                    }
                  });
            } else {
              updateServerLiveData.setValue(new FetchResult<>(FetchResult.FetchType.Update, false));
            }
          }

          @Override
          public void onFailed(int code) {
            ALog.d(LIB_TAG, TAG, "setEmojiApply checkPermission result:onFailed=" + code);
            QChatUtils.operateError(code);
            updateServerLiveData.setValue(new FetchResult<>(FetchResult.FetchType.Update, false));
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            ALog.d(LIB_TAG, TAG, "setEmojiApply checkPermission result:onException");
            QChatUtils.operateError(-1);
            updateServerLiveData.setValue(new FetchResult<>(FetchResult.FetchType.Update, false));
          }
        });
  }

  /**
   * 离开公告频道
   *
   * @param serverId 公告频道对应社区id
   */
  public void leaveServer(long serverId) {
    QChatServerRepo.leaveServer(
        serverId,
        new FetchCallback<Void>() {
          @Override
          public void onSuccess(@Nullable Void param) {
            ALog.d(LIB_TAG, TAG, "setPushConfig result:success");
            updateServerLiveData.setValue(new FetchResult<>(FetchResult.FetchType.Remove, true));
          }

          @Override
          public void onFailed(int code) {
            ALog.d(LIB_TAG, TAG, "leaveServer result:onFailed=" + code);
            QChatUtils.operateError(code);
            updateServerLiveData.setValue(new FetchResult<>(FetchResult.FetchType.Remove, false));
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            ALog.d(LIB_TAG, TAG, "leaveServer result:onException");
            QChatUtils.operateError(-1);
            updateServerLiveData.setValue(new FetchResult<>(FetchResult.FetchType.Remove, false));
          }
        });
  }

  public void deleteServer(long serverId) {

    QChatServerRepo.deleteServer(
        serverId,
        new FetchCallback<Void>() {
          @Override
          public void onSuccess(@Nullable Void param) {
            ALog.d(LIB_TAG, TAG, "deleteServer result:success");
            updateServerLiveData.setValue(new FetchResult<>(FetchResult.FetchType.Remove, true));
          }

          @Override
          public void onFailed(int code) {
            ALog.d(LIB_TAG, TAG, "deleteServer result:onFailed=" + code);
            QChatUtils.operateError(code);
            updateServerLiveData.setValue(new FetchResult<>(FetchResult.FetchType.Remove, false));
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            ALog.d(LIB_TAG, TAG, "deleteServer result:onException");
            QChatUtils.operateError(-1);
            updateServerLiveData.setValue(new FetchResult<>(FetchResult.FetchType.Remove, false));
          }
        });
  }

  /** 监听系统通知，包含用户成员是否被删除，公告频道是否被删除，及个人用户定制权限变更 */
  private final EventObserver<List<QChatSystemNotificationInfo>> notificationObserver =
      new EventObserver<List<QChatSystemNotificationInfo>>() {
        @Override
        public void onEvent(@Nullable List<QChatSystemNotificationInfo> eventList) {
          if (eventList == null || eventList.isEmpty()) {
            return;
          }
          // 获取当前用户 accId
          String currentAccount = IMKitClient.account();
          for (QChatSystemNotificationInfo item : eventList) {
            if (item.getServerId() == null
                || item.getServerId() != observerServerInfo.getServerId()) {
              continue;
            }
            QChatSystemNotificationTypeInfo type = item.getType();
            long serverId = item.getServerId() != null ? item.getServerId() : 0;
            ALog.d(TAG, "notificationObserver", "info:" + serverId);
            // 通知是否为当前聊天所处服务
            // 服务是否被移除
            boolean isServerRemoved = type == ServerRemove.INSTANCE;

            // 当前用户是否被踢出当前服务器
            boolean isKickedOutCurrentServer =
                type == ServerMemberKick.INSTANCE
                    && !TextUtils.equals(currentAccount, item.getFromAccount())
                    && (item.getToAccIds() != null && item.getToAccIds().contains(currentAccount));
            // 当前用户是否离开当前服务器
            boolean isLeftCurrentServer =
                type == ServerMemberLeave.INSTANCE
                    && TextUtils.equals(currentAccount, item.getFromAccount());

            if (isServerRemoved || isKickedOutCurrentServer || isLeftCurrentServer) {
              updateServerLiveData.setValue(new FetchResult<>(FetchResult.FetchType.Remove, true));
            } else if (type == ServerMemberUpdate.INSTANCE || type == ServerUpdate.INSTANCE) {
              QChatUpdateServerAttachment attachment =
                  (QChatUpdateServerAttachment) item.getAttachment();
              QChatServerInfo serverInfo = ServerUtils.convertServerInfo(attachment.getServer());
              FetchResult<QChatServerInfo> fetchResult = new FetchResult<>(LoadStatus.Success);
              fetchResult.setData(serverInfo);
              serverLiveData.setValue(fetchResult);
            } else if (type == ServerMemberKick.INSTANCE
                || type == ServerMemberLeave.INSTANCE
                || type == ServerMemberInviteDone.INSTANCE
                || type == ServerMemberApplyDone.INSTANCE
                || type == ServerRoleMemberAdd.INSTANCE
                || type == ServerRoleMemberDelete.INSTANCE) {
              QChatSystemNotificationAttachmentImpl attachment =
                  (QChatSystemNotificationAttachmentImpl) item.getAttachment();
              QChatServerInfo serverInfo = ServerUtils.convertServerInfo(attachment.getServer());
              if (serverInfo != null) {
                FetchResult<QChatServerInfo> fetchResult = new FetchResult<>(LoadStatus.Success);
                fetchResult.setData(serverInfo);
                serverLiveData.setValue(fetchResult);
                requestRoleInfo();
              } else {
                getAnnounceServer();
              }
            } else if (type == MemberRoleAuthUpdate.INSTANCE) {
              QChatUpdateMemberRoleAuthsAttachment attachment =
                  (QChatUpdateMemberRoleAuthsAttachment) item.getAttachment();
              Map<QChatRoleResource, QChatRoleOption> authMap = attachment.getUpdateAuths();
              authLiveData.setValue(new FetchResult<>(LoadStatus.Success, authMap));
            }
          }
        }
      };

  @Override
  protected void onCleared() {
    ALog.d(TAG, "onCleared");
    QChatServiceObserverRepo.observerSystemNotification(notificationObserver, false);
    super.onCleared();
  }
}
