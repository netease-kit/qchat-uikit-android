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
import android.util.Pair;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.netease.nimlib.sdk.qchat.result.QChatApplyServerJoinResult;
import com.netease.yunxin.kit.alog.ALog;
import com.netease.yunxin.kit.common.ui.activities.BaseActivity;
import com.netease.yunxin.kit.corekit.event.EventCenter;
import com.netease.yunxin.kit.corekit.im2.IMKitClient;
import com.netease.yunxin.kit.corekit.im2.extend.FetchCallback;
import com.netease.yunxin.kit.corekit.model.ResultInfo;
import com.netease.yunxin.kit.qchatkit.repo.QChatServerRepo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSearchResultInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerMemberInfo;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatJoinOtherServerActivityBinding;
import com.netease.yunxin.kit.qchatkit.ui.server.adapter.QChatSearchResultAdapter;
import com.netease.yunxin.kit.qchatkit.ui.server.model.QChatEnterServerEvent;
import com.netease.yunxin.kit.qchatkit.ui.server.viewmodel.QChatServerCreateViewModel;
import com.netease.yunxin.kit.qchatkit.ui.utils.QChatUtils;
import java.util.Collections;
import java.util.List;

/** 社区/公告频道查询页面 */
public class QChatJoinOtherServerActivity extends BaseActivity {
  private static final String TAG = "QChatJoinOtherServerActivity";
  private static final String KEY_PARAM_IS_ANNOUNCEMENT = "key_is_announcement";
  private final QChatServerCreateViewModel viewModel = new QChatServerCreateViewModel();
  private QChatJoinOtherServerActivityBinding binding;
  private QChatSearchResultAdapter adapter;
  private boolean isAnnouncement = false;

  /** 监听加入请求通知 */
  private final Observer<Pair<QChatSearchResultInfo, ResultInfo<QChatApplyServerJoinResult>>>
      observerForJoin =
          result -> {
            if (result.second.getSuccess()) {
              result.first.state = QChatSearchResultInfo.STATE_JOINED;
              adapter.updateItemState();
              Toast.makeText(
                      QChatJoinOtherServerActivity.this,
                      R.string.qchat_server_had_appled_tip,
                      Toast.LENGTH_SHORT)
                  .show();
            } else {
              Toast.makeText(
                      getApplicationContext(),
                      getString(R.string.qchat_server_request_fail),
                      Toast.LENGTH_SHORT)
                  .show();
              ALog.e(TAG, "join failed. " + result.second.getMsg());
            }
          };

