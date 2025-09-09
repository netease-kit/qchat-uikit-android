// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.server.viewmodel;

import android.text.TextUtils;
import android.util.Pair;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import com.netease.nimlib.sdk.qchat.model.QChatServer;
import com.netease.nimlib.sdk.qchat.model.systemnotification.QChatCreateServerAttachment;
import com.netease.nimlib.sdk.qchat.model.systemnotification.QChatUpdateServerAttachment;
import com.netease.yunxin.kit.common.ui.viewmodel.BaseViewModel;
import com.netease.yunxin.kit.common.utils.SPUtils;
import com.netease.yunxin.kit.corekit.im2.IMKitClient;
import com.netease.yunxin.kit.corekit.im2.extend.FetchCallback;
import com.netease.yunxin.kit.corekit.model.ErrorMsg;
import com.netease.yunxin.kit.corekit.model.ResultInfo;
import com.netease.yunxin.kit.qchatkit.EventObserver;
import com.netease.yunxin.kit.qchatkit.observer.ObserverUnreadInfoResultHelper;
import com.netease.yunxin.kit.qchatkit.repo.QChatChannelRepo;
import com.netease.yunxin.kit.qchatkit.repo.QChatServerRepo;
import com.netease.yunxin.kit.qchatkit.repo.QChatServiceObserverRepo;
import com.netease.yunxin.kit.qchatkit.repo.RepoExtends;
import com.netease.yunxin.kit.qchatkit.repo.model.ChannelCreate;
import com.netease.yunxin.kit.qchatkit.repo.model.ChannelRemove;
import com.netease.yunxin.kit.qchatkit.repo.model.ChannelUpdate;
import com.netease.yunxin.kit.qchatkit.repo.model.ChannelUpdateWhiteBlackRole;
import com.netease.yunxin.kit.qchatkit.repo.model.ChannelUpdateWhiteBlackRoleMember;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerChannelIdPair;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerMemberInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSystemNotificationInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSystemNotificationTypeInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatUnreadInfoChangedEventInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatUnreadInfoItem;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerCreate;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberApplyAccept;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberApplyDone;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberInviteAccept;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberInviteDone;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberKick;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberLeave;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerRemove;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerUpdate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/** 处理社区列表的变更及数据获取 */
public final class QChatServerListViewModel extends BaseViewModel {
  public static final int TYPE_SERVER_CREATE = 0;
  public static final int TYPE_SERVER_REMOVE = 1;
  public static final int TYPE_SERVER_UPDATE = 2;
  public static final int TYPE_REFRESH_CHANNEL = 3;
  public static final int TYPE_SERVER_VISITOR_ADD = 4;
  public static final int TYPE_SERVER_CREATE_ANNOUNCE = 5;
  private static final int LOAD_MORE_LIMIT = 50;
  private static final String SP_KEY_LAST_VISITOR_SERVER_ID = "qchat_last_visitor_server_id";

  private final MutableLiveData<ResultInfo<List<QChatServerInfo>>> loadMoreResult =
      new MutableLiveData<>();
  private final MutableLiveData<ResultInfo<List<QChatServerInfo>>> initResult =
      new MutableLiveData<>();
  private final MutableLiveData<ResultInfo<QChatServerInfo>> updateItemResult =
      new MutableLiveData<>();
  private final MutableLiveData<List<Long>> unreadInfoResult = new MutableLiveData<>();

  private final MutableLiveData<Integer> totalServerUnreadInfoResult = new MutableLiveData<>();

  private final MutableLiveData<Pair<Integer, Long>> onRefreshResult = new MutableLiveData<>();

  private final MutableLiveData<QChatServerInfo> announceResult = new MutableLiveData<>();

  private final MutableLiveData<Pair<Long, Boolean>> serverExistenceResult =
      new MutableLiveData<>();

  /** 监听社区未读数 */
  private final EventObserver<QChatUnreadInfoChangedEventInfo> unreadInfoChangedEventObserver =
      new EventObserver<QChatUnreadInfoChangedEventInfo>() {
        @Override
        public void onEvent(@Nullable QChatUnreadInfoChangedEventInfo event) {
          if (event != null) {
            unreadInfoResult.setValue(
                ObserverUnreadInfoResultHelper.appendUnreadInfoList(event.getUnreadInfos()));
            totalServerUnreadInfoResult.setValue(
                ObserverUnreadInfoResultHelper.getTotalUnreadCountForServer());
          }
        }
      };

