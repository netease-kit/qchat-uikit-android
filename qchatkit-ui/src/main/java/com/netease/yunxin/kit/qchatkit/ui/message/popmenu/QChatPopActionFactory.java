// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.message.popmenu;

import com.netease.nimlib.sdk.msg.constant.MsgDirectionEnum;
import com.netease.nimlib.sdk.msg.constant.MsgStatusEnum;
import com.netease.nimlib.sdk.msg.constant.MsgTypeEnum;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageInfo;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.message.input.ActionConstants;
import java.util.ArrayList;
import java.util.List;

/** 聊天页面，长按消息弹出菜单项提供菜单内容 */
public class QChatPopActionFactory {

  private static volatile QChatPopActionFactory instance;

  private IQChatPopMenuClickListener actionListener;

  private QChatPopActionFactory() {}

  public static QChatPopActionFactory getInstance() {
    if (instance == null) {
      synchronized (QChatPopActionFactory.class) {
        if (instance == null) {
          instance = new QChatPopActionFactory();
        }
      }
    }
    return instance;
  }

  public void setActionListener(IQChatPopMenuClickListener actionListener) {
    this.actionListener = actionListener;
  }

  /**
   * 获取普通话题聊天页面，长按消息弹出菜单项，包括：复制、撤回、删除
   *
   * @param message 消息
   * @return 菜单项
   */
  public List<QChatPopMenuAction> getNormalActions(QChatMessageInfo message) {
    List<QChatPopMenuAction> actions = new ArrayList<>();
    if (message.getMessage() == null) {
      return actions;
    }
    // 消息状态为失败或者发送中消息，只有删除，如果是文本消息还有复制
    if (message.getMessage().getStatus() == MsgStatusEnum.fail
        || message.getMessage().getStatus() == MsgStatusEnum.sending) {
      if (message.getMessage().getMsgType() == MsgTypeEnum.text) {
        actions.add(getCopyAction(message));
      }
      actions.add(getDeleteAction(message));
      return actions;
    }

    if (message.getMessage().getMsgType() == MsgTypeEnum.text) {
      // text
      actions.add(getCopyAction(message));
    }

    actions.add(getRecallAction(message));
    actions.add(getDeleteAction(message));

    return actions;
  }

  /**
   * 获取公告聊天页面，长按消息弹出菜单项，包括：复制、撤回、删除 公共频道会根据是否是管理员有不同的菜单项
   *
   * @param message 消息
   * @param isManager 是否是管理员
   * @return 菜单项
   */
  public List<QChatPopMenuAction> getAnnounceActions(QChatMessageInfo message, boolean isManager) {
    List<QChatPopMenuAction> actions = new ArrayList<>();
    if (message.getMessage() == null) {
      return actions;
    }
    // 消息状态为失败或者发送中消息，只有删除，如果是文本消息还有复制
    if (message.getMessage().getStatus() == MsgStatusEnum.fail
        || message.getMessage().getStatus() == MsgStatusEnum.sending) {
      if (message.getMessage().getMsgType() == MsgTypeEnum.text) {
        actions.add(getCopyAction(message));
      }
      actions.add(getDeleteAction(message));
      return actions;
    }

    if (message.getMessage().getMsgType() == MsgTypeEnum.text) {
      // text
      actions.add(getCopyAction(message));
    }

    // 只有管理员才有撤回权限
    if (isManager && message.getMessage().getDirect() == MsgDirectionEnum.Out) {
      actions.add(getRecallAction(message));
    }
    // 只有管理员才有删除权限
    if (isManager) {
      actions.add(getDeleteAction(message));
    }

    return actions;
  }

  // 获取菜单中，快捷评论表情的菜单项
  public QChatPopMenuAction getEmojiActions(QChatMessageInfo message, boolean useArrow) {
    if (message.getMessage().getStatus() == MsgStatusEnum.fail
        || message.getMessage().getStatus() == MsgStatusEnum.sending) {
      return null;
    }
    return useArrow ? getEmojiActionWithArrow(message) : getEmojiAction(message);
  }

  private QChatPopMenuAction getCopyAction(QChatMessageInfo message) {
    return new QChatPopMenuAction(
        ActionConstants.POP_ACTION_COPY,
        R.string.qchat_message_action_copy,
        R.drawable.ic_message_copy,
        (view, messageInfo) -> {
          if (actionListener != null) {
            actionListener.onCopy(message);
          }
        });
  }

  private QChatPopMenuAction getDeleteAction(QChatMessageInfo message) {
    return new QChatPopMenuAction(
        ActionConstants.POP_ACTION_DELETE,
        R.string.qchat_message_action_delete,
        R.drawable.ic_message_delete,
        (view, messageInfo) -> {
          if (actionListener != null) {
            actionListener.onDelete(message);
          }
        });
  }

  private QChatPopMenuAction getRecallAction(QChatMessageInfo message) {
    return new QChatPopMenuAction(
        ActionConstants.POP_ACTION_RECALL,
        R.string.qchat_message_action_recall,
        R.drawable.ic_qchat_message_recall,
        (view, messageInfo) -> {
          if (actionListener != null) {
            actionListener.onRecall(message);
          }
        });
  }

  private QChatPopMenuAction getEmojiActionWithArrow(QChatMessageInfo message) {
    QChatPopMenuAction emojiAction =
        new QChatPopMenuAction(
            ActionConstants.POP_ACTION_EMOJI,
            null,
            R.drawable.ic_qchat_emoji_arrow_down,
            (view, messageInfo) -> {
              if (actionListener != null) {
                actionListener.onEmojiClick(message, Integer.parseInt(view.getTag().toString()));
              }
            });

    // 菜单中表情评论的默认值
    List<Integer> emojiList = new ArrayList<>();
    emojiList.add(84);
    emojiList.add(0);
    emojiList.add(21);
    emojiList.add(85);
    emojiList.add(86);
    emojiList.add(55);
    emojiAction.addActionData(ActionConstants.POP_ACTION_EXT_DATA, emojiList);
    return emojiAction;
  }

  private QChatPopMenuAction getEmojiAction(QChatMessageInfo message) {
    QChatPopMenuAction emojiAction =
        new QChatPopMenuAction(
            ActionConstants.POP_ACTION_EMOJI,
            null,
            R.drawable.ic_qchat_emoji_arrow_down,
            (view, messageInfo) -> {
              if (actionListener != null) {
                actionListener.onEmojiClick(message, Integer.parseInt(view.getTag().toString()));
              }
            });

    List<Integer> emojiList = new ArrayList<>();
    emojiList.add(84);
    emojiList.add(0);
    emojiList.add(21);
    emojiList.add(85);
    emojiList.add(86);
    emojiList.add(55);
    emojiList.add(48);
    emojiAction.addActionData(ActionConstants.POP_ACTION_EXT_DATA, emojiList);
    return emojiAction;
  }
}
