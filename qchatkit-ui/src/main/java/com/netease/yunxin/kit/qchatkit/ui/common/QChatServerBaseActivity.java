// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.common;

import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import com.netease.nimlib.sdk.qchat.model.systemnotification.QChatKickServerMembersDoneAttachment;
import com.netease.yunxin.kit.common.ui.activities.BaseActivity;
import com.netease.yunxin.kit.corekit.im2.IMKitClient;
import com.netease.yunxin.kit.qchatkit.EventObserver;
import com.netease.yunxin.kit.qchatkit.repo.QChatServiceObserverRepo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSystemNotificationInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSystemNotificationTypeInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberKick;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerMemberLeave;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerRemove;
import java.util.Arrays;
import java.util.List;

public class QChatServerBaseActivity extends BaseActivity {
  private long innerServerId;

  private final EventObserver<List<QChatSystemNotificationInfo>>
      innerObserverForServerNotification =
          new EventObserver<List<QChatSystemNotificationInfo>>() {
            @Override
            public void onEvent(@Nullable List<QChatSystemNotificationInfo> event) {
              if (event == null) {
                return;
              }
              String currentAccount = IMKitClient.account();
              for (QChatSystemNotificationInfo info : event) {
                if (info == null || innerServerId != info.getServerId()) {
                  continue;
                }
                QChatSystemNotificationTypeInfo type = info.getType();
                Object attachment = info.getAttachment();
                if (type == ServerRemove.INSTANCE) { // to finish from accId
                  finish();
                } else if (type == ServerMemberLeave.INSTANCE) { // to finish from accId
                  if (TextUtils.equals(currentAccount, info.getFromAccount())) {
                    finish();
                  }
                } else if (type == ServerMemberKick.INSTANCE
                    && attachment
                        instanceof
                        QChatKickServerMembersDoneAttachment) { //QChatKickServerMembersDoneAttachment
                  List<String> kickedIds =
                      ((QChatKickServerMembersDoneAttachment) attachment).getKickedAccids();
                  if (kickedIds != null) {
                    if (kickedIds.contains(currentAccount)) {
                      finish();
                    }
                  }
                }
              }
            }
          };

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    QChatServiceObserverRepo.observerSystemNotificationWithType(
        innerObserverForServerNotification,
        Arrays.asList(
            ServerRemove.INSTANCE, ServerMemberLeave.INSTANCE, ServerMemberKick.INSTANCE));
  }

  public void configServerId(long serverId) {
    this.innerServerId = serverId;
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    QChatServiceObserverRepo.observerSystemNotification(innerObserverForServerNotification, false);
  }
}
