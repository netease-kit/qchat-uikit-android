// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.server;

import static com.netease.yunxin.kit.qchatkit.ui.server.viewmodel.QChatServerListViewModel.ServerVisitorInfoMgr;
import static com.netease.yunxin.kit.qchatkit.ui.server.viewmodel.QChatServerListViewModel.TYPE_REFRESH_CHANNEL;
import static com.netease.yunxin.kit.qchatkit.ui.server.viewmodel.QChatServerListViewModel.TYPE_SERVER_CREATE;
import static com.netease.yunxin.kit.qchatkit.ui.server.viewmodel.QChatServerListViewModel.TYPE_SERVER_REMOVE;
import static com.netease.yunxin.kit.qchatkit.ui.server.viewmodel.QChatServerListViewModel.TYPE_SERVER_VISITOR_ADD;

import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.netease.yunxin.kit.alog.ALog;
import com.netease.yunxin.kit.common.ui.fragments.BaseFragment;
import com.netease.yunxin.kit.common.utils.NetworkUtils;
import com.netease.yunxin.kit.corekit.event.EventCenter;
import com.netease.yunxin.kit.corekit.event.EventNotify;
import com.netease.yunxin.kit.corekit.im2.IMKitClient;
import com.netease.yunxin.kit.corekit.im2.extend.FetchCallback;
import com.netease.yunxin.kit.corekit.model.ErrorMsg;
import com.netease.yunxin.kit.corekit.model.ResultInfo;
import com.netease.yunxin.kit.qchatkit.observer.ObserverUnreadInfoResultHelper;
import com.netease.yunxin.kit.qchatkit.repo.QChatServerRepo;
import com.netease.yunxin.kit.qchatkit.repo.model.NextInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerMemberInfo;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.channel.QChatChannelListFragment;
import com.netease.yunxin.kit.qchatkit.ui.common.LoadMoreRecyclerViewDecorator;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatFragmentBinding;
import com.netease.yunxin.kit.qchatkit.ui.message.ChannelMessageActivity;
import com.netease.yunxin.kit.qchatkit.ui.server.adapter.QChatFragmentServerAdapter;
import com.netease.yunxin.kit.qchatkit.ui.server.model.QChatEnterServerEvent;
import com.netease.yunxin.kit.qchatkit.ui.server.model.QChatFragmentServerInfo;
import com.netease.yunxin.kit.qchatkit.ui.server.viewmodel.QChatServerListViewModel;
import com.netease.yunxin.kit.qchatkit.ui.utils.QChatUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** 圈组页面，包含圈组社区列表以及当前社区的话题列表 */
public class QChatServerFragment extends BaseFragment {
  private static final String TAG = "QChatServerFragment";
  /** 用于展示当前社区的圈组话题列表 */
  private final QChatChannelListFragment fragment = new QChatChannelListFragment();

  private QChatFragmentBinding viewBinding;
  private LinearLayoutManager layoutManager;
  private QChatFragmentServerAdapter adapter;
  /** The server data source. */
  private final QChatServerListViewModel viewModel = new QChatServerListViewModel();
  /** 监听圈组不同通知用于更新社区列表 */
  private final Observer<Pair<Integer, Long>> notificationObserver =
      new Observer<Pair<Integer, Long>>() {
        @Override
        public void onChanged(Pair<Integer, Long> integerLongPair) {
          switch (integerLongPair.first) {
            case TYPE_REFRESH_CHANNEL: // 刷新话题数据
              fragment.refreshData(
                  integerLongPair.second,
                  ObserverUnreadInfoResultHelper.getUnreadInfoMap(integerLongPair.second));
              break;
            case TYPE_SERVER_REMOVE: // 社区移除通知
              adapter.removeServerById(integerLongPair.second);
              if (adapter.getNormalQChatServerCount() == 0) {
                viewBinding.groupNoServerTip.setVisibility(View.VISIBLE);
                viewBinding.ryChannelFragment.setVisibility(View.GONE);
              }
              break;
            case TYPE_SERVER_CREATE: // 社区创建通知
            case TYPE_SERVER_VISITOR_ADD: // 游客加入社区通知
              if (viewBinding != null) {
                viewModel.init();
                adapter.updateFocus(integerLongPair.second);
              }
          }
        }
      };

