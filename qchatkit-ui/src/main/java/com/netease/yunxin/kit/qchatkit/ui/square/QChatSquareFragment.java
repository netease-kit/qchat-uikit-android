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
import com.google.android.material.tabs.TabLayoutMediator;
import com.netease.yunxin.kit.common.ui.fragments.BaseFragment;
import com.netease.yunxin.kit.common.utils.NetworkUtils;
import com.netease.yunxin.kit.corekit.model.ResultInfo;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatSquareFragmentBinding;
import com.netease.yunxin.kit.qchatkit.ui.square.adapter.QChatSquarePageAdapter;
import com.netease.yunxin.kit.qchatkit.ui.square.model.QChatServerInfoWithJoinState;
import com.netease.yunxin.kit.qchatkit.ui.square.model.QChatSquarePageInfo;
import com.netease.yunxin.kit.qchatkit.ui.square.viewmodel.QChatSquareViewModel;
import java.util.List;

/** 圈组广场根页面，展示广场包含的社区类型列表及数据 */
public class QChatSquareFragment extends BaseFragment {

  private QChatSquareFragmentBinding viewBinding;
  private IQChatSquareCallback qChatSquareCallback;
  private QChatSquarePageAdapter adapter;

  private final QChatSquareViewModel viewModel = new QChatSquareViewModel();
  /** 监听社区更新通知 */
  private final Observer<QChatServerInfoWithJoinState> observerForServerInfoUpdate =
      serverInfo -> {
        if (adapter != null) {
          adapter.updateServerInfo(serverInfo);
        }
      };
  /** 监听社区加入状态变化 */
  private final Observer<QChatServerInfoWithJoinState> observerForServerJoinState =
      serverInfo -> {
        if (adapter != null) {
          adapter.updateServerJoinedState(serverInfo);
        }
      };

  /** 监听广场数据列表根据具体类型请求结果通知 */
  private final Observer<ResultInfo<List<QChatSquarePageInfo>>> observerForSquareTypeResult =
      listResultInfo -> {
        if (listResultInfo == null) {
          return;
        }
        if (listResultInfo.getSuccess()) {
          if (listResultInfo.getValue() == null || adapter == null) {
            return;
          }
          adapter.setData(listResultInfo.getValue());
          if (viewBinding == null) {
            return;
          }
          if (adapter.getItemCount() > 0) {
            viewBinding.vpSquareType.setOffscreenPageLimit(adapter.getItemCount());
            viewBinding.groupEmpty.setVisibility(View.GONE);
          } else {
            viewBinding.groupEmpty.setVisibility(View.VISIBLE);
            if (!NetworkUtils.isConnected()) {
              Toast.makeText(
                      getContext(), getString(R.string.common_network_error), Toast.LENGTH_SHORT)
                  .show();
            }
          }
        } else {
          Toast.makeText(getContext(), getString(R.string.common_network_error), Toast.LENGTH_SHORT)
              .show();
        }
      };
  /** 网络监听 */
  private final NetworkUtils.NetworkStateListener networkStateListener =
      new NetworkUtils.NetworkStateListener() {
        @Override
        public void onConnected(NetworkUtils.NetworkType networkType) {
          if (adapter != null && adapter.getItemCount() <= 0) {
            initData();
          }
        }

        @Override
        public void onDisconnected() {}
      };

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    viewBinding = QChatSquareFragmentBinding.inflate(inflater, container, false);
    return viewBinding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    viewModel.getServerInfoUpdate().observeForever(observerForServerInfoUpdate);
    viewModel.getServerJoinedStateChanged().observeForever(observerForServerJoinState);
    viewModel.getSquareSearchTypeResult().observeForever(observerForSquareTypeResult);
    NetworkUtils.registerNetworkStatusChangedListener(networkStateListener);

    initUI();
    initData();
  }

  /** 初始化ui */
  private void initUI() {
    adapter =
        new QChatSquarePageAdapter(
            this,
            (serverInfo, joined) -> {
              if (qChatSquareCallback != null) {
                qChatSquareCallback.onEnterQChatServer(serverInfo, joined);
              }
            });

    viewBinding.vpSquareType.setAdapter(adapter);

    TabLayoutMediator mediator =
        new TabLayoutMediator(
            viewBinding.tlSquareType,
            viewBinding.vpSquareType,
            (tab, position) -> tab.setText(adapter.getItem(position).title));
    mediator.attach();
  }

  private void initData() {
    viewModel.initForSearchTypeList();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    viewModel.getServerInfoUpdate().removeObserver(observerForServerInfoUpdate);
    viewModel.getServerJoinedStateChanged().removeObserver(observerForServerJoinState);
    viewModel.getSquareSearchTypeResult().removeObserver(observerForSquareTypeResult);
    NetworkUtils.unregisterNetworkStatusChangedListener(networkStateListener);
  }

  public void setQChatSquareCallback(IQChatSquareCallback callback) {
    this.qChatSquareCallback = callback;
  }
}
