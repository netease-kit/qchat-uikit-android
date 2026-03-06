// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.view.ait;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.netease.yunxin.kit.common.ui.dialog.BaseBottomDialog;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.databinding.DialogAitContactSelectorBinding;
import com.netease.yunxin.kit.qchatkit.ui.model.ait.AitUserInfo;
import java.util.List;

/** @成员选人弹窗，底部抽屉样式 */
public class AitContactSelectorDialog extends BaseBottomDialog {

  /** 列表条目点击/加载更多回调 */
  public interface ItemListener {
    void onSelect(AitUserInfo item);
    void onLoadMore();
  }

  private DialogAitContactSelectorBinding binding;
  private AitContactAdapter adapter;
  private ItemListener itemListener;

  public void setOnItemListener(ItemListener listener) {
    this.itemListener = listener;
  }

  @Nullable
  @Override
  protected View getRootView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
    binding = DialogAitContactSelectorBinding.inflate(inflater, container, false);
    adapter = new AitContactAdapter();
    binding.rvMembers.setLayoutManager(new LinearLayoutManager(getContext()));
    binding.rvMembers.setAdapter(adapter);
    adapter.setOnItemClickListener(item -> {
      if (itemListener != null) {
        itemListener.onSelect(item);
      }
      // 选择成员后立即关闭弹窗
      dismiss();
    });
    return binding.getRoot();
  }

  /** 更新成员列表数据（需在主线程调用） */
  public void setData(List<AitUserInfo> data) {
    if (adapter != null) {
      adapter.submitList(data);
    }
  }

  // ========================== Adapter ==========================

  static class AitContactAdapter extends RecyclerView.Adapter<AitContactViewHolder> {

    private List<AitUserInfo> data;
    private OnItemClickListener listener;

    interface OnItemClickListener {
      void onClick(AitUserInfo item);
    }

    void setOnItemClickListener(OnItemClickListener l) {
      this.listener = l;
    }

    void submitList(List<AitUserInfo> newData) {
      this.data = newData;
      notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AitContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View itemView = LayoutInflater.from(parent.getContext())
          .inflate(R.layout.item_ait_contact, parent, false);
      return new AitContactViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull AitContactViewHolder holder, int position) {
      AitUserInfo item = data.get(position);
      holder.bind(item);
      holder.itemView.setOnClickListener(v -> {
        if (listener != null) {
          listener.onClick(item);
        }
      });
    }

    @Override
    public int getItemCount() {
      return data == null ? 0 : data.size();
    }
  }
}
