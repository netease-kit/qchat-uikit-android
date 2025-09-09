// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.server;

import static com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant.SERVER_INFO;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.netease.yunxin.kit.alog.ALog;
import com.netease.yunxin.kit.common.ui.dialog.ChoiceListener;
import com.netease.yunxin.kit.common.ui.dialog.CommonChoiceDialog;
import com.netease.yunxin.kit.common.ui.photo.PhotoChoiceDialog;
import com.netease.yunxin.kit.common.ui.utils.AvatarColor;
import com.netease.yunxin.kit.common.ui.utils.CommonCallback;
import com.netease.yunxin.kit.common.ui.utils.ToastX;
import com.netease.yunxin.kit.common.utils.NetworkUtils;
import com.netease.yunxin.kit.corekit.im2.IMKitClient;
import com.netease.yunxin.kit.corekit.im2.extend.FetchCallback;
import com.netease.yunxin.kit.qchatkit.repo.QChatServerRepo;
import com.netease.yunxin.kit.qchatkit.repo.ResourceRepo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerInfo;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.common.QChatCallback;
import com.netease.yunxin.kit.qchatkit.ui.common.QChatServerCommonBaseActivity;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatServerSettingActivityLayoutBinding;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant;
import com.netease.yunxin.kit.qchatkit.ui.utils.QChatUtils;
import java.io.File;
import org.json.JSONException;
import org.json.JSONObject;

/** 社区设置页面 */
public class QChatServerSettingActivity extends QChatServerCommonBaseActivity {

  public static final String TAG = "QChatServerSettingActivity";

  private QChatServerSettingActivityLayoutBinding binding;

  private QChatServerInfo serverInfo;

  private String avatarUrl;

  private InputMethodManager manager;

  @Override
  public void initView() {
    changeStatusBarColor(R.color.color_eef1f4);
    manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    binding
        .title
        .setTitle(R.string.qchat_server_setting)
        .setOnBackIconClickListener(v -> onBackPressed())
        .setActionText(R.string.qchat_save)
        .setLeftText(R.string.qchat_close)
        .setActionTextColor(getResources().getColor(R.color.color_337eff))
        .setActionListener(
            v -> {
              if (!NetworkUtils.isConnected()) {
                Toast.makeText(this, R.string.qchat_network_error_tip, Toast.LENGTH_SHORT).show();
                return;
              }
              String name = binding.edtServerName.getText().toString().trim();
              if (TextUtils.isEmpty(name)) {
                Toast.makeText(
                        this, R.string.qchat_update_server_empty_name_tip, Toast.LENGTH_SHORT)
                    .show();
                return;
              }
              String topic = binding.edtServerTopic.getText().toString().trim();
              JSONObject jsonObject = new JSONObject();
              try {
                jsonObject.put("topic", topic);
              } catch (JSONException e) {
                e.printStackTrace();
              }
              // 更新社区信息
              QChatServerRepo.updateServer(
                  serverInfo.getServerId(),
                  name,
                  avatarUrl,
                  jsonObject.toString(),
                  new QChatCallback<Void>(getApplicationContext()) {
                    @Override
                    public void onSuccess(@Nullable Void param) {
                      super.onSuccess(param);
                      finish();
                    }
                  });
            });

    binding.rlyMember.setOnClickListener(
        v ->
            QChatUtils.isConnectedToastAndRun(
                this,
                () -> {
                  QChatServerMemberListActivity.launch(this, serverInfo.getServerId());
                }));

    binding.rlyTeam.setOnClickListener(
        v -> {
          QChatUtils.isConnectedToastAndRun(
              this,
              () -> {
                Intent intent =
                    new Intent(QChatServerSettingActivity.this, QChatRoleListActivity.class);
                intent.putExtra(SERVER_INFO, serverInfo);
                startActivity(intent);
              });
        });

    binding.avatar.setOnClickListener(this::gotoPicture);
    binding.ivCamera.setOnClickListener(this::gotoPicture);
  }

