// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.server;

import static com.netease.yunxin.kit.common.ui.activities.adapter.CommonMoreAdapterKt.DEFAULT_SHOW_SIZE;
import static com.netease.yunxin.kit.qchatkit.repo.QChatRoleRepo.MAX_ROLE_PAGE_SIZE;
import static com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant.MEMBER_OPERATOR_ACCID;
import static com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant.MEMBER_OPERATOR_TYPE;
import static com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant.MEMBER_OPERATOR_TYPE_CHANGED;
import static com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant.MEMBER_OPERATOR_TYPE_DELETE;
import static com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant.MEMBER_OPERATOR_TYPE_UNCHANGED;
import static com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant.MEMBER_TYPE_OWNER;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.netease.yunxin.kit.common.ui.activities.adapter.CommonMoreAdapter;
import com.netease.yunxin.kit.common.ui.activities.viewholder.BaseMoreViewHolder;
import com.netease.yunxin.kit.common.ui.dialog.ChoiceListener;
import com.netease.yunxin.kit.common.ui.dialog.CommonChoiceDialog;
import com.netease.yunxin.kit.common.ui.utils.AvatarColor;
import com.netease.yunxin.kit.common.utils.NetworkUtils;
import com.netease.yunxin.kit.common.utils.SizeUtils;
import com.netease.yunxin.kit.corekit.im.IMKitClient;
import com.netease.yunxin.kit.qchatkit.repo.QChatRoleRepo;
import com.netease.yunxin.kit.qchatkit.repo.QChatServerRepo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerMemberWithRoleInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerRoleInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerRoleResult;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.common.QChatCallback;
import com.netease.yunxin.kit.qchatkit.ui.common.QChatServerCommonBaseActivity;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatMemberInfoActivityLayoutBinding;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatRoleViewHolderBinding;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant;
import java.util.ArrayList;
import java.util.List;

/** 社区成员信息页面 */
public class QChatServerMemberInfoActivity extends QChatServerCommonBaseActivity {

  QChatMemberInfoActivityLayoutBinding binding;

  QChatServerMemberWithRoleInfo member;

  String nickname;

  CommonMoreAdapter<QChatServerRoleInfo, QChatRoleViewHolderBinding> rolesAdapter;

  private final List<QChatServerRoleInfo> joinedRoles = new ArrayList<>();

  boolean changed;

  boolean isEdit;

  int fullSize;

  @NonNull
  @Override
  public View getContentView() {
    binding = QChatMemberInfoActivityLayoutBinding.inflate(getLayoutInflater());
    return binding.getRoot();
  }

  @Override
  public void initView() {
    changeStatusBarColor(R.color.color_eef1f4);
    member =
        (QChatServerMemberWithRoleInfo)
            getIntent().getSerializableExtra(QChatConstant.SERVER_MEMBER);
    nickname = member.getNickName();
    if (member != null) {
      configServerId(member.getServerId());
    }
    binding
        .title
        .setOnBackIconClickListener(v -> onBackPressed())
        .setActionText(R.string.qchat_edit)
        .setActionTextColor(getResources().getColor(R.color.color_337eff))
        .setTitle(nickname)
        .setActionListener(
            v -> {
              if (!NetworkUtils.isConnected()) {
                Toast.makeText(this, R.string.qchat_network_error_tip, Toast.LENGTH_SHORT).show();
                return;
              }
              if (isEdit) {
                updateMemberInfo();
              } else {
                binding.lyMore.setVisibility(View.GONE);
                isEdit = true;
                binding.title.setActionText(R.string.qchat_save);
                rolesAdapter.setShowSub(true);
                getRoles();
                binding.edtMemberName.setEnableClear(true);
                binding.edtMemberName.setEditable(true);
                binding.edtMemberName.setHint(R.string.qchat_edit_nickname);
                if (member != null) {
                  binding.edtMemberName.setText(nickname);
                }
              }
            });
    binding.edtMemberName.setText(nickname);
    binding.edtMemberName.setEnableClear(false);
    binding.edtMemberName.setEditable(false);
    binding.tvKick.setText(
        String.format(getResources().getString(R.string.qchat_kick_someone), nickname));
    binding.tvKick.setOnClickListener(v -> showKickDialog());
    binding.lyMore.setOnClickListener(
        v -> {
          if (rolesAdapter.getShowSub()) {
            binding.tvMore.setText(
                String.format(getResources().getString(R.string.qchat_pack_up_with_all), fullSize));
            binding.ivDown.setImageDrawable(
                ResourcesCompat.getDrawable(getResources(), R.drawable.ic_up, null));
            rolesAdapter.packUp();
          } else {
            binding.tvMore.setText(
                String.format(getResources().getString(R.string.qchat_more_with_all), fullSize));
            binding.ivDown.setImageDrawable(
                ResourcesCompat.getDrawable(getResources(), R.drawable.ic_down, null));
            rolesAdapter.showSub();
          }
        });
  }

