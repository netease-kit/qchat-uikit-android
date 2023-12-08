// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.common.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.netease.yunxin.kit.common.utils.SizeUtils;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatQuickCommentDetailInfo;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.announce.QuickEmojiManager;
import com.netease.yunxin.kit.qchatkit.ui.server.viewmodel.QChatServerListViewModel;
import com.netease.yunxin.kit.qchatkit.ui.utils.MessageUtil;
import com.netease.yunxin.kit.qchatkit.ui.utils.QChatUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HorizontalEmojiGroup extends LinearLayout {
  private static final int COLOR_ITEM_TAG_BG = Color.WHITE;
  private static final int COLOR_ITEM_TAG_TEXT = Color.parseColor("#ff666666");
  private static final int COLOR_ITEM_SELECT_TAG_TEXT = Color.parseColor("#337EFF");
  private static final int RADIUS_DP_ITEM_TAG = 14;
  private static final int TEXT_FONT_SIZE_ITEM_TAG = 14;
  private static final int HORIZONTAL_ITEM_TAG_PADDING = 6;
  private static final int HORIZONTAL_ITEM_MARGIN = 6;
  private static final int VERTICAL_ITEM_MARGIN = 6;
  private static final int MAX_ROW_COUNT = 50;

  private OnItemClickListener itemClickListener;

  private final List<QChatQuickCommentDetailInfo> tagList = new ArrayList<>();
  private final Map<Integer, QChatQuickCommentDetailInfo> tagMap = new HashMap<>();

  public HorizontalEmojiGroup(Context context) {
    super(context);
    init();
  }

  public HorizontalEmojiGroup(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public HorizontalEmojiGroup(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
    setOrientation(LinearLayout.HORIZONTAL);
  }

  private View.OnClickListener onClickListener =
      new OnClickListener() {
        @Override
        public void onClick(View v) {
          if (itemClickListener != null) {
            int type = (int) v.getTag();
            QChatQuickCommentDetailInfo detailInfo = tagMap.get(type);
            itemClickListener.onItemClick(v, type, detailInfo);
          }
        }
      };

  public void setItemClickListener(OnItemClickListener onClickListener) {
    this.itemClickListener = onClickListener;
  }

  public void setData(QChatMessageInfo data, List<QChatQuickCommentDetailInfo> tagList) {
    if (tagList != null && tagList.size() > 0) {
      this.tagList.clear();
      this.tagList.addAll(tagList);
      tagMap.clear();

      removeAllViews();
      if (QuickEmojiManager.isAllowEmojiReply()
          && !QChatServerListViewModel.ServerVisitorInfoMgr.getInstance()
              .isVisitor(data.getQChatServerId())) {
        addView(generateAddEmoji());
      }

      for (QChatQuickCommentDetailInfo tag : this.tagList) {
        tagMap.put(tag.getType(), tag);
        TextView textView = prepareTextView();

        SpannableStringBuilder mSpannableString =
            new SpannableStringBuilder("a " + QChatUtils.generateNumberText(tag.getCount()));
        Drawable d = MessageUtil.getEmotDrawable(getContext(), tag.getType() - 1, 0.53f);
        if (d != null) {
          ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_CENTER);
          mSpannableString.setSpan(span, 0, "a".length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
          AlignCenterTextSpan textSpan = new AlignCenterTextSpan();
          mSpannableString.setSpan(
              textSpan,
              "a ".length(),
              mSpannableString.length(),
              Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
        if (tag.getHasSelf()) {
          textView.setTextColor(COLOR_ITEM_SELECT_TAG_TEXT);
        } else {
          textView.setTextColor(COLOR_ITEM_TAG_TEXT);
        }
        textView.setTag(tag.getType());
        textView.setText(mSpannableString);
        textView.setBackground(prepareTagBg());
        textView.setOnClickListener(onClickListener);
        addView(textView);
      }
    }
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
    final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
    final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

    measureChildren(widthMeasureSpec, heightMeasureSpec);

    int horizontalMargin = SizeUtils.dp2px(HORIZONTAL_ITEM_MARGIN);
    int verticalMargin = SizeUtils.dp2px(VERTICAL_ITEM_MARGIN);

    int width;
    int height = 0;

    int row = 0;
    int rowWidth = 0;
    int rowMaxHeight = 0;

    final int count = getChildCount();
    for (int i = 0; i < count; i++) {
      View child = getChildAt(i);
      int childWidth = child.getMeasuredWidth();
      int childHeight = child.getMeasuredHeight();

      if (child.getVisibility() != GONE) {
        if (row + 1 >= MAX_ROW_COUNT && rowWidth + childWidth > widthSize) {
          break;
        }
        rowWidth += childWidth;
        if (rowWidth > widthSize) {
          rowWidth = childWidth;
          height += rowMaxHeight + verticalMargin;
          rowMaxHeight = childHeight;
          row++;
        } else {
          rowMaxHeight = Math.max(rowMaxHeight, childHeight);
        }
        rowWidth += horizontalMargin;
      }
    }

    height += getPaddingTop() + getPaddingBottom() + rowMaxHeight;
    if (row == 0) {
      width = rowWidth;
      width += getPaddingLeft() + getPaddingRight();
    } else {
      width = widthSize;
    }

    setMeasuredDimension(
        widthMode == MeasureSpec.EXACTLY ? widthSize : width,
        heightMode == MeasureSpec.EXACTLY ? heightSize : height);
  }

  private TextView generateAddEmoji() {
    // 添加快捷Emoji
    TextView addEmojiTv = prepareTextView();
    SpannableString spannableString = new SpannableString("a");
    int verticalPadding = SizeUtils.dp2px(4);
    int drawableSize = SizeUtils.dp2px(18);
    addEmojiTv.setPadding(verticalPadding, verticalPadding, verticalPadding, verticalPadding);
    Drawable drawable = getContext().getDrawable(R.drawable.ic_qchat_emoji_pop_add);
    drawable.setBounds(0, 0, drawableSize, drawableSize);
    ImageSpan imagespan = new ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM);
    spannableString.setSpan(imagespan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    addEmojiTv.setText(spannableString);
    addEmojiTv.setBackground(prepareTagBg());
    addEmojiTv.setOnClickListener(onClickListener);
    addEmojiTv.setTag(0);
    return addEmojiTv;
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    final int parentLeft = getPaddingLeft();
    final int parentRight = r - l - getPaddingRight();
    final int parentTop = getPaddingTop();

    int horizontalMargin = SizeUtils.dp2px(HORIZONTAL_ITEM_MARGIN);
    int verticalMargin = SizeUtils.dp2px(VERTICAL_ITEM_MARGIN);

    int childLeft = parentLeft;
    int childTop = parentTop;

    int row = 0;
    int rowMaxHeight = 0;

    int count = getChildCount();
    for (int i = 0; i < count; i++) {
      final View child = getChildAt(i);
      final int width = child.getMeasuredWidth();
      final int height = child.getMeasuredHeight();

      if (child.getVisibility() != GONE) {
        if (row + 1 >= MAX_ROW_COUNT && childLeft + width + horizontalMargin > parentRight) {
          break;
        }
        if (childLeft + width > parentRight) { // Next line
          childLeft = parentLeft;
          childTop += rowMaxHeight + verticalMargin;
          rowMaxHeight = height;
          row++;
        } else {
          rowMaxHeight = Math.max(rowMaxHeight, height);
        }

        child.layout(childLeft, childTop, childLeft + width, childTop + height);
        childLeft += width + horizontalMargin;
      }
    }
  }

  private TextView prepareTextView() {
    TextView textView = new TextView(getContext());
    textView.setTextSize(TEXT_FONT_SIZE_ITEM_TAG);
    int horizontalPadding = SizeUtils.dp2px(HORIZONTAL_ITEM_TAG_PADDING);
    textView.setPadding(horizontalPadding, 0, horizontalPadding, 0);
    textView.setMaxLines(1);
    textView.setEllipsize(TextUtils.TruncateAt.END);
    textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
    textView.setGravity(Gravity.CENTER_VERTICAL);
    return textView;
  }

  private Drawable prepareTagBg() {
    GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(COLOR_ITEM_TAG_BG);
    drawable.setCornerRadius(SizeUtils.dp2px(RADIUS_DP_ITEM_TAG));
    return drawable;
  }

  public static interface OnItemClickListener {
    void onItemClick(View view, int type, QChatQuickCommentDetailInfo detailInfo);
  }
}
