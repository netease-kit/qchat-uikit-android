// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.common;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.Nullable;

public abstract class QChatServerCommonBaseActivity extends QChatServerBaseActivity {
  protected View contentView;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    contentView = getContentView();
    if (contentView != null) {
      setContentView(contentView);
    }
    initViewModel();
    initView();
    initData();
  }

  protected abstract void initViewModel();

  protected abstract View getContentView();

  protected abstract void initView();

  protected abstract void initData();
}
