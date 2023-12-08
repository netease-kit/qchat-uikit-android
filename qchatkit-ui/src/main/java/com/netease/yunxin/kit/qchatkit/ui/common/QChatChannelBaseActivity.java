// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.common;

import android.os.Bundle;
import androidx.annotation.Nullable;
import com.netease.nimlib.sdk.qchat.enums.QChatChannelBlackWhiteOperateType;
import com.netease.nimlib.sdk.qchat.enums.QChatChannelBlackWhiteType;
import com.netease.nimlib.sdk.qchat.model.systemnotification.QChatUpdateChannelBlackWhiteMemberAttachment;
import com.netease.yunxin.kit.corekit.im.IMKitClient;
import com.netease.yunxin.kit.corekit.im.model.EventObserver;
import com.netease.yunxin.kit.qchatkit.repo.QChatServiceObserverRepo;
import com.netease.yunxin.kit.qchatkit.repo.model.ChannelRemove;
import com.netease.yunxin.kit.qchatkit.repo.model.ChannelUpdateWhiteBlackRoleMember;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSystemNotificationInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSystemNotificationTypeInfo;
import java.util.Arrays;
import java.util.List;

public class QChatChannelBaseActivity extends QChatServerBaseActivity {
  private long innerChannelId;

  private final EventObserver<List<QChatSystemNotificationInfo>>
      innerObserverForChannelNotification =
          new EventObserver<List<QChatSystemNotificationInfo>>() {
            @Override
            public void onEvent(@Nullable List<QChatSystemNotificationInfo> event) {
              if (event == null) {
                return;
              }
              String currentAccount = IMKitClient.account();
              for (QChatSystemNotificationInfo info : event) {
                if (info == null || innerChannelId != info.getChannelId()) {
                  continue;
                }

                QChatSystemNotificationTypeInfo type = info.getType();
                Object attachment = info.getAttachment();
                if (type == ChannelRemove.INSTANCE) {
                  finish();
                } else if (type == ChannelUpdateWhiteBlackRoleMember.INSTANCE
                    && attachment instanceof QChatUpdateChannelBlackWhiteMemberAttachment) {
                  QChatUpdateChannelBlackWhiteMemberAttachment infoAttachment =
                      (QChatUpdateChannelBlackWhiteMemberAttachment) attachment;
                  QChatChannelBlackWhiteType channelBlackWhiteType =
                      infoAttachment.getChannelBlackWhiteType();
                  QChatChannelBlackWhiteOperateType operateType =
                      infoAttachment.getChannelBlackWhiteOperateType();
                  List<String> accIds = infoAttachment.getChannelBlackWhiteToAccids();
                  if (accIds == null || !accIds.contains(currentAccount)) {
                    continue;
                  }
                  if ((channelBlackWhiteType == QChatChannelBlackWhiteType.BLACK
                          && operateType == QChatChannelBlackWhiteOperateType.ADD)
                      || (channelBlackWhiteType == QChatChannelBlackWhiteType.WHITE
                          && operateType == QChatChannelBlackWhiteOperateType.REMOVE)) {
                    finish();
                  }
                }
              }
            }
          };

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    QChatServiceObserverRepo.observerSystemNotificationWithType(
        innerObserverForChannelNotification,
        Arrays.asList(ChannelRemove.INSTANCE, ChannelUpdateWhiteBlackRoleMember.INSTANCE));
  }

  public void configServerIdAndChannelId(long serverId, long channelId) {
    configServerId(serverId);
    this.innerChannelId = channelId;
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    QChatServiceObserverRepo.observerSystemNotification(innerObserverForChannelNotification, false);
  }
}
