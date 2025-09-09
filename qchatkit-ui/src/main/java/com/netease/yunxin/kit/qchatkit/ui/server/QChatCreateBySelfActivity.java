// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.server;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import com.bumptech.glide.Glide;
import com.netease.yunxin.kit.alog.ALog;
import com.netease.yunxin.kit.common.ui.activities.BaseActivity;
import com.netease.yunxin.kit.common.ui.photo.PhotoChoiceDialog;
import com.netease.yunxin.kit.common.ui.utils.CommonCallback;
import com.netease.yunxin.kit.common.utils.NetworkUtils;
import com.netease.yunxin.kit.corekit.im2.extend.FetchCallback;
import com.netease.yunxin.kit.corekit.model.ResultInfo;
import com.netease.yunxin.kit.qchatkit.repo.ResourceRepo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerWithSingleChannel;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatCreateBySelfActivityBinding;
import com.netease.yunxin.kit.qchatkit.ui.message.ChannelMessageActivity;
import com.netease.yunxin.kit.qchatkit.ui.server.viewmodel.QChatServerCreateViewModel;
import com.netease.yunxin.kit.qchatkit.ui.utils.QChatUtils;
import java.io.File;

/** 创建社区/公告频道页面 */
public class QChatCreateBySelfActivity extends BaseActivity {
  private static final String TAG = "QChatCreateBySelfActivity";
  private static final String KEY_PARAM_IS_ANNOUNCEMENT = "key_is_announcement";

  private final QChatServerCreateViewModel viewModel = new QChatServerCreateViewModel();
  private QChatCreateBySelfActivityBinding binding;
  private String iconUrl = null;
  private InputMethodManager manager;
  private boolean isAnnouncement = false;
  private PhotoChoiceDialog photoChoiceDialog;

