// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.channel.blackwhite;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import com.netease.yunxin.kit.alog.ALog;
import com.netease.yunxin.kit.common.ui.viewmodel.BaseViewModel;
import com.netease.yunxin.kit.common.ui.viewmodel.FetchResult;
import com.netease.yunxin.kit.common.ui.viewmodel.LoadStatus;
import com.netease.yunxin.kit.corekit.im2.extend.FetchCallback;
import com.netease.yunxin.kit.qchatkit.repo.QChatChannelRepo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelMember;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelModeEnum;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerMemberInfo;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatArrowBean;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatBaseBean;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatServerMemberBean;
import java.util.ArrayList;
import java.util.List;

/** 黑白名单列表ViewModel */
public class BlackWhiteViewModel extends BaseViewModel {

  private static final String TAG = "BlackWhiteViewModel";
  // 成员查询结果LiveData
  private final MutableLiveData<FetchResult<List<QChatBaseBean>>> resultLiveData =
      new MutableLiveData<>();
  private final FetchResult<List<QChatBaseBean>> fetchResult = new FetchResult<>(LoadStatus.Finish);

  // 添加成员结果LiveData
  private final MutableLiveData<FetchResult<QChatChannelMember>> addLiveData =
      new MutableLiveData<>();
  private final FetchResult<QChatChannelMember> addResult = new FetchResult<>(LoadStatus.Finish);

  // 删除成员结果LiveData
  private final MutableLiveData<FetchResult<QChatChannelMember>> removeLiveData =
      new MutableLiveData<>();
  private final FetchResult<QChatChannelMember> removeResult = new FetchResult<>(LoadStatus.Finish);

  private QChatServerMemberInfo lastRoleInfo;
  private boolean roleHasMore = false;

  public MutableLiveData<FetchResult<List<QChatBaseBean>>> getResultLiveData() {
    return resultLiveData;
  }

  public MutableLiveData<FetchResult<QChatChannelMember>> getAddLiveData() {
    return addLiveData;
  }

  public MutableLiveData<FetchResult<QChatChannelMember>> getRemoveLiveData() {
    return removeLiveData;
  }

  /** 查询频道成员列表 */
  public void fetchMemberList(long serverId, long channelId, QChatChannelModeEnum type) {
    fetchMemberData(serverId, channelId, type, 0);
  }

  /** 加载页面顶部功能按钮数据 */
  public ArrayList<QChatBaseBean> loadHeader() {
    ArrayList<QChatBaseBean> addList = new ArrayList<>();
    QChatArrowBean addMember = new QChatArrowBean("添加成员", 0, 0);
    addList.add(addMember);
    return addList;
  }

  /** 分页加载成员列表数据 */
  private void fetchMemberData(
      long serverId, long channelId, QChatChannelModeEnum type, long offset) {
    ALog.d(TAG, "fetchMemberData");
    QChatChannelRepo.fetchChannelBlackWhiteMembers(
        serverId,
        channelId,
        offset,
        type,
        QChatConstant.MEMBER_PAGE_SIZE,
        new FetchCallback<List<QChatServerMemberInfo>>() {
          @Override
          public void onSuccess(@Nullable List<QChatServerMemberInfo> param) {
            ArrayList<QChatBaseBean> addList = new ArrayList<>();
            if (param != null && param.size() > 0) {
              for (int index = 0; index < param.size(); index++) {
                QChatServerMemberInfo roleInfo = param.get(index);
                QChatServerMemberBean bean = new QChatServerMemberBean(roleInfo);
                addList.add(bean);
              }
              lastRoleInfo = param.get(param.size() - 1);
            }
            roleHasMore = param != null && param.size() >= QChatConstant.MEMBER_PAGE_SIZE;
            if (offset == 0) {
              fetchResult.setLoadStatus(LoadStatus.Success);
            } else {
              fetchResult.setFetchType(FetchResult.FetchType.Add);
              fetchResult.setTypeIndex(-1);
            }
            ALog.d(TAG, "fetchMemberData", "onSuccess" + addList.size());
            fetchResult.setData(addList);
            resultLiveData.postValue(fetchResult);
          }

          @Override
          public void onError(int code, @Nullable String msg) {
            ALog.d(TAG, "fetchMemberData", "onFailed" + code);
            fetchResult.setError(code, R.string.qchat_channel_fetch_member_error);
            resultLiveData.postValue(fetchResult);
          }
        });
  }

  /** 移除成员 */
  public void deleteMember(
      long serverId, long channelId, int channelType, String accId, int position) {
    ArrayList<String> accIdList = new ArrayList<>();
    accIdList.add(accId);
    QChatChannelRepo.removeChannelBlackWhiteMembers(
        serverId,
        channelId,
        channelType,
        accIdList,
        new FetchCallback<Void>() {
          @Override
          public void onSuccess(@Nullable Void param) {
            ALog.d(TAG, "deleteMember", "onSuccess" + accId);
            removeResult.setFetchType(FetchResult.FetchType.Remove);
            removeResult.setTypeIndex(position);
            removeLiveData.setValue(removeResult);
          }

          @Override
          public void onError(int code, @Nullable String msg) {
            removeResult.setError(code, R.string.qchat_channel_member_delete_error);
            removeLiveData.setValue(removeResult);
            ALog.d(TAG, "fetchMemberData", "onFailed" + code);
          }
        });
  }

  /** 添加成员 */
  public void addMember(
      long serverId, long channelId, List<String> accIdList, QChatChannelModeEnum type) {
    QChatChannelRepo.addChannelBlackWhiteMembers(
        serverId,
        channelId,
        accIdList,
        type,
        new FetchCallback<Void>() {

          @Override
          public void onError(int code, @Nullable String msg) {
            ALog.d(TAG, "addMember", "onFailed" + code);
            if (code == QChatConstant.ERROR_CODE_IM_NO_PERMISSION) {
              addResult.setError(code, R.string.qchat_no_permission);
            } else {
              addResult.setError(code, R.string.qchat_channel_member_add_error);
            }
            addLiveData.postValue(addResult);
          }

          @Override
          public void onSuccess(@Nullable Void param) {
            ALog.d(TAG, "deleteMember", "onSuccess" + accIdList.size());
            addResult.setLoadStatus(LoadStatus.Success);
            addLiveData.postValue(addResult);
          }
        });
  }

  /** 成员列表加载更多 */
  public void loadMore(long serverId, long channelId, QChatChannelModeEnum type) {
    long offset = 0;
    if (lastRoleInfo != null) {
      offset = lastRoleInfo.getCreateTime();
    }
    fetchMemberData(serverId, channelId, type, offset);
  }

  /** 是否还有更多数据 */
  public boolean hasMore() {
    return roleHasMore;
  }
}
