// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.announce;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import com.netease.yunxin.kit.common.ui.activities.BaseActivity;
import com.netease.yunxin.kit.common.utils.NetworkUtils;
import com.netease.yunxin.kit.corekit.im.IMKitClient;
import com.netease.yunxin.kit.corekit.model.ResultInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerInfo;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.announce.viewmodel.UpdateInfoViewModel;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatAnnounceUpdateInfoActivityBinding;
import com.netease.yunxin.kit.qchatkit.ui.utils.QChatUtils;

/** 公告频道信息更新页面（名称/说明） */
public class UpdateInfoActivity extends BaseActivity {
  public static final String KEY_PARAM_SERVER_INFO = "key_param_server_info";
  public static final String KEY_PARAM_UPDATE_TYPE = "key_param_update_type";
  public static final String KEY_PARAM_UPDATE_VALUE = "key_param_update_value";
  /** 更新公告频道名称 */
  public static final int UPDATE_TYPE_NAME = 1;
  /** 更新公告频道说明 */
  public static final int UPDATE_TYPE_TOPIC = 2;

  private static final String MAX_COUNT_NAME_STR = "/50";
  private static final String MAX_COUNT_TOPIC_STR = "/64";

  private final UpdateInfoViewModel viewModel = new UpdateInfoViewModel();

  private int updateType;
  private QChatServerInfo serverInfo;
  private String maxCountStr = null;
  private boolean canUpdate = false;

  private View cancelView;
  private View ivClear;
  private TextView tvTitle;
  private TextView tvFlag;
  private TextView tvSave;
  private EditText etInfo;
  private String toUpdateInfo;

  private boolean hasPermission = false;

  /** 监听权限校验通知 */
  private final Observer<Boolean> observerForPermission =
      aBoolean -> {
        hasPermission = aBoolean;
        updatePermissionUI();
      };

  /** 监听公告频道信息变更通知 */
  private final Observer<ResultInfo<Void>> observerForUpdateInfo =
      result -> {
        if (!result.getSuccess()) {
          if (!NetworkUtils.isConnected()) {
            Toast.makeText(this, R.string.qchat_network_error_tip, Toast.LENGTH_SHORT).show();
          } else {
            Toast.makeText(this, R.string.qchat_server_request_fail, Toast.LENGTH_SHORT).show();
          }
          return;
        }
        if (!TextUtils.equals(toUpdateInfo, String.valueOf(etInfo.getText()))) {
          canUpdate = true;
        }
        toUpdateInfo = String.valueOf(etInfo.getText());
        finish();
      };

  /** 监听页面关闭通知 */
  private final Observer<Object> observerForFinish = o -> finish();

  @SuppressLint("SetTextI18n")
  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    View rootView = initViewAndGetRootView();
    setContentView(rootView);

    changeStatusBarColor(R.color.color_eff1f4);
    serverInfo = (QChatServerInfo) getIntent().getSerializableExtra(KEY_PARAM_SERVER_INFO);
    updateType = getIntent().getIntExtra(KEY_PARAM_UPDATE_TYPE, UPDATE_TYPE_NAME);

    if (serverInfo == null || (updateType != UPDATE_TYPE_NAME && updateType != UPDATE_TYPE_TOPIC)) {
      finish();
      return;
    }

    QChatServerInfo.AnnouncementInfo announcementInfo = serverInfo.getAnnouncementInfo();
    if (announcementInfo == null) {
      finish();
      return;
    }

    viewModel.config(serverInfo.getServerId(), announcementInfo.getChannelId());
    viewModel.getPermissionResult().observeForever(observerForPermission);
    viewModel.getUpdateInfoResult().observeForever(observerForUpdateInfo);
    viewModel.getFinishResult().observeForever(observerForFinish);

    cancelView.setOnClickListener(v -> finish());

    if (updateType == UPDATE_TYPE_NAME) {
      maxCountStr = MAX_COUNT_NAME_STR;
      toUpdateInfo = serverInfo.getName();
      tvTitle.setText(R.string.qchat_announcement_update_name_title);
      tvSave.setOnClickListener(
          v ->
              QChatUtils.isConnectedToastAndRun(
                  this, () -> viewModel.updateName(serverInfo, String.valueOf(etInfo.getText()))));
      etInfo.setFilters(new InputFilter[] {new InputFilter.LengthFilter(50)});
    } else {
      maxCountStr = MAX_COUNT_TOPIC_STR;
      toUpdateInfo = serverInfo.getDesc();
      tvTitle.setText(R.string.qchat_announcement_update_topic_title);
      tvSave.setOnClickListener(
          v -> viewModel.updateTopic(serverInfo, String.valueOf(etInfo.getText())));
      etInfo.setFilters(new InputFilter[] {new InputFilter.LengthFilter(64)});
    }
    tvFlag.setText(String.valueOf(toUpdateInfo).length() + maxCountStr);

