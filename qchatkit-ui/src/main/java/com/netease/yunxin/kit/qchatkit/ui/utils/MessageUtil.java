// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.utils;

import android.Manifest;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.view.View;
import android.widget.TextView;
import com.netease.nimlib.sdk.msg.attachment.ImageAttachment;
import com.netease.nimlib.sdk.msg.constant.AttachStatusEnum;
import com.netease.nimlib.sdk.msg.constant.MsgDirectionEnum;
import com.netease.nimlib.sdk.msg.constant.MsgTypeEnum;
import com.netease.nimlib.sdk.qchat.model.QChatMessage;
import com.netease.nimlib.sdk.qchat.model.QChatMsgUpdateInfo;
import com.netease.nimlib.sdk.qchat.model.QChatQuickComment;
import com.netease.nimlib.sdk.qchat.param.QChatRevokeMessageParam;
import com.netease.nimlib.sdk.qchat.param.QChatUpdateParam;
import com.netease.yunxin.kit.alog.ALog;
import com.netease.yunxin.kit.common.utils.ImageUtils;
import com.netease.yunxin.kit.corekit.im2.IMKitClient;
import com.netease.yunxin.kit.qchatkit.QChatKitClient;
import com.netease.yunxin.kit.qchatkit.QChatMsgConstants;
import com.netease.yunxin.kit.qchatkit.repo.QChatMessageRepo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageQuickCommentDetailInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatQuickCommentDetailInfo;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.image.QChatWatchImageActivity;
import com.netease.yunxin.kit.qchatkit.ui.message.emoji.EmojiManager;
import com.netease.yunxin.kit.qchatkit.utils.MessageRevokeHelper;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONException;
import org.json.JSONObject;

public class MessageUtil {

  public static final String TAG = "MessageUtil";
  private static final float DEF_SCALE = 0.6f;
  private static final float SMALL_SCALE = 0.6F;
  public static final int REVOKE_TIME_INTERVAL = 2 * 60 * 1000;

  public static void identifyFaceExpression(
      Context context, View textView, String value, int align) {
    identifyFaceExpression(context, textView, value, align, DEF_SCALE);
  }

  private static void viewSetText(View textView, SpannableString mSpannableString) {
    if (textView instanceof TextView) {
      TextView tv = (TextView) textView;
      tv.setText(mSpannableString);
    }
  }

  public static void identifyFaceExpression(
      Context context, View textView, String value, int align, float scale) {
    SpannableString mSpannableString = replaceEmoticons(context, value, scale, align);
    viewSetText(textView, mSpannableString);
  }

  private static SpannableString replaceEmoticons(
      Context context, String value, float scale, int align) {
    if (TextUtils.isEmpty(value)) {
      value = "";
    }

    SpannableString mSpannableString = new SpannableString(value);
    Matcher matcher = EmojiManager.getPattern().matcher(value);
    while (matcher.find()) {
      int start = matcher.start();
      int end = matcher.end();
      String emot = value.substring(start, end);
      Drawable d = getEmotDrawable(context, emot, scale);
      if (d != null) {
        ImageSpan span = new ImageSpan(d, align);
        mSpannableString.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      }
    }
    return mSpannableString;
  }

  private static Pattern mATagPattern = Pattern.compile("<a.*?>.*?</a>");

