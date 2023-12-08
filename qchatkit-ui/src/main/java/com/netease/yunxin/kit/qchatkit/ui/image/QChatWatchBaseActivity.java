// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.image;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.netease.yunxin.kit.qchatkit.ui.common.QChatChannelBaseActivity;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatWatchImageVideoActivityBinding;

/** 图片和视频查看页面父类 */
public abstract class QChatWatchBaseActivity extends QChatChannelBaseActivity {

  QChatWatchViewModel viewModel;
  QChatWatchImageVideoActivityBinding binding;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = QChatWatchImageVideoActivityBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    initData(getIntent());
    initViewModel();
    initDataObserver();
    initView();
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    if (hasFocus) {
      hideSystemUI();
    }
  }

  private void hideSystemUI() {
    View decorView = getWindow().getDecorView();
    decorView.setSystemUiVisibility(
        View.SYSTEM_UI_FLAG_IMMERSIVE
            // Set the content to appear under the system bars so that the
            // content doesn't resize when the system bars hide and show.
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            // Hide the nav bar and status bar
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN);
  }

  public void initData(Intent intent) {}

  public void initViewModel() {
    viewModel = new ViewModelProvider(this).get(QChatWatchViewModel.class);
  }

  public void initView() {
    binding.mediaClose.setOnClickListener(v -> finish());
    binding.mediaDownload.setOnClickListener(v -> saveMedia());
    binding.mediaContainer.addView(initMediaView());
  }

  public void initDataObserver() {}

  public abstract View initMediaView();

  public abstract void saveMedia();
}
