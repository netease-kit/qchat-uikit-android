// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.announce.viewmodel;

import android.text.TextUtils;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import com.netease.nimlib.sdk.qchat.model.systemnotification.QChatDeleteServerRoleMembersAttachment;
import com.netease.nimlib.sdk.qchat.model.systemnotification.QChatKickServerMembersDoneAttachment;
import com.netease.yunxin.kit.common.ui.viewmodel.BaseViewModel;
import com.netease.yunxin.kit.corekit.im.IMKitClient;
import com.netease.yunxin.kit.corekit.im.model.EventObserver;
import com.netease.yunxin.kit.corekit.im.provider.FetchCallback;
import com.netease.yunxin.kit.corekit.model.ErrorMsg;
import com.netease.yunxin.kit.corekit.model.ResultInfo;
import com.netease.yunxin.kit.qchatkit.repo.QChatRoleRepo;
import com.netease.yunxin.kit.qchatkit.repo.QChatServerRepo;
import com.netease.yunxin.kit.qchatkit.repo.QChatServiceObserverRepo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatAnnounceMemberInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSystemNotificationInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSystemNotificationTypeInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberKick;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberLeave;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerRemove;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerRoleMemberDelete;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class RoleMemberViewModel extends BaseViewModel {
  private static final int LOAD_MORE_LIMIT = 100;

  private final MutableLiveData<ResultInfo<List<QChatAnnounceMemberInfo>>> initManagersResult =
      new MutableLiveData<>();
  private final MutableLiveData<ResultInfo<List<QChatAnnounceMemberInfo>>> loadMoreManagersResult =
      new MutableLiveData<>();
  private final MutableLiveData<ResultInfo<List<String>>> addManagersResult =
      new MutableLiveData<>();
  private final MutableLiveData<ResultInfo<String>> kickManagerResult = new MutableLiveData<>();
  private final MutableLiveData<Object> finishResult = new MutableLiveData<>();
  private final MutableLiveData<List<String>> memberRemoveResult = new MutableLiveData<>();

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
            if (type == ServerRoleMemberDelete.INSTANCE
                && attachment
                    instanceof
                    QChatDeleteServerRoleMembersAttachment) { //QChatDeleteServerRoleMembersAttachment
              List<String> deleteIds =
                  ((QChatDeleteServerRoleMembersAttachment) attachment).getDeleteAccids();
              if (deleteIds != null) {
                if (deleteIds.contains(currentAccount)) {
                  finishResult.setValue(new Object());
                } else {
                  memberRemoveResult.setValue(deleteIds);
                }
              }
            } else if (type == ServerRemove.INSTANCE) { // to finish from accId
              finishResult.setValue(new Object());
            } else if (type == ServerMemberLeave.INSTANCE) { // to finish from accId
              if (TextUtils.equals(currentAccount, info.getFromAccount())) {
                finishResult.setValue(new Object());
              } else {
                memberRemoveResult.setValue(Collections.singletonList(info.getFromAccount()));
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
                } else {
                  memberRemoveResult.setValue(kickedIds);
                }
              }
            }
          }
        }
      };

  private long serverId;

  private long channelId;

  private long roleId;

  private String ownerId;

  public RoleMemberViewModel() {
    QChatServiceObserverRepo.observerSystemNotificationWithType(
        eventForNotification,
        Arrays.asList(
            ServerRoleMemberDelete.INSTANCE,
            ServerRemove.INSTANCE,
            ServerMemberLeave.INSTANCE,
            ServerMemberKick.INSTANCE));
  }

  public void configType(long serverId, long channelId, long roleId, String ownerId) {
    this.serverId = serverId;
    this.channelId = channelId;
    this.roleId = roleId;
    this.ownerId = ownerId;
  }

  public MutableLiveData<ResultInfo<List<QChatAnnounceMemberInfo>>> getInitManagersResult() {
    return initManagersResult;
  }

  public MutableLiveData<ResultInfo<List<QChatAnnounceMemberInfo>>> getLoadMoreManagersResult() {
    return loadMoreManagersResult;
  }

  public MutableLiveData<ResultInfo<List<String>>> getAddManagersResult() {
    return addManagersResult;
  }

  public MutableLiveData<ResultInfo<String>> getKickManagerResult() {
    return kickManagerResult;
  }

  public MutableLiveData<Object> getFinishResult() {
    return finishResult;
  }

  public MutableLiveData<List<String>> getMemberRemoveResult() {
    return memberRemoveResult;
  }

  public void init() {
    getServerRoleMembers(0, ownerId, null, initManagersResult);
  }

  public void loadMore(long timeTag, String accId) {
    getServerRoleMembers(timeTag, null, accId, loadMoreManagersResult);
  }

  public void kickManager(String accId) {
    if (TextUtils.isEmpty(accId)) {
      return;
    }
    QChatRoleRepo.removeServerRoleMember(
        serverId,
        roleId,
        Collections.singletonList(accId),
        new FetchCallback<List<String>>() {
          @Override
          public void onSuccess(@Nullable List<String> param) {
            kickManagerResult.setValue(new ResultInfo<>(accId));
            QChatRoleRepo.removeMemberRole(serverId, channelId, accId, null);
          }

          @Override
          public void onFailed(int code) {
            kickManagerResult.setValue(new ResultInfo<>(null, false, new ErrorMsg(code)));
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            kickManagerResult.setValue(
                new ResultInfo<>(
                    null, false, new ErrorMsg(-1, "Error is " + exception, exception)));
          }
        });
  }

  public void addManagers(List<String> accIdList) {
    if (accIdList == null) {
      return;
    }
    QChatRoleRepo.addServerRoleMember(
        serverId,
        roleId,
        accIdList,
        new FetchCallback<List<String>>() {
          @Override
          public void onSuccess(@Nullable List<String> param) {
            addManagersResult.setValue(new ResultInfo<>(param));
            for (String item : accIdList) {
              QChatRoleRepo.removeMemberRole(serverId, channelId, item, null);
            }
          }

          @Override
          public void onFailed(int code) {
            addManagersResult.setValue(new ResultInfo<>(null, false, new ErrorMsg(code)));
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            addManagersResult.setValue(
                new ResultInfo<>(
                    null, false, new ErrorMsg(-1, "Error is " + exception, exception)));
          }
        });
  }

  private void getServerRoleMembers(
      long timeTag,
      String ownerId,
      String anchorAccId,
      MutableLiveData<ResultInfo<List<QChatAnnounceMemberInfo>>> result) {

    QChatServerRepo.getAnnounceServerManagerMemberByPage(
        serverId,
        timeTag,
        LOAD_MORE_LIMIT,
        roleId,
        ownerId,
        anchorAccId,
        new FetchCallback<List<QChatAnnounceMemberInfo>>() {
          @Override
          public void onSuccess(@Nullable List<QChatAnnounceMemberInfo> param) {
            result.setValue(new ResultInfo<>(param));
          }

          @Override
          public void onFailed(int code) {
            result.setValue(new ResultInfo<>(null, false, new ErrorMsg(code)));
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            result.setValue(
                new ResultInfo<>(
                    null, false, new ErrorMsg(-1, "Error is " + exception, exception)));
          }
        });
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    QChatServiceObserverRepo.observerSystemNotification(eventForNotification, false);
  }
}
