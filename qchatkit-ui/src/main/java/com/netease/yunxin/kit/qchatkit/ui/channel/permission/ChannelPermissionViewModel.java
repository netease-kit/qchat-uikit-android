// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.channel.permission;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import com.netease.yunxin.kit.alog.ALog;
import com.netease.yunxin.kit.common.ui.viewmodel.BaseViewModel;
import com.netease.yunxin.kit.common.ui.viewmodel.FetchResult;
import com.netease.yunxin.kit.common.ui.viewmodel.LoadStatus;
import com.netease.yunxin.kit.corekit.im.provider.FetchCallback;
import com.netease.yunxin.kit.qchatkit.repo.QChatChannelRepo;
import com.netease.yunxin.kit.qchatkit.repo.QChatRoleRepo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelMember;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelRoleInfo;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatArrowBean;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatBaseBean;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatChannelMemberBean;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatChannelRoleBean;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatCommonBean;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatMoreBean;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatTitleBean;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatViewType;
import com.netease.yunxin.kit.qchatkit.ui.utils.ErrorUtils;
import java.util.ArrayList;
import java.util.List;

/** 话题权限设置和修改ViewModel */
public class ChannelPermissionViewModel extends BaseViewModel {

  private static final String TAG = "ChannelPermissionViewModel";

  //身份组列表LiveData
  private final MutableLiveData<FetchResult<List<QChatBaseBean>>> roleLiveData =
      new MutableLiveData<>();
  private final FetchResult<List<QChatBaseBean>> roleFetchResult =
      new FetchResult<>(LoadStatus.Finish);
  //身份组更多列表LiveData
  private final MutableLiveData<FetchResult<List<QChatBaseBean>>> roleMoreLiveData =
      new MutableLiveData<>();
  //成员列表LiveData
  private final MutableLiveData<FetchResult<List<QChatBaseBean>>> memberLiveData =
      new MutableLiveData<>();
  private final FetchResult<List<QChatBaseBean>> memberFetchResult =
      new FetchResult<>(LoadStatus.Finish);
  //member list default page size
  private final int memberPageSize = 100;
  //role list request page size
  private final int rolePageLimit = 200;
  //role list show size
  private final int rolePageSize = 5;

  private long serverId;
  private long channelId;
  private QChatChannelMember lastMemberInfo;
  private QChatMoreBean roleMoreBean;
  private int moreRoleIndex = 0;
  private boolean memberHasMore = false;

  /** role list live data */
  public MutableLiveData<FetchResult<List<QChatBaseBean>>> getRoleLiveData() {
    return roleLiveData;
  }

  /** role more item live data that to add or remove more item */
  public MutableLiveData<FetchResult<List<QChatBaseBean>>> getRoleMoreLiveData() {
    return roleMoreLiveData;
  }

  /** member list live data */
  public MutableLiveData<FetchResult<List<QChatBaseBean>>> getMemberLiveData() {
    return memberLiveData;
  }

  /** 获取话题下身份组和成员列表数据 */
  public void fetchData(long serverId, long channelId) {
    ALog.d(TAG, "fetchData", "serverId:" + serverId + "channelId:" + channelId);
    this.serverId = serverId;
    this.channelId = channelId;
    moreRoleIndex = 0;
    fetchRoleData(serverId, channelId, 0);
    fetchMemberData(serverId, channelId, 0);
  }

  /** 移除身份组或者成员 */
  public void delete(QChatBaseBean data, int position) {
    if (data instanceof QChatChannelRoleBean) {
      deleteRole((QChatChannelRoleBean) data, position);
    } else if (data instanceof QChatChannelMemberBean) {
      deleteMember((QChatChannelMemberBean) data, position);
    }
  }