  /** 选择社区头像 */
  private void gotoPicture(View v) {
    manager.hideSoftInputFromWindow(v.getWindowToken(), 0);
    new PhotoChoiceDialog(this)
        .show(
            new CommonCallback<File>() {
              @Override
              public void onSuccess(@Nullable File param) {
                if (NetworkUtils.isConnected()) {
                  // 头像上传
                  ResourceRepo.uploadFile(
                      param,
                      new FetchCallback<String>() {
                        @Override
                        public void onSuccess(@Nullable String param) {
                          //获取长传成功的头像url
                          avatarUrl = param;
                          binding.avatar.setData(
                              param,
                              serverInfo.getName(),
                              AvatarColor.avatarColor(serverInfo.getServerId()));
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
                          ALog.e(TAG, "upload icon failed code = " + code);
                        }
                      });
                } else {
                  ToastX.showShortToast(R.string.qchat_network_error_tip);
                }
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
                ALog.e(TAG, "upload icon failed code = " + code);
              }

              @Override
              public void onException(@Nullable Throwable exception) {
                Toast.makeText(
                        getApplicationContext(),
                        getString(R.string.qchat_server_request_fail),
                        Toast.LENGTH_SHORT)
                    .show();
                ALog.e(TAG, "upload icon", exception);
              }
            });
  }

  @Override
  public void initData() {
    serverInfo = (QChatServerInfo) getIntent().getSerializableExtra(SERVER_INFO);
    if (serverInfo == null) {
      return;
    }
    configServerId(serverInfo.getServerId());
    boolean isOwner = TextUtils.equals(serverInfo.getOwner(), IMKitClient.account());
    if (isOwner) {
      binding.tvDelete.setText(R.string.qchat_delete_server);
    } else {
      binding.tvDelete.setText(R.string.qchat_leave_server);
    }
    binding.tvDelete.setOnClickListener(v -> showConfirmDialog(isOwner));
    binding.avatar.setData(
        serverInfo.getIconUrl(),
        serverInfo.getName(),
        AvatarColor.avatarColor(serverInfo.getServerId()));
    binding.tvServerName.setText(serverInfo.getName());
    binding.tvId.setText(
        String.format(getString(R.string.qchat_server_id), serverInfo.getServerId()));
    binding.edtServerName.setText(serverInfo.getName());
    if (serverInfo.getCustom() != null) {
      try {
        JSONObject jsonObject = new JSONObject(serverInfo.getCustom());
        binding.edtServerTopic.setText(jsonObject.getString("topic"));
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * 展示删除/退出社区的确认弹窗
   *
   * @param isOwner true 展示删除弹窗；false 展示退出社区弹窗
   */
  private void showConfirmDialog(boolean isOwner) {
    CommonChoiceDialog commonConfirmDialog = new CommonChoiceDialog();
    commonConfirmDialog
        .setTitleStr(
            isOwner
                ? getString(R.string.qchat_delete_server)
                : getString(R.string.qchat_leave_server))
        .setContentStr(
            isOwner
                ? getString(R.string.qchat_delete_server_content)
                : getString(R.string.qchat_leave_server_content))
        .setPositiveStr(getString(isOwner ? R.string.qchat_delete : R.string.qchat_leave))
        .setNegativeStr(getString(R.string.qchat_cancel))
        .setConfirmListener(
            new ChoiceListener() {
              @Override
              public void onNegative() {
                //do nothing
              }

              @Override
              public void onPositive() {
                QChatUtils.isConnectedToastAndRun(
                    QChatServerSettingActivity.this,
                    () -> {
                      QChatCallback<Void> callback =
                          new QChatCallback<Void>(getApplicationContext()) {
                            @Override
                            public void onSuccess(@Nullable Void param) {
                              super.onSuccess(param);
                              finish();
                            }
                          };
                      if (isOwner) {
                        QChatServerRepo.deleteServer(serverInfo.getServerId(), callback);
                      } else {
                        QChatServerRepo.leaveServer(serverInfo.getServerId(), callback);
                      }
                    });
              }
            })
        .show(getSupportFragmentManager());
  }

  @NonNull
  @Override
  public View getContentView() {
    binding = QChatServerSettingActivityLayoutBinding.inflate(getLayoutInflater());
    return binding.getRoot();
  }

  @Override
  protected void initViewModel() {}

  /**
   * 页面启动方法
   *
   * @param activity 页面启动 activity
   * @param data 社区信息
   */
  public static void launch(Activity activity, QChatServerInfo data) {
    Intent intent = new Intent(activity, QChatServerSettingActivity.class);
    intent.putExtra(QChatConstant.SERVER_INFO, data);
    activity.startActivity(intent);
  }
}
