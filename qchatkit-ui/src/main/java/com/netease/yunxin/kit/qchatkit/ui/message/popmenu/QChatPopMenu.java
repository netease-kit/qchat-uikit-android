// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.message.popmenu;

import static com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant.LIB_TAG;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuAdapter;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.netease.nimlib.sdk.msg.constant.MsgDirectionEnum;
import com.netease.nimlib.sdk.msg.constant.MsgStatusEnum;
import com.netease.yunxin.kit.alog.ALog;
import com.netease.yunxin.kit.common.utils.SizeUtils;
import com.netease.yunxin.kit.corekit.im.IMKitClient;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageInfo;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatPopMenuLayoutBinding;
import com.netease.yunxin.kit.qchatkit.ui.message.emoji.EmojiAdapter;
import com.netease.yunxin.kit.qchatkit.ui.message.emoji.EmojiManager;
import com.netease.yunxin.kit.qchatkit.ui.message.input.ActionConstants;
import java.util.ArrayList;
import java.util.List;

/** 消息长按操作菜单 */
public class QChatPopMenu {

  private static final String TAG = "ChatPopMenu";

  private static final int DEFAULT_COLUMN_NUM = 5;

  // y offset for pop window
  private static final int Y_OFFSET = 8;

  private static final float ITEM_SIZE_WIDTH = 28f;

  private static final float ITEM_SIZE_HEIGHT = 42f;

  private static final float CONTAINER_PADDING = 16f;

  private static final float MENU_BAR_HEIGHT = 50f;

  private static final float EMOJI_GRID_VIEW_HEIGHT = 200f;
  private static final float EMOJI_POP_WIDTH = SizeUtils.dp2px(320);

  private final PopupWindow popupWindow;
  private final QChatPopMenuLayoutBinding layoutBinding;
  private final MenuAdapter adapter;
  private final List<QChatPopMenuAction> chatPopMenuActionList = new ArrayList<>();
  private QChatPopMenuAction chatPopMenuTitleAction;

  private QChatMessageInfo messageInfo;

  // 弹出之后保存，用于更新
  // popupX 为弹出的x坐标
  // popupY 为弹出的y坐标加上popHeight
  // 所以在更新的时候，需要减去popHeight
  private int popupX, popupY;
  // 是否在锚点View上面显示
  // 只有true在布局变化时才需要update
  boolean showTop;

  public QChatPopMenu() {
    layoutBinding =
        QChatPopMenuLayoutBinding.inflate(LayoutInflater.from(IMKitClient.getApplicationContext()));
    GridLayoutManager gridLayoutManager =
        new GridLayoutManager(IMKitClient.getApplicationContext(), DEFAULT_COLUMN_NUM);
    layoutBinding.recyclerView.setLayoutManager(gridLayoutManager);
    adapter = new MenuAdapter();
    layoutBinding.recyclerView.setAdapter(adapter);

    popupWindow =
        new PopupWindow(
            layoutBinding.getRoot(),
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            false);
    popupWindow.setTouchable(true);
    popupWindow.setOutsideTouchable(true);
  }

