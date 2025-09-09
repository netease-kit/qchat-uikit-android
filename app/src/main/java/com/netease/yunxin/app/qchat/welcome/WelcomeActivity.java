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

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ALog.d(Constant.PROJECT_TAG, TAG, "onCreateView");
    QChatApplication.setColdStart(true);
    activityWelcomeBinding = ActivityWelcomeBinding.inflate(getLayoutInflater());
    setContentView(activityWelcomeBinding.getRoot());
    startLogin();
  }

  private void showMainActivityAndFinish() {
    ALog.d(Constant.PROJECT_TAG, TAG, "showMainActivityAndFinish");
    Intent intent = new Intent();
    intent.setClass(this, MainActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    this.startActivity(intent);
    finish();
  }

  /** start login page, you can use to launch your own login */
  private void startLogin() {
    ALog.d(Constant.PROJECT_TAG, TAG, "startLogin");

      //填入你的 account and token
      String account = "";
      String token = "";

      if (!TextUtils.isEmpty(account) && !TextUtils.isEmpty(token)) {
          loginQChat(account,token);
      } else {
          showLoginView();
      }
  }

  private void showLoginView() {
    ALog.d(Constant.PROJECT_TAG, TAG, "showLoginView");
    activityWelcomeBinding.appDesc.setVisibility(View.GONE);
    activityWelcomeBinding.loginButton.setVisibility(View.VISIBLE);
    activityWelcomeBinding.appBottomIcon.setVisibility(View.GONE);
    activityWelcomeBinding.appBottomName.setVisibility(View.GONE);
    activityWelcomeBinding.tvEmailLogin.setVisibility(View.VISIBLE);
    activityWelcomeBinding.tvServerConfig.setVisibility(View.VISIBLE);
    activityWelcomeBinding.vEmailLine.setVisibility(View.VISIBLE);
    activityWelcomeBinding.loginButton.setOnClickListener(
        view -> {

        });
    activityWelcomeBinding.tvEmailLogin.setOnClickListener(
        view -> {

        });
    activityWelcomeBinding.tvServerConfig.setOnClickListener(
        view -> {
          Intent intent = new Intent(WelcomeActivity.this, ServerActivity.class);
          startActivity(intent);
        });
  }

  /** launch login activity */
  private void launchLoginPage() {
    ALog.d(Constant.PROJECT_TAG, TAG, "launchLoginPage");
    activityWelcomeBinding.getRoot().setVisibility(View.VISIBLE);
  }

  /** when your own page login success, you should login IM SDK */
  private void loginQChat(String account, String token) {
    ALog.d(Constant.PROJECT_TAG, TAG, "loginIM");
    activityWelcomeBinding.getRoot().setVisibility(View.GONE);
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
            launchLoginPage();
          }
        });
  }
}
