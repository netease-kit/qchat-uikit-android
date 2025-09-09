// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.common.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.netease.nimlib.sdk.qchat.result.QChatApplyServerJoinResult;
import com.netease.yunxin.kit.corekit.im2.extend.FetchCallback;
import com.netease.yunxin.kit.qchatkit.repo.QChatServerRepo;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatViewVisitorTipBinding;
import com.netease.yunxin.kit.qchatkit.ui.utils.QChatUtils;

public class QChatVisitorTipView extends FrameLayout {
  private static final int ERROR_CODE_OVER_SERVER_LIMIT = 419;

  private QChatViewVisitorTipBinding viewBinding;
  private FetchCallback<Boolean> callback;
  private long serverId;

  public QChatVisitorTipView(@NonNull Context context) {
    super(context);
    init(context);
  }

  public QChatVisitorTipView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public QChatVisitorTipView(
      @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  public QChatVisitorTipView(
      @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    init(context);
  }

  private void init(Context context) {
    viewBinding = QChatViewVisitorTipBinding.inflate(LayoutInflater.from(context), this, true);
    viewBinding.tvJoinBtn.setOnClickListener(
        v -> QChatUtils.isConnectedToastAndRun(getContext(), () -> joinServer(serverId, callback)));
  }

  public void configBackgroundResource(int resId) {
    viewBinding.getRoot().setBackgroundResource(resId);
  }

  public void configToJoinServerInfo(long serverId, FetchCallback<Boolean> callback) {
    this.serverId = serverId;
    this.callback = callback;
  }

  private void joinServer(long serverId, FetchCallback<Boolean> callback) {
    if (serverId <= 0L) {
      if (callback != null) {
        callback.onSuccess(false);
      }
      return;
    }
    QChatServerRepo.applyServerJoin(
        serverId,
        new FetchCallback<QChatApplyServerJoinResult>() {
          @Override
          public void onSuccess(@Nullable QChatApplyServerJoinResult param) {
            if (callback != null) {
              callback.onSuccess(true);
            }
          }

          @Override
          public void onError(int code, @Nullable String msg) {
            if (callback != null) {
              callback.onError(code, msg);
            }
            if (code == ERROR_CODE_OVER_SERVER_LIMIT) {
              Toast.makeText(
                      getContext(), R.string.qchat_server_count_over_limit, Toast.LENGTH_SHORT)
                  .show();
            } else {
              Toast.makeText(getContext(), R.string.qchat_server_request_fail, Toast.LENGTH_SHORT)
                  .show();
            }
          }
        });
  }
}