  /** 监听圈组社区信息变更通知 */
  private final Observer<ResultInfo<QChatServerInfo>> observerForUpdate =
      new Observer<ResultInfo<QChatServerInfo>>() {
        @Override
        public void onChanged(ResultInfo<QChatServerInfo> qChatServerInfoResultInfo) {
          if (qChatServerInfoResultInfo.getSuccess()
              && qChatServerInfoResultInfo.getValue() != null) {
            adapter.updateData(qChatServerInfoResultInfo.getValue());
          }
        }
      };
  /** 监听圈组社区获取首页数据结果的通知 */
  private final Observer<ResultInfo<List<QChatServerInfo>>> observerForInit =
      new ObserverWrapper(true);
  /** 监听圈组社区拉取更多时数据结果的通知，注意区分 {@link #observerForInit}. */
  private final Observer<ResultInfo<List<QChatServerInfo>>> observerForLoadMore =
      new ObserverWrapper(false);
  /** 监听圈组未读数变更的社区通知 */
  private final Observer<List<Long>> observerUnreadInfo =
      new Observer<List<Long>>() {
        @Override
        public void onChanged(List<Long> serverIdList) {
          adapter.updateUnreadInfoList(serverIdList);
          if (serverIdList != null && serverIdList.contains(fragment.getCurrentServerId())) {
            fragment.refreshUnreadData();
          }
        }
      };
  /** 监听圈组所有社区未读总数变化通知 */
  private final Observer<Integer> observerTotalServerUnreadInfo =
      new Observer<Integer>() {
        @Override
        public void onChanged(Integer count) {
          if (qChatServerCallback != null && count != null) {
            qChatServerCallback.updateUnreadCount(count);
          }
        }
      };
  /** 监听圈组游客模式变化通知，即对应社区是否为游客模式 */
  private final QChatServerListViewModel.QChatServerVisitorDataChangeObserver
      serverVisitorDataChangeObserver =
          serverInfoForVisitor -> {
            // 为空则说明当前无游客模式社区，游客模式提示隐藏
            if (serverInfoForVisitor == null) {
              viewBinding.viewQChatVisitor.setVisibility(View.GONE);
            }
          };

  /** 监听圈组公告频道创建完成通知 */
  private final Observer<QChatServerInfo> observerForAnnounce =
      new Observer<QChatServerInfo>() {
        @Override
        public void onChanged(QChatServerInfo qChatServerInfo) {
          QChatFragmentServerInfo fragmentServerInfo = new QChatFragmentServerInfo(qChatServerInfo);
          fragmentServerInfo.unreadInfoItemMap =
              ObserverUnreadInfoResultHelper.getUnreadInfoMap(qChatServerInfo.getServerId());
          adapter.addData(0, fragmentServerInfo);
        }
      };

  /** 监听检查对应圈组社区是否存在通知 */
  private final Observer<Pair<Long, Boolean>> observerForCheckServerExistence =
      longBooleanPair -> {
        if (longBooleanPair == null || adapter == null) {
          return;
        }
        // 若社区不存在则更新focus
        if (Objects.equals(longBooleanPair.first, adapter.getFocusedServerId())
            && !longBooleanPair.second) {
          adapter.updateFocus(0L);
        }
      };
  /** 监听网络状态变化 */
  private final NetworkUtils.NetworkStateListener networkStateListener =
      new NetworkUtils.NetworkStateListener() {
        @Override
        public void onConnected(NetworkUtils.NetworkType networkType) {
          // 由于圈组服务断网通知不会补发，避免断网期间删除社区则需要在网络重连时刷新社区列表并检查当前焦点社区是否存在
          if (adapter != null) {
            viewModel.init();
            viewModel.checkServerExistence(adapter.getFocusedServerId());
          }
        }

        @Override
        public void onDisconnected() {}
      };

  /** 监听页面外部通过发送事件方式进入社区游客模式展示 */
  private final EventNotify<QChatEnterServerEvent> notifyForEvent =
      new EventNotify<QChatEnterServerEvent>() {
        @Override
        public void onNotify(@NonNull QChatEnterServerEvent event) {
          // 进入社区列表
          enterQChatServer(event.serverInfo, null);
        }

        @NonNull
        @Override
        public String getEventType() {
          return QChatEnterServerEvent.TYPE;
        }
      };

  /** 拉取更多数据包装类 */
  private LoadMoreRecyclerViewDecorator<QChatFragmentServerInfo> loadMoreRecyclerViewDecorator;
  /** 圈组外部通知回调，主要用户不同模块通信 */
  private IQChatServerCallback qChatServerCallback;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    viewBinding = QChatFragmentBinding.inflate(inflater, container, false);
    return viewBinding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    FragmentManager manager = getChildFragmentManager();

