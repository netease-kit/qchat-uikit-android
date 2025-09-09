// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.announce.viewmodel;

import android.text.TextUtils;
import android.util.Pair;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import com.netease.nimlib.sdk.qchat.enums.QChatRoleOption;
import com.netease.nimlib.sdk.qchat.enums.QChatRoleResource;
import com.netease.nimlib.sdk.qchat.model.systemnotification.QChatAddServerRoleMembersAttachment;
import com.netease.nimlib.sdk.qchat.model.systemnotification.QChatDeleteServerRoleMembersAttachment;
import com.netease.nimlib.sdk.qchat.model.systemnotification.QChatKickServerMembersDoneAttachment;
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
import com.netease.yunxin.kit.qchatkit.repo.model.QChatAnnounceMemberInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSystemNotificationInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSystemNotificationTypeInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberKick;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberLeave;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerRemove;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerRoleMemberAdd;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerRoleMemberDelete;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ServerMemberViewModel extends BaseViewModel {
  private static final int LOAD_MORE_LIMIT = 100;

  private final MutableLiveData<ResultInfo<List<QChatAnnounceMemberInfo>>> initServerMembersResult =
      new MutableLiveData<>();
  private final MutableLiveData<ResultInfo<List<QChatAnnounceMemberInfo>>>
      loadMoreServerMembersResult = new MutableLiveData<>();
  private final MutableLiveData<Boolean> permissionResult = new MutableLiveData<>();
  private final MutableLiveData<ResultInfo<List<String>>> inviteMembersResult =
      new MutableLiveData<>();
  private final MutableLiveData<ResultInfo<String>> kickMemberResult = new MutableLiveData<>();
  private final MutableLiveData<List<String>> memberRemoveResult = new MutableLiveData<>();
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
            if (type == ServerRoleMemberAdd.INSTANCE
                && attachment
                    instanceof
                    QChatAddServerRoleMembersAttachment) { //QChatAddServerRoleMembersAttachment
              List<String> addIds =
                  ((QChatAddServerRoleMembersAttachment) attachment).getAddAccids();
              if (addIds != null) {
                memberRemoveResult.setValue(addIds);
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
            } else if (type == MemberRoleAuthUpdate.INSTANCE) {
              checkPermission();
            } else if (type == ServerRoleMemberDelete.INSTANCE
                && attachment instanceof QChatDeleteServerRoleMembersAttachment) {
              List<String> deleteIds =
                  ((QChatDeleteServerRoleMembersAttachment) attachment).getDeleteAccids();
              if (deleteIds != null && deleteIds.contains(currentAccount)) {
                finishResult.setValue(new Object());
              }
            }
          }
        }
      };

  private final List<String> filterAccIdList = new ArrayList<>();

  private long serverId;

  private long roleId;

  private String ownerId;

  private long channelId;

  public ServerMemberViewModel() {
    QChatServiceObserverRepo.observerSystemNotificationWithType(
        eventForNotification,
        Arrays.asList(
            ServerRoleMemberAdd.INSTANCE,
            ServerRoleMemberDelete.INSTANCE,
            ServerRemove.INSTANCE,
            ServerMemberLeave.INSTANCE,
            MemberRoleAuthUpdate.INSTANCE,
            ServerMemberKick.INSTANCE));
  }

  public void configType(long serverId, long roleId, long channelId, String ownerId) {
    this.serverId = serverId;
    this.roleId = roleId;
    this.ownerId = ownerId;
    this.channelId = channelId;
  }

  public List<String> getFilterAccIdList() {
    return filterAccIdList;
  }

  public MutableLiveData<ResultInfo<List<QChatAnnounceMemberInfo>>> getInitServerMembersResult() {
    return initServerMembersResult;
  }

  public MutableLiveData<ResultInfo<List<QChatAnnounceMemberInfo>>>
      getLoadMoreServerMembersResult() {
    return loadMoreServerMembersResult;
  }

  public MutableLiveData<Boolean> getPermissionResult() {
    return permissionResult;
  }

  public MutableLiveData<ResultInfo<List<String>>> getInviteMembersResult() {
    return inviteMembersResult;
  }

  public MutableLiveData<ResultInfo<String>> getKickMemberResult() {
    return kickMemberResult;
  }

  public MutableLiveData<List<String>> getMemberRemoveResult() {
    return memberRemoveResult;
  }

  public MutableLiveData<Object> getFinishResult() {
    return finishResult;
  }

  public void init() {
    filterAccIdList.clear();
    getServerMemberList(0, initServerMembersResult);
  }

  public void loadMore(long timeTag) {
    getServerMemberList(timeTag, loadMoreServerMembersResult);
  }

  public void checkPermission() {
    QChatRoleRepo.checkPermissions(
        serverId,
        channelId,
        Arrays.asList(
            QChatRoleResource.INVITE_SERVER,
            QChatRoleResource.KICK_SERVER,
            QChatRoleResource.MANAGE_ROLE),
        new FetchCallback<Map<QChatRoleResource, QChatRoleOption>>() {
          @Override
          public void onError(int i, @Nullable String s) {
            permissionResult.setValue(false);
          }

          @Override
          public void onSuccess(@Nullable Map<QChatRoleResource, QChatRoleOption> param) {
            if (param == null) {
              permissionResult.setValue(false);
              return;
            }
            if (param.get(QChatRoleResource.INVITE_SERVER) != QChatRoleOption.ALLOW
                || param.get(QChatRoleResource.KICK_SERVER) != QChatRoleOption.ALLOW
                || param.get(QChatRoleResource.MANAGE_ROLE) != QChatRoleOption.ALLOW) {
              permissionResult.setValue(false);
              return;
            }
            permissionResult.setValue(true);
          }
        });
  }

  public void kickMember(String accId) {
    if (TextUtils.isEmpty(accId)) {
      return;
    }
    QChatServerRepo.kickMember(
        serverId,
        accId,
        new FetchCallback<Void>() {
          @Override
          public void onError(int code, @Nullable String msg) {
            kickMemberResult.setValue(new ResultInfo<>(null, false, new ErrorMsg(code, msg)));
          }

          @Override
          public void onSuccess(@Nullable Void param) {
            kickMemberResult.setValue(new ResultInfo<>(accId));
          }
        });
  }

  public void inviteMember(List<String> accIdList) {
    if (accIdList == null) {
      return;
    }
    QChatServerRepo.inviteServerMembers(
        serverId,
        accIdList,
        new FetchCallback<List<String>>() {
          @Override
          public void onError(int code, @Nullable String msg) {
            inviteMembersResult.setValue(new ResultInfo<>(null, false, new ErrorMsg(code, msg)));
          }

          @Override
          public void onSuccess(@Nullable List<String> param) {
            if (param != null && param.size() == accIdList.size()) {
              inviteMembersResult.setValue(new ResultInfo<>(null, false));
            } else {
              inviteMembersResult.setValue(new ResultInfo<>(param));
            }
          }
        });
  }

  private void getServerMemberList(
      long timeTag, MutableLiveData<ResultInfo<List<QChatAnnounceMemberInfo>>> result) {

    QChatServerRepo.getAnnounceServerNormalMemberByPage(
        serverId,
        timeTag,
        LOAD_MORE_LIMIT,
        roleId,
        ownerId,
        new FetchCallback<Pair<List<QChatAnnounceMemberInfo>, List<String>>>() {
          @Override
          public void onError(int code, @Nullable String s) {
            result.setValue(new ResultInfo<>(null, false, new ErrorMsg(code)));
          }

          @Override
          public void onSuccess(@Nullable Pair<List<QChatAnnounceMemberInfo>, List<String>> param) {
            if (param != null) {
              if (param.second != null) {
                filterAccIdList.addAll(param.second);
              }
              result.setValue(new ResultInfo<>(param.first));
            } else {
              result.setValue(new ResultInfo<>());
            }
          }
        });
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    QChatServiceObserverRepo.observerSystemNotification(eventForNotification, false);
  }
}
