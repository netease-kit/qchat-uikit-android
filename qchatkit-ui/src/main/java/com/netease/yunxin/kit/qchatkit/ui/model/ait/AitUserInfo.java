// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.model.ait;

/** @候选人展示模型 */
public class AitUserInfo {

  private final String account;  // 用户账号ID
  private final String name;     // 列表展示名
  private final String aitName;  // @后插入到输入框的名字
  private final String avatar;   // 头像URL

  public AitUserInfo(String account, String name, String aitName, String avatar) {
    this.account = account;
    this.name = name;
    this.aitName = aitName;
    this.avatar = avatar;
  }

  public String getAccount() {
    return account;
  }

  public String getName() {
    return name;
  }

  public String getAitName() {
    return aitName != null ? aitName : name;
  }

  public String getAvatar() {
    return avatar;
  }
}
