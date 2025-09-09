// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.channel.add;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import com.netease.nimlib.sdk.ResponseCode;
import com.netease.yunxin.kit.alog.ALog;
import com.netease.yunxin.kit.common.ui.viewmodel.BaseViewModel;
import com.netease.yunxin.kit.common.ui.viewmodel.FetchResult;
import com.netease.yunxin.kit.common.ui.viewmodel.LoadStatus;
import com.netease.yunxin.kit.corekit.im2.extend.FetchCallback;
import com.netease.yunxin.kit.qchatkit.repo.QChatRoleRepo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelRoleInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerRoleInfo;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatArrowBean;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant;
import com.netease.yunxin.kit.qchatkit.ui.utils.ErrorUtils;
import java.util.ArrayList;
import java.util.List;

public class AddRoleViewModel extends BaseViewModel {

  private static final String TAG = "AddRoleViewModel";

  // 身份组查询LiveData
  private final MutableLiveData<FetchResult<List<QChatArrowBean>>> roleLiveData =
      new MutableLiveData<>();
  // 身份组查询结果
  private final FetchResult<List<QChatArrowBean>> fetchResult =
      new FetchResult<>(LoadStatus.Finish);

  // 添加身份组LiveData
  private final MutableLiveData<FetchResult<QChatChannelRoleInfo>> addLiveData =
      new MutableLiveData<>();
  private final FetchResult<QChatChannelRoleInfo> addResult = new FetchResult<>(LoadStatus.Finish);

  // 身份组查询最后一个身份组记录，用于分页查询
  private QChatServerRoleInfo lastRoleInfo;
  // 身份组列表是否还有更多数据
  private boolean roleHasMore = false;

  public MutableLiveData<FetchResult<List<QChatArrowBean>>> getRoleLiveData() {
    return roleLiveData;
  }

  public MutableLiveData<FetchResult<QChatChannelRoleInfo>> getAddLiveData() {
    return addLiveData;
  }

  /**
   * 查询身份组列表接口
   *
   * @param serverId 社区ID
   * @param channelId 频道ID
   */
  public void fetchRoleList(long serverId, long channelId) {
    ALog.d(TAG, "fetchRoleList", "info:" + serverId + "," + channelId);
    fetchRoleData(serverId, channelId, 0);
  }

  /**
   * 分页查询身份组列表接口
   *
   * @param serverId 社区ID
   * @param channelId 频道ID
   * @param offset 分页查询偏移量
   */
  private void fetchRoleData(long serverId, long channelId, long offset) {
    QChatRoleRepo.fetchServerRolesWithoutChannel(
        serverId,
        channelId,
        offset,
        QChatConstant.MEMBER_PAGE_SIZE,
        new FetchCallback<List<QChatServerRoleInfo>>() {
          @Override
          public void onSuccess(@Nullable List<QChatServerRoleInfo> param) {
            ArrayList<QChatArrowBean> addList = new ArrayList<>();
            if (param != null && param.size() > 0) {
              for (int index = 0; index < param.size(); index++) {
                QChatServerRoleInfo roleInfo = param.get(index);
                int topRadius = index == 0 ? QChatConstant.CORNER_RADIUS_ARROW : 0;
                int bottomRadius =
                    index == param.size() - 1 ? QChatConstant.CORNER_RADIUS_ARROW : 0;
                QChatArrowBean bean =
                    new QChatArrowBean(roleInfo.getName(), topRadius, bottomRadius);
                bean.param = roleInfo;
                addList.add(bean);
              }
              lastRoleInfo = param.get(param.size() - 1);
            }
            roleHasMore = param != null && param.size() >= QChatConstant.MEMBER_PAGE_SIZE;

            fetchResult.setData(addList);
            if (offset == 0) {
              fetchResult.setLoadStatus(LoadStatus.Success);
            } else {
              fetchResult.setFetchType(FetchResult.FetchType.Add);
              fetchResult.setTypeIndex(-1);
            }
            roleLiveData.postValue(fetchResult);
            ALog.d(TAG, "fetchRoleData", "onSuccess:" + addList.size());
          }

          @Override
          public void onError(int code, @Nullable String msg) {
            ALog.d(TAG, "fetchRoleData", "onFailed:" + code);
            fetchResult.setError(code, R.string.qchat_fetch_role_error);
            roleLiveData.postValue(fetchResult);
          }
        });
  }

  /**
   * 添加身份组接口
   *
   * @param channelId 频道ID
   * @param roleInfo 身份组信息
   */
  public void addChannelRole(long channelId, QChatServerRoleInfo roleInfo) {
    QChatRoleRepo.addChannelRole(
        roleInfo.getServerId(),
        channelId,
        roleInfo.getRoleId(),
        new FetchCallback<QChatChannelRoleInfo>() {
          @Override
          public void onSuccess(@Nullable QChatChannelRoleInfo param) {
            ALog.d(TAG, "addChannelRole", "onSuccess:" + roleInfo.getRoleId());
            addResult.setLoadStatus(LoadStatus.Success);
            addResult.setData(param);
            addLiveData.postValue(addResult);
          }

          @Override
          public void onError(int code, @Nullable String msg) {
            ALog.d(TAG, "addChannelRole", "onFailed:" + code + "," + roleInfo.getRoleId());
            int tipRes;
            if (code == ResponseCode.RES_ENONEXIST) {
              tipRes = R.string.qchat_add_role_error_not_found;
            } else if (code == ResponseCode.RES_EEXIST) {
              tipRes = R.string.qchat_add_role_error_exist;
            } else {
              tipRes = R.string.qchat_add_role_error;
            }
            addResult.setError(code, ErrorUtils.getErrorText(code, tipRes));
            addLiveData.postValue(addResult);
          }
        });
  }

  /**
   * 分页查询更多身份组列表接口
   *
   * @param serverId 社区ID
   * @param channelId 频道ID
   */
  public void loadMore(long serverId, long channelId) {
    long offset = 0;
    if (lastRoleInfo != null) {
      offset = lastRoleInfo.getCreateTime();
    }
    fetchRoleData(serverId, channelId, offset);
  }

  /**
   * 是否还有更多数据
   *
   * @return true:还有更多数据
   */
  public boolean hasMore() {
    return roleHasMore;
  }
}
