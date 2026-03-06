// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.message.interfaces;

import android.content.Context;
import java.io.File;
import org.json.JSONObject;

public interface IMessageProxy {

  boolean sendTextMessage(String msg);

  /**
   * 发送文本消息，携带 @(Ait) 扩展数据。
   * 默认实现回退到普通发送，子类可覆盖以处理 Ait 扩展。
   *
   * @param msg     消息正文
   * @param aitData @扩展 JSON（存入 remoteExtension["yxAitMsg"]）
   */
  default boolean sendTextMessage(String msg, JSONObject aitData) {
    return sendTextMessage(msg);
  }

  boolean sendImage();

  boolean sendFile();

  boolean sendEmoji();

  boolean sendVoice();

  boolean pickMedia();

  boolean takePicture();

  boolean captureVideo();

  boolean hasPermission(String permission);

  boolean sendAudio(File audioFile, long audioLength);

  void onInputPanelExpand();

  void shouldCollapseInputPanel();

  String getAccount();

  Context getActivityContext();
}