  /**
   * 加载标题栏中表情内容 初始化如果获取不到chatPopMenuTitleAction，则表示不需要展示快捷表情相关按钮
   *
   * @param message 消息
   */
  public void loadTitleBar(QChatMessageInfo message) {
    if (chatPopMenuTitleAction == null
        || chatPopMenuTitleAction.getActionData(ActionConstants.POP_ACTION_EXT_DATA) == null) {
      layoutBinding.qChatMenuBarLayout.setVisibility(View.GONE);
      layoutBinding.qChatMenuDivider.setVisibility(View.GONE);
      layoutBinding.qChatMenuEmojiGridView.setVisibility(View.GONE);
      return;
    } else {
      layoutBinding.qChatMenuBarLayout.setVisibility(View.VISIBLE);
      layoutBinding.qChatMenuDivider.setVisibility(View.VISIBLE);
      layoutBinding.qChatMenuEmojiGridView.setVisibility(View.VISIBLE);
    }
    List<Integer> emojiIndex =
        (List<Integer>) chatPopMenuTitleAction.getActionData(ActionConstants.POP_ACTION_EXT_DATA);

    if (emojiIndex != null && emojiIndex.size() > 0) {
      layoutBinding.qChatMenuEmojiBarLayout.setVisibility(View.VISIBLE);
      layoutBinding.qChatMenuEmojiBarLayout.removeAllViews();
      for (int index = 0; index < emojiIndex.size(); index++) {
        Drawable drawable =
            EmojiManager.getDrawable(layoutBinding.getRoot().getContext(), emojiIndex.get(index));
        if (drawable != null) {
          ImageView imageView = generateEmojiImageView();
          imageView.setImageDrawable(drawable);
          imageView.setTag(emojiIndex.get(index));
          imageView.setOnClickListener(
              v -> {
                if (chatPopMenuTitleAction.getActionClickListener() != null) {
                  chatPopMenuTitleAction.getActionClickListener().onClick(v, message);
                }
                hide();
              });
        }
      }

      if (emojiIndex.size() <= 6) {
        layoutBinding.qChatMenuEmojiBarBack.setVisibility(View.VISIBLE);
        layoutBinding.qChatMenuEmojiGridView.setVisibility(View.GONE);
        layoutBinding.qChatMenuEmojiBarBack.setOnClickListener(
            v -> {
              if (layoutBinding.qChatMenuEmojiGridView.getVisibility() == View.VISIBLE) {
                layoutBinding.qChatMenuEmojiGridView.setVisibility(View.GONE);
                layoutBinding.qChatMenuEmojiBarBack.setImageResource(
                    R.drawable.ic_qchat_emoji_arrow_down);
              } else {
                layoutBinding.qChatMenuEmojiGridView.setVisibility(View.VISIBLE);
                layoutBinding.qChatMenuEmojiBarBack.setImageResource(
                    R.drawable.ic_qchat_emoji_arrow_up);
              }
              boolean showEmoji =
                  layoutBinding.qChatMenuEmojiGridView.getVisibility() == View.VISIBLE;
              updateEmoji(showEmoji);
            });
      } else {
        layoutBinding.qChatMenuEmojiBarBack.setVisibility(View.GONE);
        layoutBinding.qChatMenuEmojiGridView.setVisibility(View.VISIBLE);
      }
      addEmoji(message);
    }
  }

  /**
   * 生成表情ImageView
   *
   * @return ImageView
   */
  private ImageView generateEmojiImageView() {
    ImageView imageView = new ImageView(layoutBinding.getRoot().getContext());
    LinearLayout.LayoutParams imageParam =
        new LinearLayout.LayoutParams((SizeUtils.dp2px(30)), SizeUtils.dp2px(30));
    imageParam.setMarginStart(SizeUtils.dp2px(13));
    imageParam.gravity = Gravity.CENTER_VERTICAL;
    layoutBinding.qChatMenuEmojiBarLayout.addView(imageView, imageParam);
    return imageView;
  }

  /**
   * 加载表情内容
   *
   * @param messageInfo 消息
   */
  private void addEmoji(QChatMessageInfo messageInfo) {
    EmojiAdapter emojiAdapter = new EmojiAdapter(layoutBinding.getRoot().getContext(), 0);
    emojiAdapter.setPageSize(EmojiManager.getDisplayCount());
    emojiAdapter.setShowDel(false);
    layoutBinding.qChatMenuEmojiGridView.setAdapter(emojiAdapter);
    layoutBinding.qChatMenuEmojiGridView.setOnItemClickListener(
        (parent, view, position, id) -> {
          if (chatPopMenuTitleAction.getActionClickListener() != null) {
            view.setTag(position);
            chatPopMenuTitleAction.getActionClickListener().onClick(view, messageInfo);
          }
          hide();
        });
  }

