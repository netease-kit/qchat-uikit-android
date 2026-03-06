// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.app.qchat.welcome;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.netease.yunxin.app.qchat.QChatApplication;
import com.netease.yunxin.app.qchat.R;
import com.netease.yunxin.app.qchat.databinding.ActivityWelcomeBinding;
import com.netease.yunxin.app.qchat.main.MainActivity;
import com.netease.yunxin.app.qchat.main.mine.setting.ConfigDataUtils;
import com.netease.yunxin.app.qchat.main.mine.setting.ConfigInfoActivity;
import com.netease.yunxin.app.qchat.utils.Constant;
import com.netease.yunxin.kit.alog.ALog;
import com.netease.yunxin.kit.common.ui.activities.BaseActivity;
import com.netease.yunxin.kit.common.ui.utils.ToastX;
import com.netease.yunxin.kit.corekit.im2.extend.FetchCallback;
import com.netease.yunxin.kit.qchatkit.QChatKitClient;

/** Welcome Page is launch page */
public class WelcomeActivity extends BaseActivity {

  private static final String TAG = "WelcomeActivity";
  private ActivityWelcomeBinding activityWelcomeBinding;

  // 硬编码的默认账号（当配置中无账号时作为 fallback）
  private static final String DEFAULT_ACCOUNT = "";
  private static final String DEFAULT_TOKEN = "";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ALog.d(Constant.PROJECT_TAG, TAG, "onCreateView");
    QChatApplication.setColdStart(true);
    activityWelcomeBinding = ActivityWelcomeBinding.inflate(getLayoutInflater());
    setContentView(activityWelcomeBinding.getRoot());
    showLoginView();
  }

  private void showMainActivityAndFinish() {
    ALog.d(Constant.PROJECT_TAG, TAG, "showMainActivityAndFinish");
    Intent intent = new Intent();
    intent.setClass(this, MainActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    this.startActivity(intent);
    finish();
  }

  /** 展示登录视图，点击登录按钮后触发登录 */
  private void showLoginView() {
    ALog.d(Constant.PROJECT_TAG, TAG, "showLoginView");
    activityWelcomeBinding.appDesc.setVisibility(View.GONE);
    activityWelcomeBinding.loginButton.setVisibility(View.VISIBLE);
    activityWelcomeBinding.appBottomIcon.setVisibility(View.GONE);
    activityWelcomeBinding.appBottomName.setVisibility(View.GONE);
    activityWelcomeBinding.tvEmailLogin.setVisibility(View.GONE);
    activityWelcomeBinding.tvServerConfig.setVisibility(View.VISIBLE);
    activityWelcomeBinding.vEmailLine.setVisibility(View.GONE);

    // OpenClaw 智能体配置入口
    if (activityWelcomeBinding.tvAgentConfig != null) {
      activityWelcomeBinding.tvAgentConfig.setVisibility(View.VISIBLE);
      activityWelcomeBinding.tvAgentConfig.setOnClickListener(
          view -> {
            Intent intent = new Intent(WelcomeActivity.this, ConfigInfoActivity.class);
            startActivity(intent);
          });
    }

    activityWelcomeBinding.tvServerConfig.setOnClickListener(
        view -> {
          Intent intent = new Intent(WelcomeActivity.this, ServerActivity.class);
          startActivity(intent);
        });

    // 登录按钮：点击后优先从配置中读取账号，fallback 到默认值
    activityWelcomeBinding.loginButton.setOnClickListener(view -> performLogin());
  }

  /**
   * 执行登录：优先使用 ConfigDataUtils 中保存的账号/Token，
   * 若未配置则使用默认值。
   */
  private void performLogin() {
    String account = ConfigDataUtils.getAccount(this);
    String token = ConfigDataUtils.getToken(this);

    // fallback 到硬编码默认值
    if (TextUtils.isEmpty(account) || TextUtils.isEmpty(token)) {
      ALog.d(Constant.PROJECT_TAG, TAG, "performLogin: no config found, use default account");
      account = DEFAULT_ACCOUNT;
      token = DEFAULT_TOKEN;
    } else {
      ALog.d(Constant.PROJECT_TAG, TAG, "performLogin: use config account");
    }

    loginQChat(account, token);
  }

  /** 设置登录中状态（禁用按钮，防止重复点击）*/
  private void setLoginLoading(boolean loading) {
    activityWelcomeBinding.loginButton.setEnabled(!loading);
    activityWelcomeBinding.loginButton.setText(
        loading ? getString(R.string.logging_in) : getString(R.string.welcome_button));
  }

  /** when your own page login success, you should login IM SDK */
  private void loginQChat(String account, String token) {
    ALog.d(Constant.PROJECT_TAG, TAG, "loginIM");
    setLoginLoading(true);
    QChatKitClient.login(
        account,
        token,
        null,
        new FetchCallback<Void>() {
          @Override
          public void onSuccess(@Nullable Void unused) {
            showMainActivityAndFinish();
          }

          @Override
          public void onError(int errorCode, @NonNull String errorMsg) {
            ToastX.showShortToast(
                String.format(getResources().getString(R.string.login_fail), errorCode));
            setLoginLoading(false);
          }
        });
  }
}
