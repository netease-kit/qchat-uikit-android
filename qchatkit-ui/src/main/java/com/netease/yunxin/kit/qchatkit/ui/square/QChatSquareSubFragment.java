// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.square;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.GridLayoutManager;
import com.netease.yunxin.kit.alog.ALog;
import com.netease.yunxin.kit.common.ui.fragments.BaseFragment;
import com.netease.yunxin.kit.common.utils.NetworkUtils;
import com.netease.yunxin.kit.corekit.model.ResultInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.NextInfo;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.common.LoadMoreRecyclerViewDecorator;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatSquareSubFragmentBinding;
import com.netease.yunxin.kit.qchatkit.ui.square.adapter.QChatSquareServerListAdapter;
import com.netease.yunxin.kit.qchatkit.ui.square.model.QChatServerInfoWithJoinState;
import com.netease.yunxin.kit.qchatkit.ui.square.viewmodel.QChatSquareViewModel;
import java.util.Collections;
import java.util.List;

/** 圈组广场具体类型下的社区列表展示页面 */
public class QChatSquareSubFragment extends BaseFragment {
  private static final String TAG = "QChatSquareSubFragment";
  private static final int SPAN_COUNT = 2;
  private final QChatSquareViewModel viewModel = new QChatSquareViewModel();
  private QChatSquareSubFragmentBinding viewBinding;
  private QChatSquareServerListAdapter adapter;
  private IQChatSquareServerItemClickListener clickListener;
  private LoadMoreRecyclerViewDecorator<QChatServerInfoWithJoinState> loadMoreRecyclerViewDecorator;

  /** 监听广场社区列表请求初始化数据结果通知 */
  private final Observer<ResultInfo<List<QChatServerInfoWithJoinState>>> observerForInit =
      new ObserverWrapper(true);

  /** 监听广场社区列表请求更多结果通知 */
  private final Observer<ResultInfo<List<QChatServerInfoWithJoinState>>> observerForLoadMore =
      new ObserverWrapper(false);

  /** 监听社区加入状态变更通知 */
  private final Observer<QChatServerInfoWithJoinState> observerForJoinState =
      serverInfoWithJoinState -> {
        if (adapter != null) {
          adapter.updateServerJoinedState(serverInfoWithJoinState);
        }
      };

  /** 网络监听 */
  private final NetworkUtils.NetworkStateListener networkStateListener =
      new NetworkUtils.NetworkStateListener() {
        @Override
        public void onConnected(NetworkUtils.NetworkType networkType) {
          if (adapter != null && adapter.getItemCount() <= 0) {
            viewModel.initForServerListData();
          }
        }

        @Override
        public void onDisconnected() {}
      };

  public void init(int searchType) {
    viewModel.configSearchType(searchType);
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    viewBinding = QChatSquareSubFragmentBinding.inflate(inflater, container, false);
    return viewBinding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    viewModel.getInitForServerListResult().observeForever(observerForInit);
    viewModel.getLoadMoreForServerListResult().observeForever(observerForLoadMore);
    viewModel.getServerJoinedStateChanged().observeForever(observerForJoinState);
    NetworkUtils.registerNetworkStatusChangedListener(networkStateListener);

    initUI();
    // init data.
    requestData();
    viewBinding.ivEmpty.setOnClickListener(v -> viewModel.initForServerListData());
  }

  /** 更新社区信息 */
  public void updateServerInfo(QChatServerInfoWithJoinState info) {
    if (adapter == null) {
      return;
    }
    adapter.updateServerInfo(info);
  }

  /** 更新社区的加入状态 */
  public void updateServerJoinedState(QChatServerInfoWithJoinState info) {
    if (adapter == null) {
      return;
    }
    adapter.updateServerJoinedState(info);
  }

  /** 设置广场具体某社区点击事件监听 */
  public void setQChatSquareServerItemClickListener(
      IQChatSquareServerItemClickListener clickListener) {
    this.clickListener = clickListener;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    viewModel.getInitForServerListResult().removeObserver(observerForInit);
    viewModel.getLoadMoreForServerListResult().removeObserver(observerForLoadMore);
    viewModel.getServerJoinedStateChanged().removeObserver(observerForJoinState);
    NetworkUtils.unregisterNetworkStatusChangedListener(networkStateListener);
  }

  private void initUI() {
    viewBinding.srlContent.setOnRefreshListener(this::requestData);
    viewBinding.srlContent.setColorSchemeResources(R.color.color_337eff);
    adapter = new QChatSquareServerListAdapter(getContext());
    adapter.setItemClickListener(
        (data, holder) -> {
          if (!NetworkUtils.isConnected()) {
            Toast.makeText(
                    getContext(), getString(R.string.qchat_network_error_tip), Toast.LENGTH_SHORT)
                .show();
            return;
          }
          if (clickListener != null) {
            clickListener.onItemClick(data.serverInfo, data.joined);
          }
        });
    GridLayoutManager layoutManager = new GridLayoutManager(getContext(), SPAN_COUNT);
    viewBinding.rvSquareSubServerList.setLayoutManager(layoutManager);
    viewBinding.rvSquareSubServerList.setAdapter(adapter);

    // to hook loading more data.
    loadMoreRecyclerViewDecorator =
        new LoadMoreRecyclerViewDecorator<>(
            viewBinding.rvSquareSubServerList, layoutManager, adapter);
    loadMoreRecyclerViewDecorator.setLoadMoreListener(
        data -> {
          // In the function, user can load more data by QChatSquareSubViewModel.
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
          viewModel.loadMoreForServerList(timeTag);
        });
  }

  private void requestData() {
    if (viewBinding != null) {
      if (adapter != null && adapter.getItemCount() <= 0) {
        viewBinding.ivTypeContentEmpty.setVisibility(View.VISIBLE);
      }
      viewBinding.groupEmpty.setVisibility(View.GONE);
    }
    viewModel.initForServerListData();
  }

  private void actionForSuccess(List<QChatServerInfoWithJoinState> data, boolean clear) {
    ALog.d(TAG, "actionForSuccess, clear flag is " + clear + ", data is " + data);
    if (data != null && !data.isEmpty()) {
      adapter.addDataList(data, clear);
    } else if (adapter.getItemCount() == 0 || clear) {
      adapter.addDataList(Collections.emptyList(), true);
    }
  }

  /** Handling the change of initialization or loading more data. */
  private class ObserverWrapper
      implements Observer<ResultInfo<List<QChatServerInfoWithJoinState>>> {
    public final boolean clear;

    public ObserverWrapper(boolean clear) {
      this.clear = clear;
    }

    @Override
    public void onChanged(ResultInfo<List<QChatServerInfoWithJoinState>> listResultInfo) {
      if (listResultInfo.getSuccess()) {
        viewBinding.tvEmpty.setText(R.string.qchat_square_server_empty_tip);
        List<QChatServerInfoWithJoinState> data = listResultInfo.getValue();
        actionForSuccess(data, clear);
      } else {
        viewBinding.tvEmpty.setText(R.string.qchat_network_error_tip);
        Toast.makeText(getContext(), getString(R.string.qchat_network_error_tip), Toast.LENGTH_SHORT)
            .show();
      }
      if (viewBinding != null) {
        viewBinding.srlContent.setRefreshing(false);
        viewBinding.ivTypeContentEmpty.setVisibility(View.GONE);
        if (adapter.getItemCount() > 0) {
          viewBinding.groupEmpty.setVisibility(View.GONE);
        } else {
          viewBinding.groupEmpty.setVisibility(View.VISIBLE);
        }
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
