// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.image;

import static com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant.LIB_TAG;

import android.text.TextUtils;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import com.netease.nimlib.sdk.msg.attachment.FileAttachment;
import com.netease.nimlib.sdk.msg.constant.AttachStatusEnum;
import com.netease.yunxin.kit.alog.ALog;
import com.netease.yunxin.kit.common.ui.viewmodel.BaseViewModel;
import com.netease.yunxin.kit.common.ui.viewmodel.FetchResult;
import com.netease.yunxin.kit.common.ui.viewmodel.LoadStatus;
import com.netease.yunxin.kit.corekit.im2.extend.FetchCallback;
import com.netease.yunxin.kit.qchatkit.EventObserver;
import com.netease.yunxin.kit.qchatkit.repo.QChatMessageRepo;
import com.netease.yunxin.kit.qchatkit.repo.QChatServiceObserverRepo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageInfo;

/** 图片和视频查看页面ViewModel */
public class QChatWatchViewModel extends BaseViewModel {
  private static final String TAG = "QChatWatchViewModel";

  private final MutableLiveData<FetchResult<QChatMessageInfo>> statusMessageLiveData =
      new MutableLiveData<>();
  private final FetchResult<QChatMessageInfo> statusMessageResult =
      new FetchResult<>(LoadStatus.Finish);

  // 监听图片和视频消息下载状态
  private final EventObserver<QChatMessageInfo> msgStatusObserver =
      new EventObserver<QChatMessageInfo>() {
        @Override
        public void onEvent(@Nullable QChatMessageInfo msg) {
          if (msg == null) {
            return;
          }

          if (isFileHasDownloaded(msg)) {
            onDownloadSuccess(msg);
          } else if (msg.getMessage().getAttachStatus() == AttachStatusEnum.fail) {
            onDownloadFail(msg);
          }
        }
      };

  public QChatWatchViewModel() {
    registerObservers(true);
  }

  public MutableLiveData<FetchResult<QChatMessageInfo>> getStatusMessageLiveData() {
    return statusMessageLiveData;
  }

  private void registerObservers(boolean register) {
    QChatServiceObserverRepo.observeMessageStatusChange(msgStatusObserver, register);
  }

  /**
   * 判断图片或视频是否已经下载
   *
   * @param message 消息
   * @return 是否已经下载
   */
  private boolean isFileHasDownloaded(final QChatMessageInfo message) {
    return message.getAttachStatus() == AttachStatusEnum.transferred
        && !TextUtils.isEmpty(((FileAttachment) message.getAttachment()).getPath());
  }

  /**
   * 请求图片或视频下载，进入查看器开始下载原图（消息页面使用缩略图查看），下载成功加载大图
   *
   * @param message 消息
   */
  public void requestFile(QChatMessageInfo message) {
    ALog.d(LIB_TAG, TAG, "requestFile:" + (message == null ? "null" : message.getUuid()));
    if (isFileHasDownloaded(message)) {
      ALog.d(LIB_TAG, TAG, "request file has downloaded.");
      // onDownloadSuccess(message);
      return;
    }
    onDownloadStart(message);
    downloadAttachment(message, false);
  }

  /**
   * 图片或视频下载开始
   *
   * @param message 消息
   */
  private void onDownloadStart(QChatMessageInfo message) {
    ALog.d(LIB_TAG, TAG, "onDownloadStart :" + (message == null ? "null" : message.getUuid()));
    if (message == null || message.getAttachment() == null) {
      return;
    }
    if (message.getAttachment() instanceof FileAttachment
        && ((FileAttachment) message.getAttachment()).getPath() == null) {
      statusMessageResult.setLoadStatus(LoadStatus.Loading);
    } else {
      statusMessageResult.setLoadStatus(LoadStatus.Finish);
    }
    statusMessageResult.setData(message);
    statusMessageResult.setType(FetchResult.FetchType.Update);
    statusMessageResult.setTypeIndex(-1);
    statusMessageLiveData.postValue(statusMessageResult);
  }

  /**
   * 图片或视频下载成功
   *
   * @param message 消息
   */
  private void onDownloadSuccess(QChatMessageInfo message) {
    ALog.d(
        TAG, "on download success -->> " + (((FileAttachment) message.getAttachment()).getPath()));
    statusMessageResult.setLoadStatus(LoadStatus.Success);
    statusMessageResult.setData(message);
    statusMessageResult.setType(FetchResult.FetchType.Update);
    statusMessageResult.setTypeIndex(-1);
    statusMessageLiveData.postValue(statusMessageResult);
  }

  /**
   * 图片或视频下载失败
   *
   * @param message 消息
   */
  private void onDownloadFail(QChatMessageInfo message) {
    ALog.d(
        LIB_TAG,
        TAG,
        "on download fail -->> " + (((FileAttachment) message.getAttachment()).getPath()));
    statusMessageResult.setLoadStatus(LoadStatus.Error);
    statusMessageResult.setData(message);
    statusMessageResult.setType(FetchResult.FetchType.Update);
    statusMessageResult.setTypeIndex(-1);
    statusMessageLiveData.postValue(statusMessageResult);
  }

  /**
   * 下载图片或视频
   *
   * @param message 消息
   * @param thumb 是否下载缩略图
   */
  public void downloadAttachment(QChatMessageInfo message, boolean thumb) {
    QChatMessageRepo.downloadAttachment(
        message,
        thumb,
        new FetchCallback<Void>() {
          @Override
          public void onSuccess(@Nullable Void param) {
            ALog.d(TAG, "download success");
            onDownloadSuccess(message);
          }

          @Override
          public void onError(int code, @Nullable String msg) {
            ALog.d(TAG, "download failed code:" + code);
            onDownloadFail(message);
          }
        });
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    registerObservers(false);
  }
}
