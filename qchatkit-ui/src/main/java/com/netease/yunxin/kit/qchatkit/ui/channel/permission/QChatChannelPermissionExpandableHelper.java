// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.channel.permission;

public class QChatChannelPermissionExpandableHelper {
  private static boolean isExpandable = false;

  public static int NUM_MAX_EXPANDABLE_ITEM = 8;

  public static void init() {
    QChatChannelPermissionExpandableHelper.isExpandable = false;
  }

  public static void expand() {
    QChatChannelPermissionExpandableHelper.isExpandable = true;
  }

  public static void collapse() {
    QChatChannelPermissionExpandableHelper.isExpandable = false;
  }

  public static void reverse() {
    QChatChannelPermissionExpandableHelper.isExpandable =
        !QChatChannelPermissionExpandableHelper.isExpandable;
  }

  public static boolean isExpandable() {
    return QChatChannelPermissionExpandableHelper.isExpandable;
  }
}
