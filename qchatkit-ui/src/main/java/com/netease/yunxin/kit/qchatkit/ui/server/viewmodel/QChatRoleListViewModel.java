// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.server.viewmodel;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import com.netease.nimlib.sdk.qchat.enums.QChatRoleResource;
import com.netease.yunxin.kit.common.ui.viewmodel.BaseViewModel;
import com.netease.yunxin.kit.common.ui.viewmodel.FetchResult;
import com.netease.yunxin.kit.common.ui.viewmodel.LoadStatus;
import com.netease.yunxin.kit.corekit.im2.extend.FetchCallback;
import com.netease.yunxin.kit.qchatkit.EventObserver;
import com.netease.yunxin.kit.qchatkit.repo.QChatRoleRepo;
import com.netease.yunxin.kit.qchatkit.repo.QChatServiceObserverRepo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSystemNotificationInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSystemNotificationTypeInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerRoleAuthUpdate;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerRoleMemberAdd;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerRoleMemberDelete;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerRoleResult;
import java.util.List;

/** 社区成员列表管理 */
public class QChatRoleListViewModel extends BaseViewModel {

  private final int PAGE_SIZE = 100;

  private long currentSeverId = -1;
  private final MutableLiveData<FetchResult<ServerRoleResult>> roleListLiveData =
      new MutableLiveData<>();

  private final MutableLiveData<FetchResult<Boolean>> rolePermissionLiveData =
      new MutableLiveData<>();

  public MutableLiveData<FetchResult<ServerRoleResult>> getRoleListLiveData() {
    return roleListLiveData;
  }

  public MutableLiveData<FetchResult<Boolean>> getRolePermissionLiveData() {
    return rolePermissionLiveData;
  }

  public QChatRoleListViewModel() {
    QChatServiceObserverRepo.observerSystemNotification(notificationObserver, true);
  }

  /**
   * 获取社区身份组列表
   *
   * @param severId 社区id
   * @param time 请求锚点时间戳
   */
  public void fetchServerRoles(long severId, long time) {
    currentSeverId = severId;
    QChatRoleRepo.fetchServerRoles(
        severId,
        time,
        PAGE_SIZE,
        new FetchCallback<ServerRoleResult>() {
          @Override
          public void onSuccess(@Nullable ServerRoleResult param) {
            FetchResult<ServerRoleResult> result = new FetchResult<>(LoadStatus.Finish, param);
            if (time == 0) {
              result.setFetchType(FetchResult.FetchType.Init);
            } else {
              result.setFetchType(FetchResult.FetchType.Add);
            }
            roleListLiveData.setValue(result);
          }

          @Override
          public void onError(int code, @Nullable String msg) {
            roleListLiveData.setValue(new FetchResult<>(LoadStatus.Error));
          }
        });
  }

  /** 检查{@link QChatRoleResource#MANAGE_ROLE}权限是否存在 */
  public void checkPermission() {
    if (currentSeverId < 0) {
      return;
    }
    QChatRoleRepo.checkPermission(
        currentSeverId,
        null,
        QChatRoleResource.MANAGE_ROLE,
        new FetchCallback<Boolean>() {
          @Override
          public void onSuccess(@Nullable Boolean param) {

            rolePermissionLiveData.setValue(new FetchResult<>(LoadStatus.Success, param));
          }

          @Override
          public void onError(int code, @Nullable String msg) {
            rolePermissionLiveData.setValue(new FetchResult<>(LoadStatus.Error, false));
          }
        });
  }

  /** 监听身份组成员增删及成员权限变更通知 */
  private final EventObserver<List<QChatSystemNotificationInfo>> notificationObserver =
      new EventObserver<List<QChatSystemNotificationInfo>>() {
        @Override
        public void onEvent(@Nullable List<QChatSystemNotificationInfo> eventList) {
          if (eventList == null || eventList.isEmpty()) {
            return;
          }
          for (QChatSystemNotificationInfo info : eventList) {
            QChatSystemNotificationTypeInfo type = info.getType();
            if (info.getServerId() == null || info.getServerId() != currentSeverId) {
              continue;
            }
            if (type == ServerRoleAuthUpdate.INSTANCE
                || type == ServerRoleMemberAdd.INSTANCE
                || type == ServerRoleMemberDelete.INSTANCE) {
              checkPermission();
              fetchServerRoles(currentSeverId, 0);
            }
          }
        }
      };
}
