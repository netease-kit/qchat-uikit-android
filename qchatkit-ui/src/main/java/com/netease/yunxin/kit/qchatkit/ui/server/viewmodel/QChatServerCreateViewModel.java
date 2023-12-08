// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.server.viewmodel;

import android.content.Context;
import android.util.Pair;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import com.netease.nimlib.sdk.qchat.enums.QChatRoleOption;
import com.netease.nimlib.sdk.qchat.enums.QChatRoleResource;
import com.netease.nimlib.sdk.qchat.result.QChatApplyServerJoinResult;
import com.netease.yunxin.kit.common.ui.viewmodel.BaseViewModel;
import com.netease.yunxin.kit.corekit.im.provider.FetchCallback;
import com.netease.yunxin.kit.corekit.im.provider.FetchCallbackImpl;
import com.netease.yunxin.kit.corekit.model.ErrorMsg;
import com.netease.yunxin.kit.corekit.model.ResultInfo;
import com.netease.yunxin.kit.qchatkit.repo.QChatChannelRepo;
import com.netease.yunxin.kit.qchatkit.repo.QChatRoleRepo;
import com.netease.yunxin.kit.qchatkit.repo.QChatServerRepo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSearchResultInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerWithSingleChannel;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 处理创建社区/查询/加入社区 */
public class QChatServerCreateViewModel extends BaseViewModel {
  private final MutableLiveData<ResultInfo<QChatServerWithSingleChannel>> createServerResult =
      new MutableLiveData<>();
  private final MutableLiveData<Pair<QChatSearchResultInfo, ResultInfo<QChatApplyServerJoinResult>>>
      joinServerResult = new MutableLiveData<>();
  private final MutableLiveData<ResultInfo<List<QChatSearchResultInfo>>> searchServerResult =
      new MutableLiveData<>();

  public MutableLiveData<ResultInfo<QChatServerWithSingleChannel>> getCreateServerResult() {
    return createServerResult;
  }

  public MutableLiveData<Pair<QChatSearchResultInfo, ResultInfo<QChatApplyServerJoinResult>>>
      getJoinServerResult() {
    return joinServerResult;
  }

  public MutableLiveData<ResultInfo<List<QChatSearchResultInfo>>> getSearchServerResult() {
    return searchServerResult;
  }

  /**
   * 创建社区
   *
   * @param context 上下文
   * @param isAnnouncement 是否创建公告频道
   * @param name 创建的社区/公告频道名称
   * @param iconUrl 创建的社区/公告频道头像连接
   */
  public void createServer(Context context, boolean isAnnouncement, String name, String iconUrl) {
    if (isAnnouncement) {
      createAnnounceServer(name, iconUrl);
    } else {
      createNormalServer(context, name, iconUrl);
    }
  }

  /**
   * 创建公告频道
   *
   * @param name 公告频道名称
   * @param iconUrl 公告频道头像
   */
  private void createAnnounceServer(String name, String iconUrl) {
    QChatServerRepo.createAnnouncementServer(
        name,
        iconUrl,
        getAuthMapWithOptions(QChatRoleOption.ALLOW),
        new FetchCallback<ResultInfo<QChatServerWithSingleChannel>>() {
          @Override
          public void onSuccess(@Nullable ResultInfo<QChatServerWithSingleChannel> param) {
            if (param == null) {
              createServerResult.setValue(
                  new ResultInfo<>(
                      null,
                      false,
                      new ErrorMsg(
                          -1,
                          "createAnnouncementServer:onSuccess:result is null.",
                          new Exception("QChatServerWithSingleChannel result is null."))));
              return;
            }
            QChatServerWithSingleChannel resultValue = param.getValue();
            if (resultValue == null) {
              createServerResult.setValue(
                  new ResultInfo<>(
                      null,
                      false,
                      new ErrorMsg(
                          -1,
                          "createAnnouncementServer:onSuccess:value is null.",
                          new Exception("QChatServerWithSingleChannel value is null."))));
              return;
            }
            if (param.getSuccess()) {
              // 创建成功后更新 everyone 身份组权限
              QChatRoleRepo.updateEveryonePermissionForAnnounce(
                  param.getValue().getServerInfo().getServerId(),
                  getAuthMapWithOptions(QChatRoleOption.DENY),
                  null);
              createServerResult.setValue(param);
            } else {
              // 创建失败则删除
              QChatServerRepo.deleteServer(resultValue.getServerInfo().getServerId(), null);
              createServerResult.setValue(new ResultInfo<>(null, false, param.getMsg()));
            }
          }

          @Override
          public void onFailed(int code) {
            createServerResult.setValue(new ResultInfo<>(null, false, new ErrorMsg(code)));
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            createServerResult.setValue(
                new ResultInfo<>(
                    null,
                    false,
                    new ErrorMsg(-1, "createAnnouncementServer:onException", exception)));
          }
        });
  }