  /**
   * 检查是否需要隐藏 如果当前弹窗正在显示，且消息不为空，且消息的id和当前弹窗的消息id相同，则隐藏
   *
   * @param message 消息
   */
  public void checkPop(QChatMessageInfo message) {
    if (isShowing() && message != null) {
      if (messageInfo != null) {
        if (message.getMsgIdServer() == messageInfo.getMsgIdServer()
            || message.getUuid().equals(messageInfo.getUuid())) {
          hide();
        }
      } else {
        hide();
      }
    }
  }

  /**
   * 显示弹窗 如果onlyEmoji为true，则只展示Emoji表情列表，不展示操作删除等按钮和标题栏
   *
   * @param anchorView 锚点View
   * @param message 消息
   * @param minY 最小Y坐标
   * @param onlyEmoji 是否只显示表情
   */
  public void showEmoji(View anchorView, QChatMessageInfo message, int minY, boolean onlyEmoji) {
    showEmoji(anchorView, message, minY, onlyEmoji, false, false);
  }

  /**
   * 显示弹窗，只展示操作按钮，不展示Emoji标题栏
   *
   * @param anchorView 锚点View
   * @param message 消息
   * @param minY 最小Y坐标
   */
  public void show(View anchorView, QChatMessageInfo message, int minY) {
    show(anchorView, message, minY, false, false);
  }

  /**
   * 显示弹窗，操作按钮，不展示Emoji标题栏 如果是公告频道，会根据是否管理员，则展示相关权限下的操作功能 如果不公告频道，则正常展示操作功能
   *
   * @param anchorView 锚点View
   * @param message 消息
   * @param minY 最小Y坐标
   * @param isAnnounce 是否是公告
   * @param isManager 是否是管理员
   */
  public void show(
      View anchorView, QChatMessageInfo message, int minY, boolean isAnnounce, boolean isManager) {
    ALog.d(LIB_TAG, TAG, "show");
    adapter.setMessageInfo(message);
    initDefaultAction(message, false, false, isAnnounce, isManager);
    if (chatPopMenuActionList.size() < 1) {
      return;
    }
    computeLocationAndShow(anchorView, message, minY);
  }

