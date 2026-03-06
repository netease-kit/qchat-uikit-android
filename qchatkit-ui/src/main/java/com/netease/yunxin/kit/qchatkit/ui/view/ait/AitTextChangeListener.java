// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.view.ait;

/** AitManager 向 View 层发出文字插入/删除指令的回调接口 */
public interface AitTextChangeListener {

  /**
   * AitManager 计算出需要插入@文字时回调，View 层负责实际写入 EditText
   * @param content 要插入的内容（"名字 " 或 "@名字 "）
   * @param start   插入起始位置
   * @param length  内容长度
   * @param hasAt   content 中是否已包含"@"前缀（true时直接替换，false时需额外加"@"）
   */
  void onTextAdd(String content, int start, int length, boolean hasAt);

  /**
   * AitManager 计算出需要整块删除@文字时回调，View 层负责实际删除
   * @param start  删除起始位置
   * @param length 删除长度
   */
  void onTextDelete(int start, int length);
}
