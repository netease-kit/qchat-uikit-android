// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.channel;

import static android.app.Activity.RESULT_OK;
import static com.netease.yunxin.kit.qchatkit.ui.server.viewmodel.QChatServerListViewModel.ServerVisitorInfoMgr;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.netease.nimlib.sdk.qchat.enums.QChatRoleResource;
import com.netease.yunxin.kit.alog.ALog;
import com.netease.yunxin.kit.common.ui.fragments.BaseFragment;
import com.netease.yunxin.kit.common.utils.NetworkUtils;
import com.netease.yunxin.kit.corekit.im.provider.FetchCallbackImpl;
import com.netease.yunxin.kit.corekit.model.ResultInfo;
import com.netease.yunxin.kit.qchatkit.observer.ObserverUnreadInfoResultHelper;
import com.netease.yunxin.kit.qchatkit.repo.QChatChannelRepo;
import com.netease.yunxin.kit.qchatkit.repo.model.NextInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelInfoWithLastMessage;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatUnreadInfoItem;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.channel.adapter.QChatFragmentChannelAdapter;
import com.netease.yunxin.kit.qchatkit.ui.channel.viewmodel.QChatChannelListViewModel;
import com.netease.yunxin.kit.qchatkit.ui.common.LoadMoreRecyclerViewDecorator;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatFragmentChannelListBinding;
import com.netease.yunxin.kit.qchatkit.ui.message.ChannelMessageActivity;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant;
import com.netease.yunxin.kit.qchatkit.ui.server.QChatServerSettingActivity;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 话题列表. */
public class QChatChannelListFragment extends BaseFragment {
  private static final String TAG = "QChatChannelListFragment";
  private static final long DELAY_TIME_TO_UPDATE_LAST_MESSAGE = 2000;

  private final QChatChannelListViewModel viewModel = new QChatChannelListViewModel();
  private final Map<QChatRoleResource, Boolean> resourceBooleanMap = new HashMap<>();
  private QChatServerInfo serverInfo;

  private QChatFragmentChannelListBinding binding;
  private QChatFragmentChannelAdapter adapter;
  private ActivityResultLauncher<Intent> launcher;
  private LoadMoreRecyclerViewDecorator<QChatChannelInfoWithLastMessage>
      loadMoreRecyclerViewDecorator;

  // 网络状态监听，断网展示红色标题栏
  private final NetworkUtils.NetworkStateListener networkStateListener =
      new NetworkUtils.NetworkStateListener() {

        @Override
        public void onConnected(NetworkUtils.NetworkType networkType) {
          if (binding == null) {
            return;
          }
          binding.networkTip.getRoot().setVisibility(View.GONE);
          binding
              .getRoot()
              .postDelayed(
                  () -> {
                    if (serverInfo != null) {
                      updateData(
                          serverInfo,
                          ObserverUnreadInfoResultHelper.getUnreadInfoMap(
                              serverInfo.getServerId()));
                    }
                  },
                  DELAY_TIME_TO_UPDATE_LAST_MESSAGE);
        }

        @Override
        public void onDisconnected() {
          if (binding == null) {
            return;
          }
          binding.networkTip.getRoot().setVisibility(View.VISIBLE);
        }
      };

  // 监听消息更新
  private final Observer<Pair<Long, QChatMessageInfo>> observerForMessageUpdate =
      pairInfo -> {
        if (pairInfo == null) {
          return;
        }
        adapter.updateLastMessageInfo(pairInfo.first, pairInfo.second);
      };

  // 监听查询结果
  private final Observer<ResultInfo<List<QChatChannelInfoWithLastMessage>>> observerForInit =
      new ObserverWrapper(true);

  // 监听频道列表加载更多
  private final Observer<ResultInfo<List<QChatChannelInfoWithLastMessage>>> observerForLoadMore =
      new ObserverWrapper(false);

  // 监听权限查询结果
  private final Observer<Map<QChatRoleResource, Boolean>> observerForCheckPermission =
      resultMap -> {
        resourceBooleanMap.clear();
        if (resultMap != null) {
          resourceBooleanMap.putAll(resultMap);
        }
        toUpdateViewVisibleWithPermission();
      };

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    binding = QChatFragmentChannelListBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    viewModel.getInitResult().observeForever(observerForInit);
    viewModel.getLoadMoreResult().observeForever(observerForLoadMore);
    viewModel.getMessageUpdateResult().observeForever(observerForMessageUpdate);
    viewModel.getCheckPermissionResult().observeForever(observerForCheckPermission);
    NetworkUtils.registerNetworkStatusChangedListener(networkStateListener);