  /**
   * 计算PopupWindow的位置并显示，不存在Emoji标题栏的弹窗大小计算
   *
   * @param anchorView 锚点View
   * @param message 消息
   * @param minY 最小Y坐标
   */
  private void computeLocationAndShow(View anchorView, QChatMessageInfo message, int minY) {
    float anchorWidth = anchorView.getWidth();
    float anchorHeight = anchorView.getHeight();
    int[] location = new int[2];
    anchorView.getLocationOnScreen(location);

    int rowCount = (int) Math.ceil(chatPopMenuActionList.size() * 1.0f / DEFAULT_COLUMN_NUM);
    if (popupWindow != null) {

      int itemWidth = SizeUtils.dp2px(ITEM_SIZE_WIDTH);
      int itemHeight = SizeUtils.dp2px(ITEM_SIZE_HEIGHT);

      int paddingLeftRight = SizeUtils.dp2px(CONTAINER_PADDING);
      int paddingTopBottom = SizeUtils.dp2px(CONTAINER_PADDING);

      int columnNum = Math.min(chatPopMenuActionList.size(), DEFAULT_COLUMN_NUM);
      GridLayoutManager gridLayoutManager =
          new GridLayoutManager(IMKitClient.getApplicationContext(), columnNum);
      layoutBinding.recyclerView.setLayoutManager(gridLayoutManager);
      int popWidth = itemWidth * columnNum + paddingLeftRight * (columnNum * 2);
      int popHeight = itemHeight * rowCount + paddingTopBottom * (rowCount + 1);

      int x = location[0];
      int y = location[1] - popHeight - Y_OFFSET;
      // if this is a send message,show on right
      if (message.getMessage().getDirect() == MsgDirectionEnum.Out) {
        x = (int) (location[0] + anchorWidth - popWidth);
      }
      // if is top show pop below anchorView,else show above
      boolean isTop = y <= minY;
      if (isTop) {
        y = (int) (location[1] + anchorHeight) + Y_OFFSET;
      }

      popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, x, y);
    }
  }

  /**
   * PopupWindow显示，存在Emoji标题栏的弹窗大小计算
   *
   * @param anchorView 锚点View
   * @param message 消息
   * @param minY 最小Y坐标
   * @param onlyEmoji 是否只显示表情
   * @param isAnnounce 是否是公告
   * @param isManager 是否是管理员
   */
  public void showEmoji(
      View anchorView,
      QChatMessageInfo message,
      int minY,
      boolean onlyEmoji,
      boolean isAnnounce,
      boolean isManager) {
    ALog.d(LIB_TAG, TAG, "show");
    adapter.setMessageInfo(message);
    initDefaultAction(message, true, !onlyEmoji, isAnnounce, isManager);
    if (message.getMessage().getStatus() == MsgStatusEnum.fail
        || message.getMessage().getStatus() == MsgStatusEnum.sending) {
      computeLocationAndShow(anchorView, message, minY);
    } else {
      computeLocationAndShowEmoji(anchorView, message, minY, onlyEmoji);
    }
  }

  /**
   * 计算PopupWindow的位置并显示，存在Emoji标题栏的弹窗大小计算
   *
   * @param anchorView 锚点View
   * @param message 消息
   * @param minY 最小Y坐标
   * @param onlyEmoji 是否只显示表情
   */
  private void computeLocationAndShowEmoji(
      View anchorView, QChatMessageInfo message, int minY, boolean onlyEmoji) {
    float anchorWidth = anchorView.getWidth();
    float anchorHeight = anchorView.getHeight();
    int[] location = new int[2];
    anchorView.getLocationOnScreen(location);

    int rowCount = (int) Math.ceil(chatPopMenuActionList.size() * 1.0f / DEFAULT_COLUMN_NUM);
    if (popupWindow != null) {

      int itemWidth = SizeUtils.dp2px(ITEM_SIZE_WIDTH);
      int itemHeight = SizeUtils.dp2px(ITEM_SIZE_HEIGHT);
      int menuBarHeight = SizeUtils.dp2px(MENU_BAR_HEIGHT);

      int paddingLeftRight = SizeUtils.dp2px(CONTAINER_PADDING);
      int paddingTopBottom = SizeUtils.dp2px(CONTAINER_PADDING);

      int columnNum = Math.min(chatPopMenuActionList.size(), DEFAULT_COLUMN_NUM);
      if (columnNum > 0) {
        GridLayoutManager gridLayoutManager =
            new GridLayoutManager(IMKitClient.getApplicationContext(), columnNum);
        layoutBinding.recyclerView.setLayoutManager(gridLayoutManager);
        layoutBinding.recyclerView.setVisibility(View.VISIBLE);
      } else {
        layoutBinding.recyclerView.setVisibility(View.GONE);
      }

      //带表情快捷回复的弹窗宽度是固定的
      float popWidth = EMOJI_POP_WIDTH;
      // popHeight 增加表情的高度
      int popHeight;
      if (onlyEmoji) {
        popHeight = menuBarHeight + SizeUtils.dp2px(EMOJI_GRID_VIEW_HEIGHT);
      } else {
        popHeight = itemHeight * rowCount + paddingTopBottom * (rowCount + 1) + menuBarHeight;
      }

      int x = location[0];
      int y = location[1] - popHeight - Y_OFFSET;
      // if this is a send message,show on right
      if (message.getMessage().getDirect() == MsgDirectionEnum.Out) {
        x = (int) (location[0] + anchorWidth - popWidth);
      }
      // if is top show pop below anchorView,else show above
      boolean isTop = y <= minY;
      if (isTop) {
        y = (int) (location[1] + anchorHeight) + Y_OFFSET;
      }

      // 保存弹出的位置
      popupX = x;
      popupY = y + popHeight;
      showTop = !isTop;
      popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, x, y);
    }
  }

  private void updateEmoji(boolean showEmoji) {
    int rowCount = (int) Math.ceil(chatPopMenuActionList.size() * 1.0f / DEFAULT_COLUMN_NUM);
    if (popupWindow != null && showTop) {
      int itemHeight = SizeUtils.dp2px(ITEM_SIZE_HEIGHT);
      int menuBarHeight = SizeUtils.dp2px(MENU_BAR_HEIGHT);

      int paddingTopBottom = SizeUtils.dp2px(CONTAINER_PADDING);

      // popHeight 增加表情的高度
      int popHeight;
      if (showEmoji) {
        popHeight = menuBarHeight + SizeUtils.dp2px(EMOJI_GRID_VIEW_HEIGHT);
      } else {
        popHeight = itemHeight * rowCount + paddingTopBottom * (rowCount + 1) + menuBarHeight;
      }
      int x = popupX;
      int y = popupY - popHeight;
      // 更新位置,需要更新的只有Y坐标，-1表示忽略
      popupWindow.update(x, y, -1, -1);
    }
  }

  public boolean isShowing() {
    return popupWindow != null && popupWindow.isShowing();
  }

  public void hide() {
    if (popupWindow != null && popupWindow.isShowing()) {
      popupWindow.dismiss();
    }
  }

  /**
   * 初始化默认的操作菜单
   *
   * @param message 消息
   * @param hasEmoji 是否有表情
   * @param useArrow 是否使用箭头
   * @param isAnnounce 是否是公告
   * @param isManager 是否是管理员
   */
  @SuppressLint("NotifyDataSetChanged")
  private void initDefaultAction(
      QChatMessageInfo message,
      boolean hasEmoji,
      boolean useArrow,
      boolean isAnnounce,
      boolean isManager) {
    chatPopMenuActionList.clear();
    messageInfo = message;
    if (isAnnounce) {
      chatPopMenuActionList.addAll(
          QChatPopActionFactory.getInstance().getAnnounceActions(message, isManager));
    } else {
      chatPopMenuActionList.addAll(QChatPopActionFactory.getInstance().getNormalActions(message));
    }

    chatPopMenuTitleAction = null;
    if (hasEmoji) {
      chatPopMenuTitleAction =
          QChatPopActionFactory.getInstance().getEmojiActions(message, useArrow);
    }
    loadTitleBar(message);
    adapter.notifyDataSetChanged();
  }

  private QChatPopMenuAction getChatPopMenuAction(int position) {
    return chatPopMenuActionList.get(position);
  }

  class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.MenuItemViewHolder> {

    private QChatMessageInfo messageInfo;

    public void setMessageInfo(QChatMessageInfo messageBean) {
      messageInfo = messageBean;
    }

    @NonNull
    @Override
    public MenuItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View view =
          LayoutInflater.from(IMKitClient.getApplicationContext())
              .inflate(R.layout.q_chat_pop_menu_item_layout, parent, false);
      return new MenuItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MenuItemViewHolder holder, int position) {
      QChatPopMenuAction chatPopMenuAction = getChatPopMenuAction(position);
      holder.title.setText(chatPopMenuAction.getTitle());
      Drawable drawable =
          ResourcesCompat.getDrawable(
              IMKitClient.getApplicationContext().getResources(),
              chatPopMenuAction.getIcon(),
              null);
      holder.icon.setImageDrawable(drawable);
      holder.itemView.setOnClickListener(
          v -> {
            if (chatPopMenuAction.getActionClickListener() != null) {
              chatPopMenuAction.getActionClickListener().onClick(v, messageInfo);
            }
            hide();
          });
    }

    @Override
    public int getItemCount() {
      return chatPopMenuActionList.size();
    }

    class MenuItemViewHolder extends RecyclerView.ViewHolder {
      public TextView title;
      public ImageView icon;

      public MenuItemViewHolder(@NonNull View itemView) {
        super(itemView);
        title = itemView.findViewById(R.id.menu_title);
        icon = itemView.findViewById(R.id.menu_icon);
      }
    }
  }
}
