// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.announce.viewmodel;

import android.text.TextUtils;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import com.netease.nimlib.sdk.qchat.enums.QChatRoleResource;
import com.netease.nimlib.sdk.qchat.model.systemnotification.QChatDeleteServerRoleMembersAttachment;
import com.netease.nimlib.sdk.qchat.model.systemnotification.QChatKickServerMembersDoneAttachment;
import com.netease.yunxin.kit.alog.ALog;
import com.netease.yunxin.kit.common.ui.viewmodel.BaseViewModel;
import com.netease.yunxin.kit.corekit.im2.IMKitClient;
import com.netease.yunxin.kit.corekit.im2.extend.FetchCallback;
import com.netease.yunxin.kit.corekit.model.ErrorMsg;
import com.netease.yunxin.kit.corekit.model.ResultInfo;
import com.netease.yunxin.kit.qchatkit.EventObserver;
import com.netease.yunxin.kit.qchatkit.repo.QChatRoleRepo;
import com.netease.yunxin.kit.qchatkit.repo.QChatServerRepo;
import com.netease.yunxin.kit.qchatkit.repo.QChatServiceObserverRepo;
import com.netease.yunxin.kit.qchatkit.repo.model.MemberRoleAuthUpdate;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSystemNotificationInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSystemNotificationTypeInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberKick;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberLeave;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerRemove;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerRoleMemberDelete;
import java.util.Arrays;
import java.util.List;

/** 公告频道信息更新管理 */
public class UpdateInfoViewModel extends BaseViewModel {
  private static final String TAG = "QChatAnnounceUpdateInfoViewModel";

  private final MutableLiveData<Boolean> permissionResult = new MutableLiveData<>();
  private final MutableLiveData<ResultInfo<Void>> updateInfoResult = new MutableLiveData<>();
  private long serverId;
  private long channelId;

  private final MutableLiveData<Object> finishResult = new MutableLiveData<>();

  private final EventObserver<List<QChatSystemNotificationInfo>> eventForNotification =
      new EventObserver<List<QChatSystemNotificationInfo>>() {
        @Override
        public void onEvent(@Nullable List<QChatSystemNotificationInfo> event) {
          if (event == null) {
            return;
          }
          String currentAccount = IMKitClient.account();
          for (QChatSystemNotificationInfo info : event) {
            if (info == null || serverId != info.getServerId()) {
              continue;
            }
            QChatSystemNotificationTypeInfo type = info.getType();
            Object attachment = info.getAttachment();
            if (type == ServerRemove.INSTANCE) { // to finish from accId
              finishResult.setValue(new Object());
            } else if (type == ServerMemberLeave.INSTANCE) { // to finish from accId
              if (TextUtils.equals(currentAccount, info.getFromAccount())) {
                finishResult.setValue(new Object());
              }
            } else if (type == ServerMemberKick.INSTANCE
                && attachment
                    instanceof
                    QChatKickServerMembersDoneAttachment) { //QChatKickServerMembersDoneAttachment
              List<String> kickedIds =
                  ((QChatKickServerMembersDoneAttachment) attachment).getKickedAccids();
              if (kickedIds != null) {
                if (kickedIds.contains(currentAccount)) {
                  finishResult.setValue(new Object());
                }
              }
            } else if (type == MemberRoleAuthUpdate.INSTANCE) {
              checkPermission();
            } else if (type == ServerRoleMemberDelete.INSTANCE
                && attachment instanceof QChatDeleteServerRoleMembersAttachment) {
              List<String> deleteIds =
                  ((QChatDeleteServerRoleMembersAttachment) attachment).getDeleteAccids();
              if (deleteIds != null && deleteIds.contains(currentAccount)) {
                checkPermission();
              }
            }
          }
        }
      };

  public MutableLiveData<Boolean> getPermissionResult() {
    return permissionResult;
  }

  public MutableLiveData<ResultInfo<Void>> getUpdateInfoResult() {
    return updateInfoResult;
  }

  public MutableLiveData<Object> getFinishResult() {
    return finishResult;
  }

  public UpdateInfoViewModel() {
    QChatServiceObserverRepo.observerSystemNotificationWithType(
        eventForNotification,
        Arrays.asList(
            ServerRoleMemberDelete.INSTANCE,
            ServerRemove.INSTANCE,
            ServerMemberLeave.INSTANCE,
            MemberRoleAuthUpdate.INSTANCE,
            ServerMemberKick.INSTANCE));
  }

  public void config(long serverId, long channelId) {
    this.serverId = serverId;
    this.channelId = channelId;
  }

  public void checkPermission() {
    QChatRoleRepo.checkPermission(
        serverId,
        channelId,
        QChatRoleResource.MANAGE_CHANNEL,
        new FetchCallback<Boolean>() {
          @Override
          public void onSuccess(@Nullable Boolean param) {
            if (param == null) {
              permissionResult.setValue(false);
              return;
            }
            permissionResult.setValue(param);
          }

          @Override
          public void onError(int code, @Nullable String msg) {
            permissionResult.setValue(false);
          }
        });
  }

  public void updateTopic(QChatServerInfo serverInfo, String topic) {
    updateInfo(
        serverInfo.getName(),
        QChatServerInfo.generateCustom(serverInfo.getAnnouncementInfo(), topic));
  }

  public void updateName(QChatServerInfo serverInfo, String name) {
    updateInfo(name, serverInfo.getCustom());
  }

  private void updateInfo(String name, String custom) {
    QChatServerRepo.updateServer(
        serverId,
        name,
        null,
        custom,
        new FetchCallback<Void>() {
          @Override
          public void onSuccess(@Nullable Void param) {
            ALog.e(TAG, "updateInfo:onSuccess");
            updateInfoResult.setValue(new ResultInfo<>());
          }

          @Override
          public void onError(int code, @Nullable String msg) {
            ALog.e(TAG, "updateInfo:onFailed:" + code);
            updateInfoResult.setValue(new ResultInfo<>(null, false, new ErrorMsg(code)));
          }
        });
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    QChatServiceObserverRepo.observerSystemNotification(eventForNotification, false);
  }
}
