// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.common.widget;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.ReplacementSpan;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class AlignCenterTextSpan extends ReplacementSpan {
  @Override
  public int getSize(
      @NonNull Paint paint,
      CharSequence text,
      int start,
      int end,
      @Nullable Paint.FontMetricsInt fm) {
    return (int) Math.ceil(paint.measureText(String.valueOf(text.subSequence(start, end))));
  }

  @Override
  public void draw(
      @NonNull Canvas canvas,
      CharSequence text,
      int start,
      int end,
      float x,
      int top,
      int y,
      int bottom,
      @NonNull Paint paint) {
    canvas.save();
    canvas.drawText(
        String.valueOf(text.subSequence(start, end)), x, (bottom - top) / 2f + 16, paint);
  }
}