    if (!TextUtils.isEmpty(toUpdateInfo)) {
      etInfo.setText(toUpdateInfo);
      ivClear.setVisibility(View.VISIBLE);
      tvFlag.setText(toUpdateInfo.length() + maxCountStr);
    }

    etInfo.requestFocus();
    ivClear.setOnClickListener(v -> etInfo.setText(""));
    etInfo.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {}

          @Override
          public void afterTextChanged(Editable s) {
            if (s == null) {
              return;
            }
            if (updateType == UPDATE_TYPE_NAME && TextUtils.isEmpty(String.valueOf(s).trim())) {
              ivClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
              tvSave.setAlpha(0.5f);
              tvSave.setEnabled(false);
            } else if (updateType == UPDATE_TYPE_TOPIC
                && TextUtils.isEmpty(String.valueOf(s).trim())) {
              ivClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            } else {
              ivClear.setVisibility(View.VISIBLE);
              tvSave.setAlpha(1f);
              tvSave.setEnabled(true);
            }
            tvFlag.setText(String.valueOf(s).length() + maxCountStr);
          }
        });
    updatePermissionUI();
    QChatUtils.isConnectedToastAndRun(this, viewModel::checkPermission);
  }

  /** 根据权限决定是否展示编辑按钮及输入框是否可编辑 */
  private void updatePermissionUI() {
    if (hasPermission || TextUtils.equals(serverInfo.getOwner(), IMKitClient.account())) {
      tvSave.setVisibility(View.VISIBLE);
      etInfo.setEnabled(true);
      if (!TextUtils.isEmpty(etInfo.getText().toString().trim())) {
        ivClear.setVisibility(View.VISIBLE);
      }
    } else {
      tvSave.setVisibility(View.GONE);
      etInfo.setEnabled(false);
      ivClear.setVisibility(View.GONE);
    }
  }

  private View initViewAndGetRootView() {
    QChatAnnounceUpdateInfoActivityBinding binding =
        QChatAnnounceUpdateInfoActivityBinding.inflate(getLayoutInflater());
    cancelView = binding.tvCancel;
    ivClear = binding.ivClear;
    tvTitle = binding.tvTitle;
    tvFlag = binding.tvFlag;
    tvSave = binding.tvSave;
    etInfo = binding.etInfo;
    return binding.getRoot();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    viewModel.getPermissionResult().removeObserver(observerForPermission);
    viewModel.getUpdateInfoResult().removeObserver(observerForUpdateInfo);
    viewModel.getFinishResult().removeObserver(observerForFinish);
  }

  @Override
  public void finish() {
    if (hasPermission && canUpdate) {
      Intent intent = new Intent();
      intent.putExtra(KEY_PARAM_UPDATE_VALUE, toUpdateInfo);
      setResult(RESULT_OK, intent);
    }
    super.finish();
  }

  /**
   * 页面启动方法
   *
   * @param context 上下问
   * @param serverInfo 公告频道信息
   * @param updateType 更新类型{@link #UPDATE_TYPE_NAME}及{@link #UPDATE_TYPE_TOPIC}
   * @param launcher 页面启动launcher，用于通知启动方数据
   */
  public static void launch(
      Context context,
      QChatServerInfo serverInfo,
      int updateType,
      ActivityResultLauncher<Intent> launcher) {
    Intent intent = new Intent(context, UpdateInfoActivity.class);
    intent.putExtra(KEY_PARAM_SERVER_INFO, serverInfo);
    intent.putExtra(KEY_PARAM_UPDATE_TYPE, updateType);
    if (!(context instanceof Activity)) {
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
    launcher.launch(intent);
  }

  /**
   * 页面启动方法
   *
   * @param activity 页面启动 activity
   * @param serverInfo 公告频道信息
   * @param updateType 更新类型{@link #UPDATE_TYPE_NAME}及{@link #UPDATE_TYPE_TOPIC}
   * @param requestCode 页面启动方通过 requestCode 在 {@link #onActivityResult(int, int, Intent)} 方法中区别回调数据
   */
  public static void launch(
      Activity activity, QChatServerInfo serverInfo, int updateType, int requestCode) {
    Intent intent = new Intent(activity, UpdateInfoActivity.class);
    intent.putExtra(KEY_PARAM_SERVER_INFO, serverInfo);
    intent.putExtra(KEY_PARAM_UPDATE_TYPE, updateType);
    activity.startActivityForResult(intent, requestCode);
  }
}