    adapter = new QChatFragmentChannelAdapter(getContext());
    adapter.setItemClickListener(
        (data, holder) ->
            ChannelMessageActivity.launch(
                this.getActivity(),
                null,
                data.getChannelInfo().getServerId(),
                data.getChannelInfo().getChannelId(),
                serverInfo != null ? serverInfo.getOwner() : null,
                data.getChannelInfo().getName()));
    binding.ryChannelList.setAdapter(adapter);
    LinearLayoutManager layoutManager =
        new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
    binding.ryChannelList.setLayoutManager(layoutManager);
    binding.ivMore.setOnClickListener(
        v -> QChatServerSettingActivity.launch(this.getActivity(), serverInfo));
    // 创建频道launcher
    launcher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
              Intent data = result.getData();
              if (result.getResultCode() != RESULT_OK || data == null) {
                return;
              }
              long channelId = data.getLongExtra(QChatConstant.CHANNEL_ID, -1);
              String channelName = data.getStringExtra(QChatConstant.CHANNEL_NAME);
              if (channelId > 0) {
                ChannelMessageActivity.launch(
                    this.getActivity(),
                    null,
                    serverInfo.getServerId(),
                    channelId,
                    serverInfo.getOwner(),
                    channelName);
              }
            });
    binding.ivAddChannel.setOnClickListener(
        v -> {
          Intent intent = new Intent(this.getActivity(), QChatChannelCreateActivity.class);
          intent.putExtra(QChatConstant.SERVER_ID, serverInfo.getServerId());
          launcher.launch(intent);
        });

    loadMoreRecyclerViewDecorator =
        new LoadMoreRecyclerViewDecorator<>(binding.ryChannelList, layoutManager, adapter);
    loadMoreRecyclerViewDecorator.setLoadMoreListener(
        data -> {
          if (serverInfo == null) {
            return;
          }
          NextInfo info;
          if (data == null || data.getNextInfo() == null) {
            info = null;
          } else {
            info = data.getNextInfo();
          }
          if (info != null && !info.getHasMore()) {
            return;
          }
          long timeTag = info != null ? info.getNextTimeTag() : 0;
          if (timeTag == 0) {
            return;
          }
          viewModel.loadMore(timeTag);
        });
  }

  public void updateData(QChatServerInfo serverInfo, Map<Long, QChatUnreadInfoItem> unReadInfo) {
    if (serverInfo == null) {
      ALog.e(TAG, "updateData:serverInfo is null");
      return;
    }
    adapter.updateUnreadCount(unReadInfo);
    this.serverInfo = serverInfo;
    binding.tvTitle.setText(serverInfo.getName());
    viewModel.configServerId(serverInfo.getServerId());
    viewModel.init();
    viewModel.checkPermissions();
    if (ServerVisitorInfoMgr.getInstance().isVisitor(serverInfo.getServerId())) {
      subscribeMaxChannelsForVisitor();
      binding.ivMore.setVisibility(View.GONE);
      binding.ivAddChannel.setVisibility(View.GONE);
    } else {
      toUpdateViewVisibleWithPermission();
    }
  }

  /** 根据权限更新UI */
  private void toUpdateViewVisibleWithPermission() {
    if (checkPermission(QChatRoleResource.MANAGE_CHANNEL)) {
      binding.ivAddChannel.setVisibility(View.VISIBLE);
    } else {
      binding.ivAddChannel.setVisibility(View.GONE);
    }
    binding.ivMore.setVisibility(View.VISIBLE);
  }

  /**
   * 检查权限
   *
   * @param resource
   * @return
   */
  private boolean checkPermission(QChatRoleResource resource) {
    return Boolean.TRUE.equals(resourceBooleanMap.get(resource));
  }

  /** 订阅最大频道数 */
  private void subscribeMaxChannelsForVisitor() {
    long serverId = serverInfo.getServerId();
    QChatChannelRepo.fetchMaxChannelIdsByServerIdForVisitor(
        serverId,
        new FetchCallbackImpl<List<Long>>() {
          @Override
          public void onSuccess(@Nullable List<Long> param) {
            if (param != null) {
              QChatChannelRepo.subscribeAsVisitor(serverId, param, true, null);
            }
          }
        });
  }

  /** 数据刷新 */
  public void refreshData(long serverId, Map<Long, QChatUnreadInfoItem> unReadInfo) {
    adapter.updateUnreadCount(unReadInfo);
    if (serverInfo != null && serverInfo.getServerId() == serverId) {
      viewModel.init();
    }
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    viewModel.getInitResult().removeObserver(observerForInit);
    viewModel.getLoadMoreResult().removeObserver(observerForLoadMore);
    viewModel.getMessageUpdateResult().removeObserver(observerForMessageUpdate);
    viewModel.getCheckPermissionResult().removeObserver(observerForCheckPermission);
    NetworkUtils.unregisterNetworkStatusChangedListener(networkStateListener);
  }

  /** update unread info */
  @SuppressLint("NotifyDataSetChanged")
  public void refreshUnreadData() {
    if (serverInfo != null) {
      adapter.updateUnreadCount(
          ObserverUnreadInfoResultHelper.getUnreadInfoMap(serverInfo.getServerId()));
      adapter.notifyDataSetChanged();
    }
  }

  public Long getCurrentServerId() {
    return serverInfo != null ? serverInfo.getServerId() : null;
  }

  private class ObserverWrapper
      implements Observer<ResultInfo<List<QChatChannelInfoWithLastMessage>>> {
    public final boolean clear;

    public ObserverWrapper(boolean clear) {
      this.clear = clear;
    }

    @Override
    public void onChanged(ResultInfo<List<QChatChannelInfoWithLastMessage>> resultInfo) {
      if (resultInfo.getSuccess()) {
        List<QChatChannelInfoWithLastMessage> data = resultInfo.getValue();
        if (data != null && !data.isEmpty()) {
          binding.ryChannelList.setVisibility(View.VISIBLE);
          binding.groupNoChannelTip.setVisibility(View.GONE);
          adapter.addDataList(data, clear);
        } else if (adapter.getItemCount() == 0 || clear) {
          binding.groupNoChannelTip.setVisibility(View.VISIBLE);
          adapter.addDataList(Collections.emptyList(), true);
        }

      } else if (!NetworkUtils.isConnected()) {
        Toast.makeText(getContext(), getString(R.string.common_network_error), Toast.LENGTH_SHORT)
            .show();
      } else {
        Toast.makeText(
                getContext(), getString(R.string.qchat_server_request_fail), Toast.LENGTH_SHORT)
            .show();
      }
      if (loadMoreRecyclerViewDecorator != null) {
        if (clear) {
          loadMoreRecyclerViewDecorator.init();
        }
        loadMoreRecyclerViewDecorator.notifyResult(resultInfo.getSuccess());
      }
    }
  }
}