  public static void replaceEmoticons(Context context, Editable editable, int start, int count) {
    if (count <= 0 || editable.length() < start + count) return;

    CharSequence s = editable.subSequence(start, start + count);
    Matcher matcher = EmojiManager.getPattern().matcher(s);
    while (matcher.find()) {
      int from = start + matcher.start();
      int to = start + matcher.end();
      String emot = editable.subSequence(from, to).toString();
      Drawable d = getEmotDrawable(context, emot, SMALL_SCALE);
      if (d != null) {
        ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
        editable.setSpan(span, from, to, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      }
    }
  }

  public static Drawable getEmotDrawable(Context context, String text, float scale) {
    Drawable drawable = EmojiManager.getDrawable(context, text);

    // scale
    if (drawable != null) {
      int width = (int) (drawable.getIntrinsicWidth() * scale);
      int height = (int) (drawable.getIntrinsicHeight() * scale);
      drawable.setBounds(0, 0, width, height);
    }

    return drawable;
  }

  public static Drawable getEmotDrawable(Context context, int text, float scale) {
    Drawable drawable = EmojiManager.getDrawable(context, text);

    // scale
    if (drawable != null) {
      int width = (int) (drawable.getIntrinsicWidth() * scale);
      int height = (int) (drawable.getIntrinsicHeight() * scale);
      drawable.setBounds(0, 0, width, height);
    }

    return drawable;
  }

  private static class ATagSpan extends ClickableSpan {
    private int start;
    private int end;
    private String mUrl;
    private String tag;

    ATagSpan(String tag, String url) {
      this.tag = tag;
      this.mUrl = url;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
      super.updateDrawState(ds);
      ds.setUnderlineText(true);
    }

    public String getTag() {
      return tag;
    }

    public void setRange(int start, int end) {
      this.start = start;
      this.end = end;
    }

    @Override
    public void onClick(View view) {
      try {
        if (TextUtils.isEmpty(mUrl)) return;
        Uri uri = Uri.parse(mUrl);
        String scheme = uri.getScheme();
        if (TextUtils.isEmpty(scheme)) {
          mUrl = "http://" + mUrl;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public static void startWatchImage(
      Context context, QChatMessageInfo messageInfo, List<QChatMessageInfo> imageMessages) {
    int index = 0;
    if (messageInfo.getMessage().getAttachStatus() != AttachStatusEnum.transferred
        && messageInfo.getMessage().getAttachStatus() != AttachStatusEnum.transferring) {
      QChatMessageRepo.downloadAttachment(messageInfo, true, null);
    }
    ArrayList<QChatMessageInfo> messages = new ArrayList<>();
    int maxLimit = 100;
    int halfLimit = 50;
    int arraySize = imageMessages.size();
    for (int i = 0; i < arraySize; ++i) {
      if (messageInfo.equals(imageMessages.get(i))) {
        index = i;
      }
      messages.add(imageMessages.get(i));
    }
    // 防止消息数量过多造成传递数据超限，设置消息最多100个
    if (arraySize > maxLimit) {
      int start = 0;
      int end = arraySize;
      if (index > halfLimit) {
        if (index + halfLimit >= arraySize) {
          start = arraySize - maxLimit;
          index = index - start;
        } else if (index + halfLimit < arraySize) {
          start = index - halfLimit;
          end = index + halfLimit;
          index = halfLimit;
        }
      } else {
        end = maxLimit;
      }
      messages = new ArrayList<>(messages.subList(start, end));
    }
    QChatWatchImageActivity.launch(context, messages, index);
  }

  public static ImageAttachment createImageMessage(String path) {
    ImageAttachment imageAttachment = new ImageAttachment();
    imageAttachment.setPath(path);
    File imageFile = new File(path);
    if (imageFile.exists()) {
      imageAttachment.setSize(imageFile.length());
      int[] dimension = ImageUtils.getSize(path);
      imageAttachment.setWidth(dimension[0]);
      imageAttachment.setHeight(dimension[1]);
    }
    ALog.d(TAG, "createImageMessage", "info:" + path);
    return imageAttachment;
  }

  public static String getPermissionText(Context context, String permission) {
    String text = context.getString(R.string.qchat_permission_default);
    if (TextUtils.equals(permission, Manifest.permission.CAMERA)) {
      text = context.getString(R.string.qchat_permission_camera);
    } else if (TextUtils.equals(permission, Manifest.permission.READ_EXTERNAL_STORAGE)) {
      text = context.getString(R.string.qchat_permission_storage);
    } else if (TextUtils.equals(permission, Manifest.permission.RECORD_AUDIO)) {
      text = context.getString(R.string.qchat_permission_audio);
    }
    return text;
  }

  public static boolean revokeMsgIsEdit(QChatMessageInfo data) {
    QChatMessage message = data.getMessage();
    return !isReceivedMessage(data)
        && message.getMsgType() == MsgTypeEnum.text
        && (System.currentTimeMillis() - message.getTime() < REVOKE_TIME_INTERVAL)
        && !TextUtils.isEmpty(MessageRevokeHelper.getContent(data.getMsgIdServer()));
  }

  public static boolean isReceivedMessage(QChatMessageInfo message) {
    return message.getMessage().getDirect() == MsgDirectionEnum.In;
  }

  public static boolean loadRevokeMessage(QChatMessageInfo message) {
    QChatMsgUpdateInfo updateInfo = message.getMessage().getUpdateOperatorInfo();
    if (updateInfo == null) {
      return false;
    }
    String extStr = updateInfo.getExt();
    if (!TextUtils.isEmpty(extStr)) {
      try {

        JSONObject jsonObject = new JSONObject(extStr);
        boolean isRevoke = false;
        if (jsonObject.has(QChatMsgConstants.KEY_QCHAT_REVOKE_MSG)) {
          isRevoke = jsonObject.getBoolean(QChatMsgConstants.KEY_QCHAT_REVOKE_MSG);
        }
        if (jsonObject.has(QChatMsgConstants.KEY_QCHAT_REVOKE_MSG_CONTENT)) {
          String content = jsonObject.getString(QChatMsgConstants.KEY_QCHAT_REVOKE_MSG_CONTENT);
          message.setRevokeText(content);
        }
        message.setRevoke(isRevoke);
        return isRevoke;
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }
    return false;
  }

  public static QChatRevokeMessageParam buildRevokeParam(QChatMessageInfo message) {
    QChatUpdateParam updateParam = new QChatUpdateParam();
    updateParam.setPostscript(
        IMKitClient.getApplicationContext().getString(R.string.qchat_message_revoked));
    JSONObject extJson = new JSONObject();
    try {
      extJson.put(QChatMsgConstants.KEY_QCHAT_REVOKE_MSG, true);
      if (message.getMsgType() == MsgTypeEnum.text) {
        MessageRevokeHelper.recordContent(message.getMsgIdServer(), message.getContent());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    updateParam.setExtension(extJson.toString());

    return new QChatRevokeMessageParam(
        updateParam,
        message.getQChatServerId(),
        message.getQChatChannelId(),
        message.getTime(),
        message.getMsgIdServer());
  }

  public static QChatMessageQuickCommentDetailInfo buildQuickCommentDetail(
      QChatQuickComment comment) {
    List<QChatQuickCommentDetailInfo> detailInfos = new ArrayList<>();
    List<String> opAccid = new ArrayList<>();
    opAccid.add(comment.getOpeAccid());
    QChatQuickCommentDetailInfo detail =
        new QChatQuickCommentDetailInfo(
            comment.getType(),
            1,
            TextUtils.equals(comment.getOpeAccid(), QChatKitClient.account()),
            System.currentTimeMillis(),
            opAccid);
    detailInfos.add(detail);
    return new QChatMessageQuickCommentDetailInfo(
        comment.getServerId(),
        comment.getChannelId(),
        comment.getMsgIdServer(),
        1,
        System.currentTimeMillis(),
        detailInfos);
  }

  public static void addQuickComment(
      QChatMessageQuickCommentDetailInfo detailInfo, QChatQuickComment comment) {
    detailInfo.setTotalCount(detailInfo.getTotalCount() + 1);
    List<QChatQuickCommentDetailInfo> commentDetail = detailInfo.getDetails();
    if (commentDetail == null) {
      commentDetail = new ArrayList<>();
    }
    boolean hasType = false;
    for (QChatQuickCommentDetailInfo detail : commentDetail) {
      if (detail.getType() == comment.getType()) {
        detail.setCount(detail.getCount() + 1);
        detail.getSeveralAccids().add(comment.getOpeAccid());
        if (!detail.getHasSelf()
            && TextUtils.equals(comment.getOpeAccid(), QChatKitClient.account())) {
          detail.setHasSelf(true);
        }
        hasType = true;
        break;
      }
    }

    if (!hasType) {
      List<String> serAccids = new ArrayList<>();
      serAccids.add(comment.getOpeAccid());
      QChatQuickCommentDetailInfo detail =
          new QChatQuickCommentDetailInfo(
              comment.getType(),
              1,
              TextUtils.equals(comment.getOpeAccid(), QChatKitClient.account()),
              System.currentTimeMillis(),
              serAccids);
      commentDetail.add(detail);
    }
  }

  public static void removeQuickComment(
      QChatMessageQuickCommentDetailInfo detailInfo, QChatQuickComment comment) {
    List<QChatQuickCommentDetailInfo> commentDetail = detailInfo.getDetails();
    if (commentDetail == null) return;
    for (int index = commentDetail.size() - 1; index >= 0; index--) {
      QChatQuickCommentDetailInfo detail = commentDetail.get(index);
      if (detail.getType() == comment.getType()) {
        if (detail.getCount() > 1) {
          detail.setCount(detail.getCount() - 1);
          detail.getSeveralAccids().remove(comment.getOpeAccid());
          if (detail.getHasSelf()
              && TextUtils.equals(comment.getOpeAccid(), QChatKitClient.account())) {
            detail.setHasSelf(false);
          }
        } else {
          commentDetail.remove(detail);
        }
        break;
      }
    }
    detailInfo.setTotalCount(detailInfo.getTotalCount() - 1);
  }
}