  /** 获取身份组列表数据 */
  private void fetchRoleData(long serverId, long channelId, long offset) {
    ALog.d(TAG, "fetchRoleData", "serverId:" + serverId + "channelId:" + channelId);
    QChatChannelRepo.fetchChannelRoles(
        serverId,
        channelId,
        offset,
        rolePageLimit,
        new FetchCallback<List<QChatChannelRoleInfo>>() {
          @Override
          public void onSuccess(@Nullable List<QChatChannelRoleInfo> param) {
            ArrayList<QChatBaseBean> addList = new ArrayList<>();
            if (param != null && param.size() > 0) {
              ALog.d(TAG, "fetchRoleData", "onSuccess:" + param.size());
              if (offset == 0) {
                QChatBaseBean titleBean = new QChatTitleBean(R.string.qchat_role_list_title);
                addList.add(titleBean);
              }
              int size = Math.min(param.size(), rolePageSize);
              for (int index = 0; index < param.size(); index++) {
                QChatChannelRoleInfo roleInfo = param.get(index);
                int topRadius = index == 0 && offset == 0 ? QChatConstant.CORNER_RADIUS_ARROW : 0;
                QChatChannelRoleBean bean = new QChatChannelRoleBean(roleInfo, topRadius, 0);
                bean.router = QChatConstant.ROUTER_ROLE_PERMISSION;
                addList.add(bean);
              }
              if (offset == 0 && rolePageSize < param.size()) {
                roleMoreBean = new QChatMoreBean(R.string.qchat_more_title);
                roleMoreBean.extend = param.size();
                addList.add(roleMoreBean);
                size++;
              }
              addList.add(new QChatCommonBean(QChatViewType.CORNER_VIEW_TYPE));
              roleFetchResult.setData(addList);
              FetchResult.FetchType type = FetchResult.FetchType.Add;
              roleFetchResult.setFetchType(type);
              roleFetchResult.setTypeIndex(moreRoleIndex);
              roleLiveData.setValue(roleFetchResult);
              moreRoleIndex = moreRoleIndex + size;
            }
          }

          @Override
          public void onFailed(int code) {
            ALog.d(TAG, "fetchRoleData", "onFailed:" + code);
            roleFetchResult.setError(
                code, ErrorUtils.getErrorText(code, R.string.qchat_channel_fetch_role_error));
            roleLiveData.setValue(roleFetchResult);
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            String errorMsg = exception != null ? exception.getMessage() : "";
            ALog.d(TAG, "fetchRoleData", "exception:" + errorMsg);
            roleFetchResult.setError(
                QChatConstant.ERROR_CODE_CHANNEL_ROLE_FETCH,
                R.string.qchat_channel_fetch_role_error);
            roleLiveData.setValue(roleFetchResult);
          }
        });
  }

  /** 移除身份组 */
  public void deleteRole(QChatChannelRoleBean bean, int position) {
    QChatRoleRepo.deleteChannelRole(
        serverId,
        channelId,
        bean.channelRole.getRoleId(),
        new FetchCallback<Void>() {
          @Override
          public void onSuccess(@Nullable Void param) {
            ALog.d(TAG, "deleteRole", "onSuccess:" + position);
            if (roleMoreBean != null) {
              roleMoreBean.extend--;
            }
            FetchResult.FetchType type = FetchResult.FetchType.Remove;
            roleFetchResult.setFetchType(type);
            roleFetchResult.setTypeIndex(position);
            roleLiveData.setValue(roleFetchResult);
          }

          @Override
          public void onFailed(int code) {
            ALog.d(TAG, "deleteRole", "code:" + code);
            roleFetchResult.setError(
                code, ErrorUtils.getErrorText(code, R.string.qchat_channel_role_delete_error));
            roleLiveData.setValue(roleFetchResult);
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            String errorMsg = exception != null ? exception.getMessage() : "";
            ALog.d(TAG, "deleteRole", "exception:" + errorMsg);
            roleFetchResult.setError(
                QChatConstant.ERROR_CODE_CHANNEL_ROLE_DELETE,
                R.string.qchat_channel_role_delete_error);
            roleLiveData.setValue(roleFetchResult);
          }
        });
  }

