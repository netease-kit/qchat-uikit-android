// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.message.popmenu;

import android.view.View;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import com.netease.yunxin.kit.corekit.im.IMKitClient;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageInfo;
import java.util.HashMap;
import java.util.Map;

public class QChatPopMenuAction {

  private String title;
  private @DrawableRes int actionIcon;
  private OnClickListener actionClickListener;
  private String action;

  private Map<String, Object> actionData = new HashMap<>();

  public QChatPopMenuAction(String action, String title, @DrawableRes int actionIcon) {
    this(action, title, actionIcon, null);
  }

  public QChatPopMenuAction(
      String action,
      @StringRes int nameRes,
      @DrawableRes int actionIcon,
      OnClickListener actionClickListener) {
    this(
        action,
        IMKitClient.getApplicationContext().getString(nameRes),
        actionIcon,
        actionClickListener);
  }

  public void addActionData(String key, Object value) {
    actionData.put(key, value);
  }

  public Object getActionData(String key) {
    return actionData.get(key);
  }

  public QChatPopMenuAction(
      String action,
      String title,
      @DrawableRes int actionIcon,
      OnClickListener actionClickListener) {
    this.action = action;
    this.title = title;
    this.actionIcon = actionIcon;
    this.actionClickListener = actionClickListener;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getTitle() {
    return title;
  }

  public String getAction() {
    return action;
  }

  public void setIcon(@DrawableRes int actionIcon) {
    this.actionIcon = actionIcon;
  }

  public @DrawableRes int getIcon() {
    return actionIcon;
  }

  public void setActionClickListener(OnClickListener actionClickListener) {
    this.actionClickListener = actionClickListener;
  }

  public OnClickListener getActionClickListener() {
    return actionClickListener;
  }

  @FunctionalInterface
  public interface OnClickListener {
    void onClick(View view, QChatMessageInfo messageInfo);
  }
}
