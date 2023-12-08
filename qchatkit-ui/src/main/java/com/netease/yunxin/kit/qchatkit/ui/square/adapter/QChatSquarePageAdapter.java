// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.square.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.netease.yunxin.kit.qchatkit.ui.square.IQChatSquareServerItemClickListener;
import com.netease.yunxin.kit.qchatkit.ui.square.QChatSquareSubFragment;
import com.netease.yunxin.kit.qchatkit.ui.square.model.QChatServerInfoWithJoinState;
import com.netease.yunxin.kit.qchatkit.ui.square.model.QChatSquarePageInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QChatSquarePageAdapter extends FragmentStateAdapter {
  private final List<QChatSquarePageInfo> pageInfoList = new ArrayList<>();
  private final Map<Integer, QChatSquareSubFragment> searchTypeFragmentMap = new HashMap<>();
  private final IQChatSquareServerItemClickListener itemClickListener;

  public QChatSquarePageAdapter(
      @NonNull Fragment fragment, IQChatSquareServerItemClickListener itemClickListener) {
    super(fragment);
    this.itemClickListener = itemClickListener;
  }

  @NonNull
  @Override
  public Fragment createFragment(int position) {
    QChatSquarePageInfo info = pageInfoList.get(position);

    QChatSquareSubFragment fragment = getFragment(info.serverType);
    fragment.init(info.serverType);
    fragment.setQChatSquareServerItemClickListener(itemClickListener);
    return fragment;
  }

  @Override
  public int getItemCount() {
    return pageInfoList.size();
  }

  public QChatSquarePageInfo getItem(int position) {
    if (position < 0 || position >= pageInfoList.size()) {
      return null;
    }
    return pageInfoList.get(position);
  }

  public void setData(List<QChatSquarePageInfo> dataList) {
    if (dataList == null) {
      return;
    }
    pageInfoList.clear();
    pageInfoList.addAll(dataList);
    notifyItemRangeChanged(0, getItemCount());
  }

  public void updateServerInfo(QChatServerInfoWithJoinState serverInfoWithJoinState) {
    if (serverInfoWithJoinState == null) {
      return;
    }
    int searchType = serverInfoWithJoinState.serverInfo.getSearchType();
    getFragment(searchType).updateServerInfo(serverInfoWithJoinState);
  }

  public void updateServerJoinedState(QChatServerInfoWithJoinState serverInfoWithJoinState) {
    if (serverInfoWithJoinState == null) {
      return;
    }
    int searchType = serverInfoWithJoinState.serverInfo.getSearchType();
    getFragment(searchType).updateServerJoinedState(serverInfoWithJoinState);
  }

  private QChatSquareSubFragment getFragment(int searchType) {
    QChatSquareSubFragment fragment = searchTypeFragmentMap.get(searchType);
    if (fragment != null) {
      return fragment;
    }
    fragment = new QChatSquareSubFragment();
    searchTypeFragmentMap.put(searchType, fragment);
    return fragment;
  }
}
