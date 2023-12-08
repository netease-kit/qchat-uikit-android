// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.message.viewholder;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.netease.nimlib.sdk.msg.attachment.ImageAttachment;
import com.netease.yunxin.kit.common.ui.widgets.ShapeDrawable;
import com.netease.yunxin.kit.common.utils.ImageUtils;
import com.netease.yunxin.kit.common.utils.ScreenUtils;
import com.netease.yunxin.kit.common.utils.SizeUtils;
import com.netease.yunxin.kit.corekit.im.provider.FetchCallback;
import com.netease.yunxin.kit.qchatkit.repo.QChatMessageRepo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageInfo;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatBaseMessageViewHolderBinding;
import com.netease.yunxin.kit.qchatkit.ui.databinding.QChatImageMessageViewHolderBinding;

/** 图片消息ViewHolder */
public class QChatImageMessageViewHolder extends QChatBaseMessageViewHolder {

  private QChatImageMessageViewHolderBinding imageBinding;
  protected static int maxEdge = 0;
  protected static int minEdge = 0;

  public QChatImageMessageViewHolder(@NonNull QChatBaseMessageViewHolderBinding parent) {
    super(parent);
  }

  @Override
  public void addContainer() {
    imageBinding =
        QChatImageMessageViewHolderBinding.inflate(
            LayoutInflater.from(getParent().getContext()), getContainer(), true);
  }

  @Override
  public void bindData(QChatMessageInfo data, int position, QChatMessageInfo lastMessage) {
    super.bindData(data, position, lastMessage);
    String imageAttachStr = data.getAttachStr();
    if (TextUtils.isEmpty(imageAttachStr)) {
      return;
    }
    ImageAttachment imageAttachment = new ImageAttachment(imageAttachStr);
    if (TextUtils.isEmpty(imageAttachment.getPath())
        && TextUtils.isEmpty(imageAttachment.getUrl())) {
      return;
    }
    data.setAttachment(imageAttachment);
    String path = imageAttachment.getPath();
    if (TextUtils.isEmpty(path)) {
      path = imageAttachment.getUrl();
    }

    int[] bounds = getBounds(path);
    int w = bounds[0];
    int h = bounds[1];
    if (w == 0 || h == 0) {
      w = getImageMaxEdge();
      h = getImageMaxEdge();
    }
    int thumbMinEdge = getImageThumbMinEdge();
    if (w < thumbMinEdge) {
      w = thumbMinEdge;
      h = bounds[0] != 0 ? w * bounds[1] / bounds[0] : 0;
    }
    int thumbMaxEdge = getImageMaxEdge();
    int thumbMaxHeight = (int) (0.3 * ScreenUtils.getDisplayHeight());
    if (w > thumbMaxEdge) {
      w = thumbMaxEdge;
      h = w * bounds[1] / bounds[0];
    }
    if (h > thumbMaxHeight) {
      h = thumbMaxHeight;
    }

    final int width = w;
    final int height = h;

    FrameLayout.LayoutParams thumbParams =
        (FrameLayout.LayoutParams) imageBinding.getRoot().getLayoutParams();
    thumbParams.width = w;
    thumbParams.height = h;
    imageBinding.getRoot().setLayoutParams(thumbParams);

    float[] corners = getCorners();
    ShapeDrawable.Builder shapeBuilder =
        new ShapeDrawable.Builder()
            .setStroke(1, ContextCompat.getColor(getContainer().getContext(), R.color.color_e2e5e8))
            .setRadii(
                new float[] {
                  corners[0],
                  corners[0],
                  corners[1],
                  corners[1],
                  corners[2],
                  corners[2],
                  corners[3],
                  corners[3]
                });
    if (path == null) {
      shapeBuilder.setSolid(Color.BLACK);
    }
    imageBinding.getRoot().setBackground(shapeBuilder.build());

    if (!TextUtils.isEmpty(imageAttachment.getPath())
        || !TextUtils.isEmpty(imageAttachment.getThumbPath())) {
      loadImageFromPath(imageAttachment, width, height, corners);
    } else {
      QChatMessageRepo.downloadAttachment(
          currentMessage,
          true,
          new FetchCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void param) {
              loadImageFromPath(imageAttachment, width, height, corners);
            }

            @Override
            public void onFailed(int code) {
              Glide.with(itemView.getContext())
                  .load(R.drawable.bg_image_loading_qchat)
                  .into(imageBinding.messageImage);
            }

            @Override
            public void onException(@Nullable Throwable exception) {
              Glide.with(itemView.getContext())
                  .load(R.drawable.bg_image_loading_qchat)
                  .into(imageBinding.messageImage);
            }
          });
    }
  }

  private void loadImageFromPath(
      ImageAttachment imageAttachment, int width, int height, float[] corners) {
    String loadPath =
        TextUtils.isEmpty(imageAttachment.getPath())
            ? imageAttachment.getThumbPath()
            : imageAttachment.getPath();
    Glide.with(itemView.getContext())
        .load(loadPath)
        .override(width, height)
        .placeholder(R.drawable.bg_image_loading_qchat)
        .into(imageBinding.messageImage);
  }

  private int getImageMaxEdge() {
    if (maxEdge == 0) {
      maxEdge = (int) (0.40 * ScreenUtils.getDisplayWidth());
    }
    return maxEdge;
  }

  private int getImageThumbMinEdge() {
    if (minEdge == 0) {
      minEdge = (int) (0.3 * ScreenUtils.getDisplayWidth());
    }
    return minEdge;
  }

  protected float[] getCorners() {
    int corner = SizeUtils.dp2px(12);
    boolean msgIn = isReceivedMessage(currentMessage);
    float radiusTopLeft = msgIn ? 0 : corner;
    float radiusTopRight = msgIn ? corner : 0;
    return new float[] {radiusTopLeft, radiusTopRight, corner, corner};
  }

  protected int[] getBounds(String path) {
    int[] bounds = null;
    if (path != null) {
      bounds = ImageUtils.getSize(path);
    }
    if (bounds == null || bounds[0] == 0) {
      ImageAttachment attachment = (ImageAttachment) currentMessage.getAttachment();
      bounds = new int[] {attachment.getWidth(), attachment.getHeight()};
    }
    return bounds;
  }
}