  /** 获取成员列表数据 */
  public void fetchMemberData(long serverId, long channelId, long offset) {
    QChatChannelRepo.fetchChannelRoleMembers(
        serverId,
        channelId,
        offset,
        memberPageSize,
        new FetchCallback<List<QChatChannelMember>>() {
          @Override
          public void onSuccess(@Nullable List<QChatChannelMember> param) {
            ALog.d(TAG, "fetchMemberData", "onSuccess:" + serverId + "," + channelId);
            ArrayList<QChatBaseBean> addList = new ArrayList<>();
            memberFetchResult.setData(null);
            if (param != null && param.size() > 0) {
              if (offset == 0) {
                QChatBaseBean titleBean = new QChatTitleBean(R.string.qchat_member_list_title);
                addList.add(titleBean);
              }

              for (int index = 0; index < param.size(); index++) {
                QChatChannelMember roleInfo = param.get(index);
                QChatChannelMemberBean bean = new QChatChannelMemberBean(roleInfo);
                if (index == 0 && offset == 0) {
                  bean.topRadius = QChatConstant.CORNER_RADIUS_ARROW;
                }
                addList.add(bean);
              }
              addList.add(new QChatCommonBean(QChatViewType.CORNER_VIEW_TYPE));

              lastMemberInfo = param.get(param.size() - 1);
              memberFetchResult.setData(addList);
              FetchResult.FetchType type = FetchResult.FetchType.Add;
              memberFetchResult.setFetchType(type);
              memberFetchResult.setTypeIndex(-1);
            }
            memberLiveData.setValue(memberFetchResult);

            memberHasMore = param != null && param.size() >= memberPageSize;
          }

          @Override
          public void onFailed(int code) {
            ALog.d(TAG, "fetchMemberData", "onFailed:" + code);
            memberFetchResult.setError(
                code, ErrorUtils.getErrorText(code, R.string.qchat_channel_fetch_member_error));
            memberLiveData.setValue(memberFetchResult);
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            String errorMsg = exception != null ? exception.getMessage() : "";
            ALog.d(TAG, "fetchMemberData", "exception:" + errorMsg);
            memberFetchResult.setError(
                QChatConstant.ERROR_CODE_CHANNEL_MEMBER_FETCH,
                R.string.qchat_channel_fetch_member_error);
            memberLiveData.setValue(memberFetchResult);
          }
        });
  }

  /** 移除成员 */
  public void deleteMember(QChatChannelMemberBean bean, int position) {
    ALog.d(
        TAG,
        "deleteMember",
        "info:" + serverId + "," + channelId + "," + bean.channelMember.getAccId());
    QChatRoleRepo.deleteChannelMemberRole(
        serverId,
        channelId,
        bean.channelMember.getAccId(),
        new FetchCallback<Void>() {
          @Override
          public void onSuccess(@Nullable Void param) {
            ALog.d(TAG, "deleteMember", "onSuccess");
            memberFetchResult.setFetchType(FetchResult.FetchType.Remove);
            memberFetchResult.setTypeIndex(position);
            memberLiveData.setValue(memberFetchResult);
          }

          @Override
          public void onFailed(int code) {
            ALog.d(TAG, "deleteMember", "onFailed:" + code);
            memberFetchResult.setError(
                code, ErrorUtils.getErrorText(code, R.string.qchat_channel_member_delete_error));
            memberLiveData.setValue(memberFetchResult);
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            String errorMsg = exception != null ? exception.getMessage() : "";
            ALog.d(TAG, "deleteMember", "exception:" + errorMsg);
            memberFetchResult.setError(
                QChatConstant.ERROR_CODE_CHANNEL_ROLE_DELETE,
                R.string.qchat_channel_member_delete_error);
            memberLiveData.setValue(memberFetchResult);
          }
        });
  }

  /** 加载更多成员 */
  public void loadMoreMember() {
    if (memberHasMore) {
      long offset = lastMemberInfo != null ? lastMemberInfo.getCreateTime() : 0;
      fetchMemberData(serverId, channelId, offset);
      ALog.d(TAG, "loadMoreMember");
    }
  }

  /** 是否还有更多成员 */
  public boolean hasMore() {
    return memberHasMore;
  }

  /** 获取页面顶部操作栏数据 */
  public List<QChatBaseBean> getHeaderData(String[] titleArray, String[] routerArray) {
    if (titleArray == null || titleArray.length < 1) {
      return null;
    }
    List<QChatBaseBean> data = new ArrayList<>();
    for (int index = 0; index < titleArray.length; index++) {
      QChatArrowBean addRole = new QChatArrowBean(titleArray[index], 0, 0);
      if (index == 0) {
        addRole.topRadius = QChatConstant.CORNER_RADIUS_ARROW;
      }
      if (index == titleArray.length - 1) {
        addRole.bottomRadius = QChatConstant.CORNER_RADIUS_ARROW;
      }
      addRole.router =
          routerArray != null && routerArray.length > index ? routerArray[index] : null;
      data.add(addRole);
      ALog.d(TAG, "getHeaderData", "title:" + titleArray[index]);
    }
    return data;
  }
}
