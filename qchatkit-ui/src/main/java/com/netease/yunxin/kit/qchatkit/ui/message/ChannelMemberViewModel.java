// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.message;

import android.text.TextUtils;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
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
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerMemberInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerRoleInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSystemNotificationInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSystemNotificationTypeInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberApplyAccept;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberInviteAccept;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberKick;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberLeave;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberUpdate;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerRemove;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.message.model.ChannelMemberStatusBean;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatBaseBean;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** channel member info view mode fetch channel member */
public class ChannelMemberViewModel extends BaseViewModel {

  private static final String TAG = "ChannelMemberViewModel";
  //话题成员查询 live data
  private final MutableLiveData<FetchResult<List<QChatBaseBean>>> membersLiveData =
      new MutableLiveData<>();
  private final FetchResult<List<QChatBaseBean>> fetchResult = new FetchResult<>(LoadStatus.Finish);

  //话题信息变更live data
  private final MutableLiveData<FetchResult<QChatChannelInfo>> channelInfoLiveData =
      new MutableLiveData<>();
  private final FetchResult<QChatChannelInfo> fetchChannelInfo =
      new FetchResult<>(LoadStatus.Finish);

  //成员身份组 live data
  private final MutableLiveData<FetchResult<List<QChatServerRoleInfo>>> memberRoleLiveData =
      new MutableLiveData<>();
  private final FetchResult<List<QChatServerRoleInfo>> memberRoleResult =
      new FetchResult<>(LoadStatus.Finish);

  //channel remove live data
  private final MutableLiveData<FetchResult<List<Long>>> channelNotifyLiveData =
      new MutableLiveData<>();
  private final FetchResult<List<Long>> channelListResult = new FetchResult<>(LoadStatus.Finish);

  private QChatServerMemberInfo lastRoleInfo;
  private long observerChannelId;
  private long observerServerId;
  private boolean roleHasMore = false;

  public MutableLiveData<FetchResult<List<QChatBaseBean>>> getMembersLiveData() {
    return membersLiveData;
  }

  public MutableLiveData<FetchResult<List<QChatServerRoleInfo>>> getMemberRoleLiveData() {
    return memberRoleLiveData;
  }

  public MutableLiveData<FetchResult<QChatChannelInfo>> getChannelInfoLiveData() {
    return channelInfoLiveData;
  }

  public MutableLiveData<FetchResult<List<Long>>> getChannelNotifyLiveData() {
    return channelNotifyLiveData;
  }

  /** 获取成员列表 */
  public void fetchMemberList(long serverId, long channelId) {
    fetchMemberData(serverId, channelId, 0);
  }

  /** 获取身份组列表 */
  public void fetchMemberRoleList(long serverId, String accId) {
    ALog.d(TAG, "fetchMemberRoleList", "info:" + serverId + "," + accId);
    QChatChannelRepo.fetchServerRolesByAccId(
        serverId,
        accId,
        0,
        QChatConstant.MEMBER_PAGE_SIZE,
        new FetchCallback<List<QChatServerRoleInfo>>() {
          @Override
          public void onSuccess(@Nullable List<QChatServerRoleInfo> param) {
            if (param != null) {
              memberRoleResult.setData(param);
              memberRoleResult.setLoadStatus(LoadStatus.Success);
              memberRoleLiveData.postValue(memberRoleResult);
            }
            ALog.d(TAG, "fetchMemberRoleList", "onSuccess");
          }

          @Override
          public void onError(int code, @Nullable String msg) {
            ALog.d(TAG, "fetchMemberRoleList", "onFailed:" + code);
          }
        });
  }

  /** 查询话题信息 */
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