  /** 监听创建结果 */
  private final Observer<ResultInfo<QChatServerWithSingleChannel>> observerForCreateResult =
      result -> {
        if (result.getSuccess()) {
          QChatServerWithSingleChannel serverWithSingleChannel = result.getValue();
          if (serverWithSingleChannel == null) {
            binding.tvCreate.setEnabled(true);
            return;
          }
          QChatChannelInfo channelInfo = serverWithSingleChannel.getChannelInfo();
          // 创建成功后直接跳转到话题聊天页面
          if (channelInfo != null) {
            ChannelMessageActivity.launch(
                QChatCreateBySelfActivity.this,
                isAnnouncement ? serverWithSingleChannel.getServerInfo() : null,
                channelInfo.getServerId(),
                channelInfo.getChannelId(),
                serverWithSingleChannel.getChannelInfo().getOwner(),
                channelInfo.getName());
          }
          setResult(RESULT_OK);
          finish();
        } else {
          binding.tvCreate.setEnabled(true);
          Toast.makeText(
                  getApplicationContext(),
                  getString(R.string.qchat_server_request_fail),
                  Toast.LENGTH_SHORT)
              .show();
          ALog.e(TAG, "createServer failed. " + result.getMsg());
        }
      };

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = QChatCreateBySelfActivityBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    isAnnouncement = getIntent().getBooleanExtra(KEY_PARAM_IS_ANNOUNCEMENT, false);
    manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    viewModel.getCreateServerResult().observeForever(observerForCreateResult);
    initView();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    viewModel.getCreateServerResult().removeObserver(observerForCreateResult);
  }

  private void initView() {
    binding.tvCreateTitle.setText(
        isAnnouncement ? R.string.qchat_crate_by_self_announce : R.string.qchat_crate_by_self);
    binding.etServerName.setHint(
        isAnnouncement
            ? R.string.qchat_announcement_input_server_name
            : R.string.qchat_input_server_name);
    binding.ivBack.setOnClickListener(v -> finish());
    binding.ivPortrait.setOnClickListener(
        v -> {
          manager.hideSoftInputFromWindow(v.getWindowToken(), 0);
          showPictureChosenDialog();
        });
    binding.etServerName.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {}

          @Override
          public void afterTextChanged(Editable s) {
            if (TextUtils.isEmpty(String.valueOf(s))) {
              binding.ivClear.setVisibility(View.GONE);
              binding.tvCreate.setEnabled(false);
              binding.tvCreate.setAlpha(0.5f);
            } else {
              binding.ivClear.setVisibility(View.VISIBLE);
              binding.tvCreate.setEnabled(true);
              binding.tvCreate.setAlpha(1f);
            }
          }
        });

    binding.ivClear.setOnClickListener(v -> binding.etServerName.setText(null));
    binding.tvCreate.setEnabled(false);
    binding.tvCreate.setAlpha(0.5f);
    binding.tvCreate.setOnClickListener(
        v ->
            QChatUtils.isConnectedToastAndRun(
                this,
                () -> {
                  String serverName = binding.etServerName.getText().toString().trim();
                  if (TextUtils.isEmpty(serverName)) {
                    Toast.makeText(this, R.string.qchat_server_name_empty_error, Toast.LENGTH_SHORT)
                        .show();
                    binding.etServerName.setText("");
                    return;
                  }
                  binding.tvCreate.setEnabled(false);
                  viewModel.createServer(this, isAnnouncement, serverName, iconUrl);
                }));
  }

  /** 展示社区头像选择弹窗，拍照/相册选择 */
  private void showPictureChosenDialog() {
    if (photoChoiceDialog != null && photoChoiceDialog.isShowing()) {
      return;
    }
    CommonCallback<File> callback =
        new CommonCallback<File>() {
          @Override
          public void onSuccess(@Nullable File param) {
            if (!NetworkUtils.isConnected()) {
              Toast.makeText(
                      QChatCreateBySelfActivity.this,
                      R.string.qchat_network_error_tip,
                      Toast.LENGTH_SHORT)
                  .show();
              return;
            }
            if (param == null) {
              Toast.makeText(
                      QChatCreateBySelfActivity.this,
                      R.string.qchat_server_request_fail,
                      Toast.LENGTH_SHORT)
                  .show();
              return;
            }
            ResourceRepo.uploadFile(
                param,
                new FetchCallback<String>() {
                  @Override
                  public void onSuccess(@Nullable String param) {
                    iconUrl = param;
                    Glide.with(getApplicationContext())
                        .load(param)
                        .circleCrop()
                        .into(binding.ivPortrait);
                  }

                  @Override
                  public void onError(int code, @Nullable String msg) {
                    if (code != 0) {
                      Toast.makeText(
                              getApplicationContext(),
                              getString(R.string.qchat_server_request_fail) + " " + code,
                              Toast.LENGTH_SHORT)
                          .show();
                    }
                  }
                });
          }

          @Override
          public void onFailed(int code) {
            if (code != 0) {
              Toast.makeText(
                      getApplicationContext(),
                      getString(R.string.qchat_server_request_fail) + " " + code,
                      Toast.LENGTH_SHORT)
                  .show();
            }
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            Toast.makeText(
                    getApplicationContext(),
                    getString(R.string.qchat_server_request_fail),
                    Toast.LENGTH_SHORT)
                .show();
          }
        };
    photoChoiceDialog = new PhotoChoiceDialog(this);
    photoChoiceDialog.show(callback);
  }

  @Override
  public void finish() {
    super.finish();
    overridePendingTransition(R.anim.anim_empty_with_time, R.anim.anim_from_start_to_end);
  }

  /**
   * 页面启动方法
   *
   * @param launcher 启动launcher，用于通知启动方数据
   * @param context 上下文
   * @param isAnnouncement 是否为公告频道
   */
  public static void launch(
      ActivityResultLauncher<Intent> launcher, Context context, boolean isAnnouncement) {
    Intent intent = new Intent(context, QChatCreateBySelfActivity.class);
    intent.putExtra(KEY_PARAM_IS_ANNOUNCEMENT, isAnnouncement);
    launcher.launch(intent);
  }
}
