// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.common;

import android.content.Context;
import android.text.TextUtils;
import com.netease.nimlib.sdk.msg.constant.MsgTypeEnum;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageInfo;
import com.netease.yunxin.kit.qchatkit.ui.R;

public final class QChatMessageTypeTipUtils {

  public static String getMessageContent(Context context, QChatMessageInfo lastMessageInfo) {
    if (lastMessageInfo == null) {
      return null;
    }
    if (lastMessageInfo.isRevoke()) {
      return lastMessageInfo.getFromNick()
          + ":"
          + context.getString(R.string.qchat_message_revoked);
    }
    MsgTypeEnum messageType = lastMessageInfo.getMessage().getMsgType();
    if (messageType == null) {
      return null;
    }
    String result;
    if (messageType == MsgTypeEnum.audio) {
      result = context.getString(R.string.qchat_msg_type_audio);
    } else if (messageType == MsgTypeEnum.text) {
      result = lastMessageInfo.getContent();
    } else if (messageType == MsgTypeEnum.image) {
      result = context.getString(R.string.qchat_msg_type_image);
    } else if (messageType == MsgTypeEnum.file) {
      result = context.getString(R.string.qchat_msg_type_file);
    } else if (messageType == MsgTypeEnum.video) {
      result = context.getString(R.string.qchat_msg_type_video);
    } else {
      result = context.getString(R.string.qchat_msg_type_no_tips);
    }
    if (!TextUtils.isEmpty(result)) {
      result = lastMessageInfo.getFromNick() + ":" + result;
    }
    return result;
  }
}