  /** 初始化，注册相关监听 */
  public void init(long serverId, long channelId) {
    observerChannelId = channelId;
    observerServerId = serverId;
    ALog.d(TAG, "registerDeleteChannelObserver", "info:" + serverId + "," + channelId);
    QChatServiceObserverRepo.observerSystemNotificationWithType(
        notificationObserver,
        Arrays.asList(
            ChannelRemove.INSTANCE,
            ChannelUpdate.INSTANCE,
            ChannelUpdateWhiteBlackRoleMember.INSTANCE,
            ServerRemove.INSTANCE,
            ServerMemberInviteAccept.INSTANCE,
            ServerMemberApplyAccept.INSTANCE,
            ServerMemberKick.INSTANCE,
            ServerMemberLeave.INSTANCE,
            ServerMemberUpdate.INSTANCE));
  }

  /** 分页查询成员列表 */
  private void fetchMemberData(long serverId, long channelId, long offset) {
    QChatChannelRepo.fetchChannelMembers(
        serverId,
        channelId,
        offset,
        QChatConstant.MEMBER_PAGE_SIZE,
        new FetchCallback<List<QChatServerMemberInfo>>() {
          @Override
          public void onSuccess(@Nullable List<QChatServerMemberInfo> param) {
            ArrayList<QChatBaseBean> addList = new ArrayList<>();
            if (param != null && param.size() > 0) {
              for (int index = 0; index < param.size(); index++) {
                QChatServerMemberInfo roleInfo = param.get(index);
                ChannelMemberStatusBean bean = new ChannelMemberStatusBean(roleInfo);
                addList.add(bean);
              }
              lastRoleInfo = param.get(param.size() - 1);
            }
            roleHasMore = param != null && param.size() >= QChatConstant.MEMBER_PAGE_SIZE;
            ALog.d(TAG, "fetchMemberData", "onSuccess:" + addList.size());

            fetchResult.setData(addList);
            if (offset == 0) {
              fetchResult.setLoadStatus(LoadStatus.Success);
            } else {
              fetchResult.setFetchType(FetchResult.FetchType.Add);
              fetchResult.setTypeIndex(-1);
            }
            membersLiveData.postValue(fetchResult);
          }

          @Override
          public void onError(int code, @Nullable String msg) {
            ALog.d(TAG, "fetchMemberData", "onFailed:" + code);
            fetchResult.setError(code, R.string.qchat_channel_fetch_member_error);
            membersLiveData.postValue(fetchResult);
          }
        });
  }

  public void loadMore(long serverId, long channelId) {
    long offset = 0;
    if (lastRoleInfo != null) {
      offset = lastRoleInfo.getCreateTime();
    }
    ALog.d(TAG, "loadMore");
    fetchMemberData(serverId, channelId, offset);
  }

  /** 监听系统通知 社区、话题被删除需要退出页面 当前用户被移除需要退出页面等 */
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
            if (item == null) {
              continue;
            }
            QChatSystemNotificationTypeInfo type = item.getType();
            long channelId = item.getChannelId() != null ? item.getChannelId() : 0;
            long serverId = item.getServerId() != null ? item.getServerId() : 0;
            ALog.d(TAG, "notificationObserver", "info:" + serverId + "," + channelId + "," + type);
            // 通知是否为当前聊天所处服务
            boolean isCurrentServer = observerServerId == serverId;
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
            if (isCurrentServer
                && (isServerRemoved
                    || isChannelRemoved
                    || isKickedOutCurrentServer
                    || isLeftCurrentServer
                    || isBlocked)) {
              channelListResult.setFetchType(FetchResult.FetchType.Remove);
              channelNotifyLiveData.setValue(channelListResult);
            } else if (type == ChannelUpdate.INSTANCE && channelId == observerChannelId) {
              fetchChannelInfo(observerChannelId);
            } else if (serverId == observerServerId
                    && channelId == observerChannelId
                    && type == ChannelUpdateWhiteBlackRoleMember.INSTANCE
                || type == ServerMemberInviteAccept.INSTANCE
                || type == ServerMemberApplyAccept.INSTANCE
                || type == ServerMemberKick.INSTANCE
                || type == ServerMemberLeave.INSTANCE
                || type == ServerMemberUpdate.INSTANCE) {
              fetchMemberList(observerServerId, observerChannelId);
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
