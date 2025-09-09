// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.channel.viewmodel;

import android.util.Pair;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import com.netease.nimlib.sdk.msg.constant.MsgStatusEnum;
import com.netease.nimlib.sdk.qchat.enums.QChatRoleOption;
import com.netease.nimlib.sdk.qchat.enums.QChatRoleResource;
import com.netease.yunxin.kit.alog.ALog;
import com.netease.yunxin.kit.common.ui.viewmodel.BaseViewModel;
import com.netease.yunxin.kit.corekit.event.EventCenter;
import com.netease.yunxin.kit.corekit.event.EventNotify;
import com.netease.yunxin.kit.corekit.im2.extend.FetchCallback;
import com.netease.yunxin.kit.corekit.model.ErrorMsg;
import com.netease.yunxin.kit.corekit.model.ResultInfo;
import com.netease.yunxin.kit.qchatkit.EventObserver;
import com.netease.yunxin.kit.qchatkit.repo.QChatChannelRepo;
import com.netease.yunxin.kit.qchatkit.repo.QChatMessageRepo;
import com.netease.yunxin.kit.qchatkit.repo.QChatRoleRepo;
import com.netease.yunxin.kit.qchatkit.repo.QChatServiceObserverRepo;
import com.netease.yunxin.kit.qchatkit.repo.model.ChannelRoleAuthUpdate;
import com.netease.yunxin.kit.qchatkit.repo.model.MemberRoleAuthUpdate;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelInfoWithLastMessage;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageDeleteEventInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageRevokeEventInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSystemNotificationInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerRoleAuthUpdate;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerRoleMemberAdd;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerRoleMemberDelete;
import com.netease.yunxin.kit.qchatkit.ui.message.model.QChatMsgEvent;
import com.netease.yunxin.kit.qchatkit.ui.server.viewmodel.QChatServerListViewModel;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class QChatChannelListViewModel extends BaseViewModel {
  private static final String TAG = "QChatChannelListViewModel";
  private static final int LOAD_MORE_LIMIT = 20;
  private final Map<Long, QChatMessageInfo> channelIdLastMessageMap = new HashMap<>();

  private final MutableLiveData<ResultInfo<List<QChatChannelInfoWithLastMessage>>> initResult =
      new MutableLiveData<>();

  private final MutableLiveData<ResultInfo<List<QChatChannelInfoWithLastMessage>>> loadMoreResult =
      new MutableLiveData<>();

  private final MutableLiveData<Pair<Long, QChatMessageInfo>> messageUpdateResult =
      new MutableLiveData<>();

  private final MutableLiveData<Map<QChatRoleResource, Boolean>> checkPermissionResult =
      new MutableLiveData<>();

  // 监听话题中接受消息
  private final EventObserver<List<QChatMessageInfo>> receiveMessageObserver =
      new EventObserver<List<QChatMessageInfo>>() {
        @Override
        public void onEvent(@Nullable List<QChatMessageInfo> eventList) {
          if (eventList == null) {
            return;
          }
          for (QChatMessageInfo item : eventList) {
            if (Objects.equals(item.getQChatServerId(), serverId)) {
              channelIdLastMessageMap.put(item.getQChatChannelId(), item);
              messageUpdateResult.setValue(new Pair<>(item.getQChatChannelId(), item));
            }
          }
        }
      };

  // 监听话题中消息状态变化
  private final EventObserver<QChatMessageInfo> messageStateUpdateObserver =
      new EventObserver<QChatMessageInfo>() {
        @Override
        public void onEvent(@Nullable QChatMessageInfo event) {
          if (event != null
              && Objects.equals(event.getQChatServerId(), serverId)
              && event.getStatus() == MsgStatusEnum.success) {
            QChatMessageInfo lastMessageInfo =
                channelIdLastMessageMap.get(event.getQChatChannelId());
            if (lastMessageInfo != null && lastMessageInfo.getTime() > event.getTime()) {
              return;
            }
            channelIdLastMessageMap.put(event.getQChatChannelId(), event);
            messageUpdateResult.setValue(new Pair<>(event.getQChatChannelId(), event));
          }
        }
      };

  // 监听话题中消息删除
  private final EventObserver<QChatMessageDeleteEventInfo> messageDeleteObserver =
      new EventObserver<QChatMessageDeleteEventInfo>() {
        @Override
        public void onEvent(@Nullable QChatMessageDeleteEventInfo event) {
          if (event == null || event.getMessage() == null) {
            return;
          }
          doActionForMsgDelete(event.getMessage());
        }
      };

  // 监听话题中消息撤回
  private final EventObserver<QChatMessageRevokeEventInfo> messageRevokeObserver =
      new EventObserver<QChatMessageRevokeEventInfo>() {
        @Override
        public void onEvent(@Nullable QChatMessageRevokeEventInfo event) {
          if (event == null || event.getMessage() == null) {
            return;
          }
          doActionForMsgRevoke(event.getMessage());
        }
      };

  // 监听话题中消息撤回和删除
  private final EventNotify<QChatMsgEvent> msgEventEventNotify =
      new EventNotify<QChatMsgEvent>() {
        @Override
        public void onNotify(@NonNull QChatMsgEvent event) {
          if (event.eventType == QChatMsgEvent.EventType.Delete) {
            doActionForMsgDelete(event.msgInfo);
          } else {
            doActionForMsgRevoke(event.msgInfo);
          }
        }

        @NonNull
        @Override
        public String getEventType() {
          return "QChatMsgEvent";
        }
      };

  // 监听话题中系统通知
  private final EventObserver<List<QChatSystemNotificationInfo>> systemNotificationObserver =
      new EventObserver<List<QChatSystemNotificationInfo>>() {
        @Override
        public void onEvent(@Nullable List<QChatSystemNotificationInfo> event) {
          if (event == null) {
            return;
          }
          for (QChatSystemNotificationInfo item : event) {
            if (item == null) {
              continue;
            }
            if (Objects.equals(item.getServerId(), serverId)) {
              doCheckPermissions();
              break;
            }
          }
        }
      };

  private long serverId;

  // 初始化ViewModel 设置相关监听
  public QChatChannelListViewModel() {
    super();
    QChatServiceObserverRepo.observeReceiveMessage(receiveMessageObserver, true);
    QChatServiceObserverRepo.observeMessageStatusChange(messageStateUpdateObserver, true);
    QChatServiceObserverRepo.observeMessageDelete(messageDeleteObserver, true);
    QChatServiceObserverRepo.observeMessageRevoke(messageRevokeObserver, true);
    QChatServiceObserverRepo.observerSystemNotificationWithType(
        systemNotificationObserver,
        Arrays.asList(
            ServerRoleAuthUpdate.INSTANCE,
            ChannelRoleAuthUpdate.INSTANCE,
            ServerRoleMemberAdd.INSTANCE,
            ServerRoleMemberDelete.INSTANCE,
            MemberRoleAuthUpdate.INSTANCE));
    EventCenter.registerEventNotify(msgEventEventNotify);
  }

  public void configServerId(long serverId) {
    this.serverId = serverId;
  }

  public MutableLiveData<Pair<Long, QChatMessageInfo>> getMessageUpdateResult() {
    return messageUpdateResult;
  }

  public MutableLiveData<ResultInfo<List<QChatChannelInfoWithLastMessage>>> getInitResult() {
    return initResult;
  }

  public MutableLiveData<ResultInfo<List<QChatChannelInfoWithLastMessage>>> getLoadMoreResult() {
    return loadMoreResult;
  }

  public MutableLiveData<Map<QChatRoleResource, Boolean>> getCheckPermissionResult() {
    return checkPermissionResult;
  }

  public void init() {
    channelIdLastMessageMap.clear();
    getServerList(serverId, 0, initResult);
  }

  public void loadMore(long timeTag) {
    getServerList(serverId, timeTag, loadMoreResult);
  }

  public void checkPermissions() {
    doCheckPermissions();
  }

  /** 检查权限 */
  private void doCheckPermissions() {
    if (QChatServerListViewModel.ServerVisitorInfoMgr.getInstance().isVisitor(serverId)) {
      checkPermissionResult.setValue(new HashMap<>());
      return;
    }
    QChatRoleRepo.checkPermissions(
        serverId,
        Collections.singletonList(QChatRoleResource.MANAGE_CHANNEL),
        new FetchCallback<Map<QChatRoleResource, QChatRoleOption>>() {
          @Override
          public void onError(int code, @Nullable String msg) {
            ALog.e(TAG, "checkPermissions:onError:" + code);
          }

          final long innerServerId = serverId;

          @Override
          public void onSuccess(@Nullable Map<QChatRoleResource, QChatRoleOption> param) {
            if (innerServerId != serverId) {
              return;
            }
            if (param == null) {
              return;
            }

            Map<QChatRoleResource, Boolean> resultMap = new HashMap<>();
            QChatRoleOption manageChannelOption = param.get(QChatRoleResource.MANAGE_CHANNEL);
            resultMap.put(
                QChatRoleResource.MANAGE_CHANNEL,
                manageChannelOption == QChatRoleOption.ALLOW
                    || manageChannelOption == QChatRoleOption.INHERIT);
            QChatRoleOption manageServerOption = param.get(QChatRoleResource.MANAGE_SERVER);
            resultMap.put(
                QChatRoleResource.MANAGE_SERVER,
                manageServerOption == QChatRoleOption.ALLOW
                    || manageServerOption == QChatRoleOption.INHERIT);
            checkPermissionResult.setValue(resultMap);
          }
        });
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    QChatServiceObserverRepo.observeReceiveMessage(receiveMessageObserver, false);
    QChatServiceObserverRepo.observeMessageStatusChange(messageStateUpdateObserver, false);
    QChatServiceObserverRepo.observeMessageDelete(messageDeleteObserver, false);
    QChatServiceObserverRepo.observeMessageRevoke(messageRevokeObserver, false);
    QChatServiceObserverRepo.observerSystemNotification(systemNotificationObserver, false);
    EventCenter.unregisterEventNotify(msgEventEventNotify);
  }

  /** 处理消息删除 */
  private void doActionForMsgDelete(QChatMessageInfo messageInfo) {
    if (messageInfo == null) {
      return;
    }
    long messageServerId = messageInfo.getQChatServerId();
    if (messageServerId != serverId) {
      return;
    }
    long messageChannelId = messageInfo.getQChatChannelId();

    QChatMessageInfo lastMessage = channelIdLastMessageMap.get(messageChannelId);
    if (lastMessage != null
        && !Objects.equals(lastMessage.getMsgIdServer(), messageInfo.getMsgIdServer())) {
      return;
    }

    QChatMessageRepo.getLastMessageOfChannels(
        messageServerId,
        Collections.singletonList(messageChannelId),
        new FetchCallback<Map<Long, QChatMessageInfo>>() {
          @Override
          public void onSuccess(@Nullable Map<Long, QChatMessageInfo> param) {
            if (param == null) {
              return;
            }
            QChatMessageInfo newMessageInfo = param.get(messageChannelId);
            channelIdLastMessageMap.put(messageChannelId, newMessageInfo);
            messageUpdateResult.setValue(new Pair<>(messageChannelId, newMessageInfo));
          }

          public void onError(int code, @Nullable String msg) {
            ALog.e(TAG, "getLastMessageOfChannels:onError:" + code);
          }
        });
  }

  /** 处理消息撤回 */
  private void doActionForMsgRevoke(QChatMessageInfo messageInfo) {
    if (messageInfo == null) {
      return;
    }
    long messageServerId = messageInfo.getQChatServerId();
    if (messageServerId != serverId) {
      return;
    }
    long messageChannelId = messageInfo.getQChatChannelId();
    QChatMessageInfo lastMessage = channelIdLastMessageMap.get(messageChannelId);
    if (lastMessage != null
        && !Objects.equals(lastMessage.getMsgIdServer(), messageInfo.getMsgIdServer())) {
      return;
    }
    messageUpdateResult.setValue(new Pair<>(messageInfo.getQChatChannelId(), messageInfo));
  }

  /** 获取社区中话题列表 */
  private void getServerList(
      long serverId,
      long timeTag,
      MutableLiveData<ResultInfo<List<QChatChannelInfoWithLastMessage>>> notifyResult) {
    QChatChannelRepo.fetchChannelsByServerIdWithLastMessage(
        serverId,
        timeTag,
        LOAD_MORE_LIMIT,
        new FetchCallback<List<QChatChannelInfoWithLastMessage>>() {
          @Override
          public void onSuccess(@Nullable List<QChatChannelInfoWithLastMessage> param) {
            if (QChatChannelListViewModel.this.serverId != serverId) {
              return;
            }
            if (param != null && !param.isEmpty()) {
              for (QChatChannelInfoWithLastMessage item : param) {
                if (item == null) {
                  continue;
                }
                QChatMessageInfo lastMessage = item.getLastMessage();
                channelIdLastMessageMap.put(item.getChannelInfo().getChannelId(), lastMessage);
              }
            }
            notifyResult.setValue(new ResultInfo<>(param));
          }

          @Override
          public void onError(int code, @Nullable String msg) {
            if (QChatChannelListViewModel.this.serverId != serverId) {
              return;
            }
            notifyResult.setValue(new ResultInfo<>(null, false, new ErrorMsg(code, msg)));
          }
        });
  }
}