  /** 更新成员信息 */
  private void updateMemberInfo() {
    String nick = null;
    if (binding.edtMemberName.getText() != null) {
      nick = binding.edtMemberName.getText().trim();
    }
    if (TextUtils.isEmpty(nick)) {
      Toast.makeText(this, R.string.qchat_update_member_empty_nick_tip, Toast.LENGTH_SHORT).show();
      return;
    }
    if (!TextUtils.equals(nick, member.getNick())) {
      changed = true;
    }
    if (TextUtils.equals(IMKitClient.account(), member.getAccId())) {
      QChatServerRepo.updateMyMember(
          member.getServerId(),
          nick,
          new QChatCallback<Void>(getApplicationContext()) {
            @Override
            public void onSuccess(@Nullable Void param) {
              finishWithResult(false);
            }
          });
    } else {
      QChatServerRepo.updateOtherMember(
          member.getServerId(),
          member.getAccId(),
          nick,
          new QChatCallback<Void>(getApplicationContext()) {
            @Override
            public void onSuccess(@Nullable Void param) {
              super.onSuccess(param);
              finishWithResult(false);
            }
          });
    }
  }

  private void finishWithResult(boolean delete) {
    Intent result = new Intent();
    result.putExtra(MEMBER_OPERATOR_ACCID, member.getAccId());
    if (delete) {
      result.putExtra(MEMBER_OPERATOR_TYPE, MEMBER_OPERATOR_TYPE_DELETE);
    } else if (changed) {
      result.putExtra(MEMBER_OPERATOR_TYPE, MEMBER_OPERATOR_TYPE_CHANGED);
    } else {
      result.putExtra(MEMBER_OPERATOR_TYPE, MEMBER_OPERATOR_TYPE_UNCHANGED);
    }
    setResult(RESULT_OK, result);
    finish();
  }

  /** 展示踢出成员确认弹窗 */
  private void showKickDialog() {
    CommonChoiceDialog dialog = new CommonChoiceDialog();
    dialog
        .setTitleStr(getResources().getString(R.string.qchat_delete_member))
        .setContentStr(
            String.format(getResources().getString(R.string.qchat_delete_some_member), nickname))
        .setNegativeStr(getResources().getString(R.string.qchat_cancel))
        .setPositiveStr(getResources().getString(R.string.qchat_sure))
        .setConfirmListener(
            new ChoiceListener() {
              @Override
              public void onPositive() {
                if (!NetworkUtils.isConnected()) {
                  Toast.makeText(
                          QChatServerMemberInfoActivity.this,
                          R.string.qchat_network_error_tip,
                          Toast.LENGTH_SHORT)
                      .show();
                  return;
                }
                QChatServerRepo.kickMember(
                    member.getServerId(),
                    member.getAccId(),
                    new QChatCallback<Void>(getApplicationContext()) {
                      @Override
                      public void onSuccess(@Nullable Void param) {
                        finishWithResult(true);
                      }
                    });
              }

              @Override
              public void onNegative() {}
            })
        .show(getSupportFragmentManager());
  }

