// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.server;

import static com.netease.yunxin.kit.corekit.im.utils.RouterConstant.REQUEST_CONTACT_SELECTOR_KEY;
import static com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant.MEMBER_OPERATOR_ACCID;
import static com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant.MEMBER_OPERATOR_TYPE;
import static com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant.MEMBER_OPERATOR_TYPE_CHANGED;
import static com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant.MEMBER_OPERATOR_TYPE_DELETE;
import static com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant.MEMBER_OPERATOR_TYPE_UNCHANGED;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.netease.yunxin.kit.alog.ALog;
import com.netease.yunxin.kit.common.utils.SizeUtils;
import com.netease.yunxin.kit.corekit.im.provider.FetchCallback;
import com.netease.yunxin.kit.corekit.im.utils.RouterConstant;
import com.netease.yunxin.kit.corekit.route.XKitRouter;
import com.netease.yunxin.kit.qchatkit.repo.QChatServerRepo;
import com.netease.yunxin.kit.qchatkit.repo.model.NextInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerMemberWithRoleInfo;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.common.LoadMoreRecyclerViewDecorator;
import com.netease.yunxin.kit.qchatkit.ui.common.QChatServerBaseActivity;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatServerMbemberInfoWithRoleItemBinding;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatServerMemberListActivityBinding;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant;
import com.netease.yunxin.kit.qchatkit.ui.server.adapter.QChatServerMemberAdapter;
import com.netease.yunxin.kit.qchatkit.ui.utils.QChatUtils;
import java.util.ArrayList;
import java.util.List;

/** 社区成员列表页面 */
public class QChatServerMemberListActivity extends QChatServerBaseActivity {
  private static final String TAG = "QChatServerMemberListActivity";
  private static final String KEY_PARAM_SERVER_ID = "key_param_server_id";
  private static final int LOAD_MORE_LIMIT = 100;
  private static final int DEFAULT_DP_TIP_TRANSLATION_Y = 55;

  private long serverId;
  private QChatServerMemberListActivityBinding binding;
  private QChatServerMemberAdapter adapter;

  private final AnimatorSet animatorSet = new AnimatorSet();

  private ActivityResultLauncher<Intent> launcher;
  private LoadMoreRecyclerViewDecorator<QChatServerMemberWithRoleInfo> decorator;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = QChatServerMemberListActivityBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    serverId = getIntent().getLongExtra(KEY_PARAM_SERVER_ID, -1);
    if (serverId < 0) {
      Toast.makeText(this, "error serverId is -1", Toast.LENGTH_SHORT).show();
      finish();
      return;
    }
    configServerId(serverId);

    initView();

