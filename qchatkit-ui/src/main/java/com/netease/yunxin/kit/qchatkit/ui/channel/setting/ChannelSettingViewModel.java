// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.channel.setting;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import com.netease.yunxin.kit.alog.ALog;
import com.netease.yunxin.kit.common.ui.viewmodel.BaseViewModel;
import com.netease.yunxin.kit.common.ui.viewmodel.FetchResult;
import com.netease.yunxin.kit.common.ui.viewmodel.LoadStatus;
import com.netease.yunxin.kit.corekit.im2.extend.FetchCallback;
import com.netease.yunxin.kit.qchatkit.repo.QChatChannelRepo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelInfo;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant;

/** channel setting view model */
public class ChannelSettingViewModel extends BaseViewModel {

  private static final String TAG = "ChannelSettingViewModel";
  private MutableLiveData<FetchResult<QChatChannelInfo>> resultLiveData = new MutableLiveData<>();
  private FetchResult<QChatChannelInfo> fetchResult = new FetchResult<>(LoadStatus.Finish);

  public MutableLiveData<FetchResult<QChatChannelInfo>> getFetchResult() {
    return resultLiveData;
  }

  /** 根据话题ID 查询话题信息 */
  public void fetchChannelInfo(long channelId) {
    ALog.d(TAG, "fetchChannelInfo", "channelId:" + channelId);
    QChatChannelRepo.fetchChannelInfo(
        channelId,
        new FetchCallback<QChatChannelInfo>() {
          @Override
          public void onSuccess(@Nullable QChatChannelInfo param) {
            if (param != null) {
              fetchResult.setStatus(LoadStatus.Success);
              fetchResult.setData(param);
              resultLiveData.postValue(fetchResult);
              ALog.d(TAG, "fetchChannelInfo", "onSuccess:");
            }
          }

          @Override
          public void onError(int code, @Nullable String msg) {
            fetchResult.setError(code, R.string.qchat_channel_fetch_error);
            resultLiveData.postValue(fetchResult);
            ALog.d(TAG, "fetchChannelInfo", "onFailed:" + code);
          }
        });
  }

  /** 删除话题 */
  public void deleteChannel(long channelId) {

    QChatChannelRepo.deleteChannel(
        channelId,
        new FetchCallback<Void>() {
          @Override
          public void onSuccess(@Nullable Void param) {
            fetchResult.setFetchType(FetchResult.FetchType.Remove);
            resultLiveData.postValue(fetchResult);
          }

          @Override
          public void onError(int code, @Nullable String msg) {
            if (code == QChatConstant.ERROR_CODE_IM_NO_PERMISSION) {
              fetchResult.setError(code, R.string.qchat_no_permission);
            } else {
              fetchResult.setError(code, R.string.qchat_channel_delete_error);
            }
            resultLiveData.postValue(fetchResult);
          }
        });
  }

  /** 更新话题 */
  public void updateChannel(@NonNull long channelId, @NonNull String name, @NonNull String topic) {

    QChatChannelRepo.updateChannel(
        channelId,
        name,
        topic,
        new FetchCallback<QChatChannelInfo>() {
          @Override
          public void onSuccess(@Nullable QChatChannelInfo param) {
            fetchResult.setFetchType(FetchResult.FetchType.Remove);
            resultLiveData.postValue(fetchResult);
          }

          @Override
          public void onError(int code, @Nullable String msg) {
            if (code == QChatConstant.ERROR_CODE_IM_NO_PERMISSION) {
              fetchResult.setError(code, R.string.qchat_no_permission);
            } else {
              fetchResult.setError(code, R.string.qchat_channel_update_error);
            }
            resultLiveData.postValue(fetchResult);
          }
        });
  }
}
