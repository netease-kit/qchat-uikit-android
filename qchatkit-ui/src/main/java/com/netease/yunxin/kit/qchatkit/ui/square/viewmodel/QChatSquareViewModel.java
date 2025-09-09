// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.square.viewmodel;

import android.text.TextUtils;
import android.util.Pair;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import com.netease.yunxin.kit.common.ui.viewmodel.BaseViewModel;
import com.netease.yunxin.kit.corekit.im2.IMKitClient;
import com.netease.yunxin.kit.corekit.im2.extend.FetchCallback;
import com.netease.yunxin.kit.corekit.model.ErrorMsg;
import com.netease.yunxin.kit.corekit.model.ResultInfo;
import com.netease.yunxin.kit.qchatkit.EventObserver;
import com.netease.yunxin.kit.qchatkit.repo.QChatServerRepo;
import com.netease.yunxin.kit.qchatkit.repo.QChatServiceObserverRepo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerMemberInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSystemNotificationInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSystemNotificationTypeInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberApplyAccept;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberApplyDone;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberInviteAccept;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberInviteDone;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberKick;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberLeave;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerRemove;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerUpdate;
import com.netease.yunxin.kit.qchatkit.ui.square.SquareDataSourceHelper;
import com.netease.yunxin.kit.qchatkit.ui.square.model.QChatServerInfoWithJoinState;
import com.netease.yunxin.kit.qchatkit.ui.square.model.QChatSquarePageInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class QChatSquareViewModel extends BaseViewModel {
  // 添加缓存，用于获知数据的 searchType，通过 getServers 接口来获取
  private static final Map<Long, QChatServerInfoWithJoinState> serverInfoCacheMap = new HashMap<>();

  private final MutableLiveData<QChatServerInfoWithJoinState> serverInfoUpdate =
      new MutableLiveData<>();

  private final MutableLiveData<QChatServerInfoWithJoinState> serverJoinedStateChanged =
      new MutableLiveData<>();

  private final MutableLiveData<ResultInfo<List<QChatServerInfoWithJoinState>>>
      loadMoreForServerListResult = new MutableLiveData<>();

  private final MutableLiveData<ResultInfo<List<QChatServerInfoWithJoinState>>>
      initForServerListResult = new MutableLiveData<>();

  private final MutableLiveData<ResultInfo<List<QChatSquarePageInfo>>> squareSearchTypeResult =
      new MutableLiveData<>();

  /** the receiver of system notification. */
  private final EventObserver<List<QChatSystemNotificationInfo>> notificationObserver =
      new EventObserver<List<QChatSystemNotificationInfo>>() {
        @Override
        public void onEvent(@Nullable List<QChatSystemNotificationInfo> eventList) {
          if (eventList == null || eventList.isEmpty()) {
            return;
          }
          String currentAccount = IMKitClient.account();
          for (QChatSystemNotificationInfo item : eventList) {
            if (item == null || item.getServerId() == null) {
              continue;
            }

            QChatSystemNotificationTypeInfo type = item.getType();
            if ((type == ServerMemberInviteDone.INSTANCE
                    && !TextUtils.equals(currentAccount, item.getFromAccount())
                    && (item.getToAccIds() != null && item.getToAccIds().contains(currentAccount)))
                || (type == ServerMemberApplyDone.INSTANCE
                    && TextUtils.equals(currentAccount, item.getFromAccount()))) {
              QChatServerInfoWithJoinState info = serverInfoCacheMap.get(item.getServerId());
              if (info != null) {
                info.joined = true;
                serverJoinedStateChanged.setValue(info);
              }
              break;
            } else {
              if (type == ServerRemove.INSTANCE
                  || (type == ServerMemberLeave.INSTANCE
                      && TextUtils.equals(currentAccount, item.getFromAccount()))
                  || (type == ServerMemberKick.INSTANCE
                      && !TextUtils.equals(currentAccount, item.getFromAccount())
                      && (item.getToAccIds() != null
                          && item.getToAccIds().contains(currentAccount)))) {
                QChatServerInfoWithJoinState info = serverInfoCacheMap.get(item.getServerId());
                if (info != null) {
                  info.joined = false;
                  serverJoinedStateChanged.setValue(info);
                }
              } else if (type == ServerUpdate.INSTANCE) {
                // in the branch, the serverInfo was changed, to get the server info from sdk again.
                QChatServerInfoWithJoinState info = serverInfoCacheMap.get(item.getServerId());
                if (info != null) {
                  getServerInfo(
                      item.getServerId(),
                      info.joined,
                      new FetchCallback<QChatServerInfoWithJoinState>() {
                        @Override
                        public void onSuccess(@Nullable QChatServerInfoWithJoinState param) {
                          if (param != null) {
                            serverInfoUpdate.setValue(param);
                          }
                        }

                        @Override
                        public void onError(int code, @Nullable String msg) {}
                      });
                }
              }
            }
          }
        }
      };

  public int searchType;

  public QChatSquareViewModel() {
    QChatServiceObserverRepo.observerSystemNotificationWithType(
        notificationObserver,
        Arrays.asList(
            ServerUpdate.INSTANCE,
            ServerMemberApplyAccept.INSTANCE,
            ServerMemberApplyDone.INSTANCE,
            ServerMemberInviteDone.INSTANCE,
            ServerMemberInviteAccept.INSTANCE,
            ServerMemberKick.INSTANCE,
            ServerMemberLeave.INSTANCE));
  }

  public MutableLiveData<QChatServerInfoWithJoinState> getServerInfoUpdate() {
    return serverInfoUpdate;
  }

  public MutableLiveData<QChatServerInfoWithJoinState> getServerJoinedStateChanged() {
    return serverJoinedStateChanged;
  }

  public MutableLiveData<ResultInfo<List<QChatServerInfoWithJoinState>>>
      getLoadMoreForServerListResult() {
    return loadMoreForServerListResult;
  }

  public MutableLiveData<ResultInfo<List<QChatServerInfoWithJoinState>>>
      getInitForServerListResult() {
    return initForServerListResult;
  }

  public MutableLiveData<ResultInfo<List<QChatSquarePageInfo>>> getSquareSearchTypeResult() {
    return squareSearchTypeResult;
  }

  public void configSearchType(int searchType) {
    this.searchType = searchType;
  }

  public void initForSearchTypeList() {
    SquareDataSourceHelper.getInstance()
        .requestSquareSearchType(
            new FetchCallback<ResultInfo<List<QChatSquarePageInfo>>>() {
              @Override
              public void onError(int code, @Nullable String msg) {
                squareSearchTypeResult.setValue(new ResultInfo<>(null, false, new ErrorMsg(code)));
              }

              @Override
              public void onSuccess(@Nullable ResultInfo<List<QChatSquarePageInfo>> result) {
                squareSearchTypeResult.setValue(result);
              }
            });
  }

  public void initForServerListData() {
    SquareDataSourceHelper.getInstance()
        .requestServerInfoForSearchType(
            searchType,
            new FetchCallback<ResultInfo<List<QChatServerInfo>>>() {
              @Override
              public void onSuccess(@Nullable ResultInfo<List<QChatServerInfo>> result) {
                if (result != null && result.getSuccess()) {
                  List<QChatServerInfo> serverInfoList = result.getValue();
                  List<Long> serverIdList = new ArrayList<>();
                  List<QChatServerInfoWithJoinState> serverInfoWithJoinStateList =
                      new ArrayList<>();
                  if (serverInfoList != null) {
                    for (QChatServerInfo item : serverInfoList) {
                      serverIdList.add(item.getServerId());
                      QChatServerInfoWithJoinState info = new QChatServerInfoWithJoinState(item);
                      serverInfoWithJoinStateList.add(info);
                      serverInfoCacheMap.put(item.getServerId(), info);
                    }
                  }
                  initForServerListResult.setValue(new ResultInfo<>(serverInfoWithJoinStateList));
                  fetchAndNotifyServerJoinState(serverIdList);
                } else {
                  initForServerListResult.setValue(
                      new ResultInfo<>(Collections.emptyList(), false));
                }
              }

              @Override
              public void onError(int code, @Nullable String msg) {
                initForServerListResult.setValue(
                    new ResultInfo<>(Collections.emptyList(), false, new ErrorMsg(code)));
              }
            });
  }

  private void fetchAndNotifyServerJoinState(List<Long> serverIdList) {
    if (serverIdList == null || serverIdList.isEmpty()) {
      return;
    }
    String currentAccount = IMKitClient.account();
    List<Pair<Long, String>> serverAccountList = new ArrayList<>();
    for (Long serverId : serverIdList) {
      serverAccountList.add(new Pair<>(serverId, currentAccount));
    }
    QChatServerRepo.getServerMembers(
        serverAccountList,
        new FetchCallback<List<QChatServerMemberInfo>>() {
          @Override
          public void onSuccess(@Nullable List<QChatServerMemberInfo> param) {
            if (param != null) {
              for (QChatServerMemberInfo item : param) {
                if (item == null) {
                  continue;
                }
                QChatServerInfoWithJoinState info = serverInfoCacheMap.get(item.getServerId());
                if (info == null) {
                  continue;
                }
                info.joined = true;
                serverJoinedStateChanged.setValue(info);
              }
            }
          }

          @Override
          public void onError(int code, @Nullable String msg) {}
        });
  }

  public void loadMoreForServerList(long timeTag) {
    // TODO: 2023/10/23
  }

  @Override
  public void onDestroy() {
    serverInfoCacheMap.clear();
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    serverInfoCacheMap.clear();
    QChatServiceObserverRepo.observerSystemNotification(notificationObserver, false);
  }

  private void getServerInfo(
      long serverId, Boolean joined, FetchCallback<QChatServerInfoWithJoinState> callback) {
    if (callback == null) {

      return;
    }

    QChatServerRepo.getServers(
        Collections.singletonList(serverId),
        new FetchCallback<List<QChatServerInfo>>() {
          @Override
          public void onSuccess(@Nullable List<QChatServerInfo> param) {
            if (param != null && !param.isEmpty()) {
              {
                QChatServerInfo serverInfo = param.get(0);
                QChatServerInfoWithJoinState serverInfoWithJoinState =
                    new QChatServerInfoWithJoinState(serverInfo, joined != null ? joined : false);
                serverInfoCacheMap.put(serverInfo.getServerId(), serverInfoWithJoinState);
                callback.onSuccess(serverInfoWithJoinState);
              }
            }
          }

          @Override
          public void onError(int code, @Nullable String msg) {}
        });
  }
}