    // add channel fragment.
    manager.beginTransaction().add(R.id.ryChannelFragment, fragment).commitAllowingStateLoss();
    // creating or join a server.
    viewBinding.ivAddServer.setOnClickListener(
        v ->
            startActivity(
                new Intent(getContext(), QChatCreateWayActivity.class),
                ActivityOptionsCompat.makeCustomAnimation(
                        v.getContext(), R.anim.anim_from_bottom_to_top, R.anim.anim_empty_with_time)
                    .toBundle()));
    // render server list.
    adapter = new QChatFragmentServerAdapter(getContext());
    adapter.setItemClickListener(
        (data, holder) -> {
          if (data.serverInfo.getAnnouncementInfo() != null
              && data.serverInfo.getAnnouncementInfo().isValid()) {
            //noinspection DataFlowIssue
            ChannelMessageActivity.launch(
                getActivity(),
                data.serverInfo,
                data.serverInfo.getServerId(),
                data.serverInfo.getAnnouncementInfo().getChannelId(),
                data.serverInfo.getOwner(),
                data.serverInfo.getName());
            return;
          }
          if (ServerVisitorInfoMgr.getInstance().isVisitor(data.serverInfo.getServerId())) {
            viewBinding.viewQChatVisitor.configToJoinServerInfo(
                data.serverInfo.getServerId(), null);
            viewBinding.viewQChatVisitor.setVisibility(View.VISIBLE);
          } else {
            viewBinding.viewQChatVisitor.setVisibility(View.GONE);
          }
          fragment.updateData(data.serverInfo, data.unreadInfoItemMap);
        });
    adapter.setOnServerRemovedToNewFocusListener(
        data -> {
          if (data != null) {
            fragment.updateData(data.serverInfo, data.unreadInfoItemMap);
          }
        });
    layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
    viewBinding.ryServerList.setLayoutManager(layoutManager);
    viewBinding.ryServerList.setAdapter(adapter);
    // to hook loading more data.
    loadMoreRecyclerViewDecorator =
        new LoadMoreRecyclerViewDecorator<>(viewBinding.ryServerList, layoutManager, adapter);
    loadMoreRecyclerViewDecorator.setLoadMoreListener(
        data -> {
          // In the function, user can load more data by QChatServerListViewModel.
          ALog.d(TAG, "load more data");
          NextInfo info;
          if (data == null || data.serverInfo.getNextInfo() == null) {
            info = null;
          } else {
            info = data.serverInfo.getNextInfo();
          }
          if (info != null && !info.getHasMore()) {
            ALog.d(TAG, "load more data, info is null or no more.");
            return;
          }
          long timeTag = info != null ? info.getNextTimeTag() : 0;
          if (timeTag == 0) {
            ALog.d(TAG, "load more data, time tat is init state.");
            return;
          }
          ALog.d(TAG, "load more data, time tag is " + timeTag);
          viewModel.loadMore(timeTag);
        });

    // add observers to QChatServerListViewModel.
    viewModel.getOnRefreshResult().observeForever(notificationObserver);
    viewModel.getUpdateItemResult().observeForever(observerForUpdate);
    viewModel.getInitResult().observeForever(observerForInit);
    viewModel.getLoadMoreResult().observeForever(observerForLoadMore);
    viewModel.getUnreadInfoResult().observeForever(observerUnreadInfo);
    viewModel.getTotalServerUnreadInfoResult().observeForever(observerTotalServerUnreadInfo);
    viewModel.getAnnounceResult().observeForever(observerForAnnounce);
    viewModel.getServerExistenceResult().observeForever(observerForCheckServerExistence);
    EventCenter.registerEventNotify(notifyForEvent);
    NetworkUtils.registerNetworkStatusChangedListener(networkStateListener);
    ServerVisitorInfoMgr.getInstance().addObserver(serverVisitorDataChangeObserver);

