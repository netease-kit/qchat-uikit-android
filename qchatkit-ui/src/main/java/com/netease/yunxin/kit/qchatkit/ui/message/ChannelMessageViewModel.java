// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.message;

import android.text.TextUtils;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import com.netease.nimlib.sdk.qchat.model.systemnotification.QChatUpdateServerAttachment;
import com.netease.yunxin.kit.alog.ALog;
import com.netease.yunxin.kit.common.ui.viewmodel.BaseViewModel;
import com.netease.yunxin.kit.common.ui.viewmodel.FetchResult;
import com.netease.yunxin.kit.common.ui.viewmodel.LoadStatus;
import com.netease.yunxin.kit.corekit.im2.IMKitClient;
import com.netease.yunxin.kit.corekit.im2.extend.FetchCallback;
import com.netease.yunxin.kit.qchatkit.EventObserver;
import com.netease.yunxin.kit.qchatkit.repo.QChatChannelRepo;
import com.netease.yunxin.kit.qchatkit.repo.QChatServiceObserverRepo;
import com.netease.yunxin.kit.qchatkit.repo.model.ChannelRemove;
import com.netease.yunxin.kit.qchatkit.repo.model.ChannelUpdate;
import com.netease.yunxin.kit.qchatkit.repo.model.ChannelUpdateWhiteBlackRoleMember;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerMemberInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSystemNotificationInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSystemNotificationTypeInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberKick;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberLeave;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberUpdate;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerRemove;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerUpdate;
import com.netease.yunxin.kit.qchatkit.ui.utils.ServerUtils;
import java.util.List;

/** ChannelMessageActivity 对应ViewModel */
public class ChannelMessageViewModel extends BaseViewModel {

  private static final String TAG = "ChannelMemberViewModel";
  // channel member live data

  // 话题信息LiveData
  private final MutableLiveData<FetchResult<QChatChannelInfo>> channelInfoLiveData =
      new MutableLiveData<>();
  private final FetchResult<QChatChannelInfo> fetchChannelInfo =
      new FetchResult<>(LoadStatus.Finish);

  // 社区信息LiveData
  private final MutableLiveData<FetchResult<QChatServerInfo>> serverInfoLiveData =
      new MutableLiveData<>();

  // 话题通知LiveData
  private final MutableLiveData<FetchResult<List<Long>>> channelNotifyLiveData =
      new MutableLiveData<>();
  private final FetchResult<List<Long>> channelListResult = new FetchResult<>(LoadStatus.Finish);

  private QChatServerMemberInfo lastRoleInfo;
  private long observerChannelId;
  private long observerServerId;
  private boolean roleHasMore = false;

  public MutableLiveData<FetchResult<QChatServerInfo>> getServerUpdateLiveData() {
    return serverInfoLiveData;
  }

  public MutableLiveData<FetchResult<QChatChannelInfo>> getChannelInfoLiveData() {
    return channelInfoLiveData;
  }

  public MutableLiveData<FetchResult<List<Long>>> getChannelNotifyLiveData() {
    return channelNotifyLiveData;
  }

  /** 获取话题信息 */
  public void fetchChannelInfo(long channelId) {
    QChatChannelRepo.fetchChannelInfo(
        channelId,
        new FetchCallback<QChatChannelInfo>() {
          @Override
          public void onSuccess(@Nullable QChatChannelInfo param) {
            if (param != null) {
              ALog.d(TAG, "fetchChannelInfo", "onSuccess:");
              fetchChannelInfo.setData(param);
              fetchChannelInfo.setLoadStatus(LoadStatus.Success);
              channelInfoLiveData.postValue(fetchChannelInfo);
            }
          }

          @Override
          public void onError(int code, @Nullable String msg) {
            ALog.d(TAG, "fetchChannelInfo", "onFailed:" + code);
          }
        });
  }

  /** register channel change observer include channel remove ,change ,member change and so on */
  public void init(long serverId, long channelId) {
    observerChannelId = channelId;
    observerServerId = serverId;
    ALog.d(TAG, "registerDeleteChannelObserver", "info:" + serverId + "," + channelId);
    QChatServiceObserverRepo.observerSystemNotification(notificationObserver, true);
  }

  /** 监听系统通知，在社区或者话题发送变更时候，对页面进行刷新 */
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
            if (item == null
                || item.getServerId() == null
                || item.getServerId() != observerServerId) {
              continue;
            }
            QChatSystemNotificationTypeInfo type = item.getType();
            long channelId = item.getChannelId() != null ? item.getChannelId() : 0;
            long serverId = item.getServerId() != null ? item.getServerId() : 0;
            ALog.d(TAG, "notificationObserver", "info:" + serverId + "," + channelId + "," + type);
            // 服务是否被移除
            boolean isServerRemoved = type == ServerRemove.INSTANCE;
            // 当前聊天频道是否被移除
            boolean isChannelRemoved =
                type == ChannelRemove.INSTANCE && channelId == observerChannelId;
            // 当前用户是否被踢出当前服务器
            boolean isKickedOutCurrentServer =
                type == ServerMemberKick.INSTANCE
                    && !TextUtils.equals(currentAccount, item.getFromAccount())
                    && (item.getToAccIds() != null && item.getToAccIds().contains(currentAccount));
            // 当前用户是否离开当前服务器
            boolean isLeftCurrentServer =
                type == ServerMemberLeave.INSTANCE
                    && TextUtils.equals(currentAccount, item.getFromAccount());
            // 当前用户是否被当前频道拉入黑名单
            boolean isBlocked =
                type == ChannelUpdateWhiteBlackRoleMember.INSTANCE
                    && channelId == observerChannelId
                    && !TextUtils.equals(currentAccount, item.getFromAccount())
                    && (item.getToAccIds() != null && item.getToAccIds().contains(currentAccount));

            if (isServerRemoved
                || isChannelRemoved
                || isKickedOutCurrentServer
                || isLeftCurrentServer
                || isBlocked) {
              channelListResult.setFetchType(FetchResult.FetchType.Remove);
              channelNotifyLiveData.setValue(channelListResult);
            } else if (type == ChannelUpdate.INSTANCE && channelId == observerChannelId) {
              fetchChannelInfo(observerChannelId);
            } else if (type == ServerMemberUpdate.INSTANCE || type == ServerUpdate.INSTANCE) {
              QChatUpdateServerAttachment attachment =
                  (QChatUpdateServerAttachment) item.getAttachment();
              QChatServerInfo serverInfo = ServerUtils.convertServerInfo(attachment.getServer());
              FetchResult<QChatServerInfo> fetchResult = new FetchResult<>(LoadStatus.Success);
              fetchResult.setData(serverInfo);
              serverInfoLiveData.setValue(fetchResult);
            }
          }
        }
      };

  public boolean hasMore() {
    return roleHasMore;
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    ALog.d(TAG, "onCleared");
    QChatServiceObserverRepo.observerSystemNotification(notificationObserver, false);
  }
}