  @Override
  public void initData() {
    binding.avatar.setData(
        member.getAvatarUrl(), nickname, AvatarColor.avatarColor(member.getAccId()));
    binding.tvMember.setText(nickname);
    binding.edtMemberName.setText(member.getNick());
    initRolesList();
    getJoinedRoles();
    // 仅用户为 owner 时且不是当前用户时才可踢出成员
    if (member.getType() == MEMBER_TYPE_OWNER
        || TextUtils.equals(IMKitClient.account(), member.getAccId())) {
      binding.tvKick.setEnabled(false);
      binding.tvKick.setAlpha(0.5f);
    }
  }

  /** 获取用户已经加入的身份组列表 */
  private void getJoinedRoles() {
    QChatRoleRepo.fetchMemberJoinedRoles(
        member.getServerId(),
        member.getAccId(),
        new QChatCallback<List<QChatServerRoleInfo>>(getApplicationContext()) {
          @Override
          public void onSuccess(@Nullable List<QChatServerRoleInfo> param) {
            if (param != null) {
              if (!param.isEmpty()) {
                binding.tvRoleTitle.setVisibility(View.VISIBLE);
              }
              joinedRoles.addAll(param);
              rolesAdapter.refresh(param);
              if (param.size() > DEFAULT_SHOW_SIZE) {
                binding.lyMore.setVisibility(View.VISIBLE);
                fullSize = param.size();
                binding.tvMore.setText(
                    String.format(
                        getResources().getString(R.string.qchat_more_with_all), fullSize));
              }
            }
          }
        });
  }

  /** 获取社区所有的身份组列表 */
  private void getRoles() {
    QChatRoleRepo.fetchServerRoles(
        member.getServerId(),
        0,
        MAX_ROLE_PAGE_SIZE,
        new QChatCallback<ServerRoleResult>(getApplicationContext()) {
          @Override
          public void onSuccess(@Nullable ServerRoleResult param) {
            binding.tvRoleTitle.setVisibility(View.VISIBLE);
            if (param != null && param.getRoleList() != null) {
              List<QChatServerRoleInfo> roleInfoList = param.getRoleList();
              rolesAdapter.refresh(roleInfoList);
              if (roleInfoList.size() > DEFAULT_SHOW_SIZE) {
                binding.lyMore.setVisibility(View.VISIBLE);
                fullSize = roleInfoList.size();
                binding.tvMore.setText(
                    String.format(
                        getResources().getString(R.string.qchat_more_with_all), fullSize));
              }
            }
          }
        });
  }