    // to load data firstly, showing network error when network was broken.
    QChatUtils.isConnectedToastAndRun(getContext(), this::refreshServerList);
  }

  private void actionForSuccess(List<QChatServerInfo> data, boolean clear) {
    ALog.d(TAG, "actionForSuccess, clear flag is " + clear + ", data is " + data);
    if (data != null && !data.isEmpty()) {
      List<QChatFragmentServerInfo> serverInfoList = new ArrayList<>();
      for (QChatServerInfo serverInfo : data) {
        serverInfoList.add(new QChatFragmentServerInfo(serverInfo));
      }
      adapter.addDataList(serverInfoList, clear);
      if (adapter.getNormalQChatServerCount() > 0) {
        viewBinding.groupNoServerTip.setVisibility(View.GONE);
        viewBinding.ryChannelFragment.setVisibility(View.VISIBLE);
      }
      if (clear) {
        int totalCount = ObserverUnreadInfoResultHelper.getTotalUnreadCountForServer();
        if (qChatServerCallback != null) {
          qChatServerCallback.updateUnreadCount(totalCount);
        }
      }
    } else if (adapter.getNormalQChatServerCount() == 0 || clear) {
      viewBinding.groupNoServerTip.setVisibility(View.VISIBLE);
      viewBinding.ryChannelFragment.setVisibility(View.GONE);
      adapter.addDataList(Collections.emptyList(), true);
    }
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    // to remove observers, avoiding memory leak.
    viewModel.getUnreadInfoResult().removeObserver(observerUnreadInfo);
    viewModel.getOnRefreshResult().removeObserver(notificationObserver);
    viewModel.getInitResult().removeObserver(observerForInit);
    viewModel.getLoadMoreResult().removeObserver(observerForLoadMore);
    viewModel.getUpdateItemResult().removeObserver(observerForUpdate);
    viewModel.getTotalServerUnreadInfoResult().removeObserver(observerTotalServerUnreadInfo);
    viewModel.getAnnounceResult().removeObserver(observerForAnnounce);
    viewModel.getServerExistenceResult().removeObserver(observerForCheckServerExistence);
    EventCenter.unregisterEventNotify(notifyForEvent);
    NetworkUtils.unregisterNetworkStatusChangedListener(networkStateListener);
    ServerVisitorInfoMgr.getInstance().removeObserver(serverVisitorDataChangeObserver);
    ServerVisitorInfoMgr.getInstance().release();
  }

  public void refreshServerList() {
    ALog.d(TAG, "refreshServerList");
    viewModel.init();
  }

  /** 设置圈组页面外部回调 */
  public void setQChatServerCallback(IQChatServerCallback callback) {
    this.qChatServerCallback = callback;
  }

  /**
   * 进入社区列表，若用户未加入目标社区，则已游客模式进入列表否则正常进入社区列表
   *
   * @param serverInfo 进入的社区目标
   * @param callback 进入社区结果通知
   */
  public void enterQChatServer(QChatServerInfo serverInfo, FetchCallback<Boolean> callback) {
    if (serverInfo == null) {
      return;
    }
    // 进入社区前判断是否已经加入当前社区
    QChatServerRepo.getServerMembers(
        Collections.singletonList(new Pair<>(serverInfo.getServerId(), IMKitClient.account())),
        new FetchCallback<List<QChatServerMemberInfo>>() {
          @Override
          public void onSuccess(@Nullable List<QChatServerMemberInfo> param) {
            if (param != null && !param.isEmpty()) { // 用户已经正式加入社区
              viewBinding.viewQChatVisitor.setVisibility(View.GONE);
              if (serverInfo.getAnnouncementInfo() != null
                  && serverInfo.getAnnouncementInfo().isValid()) { // 判断是否为公告频道
                //noinspection DataFlowIssue
                ChannelMessageActivity.launch(
                    getActivity(),
                    serverInfo,
                    serverInfo.getServerId(),
                    serverInfo.getAnnouncementInfo().getChannelId(),
                    serverInfo.getOwner(),
                    serverInfo.getName());

              } else { // 普通社区则更新焦点及话题列表
                adapter.updateFocus(serverInfo.getServerId());
                fragment.updateData(
                    serverInfo,
                    ObserverUnreadInfoResultHelper.getUnreadInfoMap(serverInfo.getServerId()));
              }

              if (callback != null) {
                callback.onSuccess(true);
              }
            } else { // 未正式加入社区则以游客模式加入
              viewBinding.viewQChatVisitor.configToJoinServerInfo(serverInfo.getServerId(), null);
              viewBinding.viewQChatVisitor.setVisibility(View.VISIBLE);
              viewModel.addVisitorServer(serverInfo, callback);
            }
          }

          @Override
          public void onError(int code, @Nullable String msg) {
            if (callback != null) {
              callback.onError(code, msg);
            }
          }
        });
  }

  /** 用户同于处理初始化数据和拉取更多数据的包装内部类 */
  private class ObserverWrapper implements Observer<ResultInfo<List<QChatServerInfo>>> {
    public final boolean clear;

    public ObserverWrapper(boolean clear) {
      this.clear = clear;
    }

    @Override
    public void onChanged(ResultInfo<List<QChatServerInfo>> listResultInfo) {
      if (listResultInfo.getSuccess()) {
        List<QChatServerInfo> data = listResultInfo.getValue();
        actionForSuccess(data, clear);
        if (clear && layoutManager != null) {
          layoutManager.scrollToPosition(0);
        }
      } else if (NetworkUtils.isConnected()) {
        ErrorMsg msg = listResultInfo.getMsg();
        Toast.makeText(
                getContext(),
                getString(R.string.qchat_server_request_fail) + (msg != null ? msg.getCode() : ""),
                Toast.LENGTH_SHORT)
            .show();
      } else {
        Toast.makeText(getContext(), getString(R.string.qchat_network_error_tip), Toast.LENGTH_SHORT)
            .show();
      }
      if (loadMoreRecyclerViewDecorator != null) {
        if (clear) {
          loadMoreRecyclerViewDecorator.init();
        }
        loadMoreRecyclerViewDecorator.notifyResult(listResultInfo.getSuccess());
      }
    }
  }
}
