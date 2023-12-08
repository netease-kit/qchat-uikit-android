// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.announce;

/** 公告频道中快捷表情管理 */
public class QuickEmojiManager {

  private static boolean isAllowEmojiReply = true;

  /** 当前公告频道是否允许表情回复 */
  public static boolean isAllowEmojiReply() {
    return isAllowEmojiReply;
  }

  /** 设置当前公告频道是否允许表情回复 */
  public static void setAllowEmojiReply(boolean isAEmoji) {
    isAllowEmojiReply = isAEmoji;
  }
}