  /** 初始化成员身份组列表ui */
  private void initRolesList() {
    binding.rvIdentityGroup.setLayoutManager(new LinearLayoutManager(this));
    binding.rvIdentityGroup.addItemDecoration(
        new RecyclerView.ItemDecoration() {
          private final Paint paint = new Paint();

          {
            paint.setColor(getResources().getColor(R.color.color_f5f8fc));
          }

          @Override
          public void onDrawOver(
              @NonNull Canvas canvas,
              @NonNull RecyclerView parent,
              @NonNull RecyclerView.State state) {
            int left = parent.getPaddingLeft();
            int right = parent.getWidth() - parent.getPaddingRight();
            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount - 1; i++) {
              View child = parent.getChildAt(i);
              RecyclerView.LayoutParams params =
                  (RecyclerView.LayoutParams) child.getLayoutParams();
              int top = child.getBottom() + params.bottomMargin;
              int bottom = top + SizeUtils.dp2px(0.5f);
              canvas.drawRect(left, top, right, bottom, paint);
            }
          }
        });
    rolesAdapter =
        new CommonMoreAdapter<QChatServerRoleInfo, QChatRoleViewHolderBinding>() {

          @NonNull
          @Override
          public BaseMoreViewHolder<QChatServerRoleInfo, QChatRoleViewHolderBinding> getViewHolder(
              @NonNull ViewGroup parent, int viewType) {
            QChatRoleViewHolderBinding binding =
                QChatRoleViewHolderBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new BaseMoreViewHolder<QChatServerRoleInfo, QChatRoleViewHolderBinding>(
                binding) {
              @Override
              public void bind(QChatServerRoleInfo item) {
                binding.tvRoleName.setText(item.getName());
                if (!isEdit || item.getType() == QChatConstant.ROLE_EVERYONE_TYPE) {
                  binding.rbCheck.setVisibility(View.GONE);
                } else {
                  binding.rbCheck.setVisibility(View.VISIBLE);
                  binding.rbCheck.setOnCheckedChangeListener(null);
                  if (joinedRoles != null) {
                    binding.rbCheck.setChecked(joinedRoles.contains(item));
                  } else {
                    binding.rbCheck.setChecked(false);
                  }
                  binding.rbCheck.setOnCheckedChangeListener(
                      (buttonView, isChecked) -> addOrRemoveMemberFromRoles(item, isChecked));
                  binding
                      .getRoot()
                      .setOnClickListener(
                          v -> {
                            changed = true;
                            binding.rbCheck.setChecked(!binding.rbCheck.isChecked());
                          });
                }
              }
            };
          }
        };
    rolesAdapter.setShowSub(true);
    binding.rvIdentityGroup.setAdapter(rolesAdapter);
  }

  private volatile boolean isHandingMemberRoles = false;

  /**
   * 为成员添加/移除身份组
   *
   * @param item 社区身份组
   * @param add true 添加；false 移除
   */
  private void addOrRemoveMemberFromRoles(QChatServerRoleInfo item, boolean add) {
    // 避免快速点击导致请求和返回对应错乱
    if (isHandingMemberRoles
        || (joinedRoles.contains(item) && add)
        || (!joinedRoles.contains(item) && !add)) {
      return;
    }
    isHandingMemberRoles = true;
    List<String> accIds = new ArrayList<>();
    accIds.add(member.getAccId());
    QChatCallback<List<String>> optionCallback =
        new QChatCallback<List<String>>(getApplicationContext()) {

          @Override
          public void onSuccess(@Nullable List<String> param) {
            super.onSuccess(param);
            isHandingMemberRoles = false;
          }

          @Override
          public void onFailed(int code) {
            rollbackRoles(item, add);
            Toast.makeText(
                    getApplicationContext(),
                    getString(R.string.qchat_server_request_fail) + code,
                    Toast.LENGTH_SHORT)
                .show();
            isHandingMemberRoles = false;
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            Toast.makeText(
                    getApplicationContext(),
                    getString(R.string.qchat_server_request_fail) + exception,
                    Toast.LENGTH_SHORT)
                .show();
            rollbackRoles(item, add);
            isHandingMemberRoles = false;
          }
        }.setProcess(binding.flyProcess);
    if (add) {
      joinedRoles.add(item);
      QChatRoleRepo.addServerRoleMember(
          member.getServerId(), item.getRoleId(), accIds, optionCallback);
    } else {
      joinedRoles.remove(item);
      QChatRoleRepo.removeServerRoleMember(
          member.getServerId(), item.getRoleId(), accIds, optionCallback);
    }
  }

  /**
   * 请求失败，回退
   *
   * @param item 社区身份组
   * @param add true，添加；false 移除
   */
  private void rollbackRoles(QChatServerRoleInfo item, boolean add) {
    if (add) {
      joinedRoles.remove(item);
    } else {
      joinedRoles.add(item);
    }
    rolesAdapter.update(item);
  }

  @Override
  public void onBackPressed() {
    finishWithResult(false);
  }

  @Override
  protected void initViewModel() {}

  /**
   * 页面启动方法
   *
   * @param activity 页面启动activity
   * @param data 成员信息且包含成员所在身份组内容
   */
  public static void launch(Activity activity, QChatServerMemberWithRoleInfo data) {
    Intent intent = new Intent(activity, QChatServerMemberInfoActivity.class);
    intent.putExtra(QChatConstant.SERVER_MEMBER, data);
    activity.startActivity(intent);
  }
}
