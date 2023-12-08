// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.square;

import com.netease.yunxin.kit.corekit.im.provider.FetchCallback;
import com.netease.yunxin.kit.corekit.model.ResultInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerInfo;
import com.netease.yunxin.kit.qchatkit.ui.square.model.QChatSquarePageInfo;
import java.util.List;

/** 圈组广场数据源请求管理，为单例需要配置{@link RequesterForSquareInfo}使用 */
public class SquareDataSourceHelper {
  private SquareDataSourceHelper() {}

  private RequesterForSquareInfo requester;

  private static final class Holder {
    private static final SquareDataSourceHelper INSTANCE = new SquareDataSourceHelper();
  }

  public static SquareDataSourceHelper getInstance() {
    return Holder.INSTANCE;
  }

  /** 配置广场数据源请求 */
  public void configRequester(RequesterForSquareInfo requester) {
    this.requester = requester;
  }

  /**
   * 根据具体的广场类型请求广场社区列表
   *
   * @param searchType 广场类型
   * @param callback 请求结果通知
   */
  public void requestServerInfoForSearchType(
      int searchType, FetchCallback<ResultInfo<List<QChatServerInfo>>> callback) {
    if (requester != null) {
      requester.requestServerInfoForSearchType(searchType, callback);
    }
  }

  /**
   * 请求广场类型列表
   *
   * @param callback 请求结果通知
   */
  public void requestSquareSearchType(
      FetchCallback<ResultInfo<List<QChatSquarePageInfo>>> callback) {
    if (requester != null) {
      requester.requestSquareSearchType(callback);
    }
  }

  /** 圈组广场请求 */
  public interface RequesterForSquareInfo {
    /**
     * 请求圈组广场类型列表
     *
     * @param callback 结果回调
     */
    void requestSquareSearchType(FetchCallback<ResultInfo<List<QChatSquarePageInfo>>> callback);

    /**
     * 通过广场类型请求广场社区列表
     *
     * @param searchType 请求广场类型
     * @param callback 结果回调
     */
    void requestServerInfoForSearchType(
        int searchType, FetchCallback<ResultInfo<List<QChatServerInfo>>> callback);
  }
}