  /**
   * 创建社区
   *
   * @param context 上下文
   * @param name 社区名称
   * @param iconUrl 社区头像
   */
  private void createNormalServer(Context context, String name, String iconUrl) {
    QChatServerRepo.createServerAndCreateChannel(
        name,
        context.getString(R.string.qchat_server_channel_name_fix, "1"),
        context.getString(R.string.qchat_server_channel_name_fix, "2"),
        iconUrl,
        new FetchCallback<QChatServerWithSingleChannel>() {
          @Override
          public void onSuccess(@Nullable QChatServerWithSingleChannel param) {
            createServerResult.setValue(new ResultInfo<>(param));
          }

          @Override
          public void onFailed(int code) {
            createServerResult.setValue(new ResultInfo<>(null, false, new ErrorMsg(code)));
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            createServerResult.setValue(
                new ResultInfo<>(
                    null, false, new ErrorMsg(-1, "createNormalServer:onException", exception)));
          }
        });
  }

  /**
   * 查询社区/公告频道
   *
   * @param isAnnouncement true 公告频道类型；false 社区类型
   * @param serverId 社区/公告频道 id
   */
  public void searchServer(boolean isAnnouncement, long serverId) {
    QChatServerRepo.searchServerById(
        serverId,
        isAnnouncement,
        new FetchCallback<List<QChatSearchResultInfo>>() {
          @Override
          public void onSuccess(@Nullable List<QChatSearchResultInfo> param) {
            searchServerResult.setValue(new ResultInfo<>(param));
          }

          @Override
          public void onFailed(int code) {
            searchServerResult.setValue(new ResultInfo<>(null, false, new ErrorMsg(code)));
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            searchServerResult.setValue(
                new ResultInfo<>(
                    null, false, new ErrorMsg(-1, "searchServer:onException", exception)));
          }
        });
  }

  /** 加入社区/公告频道 */
  public void joinServer(QChatSearchResultInfo searchResultInfo) {
    if (searchResultInfo == null) {
      return;
    }
    QChatServerRepo.applyServerJoin(
        searchResultInfo.serverInfo.getServerId(),
        new FetchCallback<QChatApplyServerJoinResult>() {
          @Override
          public void onSuccess(@Nullable QChatApplyServerJoinResult param) {
            QChatChannelRepo.fetchChannelsByServerId(
                searchResultInfo.serverInfo.getServerId(),
                0,
                1,
                new FetchCallbackImpl<List<QChatChannelInfo>>() {
                  @Override
                  public void onSuccess(@Nullable List<QChatChannelInfo> channelInfo) {
                    searchResultInfo.channelInfo =
                        channelInfo != null && !channelInfo.isEmpty() ? channelInfo.get(0) : null;
                    joinServerResult.setValue(
                        new Pair<>(searchResultInfo, new ResultInfo<>(param)));
                  }

                  @Override
                  public void onFailed(int code) {
                    joinServerResult.setValue(
                        new Pair<>(searchResultInfo, new ResultInfo<>(param)));
                  }

                  @Override
                  public void onException(@Nullable Throwable exception) {
                    joinServerResult.setValue(
                        new Pair<>(searchResultInfo, new ResultInfo<>(param)));
                  }
                });
          }

          @Override
          public void onFailed(int code) {
            joinServerResult.setValue(
                new Pair<>(searchResultInfo, new ResultInfo<>(null, false, new ErrorMsg(code))));
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            joinServerResult.setValue(
                new Pair<>(
                    searchResultInfo,
                    new ResultInfo<>(
                        null, false, new ErrorMsg(-1, "joinServer:onException", exception))));
          }
        });
  }

  private Map<QChatRoleResource, QChatRoleOption> getAuthMapWithOptions(QChatRoleOption option) {
    Map<QChatRoleResource, QChatRoleOption> authMap = new HashMap<>();
    authMap.put(QChatRoleResource.SEND_MSG, option);
    authMap.put(QChatRoleResource.MANAGE_SERVER, option);
    authMap.put(QChatRoleResource.DELETE_MSG, option);
    authMap.put(QChatRoleResource.INVITE_SERVER, option);
    authMap.put(QChatRoleResource.KICK_SERVER, option);
    authMap.put(QChatRoleResource.MANAGE_ROLE, option);
    authMap.put(QChatRoleResource.MANAGE_CHANNEL, option);
    authMap.put(
        new QChatRoleResource(
            QChatConstant.QCHAT_SELF_PERMISSION_EMOJI_REPLAY,
            QChatConstant.QCHAT_PERMISSION_TYPE_ALL),
        option);
    return authMap;
  }
}
