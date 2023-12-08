// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.server;

import static com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant.REQUEST_MEMBER_FILTER_CHANNEL;
import static com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant.REQUEST_MEMBER_FILTER_ROLE;
import static com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant.REQUEST_MEMBER_SELECTOR_KEY;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.netease.yunxin.kit.common.ui.utils.AvatarColor;
import com.netease.yunxin.kit.common.utils.NetworkUtils;
import com.netease.yunxin.kit.qchatkit.repo.QChatServerRepo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatPageResult;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerRoleMemberInfo;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.common.LoadMoreRecyclerViewDecorator;
import com.netease.yunxin.kit.qchatkit.ui.common.QChatCallback;
import com.netease.yunxin.kit.qchatkit.ui.common.QChatChannelBaseActivity;
import com.netease.yunxin.kit.qchatkit.ui.common.QChatCommonAdapter;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatSelectorViewHolderBinding;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatServerMemberSelectorActivityLayoutBinding;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant;
import com.netease.yunxin.kit.qchatkit.ui.server.adapter.QChatServerMemberListAdapter;
import java.util.ArrayList;
import java.util.List;

/** 成员选择列表页面（公告频道管理员选择，话题黑白名单成员选择，身份组成员选择） */
public class QChatMemberSelectorActivity extends QChatChannelBaseActivity {
  /** 分页请求每页的数据量 */
  private static final int PAGE_SIZE = 20;
  /** 默认的最大选择人数 */
  private static final int MAX_SELECTED_NUM = 10;

  private QChatServerMemberSelectorActivityLayoutBinding binding;

  private QChatMemberSelectorAdapter selectorAdapter;
  /** 最大选择的人数 */
  private int maxSize = MAX_SELECTED_NUM;

  private long serverId;
  private long channelId;
  private long roleId;
  private int channelType;

  /** 被过滤的成员accId列表 */
  private List<String> filterList;