  private final List<Long> lastCreateAnnounceServerIdList = new ArrayList<>();

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
              // in the branch, the server list may add or reduce some, to refresh.
              ObserverUnreadInfoResultHelper.clear(item.getServerId());
              totalServerUnreadInfoResult.setValue(
                  ObserverUnreadInfoResultHelper.getTotalUnreadCountForServer());
              if (ServerVisitorInfoMgr.getInstance().isVisitor(item.getServerId())) {
                ServerVisitorInfoMgr.getInstance().removeVisitorServerInfo(item.getServerId());
              }
              init();
              break;
            } else {
              if (type == ServerRemove.INSTANCE
                  || (type == ServerMemberLeave.INSTANCE
                      && TextUtils.equals(currentAccount, item.getFromAccount()))
                  || (type == ServerMemberKick.INSTANCE
                      && !TextUtils.equals(currentAccount, item.getFromAccount())
                      && (item.getToAccIds() != null
                          && item.getToAccIds().contains(currentAccount)))) {
                // clear unread count
                ObserverUnreadInfoResultHelper.clear(item.getServerId());
                totalServerUnreadInfoResult.setValue(
                    ObserverUnreadInfoResultHelper.getTotalUnreadCountForServer());
                // in the branch, the server list will remove one.
                onRefreshResult.setValue(new Pair<>(TYPE_SERVER_REMOVE, item.getServerId()));
              } else if (type == ServerUpdate.INSTANCE) {
                // in the branch, the serverInfo was changed, to get the server info from sdk again.
                onRefreshResult.setValue(new Pair<>(TYPE_SERVER_UPDATE, item.getServerId()));
                Object attachment = item.getAttachment();
                if (attachment instanceof QChatUpdateServerAttachment) {
                  QChatServer server = ((QChatUpdateServerAttachment) attachment).getServer();
                  if (server != null) {
                    QChatServerInfo info = RepoExtends.toInfo(server, null);
                    QChatServerInfo.AnnouncementInfo announcementInfo = info.getAnnouncementInfo();
                    if (announcementInfo == null) {
                      updateItemResult.setValue(new ResultInfo<>(info));
                    } else {
                      if (announcementInfo.isValid()) {
                        long serverId = info.getServerId();
                        if (lastCreateAnnounceServerIdList.contains(serverId)) {
                          announceResult.setValue(info);
                          lastCreateAnnounceServerIdList.remove(serverId);
                        } else {
                          updateItemResult.setValue(new ResultInfo<>(info));
                        }
                      }
                    }
                  }
                }
              } else if (type == ServerCreate.INSTANCE) {
                // current the server create one.
                Object attachment = item.getAttachment();
                if (attachment instanceof QChatCreateServerAttachment) {
                  QChatServer qChatServer = ((QChatCreateServerAttachment) attachment).getServer();
                  if (qChatServer != null) {
                    QChatServerInfo info = RepoExtends.toInfo(qChatServer, null);
                    if (info.getAnnouncementInfo() != null) {
                      lastCreateAnnounceServerIdList.add(info.getServerId());
                    } else {
                      onRefreshResult.setValue(new Pair<>(TYPE_SERVER_CREATE, item.getServerId()));
                    }
                  }
                }
              } else {
                if (item.getChannelId() != null && item.getServerId() != null) {
                  ObserverUnreadInfoResultHelper.clear(item.getServerId(), item.getChannelId());
                  QChatChannelRepo.fetchChannelUnreadInfoList(
                      Collections.singletonList(
                          new QChatServerChannelIdPair(item.getServerId(), item.getChannelId())),
                      new FetchCallback<List<QChatUnreadInfoItem>>() {
                        @Override
                        public void onSuccess(@Nullable List<QChatUnreadInfoItem> param) {
                          ObserverUnreadInfoResultHelper.appendUnreadInfoList(param);
                          unreadInfoResult.setValue(Collections.singletonList(item.getServerId()));
                        }

                        @Override
                        public void onError(int code, @Nullable String msg) {
                          unreadInfoResult.setValue(Collections.singletonList(item.getServerId()));
                        }
                      });
                }
                onRefreshResult.setValue(new Pair<>(TYPE_REFRESH_CHANNEL, item.getServerId()));
                totalServerUnreadInfoResult.setValue(
                    ObserverUnreadInfoResultHelper.getTotalUnreadCountForServer());
              }
            }
          }
        }
      };

  public QChatServerListViewModel() {
    // observer system notification to refresh server and channel list.
    QChatServiceObserverRepo.observerSystemNotificationWithType(
        notificationObserver,
        Arrays.asList(
            ServerCreate.INSTANCE,
            ServerRemove.INSTANCE,
            ServerUpdate.INSTANCE,
            ChannelCreate.INSTANCE,
            ChannelRemove.INSTANCE,
            ChannelUpdate.INSTANCE,
            ChannelUpdateWhiteBlackRole.INSTANCE,
            ChannelUpdateWhiteBlackRoleMember.INSTANCE,
            ServerMemberApplyAccept.INSTANCE,
            ServerMemberApplyDone.INSTANCE,
            ServerMemberInviteDone.INSTANCE,
            ServerMemberInviteAccept.INSTANCE,
            ServerMemberKick.INSTANCE,
            ServerMemberLeave.INSTANCE));
    // observer unread notification to update unread info.
    QChatServiceObserverRepo.observeUnreadInfoChanged(unreadInfoChangedEventObserver, true);
  }

  public MutableLiveData<ResultInfo<List<QChatServerInfo>>> getLoadMoreResult() {
    return loadMoreResult;
  }

  public MutableLiveData<ResultInfo<List<QChatServerInfo>>> getInitResult() {
    return initResult;
  }

  public MutableLiveData<Pair<Integer, Long>> getOnRefreshResult() {
    return onRefreshResult;
  }

  public MutableLiveData<ResultInfo<QChatServerInfo>> getUpdateItemResult() {
    return updateItemResult;
  }

  public MutableLiveData<List<Long>> getUnreadInfoResult() {
    return unreadInfoResult;
  }

  public MutableLiveData<Integer> getTotalServerUnreadInfoResult() {
    return totalServerUnreadInfoResult;
  }

  public MutableLiveData<Pair<Long, Boolean>> getServerExistenceResult() {
    return serverExistenceResult;
  }

  public MutableLiveData<QChatServerInfo> getAnnounceResult() {
    return announceResult;
  }

  /** load the first page of server data. */
  public void init() {
    getServerList(0, initResult);
  }

  /** load more server. */
  public void loadMore(long timeTag) {
    getServerList(timeTag, loadMoreResult);
  }

  /** get server list by paging. */
  public void getServerList(
      long timeTag, MutableLiveData<ResultInfo<List<QChatServerInfo>>> result) {
    QChatServerRepo.fetchServerList(
        timeTag,
        LOAD_MORE_LIMIT,
        new FetchCallback<List<QChatServerInfo>>() {
          @Override
          public void onSuccess(@Nullable List<QChatServerInfo> param) {
            // 判断 timeTag 为0则说明为初始化请求，可以将对应的数据追加到目标头部
            QChatServerInfo qChatServerInfoForVisitor =
                ServerVisitorInfoMgr.getInstance().getVisitorServerInfo();
            if (timeTag == 0 && qChatServerInfoForVisitor != null) {
              if (param != null) {
                param.add(0, qChatServerInfoForVisitor);
              } else {
                param = new ArrayList<>();
                param.add(qChatServerInfoForVisitor);
              }
            }
            // 过滤并删除无效的公告频道
            if (param != null) {
              Iterator<QChatServerInfo> iterator = param.iterator();
              while (iterator.hasNext()) {
                QChatServerInfo item = iterator.next();
                if (item.getAnnouncementInfo() != null && !item.getAnnouncementInfo().isValid()) {
                  if (Objects.equals(item.getOwner(), IMKitClient.account())) {
                    QChatServerRepo.deleteServer(item.getServerId(), null);
                  }
                  iterator.remove();
                }
              }
            }
            result.setValue(new ResultInfo<>(param));
          }

          @Override
          public void onError(int code, @Nullable String msg) {
            result.setValue(new ResultInfo<>(null, false, new ErrorMsg(code)));
          }
        });
  }

  /**
   * 检查对应的社区是否存在
   *
   * @param serverId 社区id
   */
  public void checkServerExistence(long serverId) {
    if (serverId == 0) {
      return;
    }
    QChatServerRepo.getServerMembers(
        Collections.singletonList(new Pair<>(serverId, IMKitClient.account())),
        new FetchCallback<List<QChatServerMemberInfo>>() {
          @Override
          public void onSuccess(@Nullable List<QChatServerMemberInfo> param) {
            serverExistenceResult.setValue(new Pair<>(serverId, param != null && !param.isEmpty()));
          }

          @Override
          public void onError(int code, @Nullable String msg) {}
        });
  }

  /** 添加游客模式社区 */
  public void addVisitorServer(QChatServerInfo serverInfo, FetchCallback<Boolean> callback) {
    if (serverInfo == null) {
      return;
    }
    // 加入同一个服务器
    if (ServerVisitorInfoMgr.getInstance().isVisitor(serverInfo.getServerId())) {
      notifyVisitorEnterSuccess(serverInfo, null, callback);
      return;
    }
    // 判断是否有 游客模式服务，如果存在调用 leaveAsVisitor 接口
    // 加入游客模式服务器，加入成功通知变更；刷新页面调用 init 接口
    long lastVisitorServerId = SPUtils.getInstance().getLong(SP_KEY_LAST_VISITOR_SERVER_ID, 0L);
    if (lastVisitorServerId > 0L) {
      QChatServerRepo.leaveAsVisitor(Collections.singletonList(lastVisitorServerId), null);
    }
    QChatServerRepo.enterAsVisitor(
        Collections.singletonList(serverInfo.getServerId()),
        new FetchCallback<List<Long>>() {
          @Override
          public void onSuccess(@Nullable List<Long> param) {
            notifyVisitorEnterSuccess(serverInfo, param, callback);
          }

          @Override
          public void onError(int code, @Nullable String msg) {
            if (callback != null) {
              callback.onError(code, msg);
            }
          }
        });
  }

  private void notifyVisitorEnterSuccess(
      QChatServerInfo serverInfo, List<Long> failedIdList, FetchCallback<Boolean> callback) {
    if (failedIdList != null && failedIdList.contains(serverInfo.getServerId())) {
      if (callback != null) {
        callback.onSuccess(false);
      }
    } else {
      ServerVisitorInfoMgr.getInstance().updateVisitorServerInfo(serverInfo);
      SPUtils.getInstance().put(SP_KEY_LAST_VISITOR_SERVER_ID, serverInfo.getServerId());
      // 游客模式加入服务器后自动订阅服务器通知
      QChatServerRepo.subscribeAsVisitor(
          Collections.singletonList(serverInfo.getServerId()), true, null);
      // 通知外部刷新列表
      onRefreshResult.setValue(new Pair<>(TYPE_SERVER_VISITOR_ADD, serverInfo.getServerId()));
      if (callback != null) {
        callback.onSuccess(true);
      }
    }
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    QChatServiceObserverRepo.observerSystemNotification(notificationObserver, false);
    QChatServiceObserverRepo.observeUnreadInfoChanged(unreadInfoChangedEventObserver, false);
  }

  /** 社区游客管理 */
  public static class ServerVisitorInfoMgr {
    private final List<QChatServerVisitorDataChangeObserver> observerList = new ArrayList<>();
    private QChatServerInfo serverInfo;

    private ServerVisitorInfoMgr() {}

    private static final class Holder {
      private static final ServerVisitorInfoMgr INSTANCE = new ServerVisitorInfoMgr();
    }

    public static ServerVisitorInfoMgr getInstance() {
      return Holder.INSTANCE;
    }

    public boolean isVisitor(long serverId) {
      return serverInfo != null && serverId == serverInfo.getServerId();
    }

    public QChatServerInfo getVisitorServerInfo() {
      return serverInfo;
    }

    public void addObserver(QChatServerVisitorDataChangeObserver observer) {
      observerList.add(observer);
    }

    public void removeObserver(QChatServerVisitorDataChangeObserver observer) {
      observerList.remove(observer);
    }

    private void updateVisitorServerInfo(QChatServerInfo serverInfo) {
      if (serverInfo == null) {
        return;
      }
      this.serverInfo = serverInfo;
      notifyChanged(this.serverInfo);
    }

    private void removeVisitorServerInfo(long serverId) {
      if (serverInfo != null && Objects.equals(serverInfo.getServerId(), serverId)) {
        serverInfo = null;
        notifyChanged(null);
      }
    }

    public void release() {
      observerList.clear();
      serverInfo = null;
    }

    private void notifyChanged(QChatServerInfo serverInfo) {
      for (QChatServerVisitorDataChangeObserver observer : observerList) {
        observer.onChanged(serverInfo);
      }
    }
  }

  public interface QChatServerVisitorDataChangeObserver {
    /**
     * 游客社区更新
     *
     * @param serverInfoForVisitor 当前的进入游客模式的社区，null 则说明当前无游客模式社区
     */
    void onChanged(QChatServerInfo serverInfoForVisitor);
  }
}