    // 处理添加成员页面返回的用户列表，批量邀请成员或删除成员等
    launcher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
              if (isFinishing()) {
                return;
              }
              if (result.getResultCode() != RESULT_OK) {
                return;
              }
              Intent data = result.getData();
              if (data != null) {
                ArrayList<String> friends =
                    data.getStringArrayListExtra(REQUEST_CONTACT_SELECTOR_KEY);
                if (friends != null && !friends.isEmpty()) {
                  QChatUtils.isConnectedToastAndRun(
                      this,
                      () ->
                          QChatServerRepo.inviteServerMembers(
                              serverId,
                              friends,
                              new FetchCallback<List<String>>() {
                                @Override
                                public void onSuccess(@Nullable List<String> failedList) {
                                  handleInviteTipVisible(true);
                                  loadMore(0, true);
                                  if (failedList != null && !failedList.isEmpty()) {
                                    Toast.makeText(
                                            QChatServerMemberListActivity.this,
                                            R.string.qchat_server_request_fail,
                                            Toast.LENGTH_SHORT)
                                        .show();
                                  }
                                }

                                @Override
                                public void onFailed(int code) {
                                  String msg = "invite failed, code is " + code;
                                  ALog.w(TAG, msg);
                                  if (code == QChatConstant.ERROR_CODE_IM_NO_PERMISSION) {
                                    Toast.makeText(
                                            QChatServerMemberListActivity.this,
                                            R.string.qchat_no_permission,
                                            Toast.LENGTH_SHORT)
                                        .show();
                                  }
                                }

                                @Override
                                public void onException(@Nullable Throwable exception) {
                                  String msg = "invite failed, exceptions is " + exception;
                                  ALog.w(TAG, msg);
                                  Toast.makeText(
                                          QChatServerMemberListActivity.this,
                                          msg,
                                          Toast.LENGTH_SHORT)
                                      .show();
                                }
                              }));
                } else {
                  int type = data.getIntExtra(MEMBER_OPERATOR_TYPE, MEMBER_OPERATOR_TYPE_UNCHANGED);
                  String accId = data.getStringExtra(MEMBER_OPERATOR_ACCID);
                  if (type == MEMBER_OPERATOR_TYPE_CHANGED) {
                    QChatServerRepo.fetchServerMemberInfoWithRolesByAccId(
                        serverId,
                        accId,
                        new FetchCallback<QChatServerMemberWithRoleInfo>() {
                          @Override
                          public void onSuccess(@Nullable QChatServerMemberWithRoleInfo param) {
                            adapter.updateData(param);
                          }

                          @Override
                          public void onFailed(int code) {
                            ALog.w(TAG, "update userinfo failed. Code is " + code);
                          }

                          @Override
                          public void onException(@Nullable Throwable exception) {
                            ALog.w(TAG, "update userinfo failed. Exception is " + exception);
                          }
                        });
                  } else if (type == MEMBER_OPERATOR_TYPE_DELETE) {
                    adapter.removeData(new QChatServerMemberWithRoleInfo(serverId, accId));
                  }
                }
              }
            });
  }

  @Override
  protected void onPause() {
    super.onPause();
    handleInviteTipVisible(false);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    animatorSet.removeAllListeners();
  }

  private void initView() {
    binding.ivBack.setOnClickListener(v -> finish());

    binding.ivInviteMember.setOnClickListener(
        v ->
            QChatUtils.isConnectedToastAndRun(
                this,
                () -> {
                  // 跳转通讯录选择用户进入该社区
                  XKitRouter.withKey(RouterConstant.PATH_CONTACT_SELECTOR_PAGE)
                      .withContext(this)
                      .withParam(
                          RouterConstant.SELECTOR_CONTACT_FILTER_KEY,
                          adapter.getDataOnlyAccIdList())
                      .navigate(launcher);
                }));

    adapter = new QChatServerMemberAdapter(this, QChatServerMbemberInfoWithRoleItemBinding.class);
    adapter.setItemClickListener(
        (data, holder) -> {
          QChatUtils.isConnectedToastAndRun(
              this,
              () -> {
                // 跳转成员详情页面
                Intent intent =
                    new Intent(
                        QChatServerMemberListActivity.this, QChatServerMemberInfoActivity.class);
                intent.putExtra(QChatConstant.SERVER_MEMBER, data);
                launcher.launch(intent);
              });
        });

    LinearLayoutManager layoutManager =
        new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
    binding.ryMemberList.setLayoutManager(layoutManager);
    binding.ryMemberList.setAdapter(adapter);

    decorator = new LoadMoreRecyclerViewDecorator<>(binding.ryMemberList, layoutManager, adapter);
    decorator.setLoadMoreListener(
        data -> {
          NextInfo info;
          if (data == null || data.getNextInfo() == null) {
            info = null;
          } else {
            info = data.getNextInfo();
          }
          if (info != null && !info.getHasMore()) {
            return;
          }
          long timeTag = info != null ? info.getNextTimeTag() : 0;
          if (timeTag == 0) {
            return;
          }
          loadMore(timeTag, false);
        });

    QChatUtils.isConnectedToastAndRun(this, () -> loadMore(0, true));

    prepareAnim();
  }

  private void loadMore(long timeTag, boolean clear) {
    QChatServerRepo.fetchServerMemberInfoWithRolesList(
        serverId,
        timeTag,
        LOAD_MORE_LIMIT,
        new FetchCallback<List<QChatServerMemberWithRoleInfo>>() {
          @Override
          public void onSuccess(@Nullable List<QChatServerMemberWithRoleInfo> param) {
            adapter.addDataList(param, clear);
            if (decorator != null) {
              if (clear) {
                decorator.init();
              }
              decorator.notifyResult(true);
            }
          }

          public void onFailed(int code) {
            Toast.makeText(
                    getApplicationContext(),
                    getString(R.string.qchat_server_request_fail) + code,
                    Toast.LENGTH_SHORT)
                .show();
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            Toast.makeText(
                    getApplicationContext(),
                    getString(R.string.qchat_server_request_fail) + exception,
                    Toast.LENGTH_SHORT)
                .show();
          }
        });
  }

  /**
   * 页面启动方法
   *
   * @param context 上下文
   * @param serverId 对应的社区 id
   */
  public static void launch(Context context, long serverId) {
    Intent intent = new Intent(context, QChatServerMemberListActivity.class);
    intent.putExtra(KEY_PARAM_SERVER_ID, serverId);
    if (!(context instanceof Activity)) {
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
    context.startActivity(intent);
  }

  /** 处理邀请成员时展示的提示动画 */
  private void prepareAnim() {
    // the duration of executing anim is 0.5s
    ObjectAnimator showTransYAnimator =
        ObjectAnimator.ofFloat(
                binding.cvInviteMemberTip,
                "translationY",
                -SizeUtils.dp2px(DEFAULT_DP_TIP_TRANSLATION_Y),
                0f)
            .setDuration(500);
    ObjectAnimator dismissTransYAnimator =
        ObjectAnimator.ofFloat(
                binding.cvInviteMemberTip,
                "translationY",
                0f,
                -SizeUtils.dp2px(DEFAULT_DP_TIP_TRANSLATION_Y))
            .setDuration(500);
    animatorSet.play(showTransYAnimator);
    // the duration of showing tip is 1s.
    animatorSet.play(dismissTransYAnimator).after(1000);
    animatorSet.addListener(
        new AnimatorListenerAdapter() {
          @Override
          public void onAnimationStart(Animator animation) {
            super.onAnimationStart(animation);
            binding.cvInviteMemberTip.setVisibility(View.VISIBLE);
          }

          @Override
          public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
            binding.cvInviteMemberTip.setVisibility(View.GONE);
          }
        });
  }

  private void handleInviteTipVisible(boolean visible) {
    if (visible) {
      if (!animatorSet.isRunning()) {
        animatorSet.start();
      }
    } else {
      animatorSet.cancel();
    }
  }
}
