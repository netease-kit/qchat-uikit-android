// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.common;

import com.netease.nimlib.sdk.qchat.enums.QChatQuickCommentOperateType;
import com.netease.nimlib.sdk.qchat.model.QChatQuickComment;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageQuickCommentDetailInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatQuickCommentDetailInfo;
import com.netease.yunxin.kit.qchatkit.ui.utils.MessageUtil;
import java.util.HashMap;
import java.util.Map;

public class QChatCache {
  private static final Map<Long, QChatMessageQuickCommentDetailInfo> quickCommentMap =
      new HashMap<>();

  public static void addQuickComment(Long msgSerId, QChatMessageQuickCommentDetailInfo detail) {
    quickCommentMap.put(msgSerId, detail);
  }

  public static void addQuickComment(Map<Long, QChatMessageQuickCommentDetailInfo> quickComment) {
    if (quickComment != null) {
      quickCommentMap.putAll(quickComment);
    }
  }

  public static QChatMessageQuickCommentDetailInfo getQuickComment(Long msgSerId) {
    return quickCommentMap.get(msgSerId);
  }

  public static boolean hasCommentType(Long msgSerId, int type) {
    if (quickCommentMap.containsKey(msgSerId)) {
      QChatMessageQuickCommentDetailInfo detailInfo = quickCommentMap.get(msgSerId);
      if (detailInfo != null
          && detailInfo.getDetails() != null
          && detailInfo.getDetails().size() > 0) {
        for (QChatQuickCommentDetailInfo detail : detailInfo.getDetails()) {
          if (detail.getType() == type) {
            return true;
          }
        }
      }
    }
    return false;
  }

  public static boolean hasSelfQuickComment(Long msgSerId, int commentId) {
    boolean hasComment = false;
    if (quickCommentMap.containsKey(msgSerId)) {
      QChatMessageQuickCommentDetailInfo detailInfo = quickCommentMap.get(msgSerId);
      if (detailInfo != null
          && detailInfo.getDetails() != null
          && detailInfo.getDetails().size() > 0) {
        for (QChatQuickCommentDetailInfo detail : detailInfo.getDetails()) {
          if (detail.getType() == commentId && detail.getHasSelf()) {
            hasComment = true;
            break;
          }
        }
      }
    }
    return hasComment;
  }

  public static void updateQuickComment(QChatQuickComment comment) {
    if (comment == null) return;
    long msgSerId = comment.getMsgIdServer();
    QChatMessageQuickCommentDetailInfo detailInfo = quickCommentMap.get(msgSerId);
    if (detailInfo == null && comment.getOperateType() == QChatQuickCommentOperateType.ADD) {
      detailInfo = MessageUtil.buildQuickCommentDetail(comment);
      quickCommentMap.put(msgSerId, detailInfo);
    } else if (detailInfo != null) {
      if (QChatQuickCommentOperateType.ADD == comment.getOperateType()) {
        MessageUtil.addQuickComment(detailInfo, comment);
      } else if (QChatQuickCommentOperateType.REMOVE == comment.getOperateType()) {
        MessageUtil.removeQuickComment(detailInfo, comment);
      }
    }
  }

  public static void clear() {
    quickCommentMap.clear();
  }
}
