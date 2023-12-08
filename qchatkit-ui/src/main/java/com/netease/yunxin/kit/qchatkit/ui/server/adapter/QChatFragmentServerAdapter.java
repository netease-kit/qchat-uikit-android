// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.server.adapter;

import android.content.Context;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.netease.yunxin.kit.common.ui.utils.AvatarColor;
import com.netease.yunxin.kit.qchatkit.observer.ObserverUnreadInfoResultHelper;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerInfo;
import com.netease.yunxin.kit.qchatkit.ui.common.QChatCommonAdapter;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatFragmentServerListItemBinding;
import com.netease.yunxin.kit.qchatkit.ui.server.model.QChatFragmentServerInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for recyclerView in {@link
 * com.netease.yunxin.kit.qchatkit.ui.server.QChatServerFragment}.
 */
public class QChatFragmentServerAdapter
    extends QChatCommonAdapter<QChatFragmentServerInfo, QChatFragmentServerListItemBinding> {
  private static final String PAYLOADS_FOCUS = "focus";
  private static final String PAYLOADS_UNREAD = "unread";
  private final List<Long> bindedServerList = new ArrayList<>();
  private long focusedServerId = 0L;
  private OnClickListener<QChatFragmentServerInfo, QChatFragmentServerListItemBinding>
      clickListener;
  private OnServerRemovedToNewFocusListener onServerRemovedToNewFocusListener;

  public QChatFragmentServerAdapter(Context context) {
    super(context, QChatFragmentServerListItemBinding.class);
  }

  public long getFocusedServerId() {
    return focusedServerId;
  }

  /** update server unread count info. */
  public void updateUnreadInfoList(List<Long> serverIdList) {
    if (serverIdList == null) {
      return;
    }
    for (Long serverId : serverIdList) {
      if (serverId == null) {
        continue;
      }
      QChatFragmentServerInfo serverInfo = getInfoByServerId(serverId);
      if (serverInfo != null) {
        serverInfo.unreadInfoItemMap = ObserverUnreadInfoResultHelper.getUnreadInfoMap(serverId);
      }
    }
    notifyItemRangeChanged(0, dataSource.size(), PAYLOADS_UNREAD);
  }

  public void setItemClickListener(
      OnClickListener<QChatFragmentServerInfo, QChatFragmentServerListItemBinding> listener) {
    this.clickListener = listener;
  }

  @Override
  public void addDataList(List<QChatFragmentServerInfo> data, boolean clearOld) {
    int lastIndex = getItemCount();
    super.addDataList(data, clearOld);
    if (clearOld) {
      bindedServerList.clear();
    }
    if (focusedServerId == 0) {
      Pair<QChatFragmentServerInfo, Integer> serverInfoIndexPair =
          findNextNormalQChatServer(lastIndex);
      if (serverInfoIndexPair != null) {
        focusedServerId = serverInfoIndexPair.first.serverInfo.getServerId();
        if (onServerRemovedToNewFocusListener != null) {
          onServerRemovedToNewFocusListener.onNewFocusServer(serverInfoIndexPair.first);
        }
      }
    }
  }

  public void removeServerById(long serverId) {
    int index = dataSource.indexOf(QChatFragmentServerInfo.generateWithServerId(serverId));
    if (index >= 0) {
      bindedServerList.remove(serverId);
      dataSource.remove(index);
      notifyItemRemoved(index);
      if (serverId == focusedServerId && !dataSource.isEmpty()) {
        focusedServerId = 0L;
        Pair<QChatFragmentServerInfo, Integer> serverInfoIndexPair =
            findNextNormalQChatServer(index);
        if (serverInfoIndexPair == null) {
          return;
        }
        focusedServerId = serverInfoIndexPair.first.serverInfo.getServerId();
        notifyItemChanged(serverInfoIndexPair.second);
        if (onServerRemovedToNewFocusListener != null) {
          onServerRemovedToNewFocusListener.onNewFocusServer(serverInfoIndexPair.first);
        }
      }
    }
  }

  private Pair<QChatFragmentServerInfo, Integer> findNextNormalQChatServer(int startIndex) {
    if (getItemCount() == 0) {
      return null;
    }
    Pair<QChatFragmentServerInfo, Integer> result = null;
    for (int index = startIndex; index < getItemCount(); index++) {
      QChatFragmentServerInfo item = getItemData(index);
      if (item == null) {
        continue;
      }
      if (item.serverInfo.getAnnouncementInfo() == null) {
        result = new Pair<>(item, index);
        break;
      }
    }
    if (result != null) {
      return result;
    }
    for (int index = startIndex - 1; index >= 0; index--) {
      QChatFragmentServerInfo item = getItemData(index);
      if (item == null) {
        continue;
      }
      if (item.serverInfo.getAnnouncementInfo() == null) {
        result = new Pair<>(item, index);
        break;
      }
    }
    return result;
  }

  public void updateData(QChatServerInfo info) {
    if (info == null) {
      return;
    }
    int index =
        dataSource.indexOf(QChatFragmentServerInfo.generateWithServerId(info.getServerId()));
    if (index >= 0) {
      QChatFragmentServerInfo data = dataSource.get(index);
      data.serverInfo = info;
      notifyItemChanged(index);
    }
  }

  public void setOnServerRemovedToNewFocusListener(
      OnServerRemovedToNewFocusListener onServerRemovedToNewFocusListener) {
    this.onServerRemovedToNewFocusListener = onServerRemovedToNewFocusListener;
  }

  @Override
  public void onBindViewHolder(
      @NonNull ItemViewHolder<QChatFragmentServerListItemBinding> holder,
      int position,
      QChatFragmentServerInfo data) {
    super.onBindViewHolder(holder, position, data);
    bindedServerList.add(data.serverInfo.getServerId());

    QChatFragmentServerListItemBinding binding = holder.binding;
    binding.cavIcon.setData(
        data.serverInfo.getIconUrl(),
        data.serverInfo.getName(),
        AvatarColor.avatarColor(data.serverInfo.getServerId()));

    data.unreadInfoItemMap =
        ObserverUnreadInfoResultHelper.getUnreadInfoMap(data.serverInfo.getServerId());

    updateForUnReadCount(data, binding.tvUnReadCount);

    updateFocus(holder, data);
    if (data.serverInfo.getAnnouncementInfo() != null
        && data.serverInfo.getAnnouncementInfo().isValid()) {
      binding.ivAnnounceFlag.setVisibility(View.VISIBLE);
    } else {
      binding.ivAnnounceFlag.setVisibility(View.GONE);
    }

    holder.itemView.setOnClickListener(
        v -> {
          if (data.serverInfo.getAnnouncementInfo() != null
              && data.serverInfo.getAnnouncementInfo().isValid()) {
            if (clickListener != null) {
              clickListener.onClick(data, holder);
            }
            return;
          }
          int focusIndex =
              dataSource.indexOf(QChatFragmentServerInfo.generateWithServerId(focusedServerId));
          if (focusIndex >= 0) {
            notifyItemChanged(focusIndex, PAYLOADS_FOCUS);
          }
          focusedServerId = data.serverInfo.getServerId();
          notifyItemChanged(holder.getBindingAdapterPosition(), PAYLOADS_FOCUS);
        });
  }

  @Override
  public void onBindViewHolder(
      @NonNull ItemViewHolder<QChatFragmentServerListItemBinding> holder,
      int position,
      @NonNull List<Object> payloads) {
    QChatFragmentServerInfo data = getItemData(position);
    if (data == null) {
      return;
    }
    if (payloads.contains(PAYLOADS_FOCUS)) {
      updateFocus(holder, data);
    } else if (payloads.contains(PAYLOADS_UNREAD)) {
      QChatFragmentServerListItemBinding binding = holder.binding;
      updateForUnReadCount(data, binding.tvUnReadCount);
    } else {
      super.onBindViewHolder(holder, position, payloads);
    }
  }

  public void updateFocus(long serverId) {
    if (serverId == focusedServerId) {
      return;
    }
    if (serverId == 0 && !dataSource.isEmpty()) {
      Pair<QChatFragmentServerInfo, Integer> serverInfoIndexPair = findNextNormalQChatServer(0);
      if (serverInfoIndexPair != null) {
        focusedServerId = serverInfoIndexPair.first.serverInfo.getServerId();
        notifyItemChanged(serverInfoIndexPair.second);
        return;
      }
    }
    int focusIndex =
        dataSource.indexOf(QChatFragmentServerInfo.generateWithServerId(focusedServerId));
    focusedServerId = serverId;
    if (bindedServerList.contains(serverId)) {
      int newFocusIndex =
          dataSource.indexOf(QChatFragmentServerInfo.generateWithServerId(serverId));
      if (newFocusIndex >= 0 && getItemCount() > newFocusIndex) {
        notifyItemChanged(newFocusIndex);
      }
    }
    if (focusIndex >= 0 && getItemCount() > focusIndex) {
      notifyItemChanged(focusIndex);
    }
  }

  public int getNormalQChatServerCount() {
    int count = 0;
    for (QChatFragmentServerInfo item : dataSource) {
      if (item == null) {
        continue;
      }
      if (item.serverInfo.getAnnouncementInfo() == null
          || !item.serverInfo.getAnnouncementInfo().isValid()) {
        count++;
      }
    }
    return count;
  }

  private void updateFocus(
      ItemViewHolder<QChatFragmentServerListItemBinding> holder, QChatFragmentServerInfo data) {
    QChatFragmentServerListItemBinding binding = holder.binding;
    QChatServerInfo.AnnouncementInfo announcementInfo = data.serverInfo.getAnnouncementInfo();
    if (focusedServerId <= 0L && announcementInfo == null) {
      focusedServerId = data.serverInfo.getServerId();
    }
    if (data.serverInfo.getServerId() == focusedServerId && announcementInfo == null) {
      binding.ivFocus.setVisibility(View.VISIBLE);
      clickListener.onClick(data, holder);
    } else {
      binding.ivFocus.setVisibility(View.GONE);
    }
  }

  private void updateForUnReadCount(QChatFragmentServerInfo info, TextView view) {
    if (info == null || info.serverInfo == null || view == null) {
      return;
    }
    int unReadCount =
        ObserverUnreadInfoResultHelper.getUnreadCountForServer(info.serverInfo.getServerId());
    if (unReadCount > 0) {
      String countStr;
      if (unReadCount > 99) {
        countStr = "99+";
      } else {
        countStr = String.valueOf(unReadCount);
      }
      view.setText(countStr);
      view.setVisibility(View.VISIBLE);
    } else {
      view.setVisibility(View.GONE);
    }
  }

  private QChatFragmentServerInfo getInfoByServerId(long serverId) {
    int index = dataSource.indexOf(new QChatFragmentServerInfo(new QChatServerInfo(serverId)));
    if (index < 0) {
      return null;
    }
    return dataSource.get(index);
  }

  public interface OnServerRemovedToNewFocusListener {
    void onNewFocusServer(QChatFragmentServerInfo serverInfo);
  }
}