  private QChatServerMemberListAdapter memberAdapter;
  private LoadMoreRecyclerViewDecorator<QChatServerRoleMemberInfo> decorator;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    View contentView = getContentView();
    if (contentView != null) {
      setContentView(contentView);
    }
    initViewModel();
    initView();
    initData();
  }

  protected void initViewModel() {}

  @Nullable
  protected View getContentView() {
    binding = QChatServerMemberSelectorActivityLayoutBinding.inflate(getLayoutInflater());
    return binding.getRoot();
  }

  protected void initView() {
    binding
        .title
        .setTitle(R.string.qchat_select)
        .setOnBackIconClickListener(v -> onBackPressed())
        .setActionTextColor(getResources().getColor(R.color.color_337eff))
        .setActionText(R.string.qchat_sure)
        .setActionListener(v -> selectedAndBack());
    LinearLayoutManager selectorLayoutManager =
        new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
    binding.rvSelected.setLayoutManager(selectorLayoutManager);
    selectorAdapter = new QChatMemberSelectorAdapter(this);
    selectorAdapter.setItemClickListener((data, holder) -> unselectedItem(data));
    binding.rvSelected.setAdapter(selectorAdapter);

    LinearLayoutManager memberLayoutManager = new LinearLayoutManager(this);
    binding.rvMembers.setLayoutManager(memberLayoutManager);
    memberAdapter =
        new QChatServerMemberListAdapter(
            this,
            (item, selected) -> {
              if (selected) {
                selectorAdapter.addData(item);
              } else {
                selectorAdapter.removeData(item);
              }

              if (selectorAdapter.getItemCount() > 0) {
                binding.title.setActionText(
                    getResources()
                        .getString(R.string.qchat_sure_with_num, selectorAdapter.getItemCount()));
              } else {
                binding.title.setActionText(R.string.qchat_sure);
              }
            });
    memberAdapter.setItemClickListener(
        (data, holder) -> {
          boolean isChecked = holder.binding.rbCheck.isChecked();
          if (selectorAdapter.getItemCount() >= maxSize && !isChecked) {
            Toast.makeText(
                    QChatMemberSelectorActivity.this,
                    getString(R.string.qchat_member_selector_max_count, maxSize),
                    Toast.LENGTH_LONG)
                .show();
            return;
          }
          holder.binding.rbCheck.setChecked(!isChecked);
        });
    binding.rvMembers.setAdapter(memberAdapter);
    decorator =
        new LoadMoreRecyclerViewDecorator<>(binding.rvMembers, memberLayoutManager, memberAdapter);
    decorator.setLoadMoreListener(data -> getMembers(data == null ? 0 : data.getCreateTime()));
  }

  private void selectedAndBack() {
    if (!NetworkUtils.isConnected()) {
      Toast.makeText(this, R.string.qchat_network_error_tip, Toast.LENGTH_SHORT).show();
      return;
    }
    Intent result = new Intent();
    ArrayList<QChatServerRoleMemberInfo> selectedMemberList = selectorAdapter.getDataSource();
    if (!selectedMemberList.isEmpty()) {
      result.putExtra(REQUEST_MEMBER_SELECTOR_KEY, selectedMemberList);
    } else {
      Toast.makeText(this, R.string.qchat_selector_none_tip, Toast.LENGTH_SHORT).show();
      return;
    }
    setResult(RESULT_OK, result);
    finish();
  }

  private void unselectedItem(QChatServerRoleMemberInfo item) {
    selectorAdapter.removeData(item);
    item.setSelected(false);
    memberAdapter.updateData(item);
    if (selectorAdapter.getItemCount() > 0) {
      binding.title.setActionText(
          getResources().getString(R.string.qchat_sure_with_num, selectorAdapter.getItemCount()));
    } else {
      binding.title.setActionText(R.string.qchat_sure);
    }
  }

  protected void initData() {
    serverId = getIntent().getLongExtra(QChatConstant.SERVER_ID, 0);
    channelId = getIntent().getLongExtra(QChatConstant.CHANNEL_ID, 0);
    channelType = getIntent().getIntExtra(QChatConstant.CHANNEL_TYPE, 0);
    roleId = getIntent().getLongExtra(QChatConstant.SERVER_ROLE_ID, 0);
    configServerIdAndChannelId(serverId, channelId);
    filterList = getIntent().getStringArrayListExtra(QChatConstant.REQUEST_MEMBER_FILTER_LIST);
    maxSize = getIntent().getIntExtra(QChatConstant.REQUEST_MEMBER_MAX_SIZE, MAX_SELECTED_NUM);
    getMembers(0);
  }

  private void getMembers(long timeTag) {
    int filterType = getIntent().getIntExtra(QChatConstant.REQUEST_MEMBER_FILTER_KEY, 0);
    // 获取成员列表回调结果处理
    QChatCallback<QChatPageResult<QChatServerRoleMemberInfo>> memberCallback =
        new QChatCallback<QChatPageResult<QChatServerRoleMemberInfo>>(this) {
          @Override
          public void onSuccess(@Nullable QChatPageResult<QChatServerRoleMemberInfo> param) {
            if (param != null) {
              if (param.getDataList() != null && param.getDataList().size() > 0) {
                List<QChatServerRoleMemberInfo> memberList = new ArrayList<>(param.getDataList());
                if (filterList != null && !filterList.isEmpty()) {
                  for (QChatServerRoleMemberInfo member : param.getDataList()) {
                    if (filterList.contains(member.getAccId())) {
                      memberList.remove(member);
                    }
                  }
                }
                if (timeTag == 0) {
                  memberAdapter.addDataList(memberList, true);
                  binding.emptyLayout.setVisibility(
                      memberAdapter.getItemCount() > 0 ? View.GONE : View.VISIBLE);
                } else {
                  memberAdapter.addData(memberList);
                }
              } else {
                if (timeTag == 0) {
                  binding.emptyLayout.setVisibility(View.VISIBLE);
                }
              }
            }
            if (decorator != null) {
              if (timeTag == 0) {
                decorator.init();
              }
              decorator.notifyResult(true);
            }
          }
        };
    switch (filterType) {
        // 获取社区成员，并过滤已在身份组成员
      case REQUEST_MEMBER_FILTER_ROLE:
        QChatServerRepo.fetchServerMembersWithRolesFilter(
            serverId, roleId, timeTag, PAGE_SIZE, memberCallback);
        break;
        // 获取社区成员，并过滤已在黑白名单成员
      case REQUEST_MEMBER_FILTER_CHANNEL:
        QChatServerRepo.fetchServerMembersWithWhiteBlackFilter(
            serverId, channelId, channelType, timeTag, PAGE_SIZE, memberCallback);
        break;
      default:
        //no filter
        QChatServerRepo.getServerMembersWithFilter(serverId, timeTag, PAGE_SIZE, memberCallback);
        break;
    }
  }

  private static class QChatMemberSelectorAdapter
      extends QChatCommonAdapter<QChatServerRoleMemberInfo, QChatSelectorViewHolderBinding> {

    public QChatMemberSelectorAdapter(Context context) {
      super(context, QChatSelectorViewHolderBinding.class);
    }

    @Override
    public void onBindViewHolder(
        QChatSelectorViewHolderBinding binding,
        int position,
        QChatServerRoleMemberInfo data,
        int bingingAdapterPosition) {
      binding.avatar.setData(
          data.getAvatarUrl(), data.getNickName(), AvatarColor.avatarColor(data.getAccId()));
    }

    @Override
    public void addData(QChatServerRoleMemberInfo data) {
      if (dataSource.contains(data)) {
        return;
      }
      super.addData(data);
    }

    public ArrayList<QChatServerRoleMemberInfo> getDataSource() {
      return new ArrayList<>(dataSource);
    }
  }
}