  /** 监听查询社区结果通知 */
  private final Observer<ResultInfo<List<QChatSearchResultInfo>>> observerForSearch =
      result -> {
        if (result.getSuccess()) {
          List<QChatSearchResultInfo> serverList = result.getValue();
          if (serverList != null && !serverList.isEmpty()) {
            binding.groupNoServerTip.setVisibility(View.GONE);
            adapter.addDataList(serverList, true);
          } else {
            binding.groupNoServerTip.setVisibility(View.VISIBLE);
            adapter.addDataList(Collections.emptyList(), true);
          }
        } else {
          Toast.makeText(
                  getApplicationContext(),
                  getString(R.string.qchat_server_request_fail),
                  Toast.LENGTH_SHORT)
              .show();
          ALog.e(TAG, "search failed. " + result.getMsg());
        }
      };

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = QChatJoinOtherServerActivityBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    isAnnouncement = getIntent().getBooleanExtra(KEY_PARAM_IS_ANNOUNCEMENT, false);
    viewModel.getSearchServerResult().observeForever(observerForSearch);
    viewModel.getJoinServerResult().observeForever(observerForJoin);
    initView();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    viewModel.getSearchServerResult().removeObserver(observerForSearch);
    viewModel.getJoinServerResult().removeObserver(observerForJoin);
  }

  private void initView() {
    binding.tvTitle.setText(
        isAnnouncement ? R.string.qchat_crate_by_other_announce : R.string.qchat_crate_by_other);
    binding.etServerID.setHint(
        isAnnouncement ? R.string.qchat_announcement_search_tip : R.string.qchat_search_tip);
    binding.ivBack.setOnClickListener(v -> finish());
    binding.etServerID.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {}

          @Override
          public void afterTextChanged(Editable s) {
            if (TextUtils.isEmpty(String.valueOf(s))) {
              binding.ivClear.setVisibility(View.GONE);
            } else {
              binding.ivClear.setVisibility(View.VISIBLE);
            }
          }
        });
    // do search action.
    binding.etServerID.setOnEditorActionListener(
        (v, actionId, event) -> {
          if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            InputMethodManager manager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(v.getWindowToken(), 0);
            Long serverId = getServerIdFromEdit();
            if (serverId == null) {
              binding.groupNoServerTip.setVisibility(View.VISIBLE);
              adapter.addDataList(Collections.emptyList(), true);
            } else {
              QChatUtils.isConnectedToastAndRun(
                  this, () -> viewModel.searchServer(isAnnouncement, serverId));
            }
            return true;
          }
          return false;
        });

    binding.ivClear.setOnClickListener(v -> binding.etServerID.setText(null));

    adapter = new QChatSearchResultAdapter(this, isAnnouncement);
    // 设置点击监听
    adapter.setItemClickListener(
        (data, holder) -> {
          QChatUtils.isConnectedToastAndRun(
              this,
              () -> {
                // 如果为公告频道则判断是否为已经加入该频道，如果已经加入则可以跳转
                if (isAnnouncement) {
                  QChatServerRepo.getServerMembers(
                      Collections.singletonList(
                          new Pair<>(data.serverInfo.getServerId(), IMKitClient.account())),
                      new FetchCallback<List<QChatServerMemberInfo>>() {
                        @Override
                        public void onSuccess(@Nullable List<QChatServerMemberInfo> param) {
                          if (param != null && !param.isEmpty()) {
                            EventCenter.notifyEvent(new QChatEnterServerEvent(data.serverInfo));
                            setResult(RESULT_OK);
                            finish();
                          }
                        }

                        @Override
                        public void onError(int code, String msg) {
                          ALog.e(TAG, "getServerMembers failed. " + msg);
                        }
                      });
                } else {
                  // 如果为普通社区，则进入社区列表
                  EventCenter.notifyEvent(new QChatEnterServerEvent(data.serverInfo));
                  setResult(RESULT_OK);
                  finish();
                }
              });
        });
    adapter.setTipClickListener(
        (data, holder) ->
            QChatUtils.isConnectedToastAndRun(this, () -> viewModel.joinServer(data)));
    binding.ryServerList.setAdapter(adapter);
    binding.ryServerList.setLayoutManager(
        new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
  }

  private Long getServerIdFromEdit() {
    Editable editable = binding.etServerID.getText();
    if (editable == null) {
      return null;
    }
    Long result = null;
    try {
      result = Long.parseLong(editable.toString());
    } catch (NumberFormatException exception) {
      ALog.e(TAG, "getServerIdFromEdit", exception);
    }
    return result;
  }

  @Override
  public void finish() {
    super.finish();
    overridePendingTransition(R.anim.anim_empty_with_time, R.anim.anim_from_start_to_end);
  }

  /**
   * 页面启动方法
   *
   * @param launcher 启动 launcher， 用户当前页面将数据通知启动方
   * @param context 上下文
   * @param isAnnouncement 是否为公告频道
   */
  public static void launch(
      ActivityResultLauncher<Intent> launcher, Context context, boolean isAnnouncement) {
    Intent intent = new Intent(context, QChatJoinOtherServerActivity.class);
    intent.putExtra(KEY_PARAM_IS_ANNOUNCEMENT, isAnnouncement);
    launcher.launch(intent);
  }
}
