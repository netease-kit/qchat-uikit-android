// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.message;

import static com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant.LIB_TAG;

import android.text.TextUtils;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import com.netease.nimlib.qchat.model.systemnotification.QChatSystemNotificationAttachmentImpl;
import com.netease.nimlib.sdk.ResponseCode;
import com.netease.nimlib.sdk.msg.attachment.AudioAttachment;
import com.netease.nimlib.sdk.msg.attachment.ImageAttachment;
import com.netease.nimlib.sdk.msg.constant.MsgStatusEnum;
import com.netease.nimlib.sdk.msg.constant.MsgTypeEnum;
import com.netease.nimlib.sdk.qchat.enums.QChatInOutType;
import com.netease.nimlib.sdk.qchat.enums.QChatQuickCommentOperateType;
import com.netease.nimlib.sdk.qchat.model.QChatQuickComment;
import com.netease.nimlib.sdk.qchat.model.systemnotification.QChatAddServerRoleMembersAttachment;
import com.netease.nimlib.sdk.qchat.model.systemnotification.QChatDeleteServerRoleMembersAttachment;
import com.netease.nimlib.sdk.qchat.model.systemnotification.QChatQuickCommentAttachment;
import com.netease.nimlib.sdk.qchat.model.systemnotification.QChatServerEnterLeaveAttachment;
import com.netease.nimlib.sdk.qchat.param.QChatRevokeMessageParam;
import com.netease.nimlib.sdk.qchat.param.QChatSendMessageParam;
import com.netease.yunxin.kit.alog.ALog;
import com.netease.yunxin.kit.common.ui.utils.ToastX;
import com.netease.yunxin.kit.common.ui.viewmodel.BaseViewModel;
import com.netease.yunxin.kit.common.ui.viewmodel.FetchResult;
import com.netease.yunxin.kit.common.ui.viewmodel.LoadStatus;
import com.netease.yunxin.kit.corekit.event.EventCenter;
import com.netease.yunxin.kit.corekit.im.model.EventObserver;
import com.netease.yunxin.kit.corekit.im.provider.FetchCallback;
import com.netease.yunxin.kit.corekit.qchat.QChatKitClient;
import com.netease.yunxin.kit.qchatkit.TimerCacheWithQChatMsg;
import com.netease.yunxin.kit.qchatkit.repo.QChatMessageRepo;
import com.netease.yunxin.kit.qchatkit.repo.QChatRoleRepo;
import com.netease.yunxin.kit.qchatkit.repo.QChatServiceObserverRepo;
import com.netease.yunxin.kit.qchatkit.repo.model.ChannelUpdateQuickComment;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatGetQuickCommentsResultInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageDeleteEventInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatMessageRevokeEventInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerRoleInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSystemNotificationInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSystemNotificationTypeInfo;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerEnterLeave;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerRoleMemberAdd;
import com.netease.yunxin.kit.qchatkit.repo.model.ServerRoleMemberDelete;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.common.QChatCache;
import com.netease.yunxin.kit.qchatkit.ui.message.model.QChatMsgEvent;
import com.netease.yunxin.kit.qchatkit.ui.message.model.QChatQuickCommentImpl;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant;
import com.netease.yunxin.kit.qchatkit.ui.utils.MessageUtil;
import com.netease.yunxin.kit.qchatkit.ui.utils.QChatUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** message view model fetch and send message to channel */
public class MessageViewModel extends BaseViewModel {

  private static final String TAG = "MessageViewModel";
  private static final long TIME_FOR_CACHE_MSG = 100; // 毫秒

  private int messagePageSize = 100;
  private QChatMessageInfo forwardMessage;
  private boolean hasForward = true;
  private long mServerId;
  private long mChannelId;
  private final TimerCacheWithQChatMsg timerCache = new TimerCacheWithQChatMsg();
  private final MutableLiveData<FetchResult<List<QChatMessageInfo>>> messageLiveData =
      new MutableLiveData<>();

  private final MutableLiveData<FetchResult<List<Long>>> quickCommentLiveData =
      new MutableLiveData<>();
  private final FetchResult<List<QChatMessageInfo>> messageFetchResult =
      new FetchResult<>(LoadStatus.Finish);

  private final MutableLiveData<FetchResult<QChatMessageInfo>> sendMessageLiveData =
      new MutableLiveData<>();

  private final MutableLiveData<FetchResult<QChatServerRoleInfo>> serverRoleLiveData =
      new MutableLiveData<>();
  private final FetchResult<QChatMessageInfo> sendMessageFetchResult =
      new FetchResult<>(LoadStatus.Finish);
  private final MutableLiveData<FetchResult<QChatMessageInfo>> deleteMessageLiveData =
      new MutableLiveData<>();
  private final MutableLiveData<FetchResult<QChatMessageInfo>> revokeMessageLiveData =
      new MutableLiveData<>();

  private final MutableLiveData<FetchResult<Boolean>> enterLeaveServerLiveData =
      new MutableLiveData<>();

  private final Comparator<QChatMessageInfo> comparator =
      (o1, o2) -> {
        long result = o1.getTime() - o2.getTime();
        return result == 0 ? 0 : (result < 0 ? -1 : 1);
      };

  // fetch message live data
  public MutableLiveData<FetchResult<List<QChatMessageInfo>>> getQueryMessageLiveData() {
    return messageLiveData;
  }

  // send message live data
  public MutableLiveData<FetchResult<QChatMessageInfo>> getSendMessageLiveData() {
    return sendMessageLiveData;
  }

  public MutableLiveData<FetchResult<QChatServerRoleInfo>> getServerRoleLiveData() {
    return serverRoleLiveData;
  }

  public MutableLiveData<FetchResult<QChatMessageInfo>> getDeleteMessageLiveData() {
    return deleteMessageLiveData;
  }

  public MutableLiveData<FetchResult<QChatMessageInfo>> getRevokeMessageLiveData() {
    return revokeMessageLiveData;
  }

  public MutableLiveData<FetchResult<List<Long>>> getQuickCommentLiveData() {
    return quickCommentLiveData;
  }

  public MutableLiveData<FetchResult<Boolean>> getEnterLeaveServerLiveData() {
    return enterLeaveServerLiveData;
  }

  public void init(long serverId, long channelId) {
    mServerId = serverId;
    mChannelId = channelId;
    ALog.d(TAG, "init", "info:" + mServerId + "," + mChannelId);
    timerCache.init(
        TIME_FOR_CACHE_MSG,
        cache -> {
          // 按照消息时间戳排序
          Collections.sort(cache, comparator);
          messageFetchResult.setLoadStatus(LoadStatus.Finish);
          messageFetchResult.setData(cache);
          messageFetchResult.setType(FetchResult.FetchType.Add);
          messageFetchResult.setTypeIndex(-1);
          messageLiveData.setValue(messageFetchResult);
          return null;
        });
    registerMessageObserver();
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    timerCache.unInit();
    unregisterMessageObserver();
  }

  public void unregisterMessageObserver() {
    QChatServiceObserverRepo.observeReceiveMessage(
        mServerId, mChannelId, receiveMessageObserver, false);
    QChatServiceObserverRepo.observeMessageDelete(deleteMessageObserver, false);
    QChatServiceObserverRepo.observeMessageRevoke(revokeMessageObserver, false);
    QChatServiceObserverRepo.observerSystemNotification(systemNotifyObserver, false);
  }

  public void registerMessageObserver() {
    QChatServiceObserverRepo.observeReceiveMessage(
        mServerId, mChannelId, receiveMessageObserver, true);
    QChatServiceObserverRepo.observeMessageDelete(deleteMessageObserver, true);
    QChatServiceObserverRepo.observeMessageRevoke(revokeMessageObserver, true);
    QChatServiceObserverRepo.observerSystemNotification(systemNotifyObserver, true);
  }

  public void fetchMessageList() {
    queryMessage(0, 0, false);
  }

  public void queryManager(long serverId, String accid, long managerRoleId) {
    QChatRoleRepo.fetchMemberJoinedRoles(
        serverId,
        accid,
        new FetchCallback<List<QChatServerRoleInfo>>() {
          @Override
          public void onSuccess(@Nullable List<QChatServerRoleInfo> param) {

            QChatServerRoleInfo result = null;
            if (param != null && param.size() > 0) {
              for (QChatServerRoleInfo info : param) {
                if (info.getRoleId() == managerRoleId) {
                  result = info;
                  break;
                }
              }
            }
            FetchResult<QChatServerRoleInfo> fetchResult = new FetchResult<>(LoadStatus.Success);
            fetchResult.setData(result);
            serverRoleLiveData.setValue(fetchResult);
          }

          @Override
          public void onFailed(int code) {
            FetchResult<QChatServerRoleInfo> fetchResult = new FetchResult<>(LoadStatus.Success);
            serverRoleLiveData.setValue(fetchResult);
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            FetchResult<QChatServerRoleInfo> fetchResult = new FetchResult<>(LoadStatus.Success);
            serverRoleLiveData.setValue(fetchResult);
          }
        });
  }

  public void loadMessageCache() {
    ALog.d(TAG, "loadMessageCache");
    QChatMessageRepo.getMessageCache(
        mServerId,
        mChannelId,
        false,
        new FetchCallback<List<QChatMessageInfo>>() {
          @Override
          public void onSuccess(@Nullable List<QChatMessageInfo> param) {
            ALog.d(TAG, "queryMessage", "onSuccess");
            messageFetchResult.setLoadStatus(LoadStatus.Success);
            messageFetchResult.setData(param);
            messageLiveData.setValue(messageFetchResult);
          }

          @Override
          public void onFailed(int code) {
            ALog.d(TAG, "queryMessage", "onFailed:" + code);
            messageFetchResult.setError(code, R.string.qchat_channel_message_fetch_error);
            messageLiveData.setValue(messageFetchResult);
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            String errorMsg = exception != null ? exception.getMessage() : "";
            ALog.d(TAG, "fetchMessageHistory", "onException:" + errorMsg);
            messageFetchResult.setError(
                QChatConstant.ERROR_CODE_MESSAGE_FETCH, R.string.qchat_channel_message_fetch_error);
            messageLiveData.setValue(messageFetchResult);
          }
        });
  }

  public void fetchForwardMessage(QChatMessageInfo messageInfo) {
    forwardMessage = messageInfo;
    queryMessage(0, messageInfo.getTime(), false);
  }

  public void fetchBackwardMessage(QChatMessageInfo messageInfo) {
    queryMessage(messageInfo.getTime(), 0, true);
  }

  private void queryMessage(long fromTime, long toTime, boolean reverse) {
    ALog.d(TAG, "queryMessage", "info:" + fromTime + "," + toTime);
    QChatMessageRepo.fetchMessageHistory(
        mServerId,
        mChannelId,
        fromTime,
        toTime,
        messagePageSize,
        reverse,
        new FetchCallback<List<QChatMessageInfo>>() {
          @Override
          public void onSuccess(@Nullable List<QChatMessageInfo> messageList) {
            ALog.d(TAG, "queryMessage", "onSuccess");

            QChatMessageRepo.getQuickComment(
                mServerId,
                mChannelId,
                messageList,
                new FetchCallback<QChatGetQuickCommentsResultInfo>() {
                  @Override
                  public void onSuccess(@Nullable QChatGetQuickCommentsResultInfo param) {
                    if (param != null) {
                      QChatCache.addQuickComment(param.getMessageQuickCommentDetailMap());
                    }
                    ALog.d(TAG, "queryMessageQuickComment", "onSuccess");
                    loadMessageData(fromTime, toTime, messageList);
                  }

                  @Override
                  public void onFailed(int code) {
                    ALog.d(TAG, "queryMessageQuickComment", "onFailed:" + code);
                    loadMessageData(fromTime, toTime, messageList);
                  }

                  @Override
                  public void onException(@Nullable Throwable exception) {
                    String errorMsg = exception != null ? exception.getMessage() : "";
                    ALog.d(TAG, "queryMessageQuickComment", "onException:" + errorMsg);
                    loadMessageData(fromTime, toTime, messageList);
                  }
                });
          }

          @Override
          public void onFailed(int code) {
            ALog.d(TAG, "queryMessage", "onFailed:" + code);
            messageFetchResult.setError(code, R.string.qchat_channel_message_fetch_error);
            messageLiveData.setValue(messageFetchResult);
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            String errorMsg = exception != null ? exception.getMessage() : "";
            ALog.d(TAG, "fetchMessageHistory", "onException:" + errorMsg);
            messageFetchResult.setError(
                QChatConstant.ERROR_CODE_MESSAGE_FETCH, R.string.qchat_channel_message_fetch_error);
            messageLiveData.setValue(messageFetchResult);
          }
        });
  }

  private void loadMessageData(long fromTime, long toTime, List<QChatMessageInfo> messageList) {
    if (fromTime == 0 && toTime == 0) {
      messageFetchResult.setLoadStatus(LoadStatus.Success);
    } else if (fromTime > 0 && toTime == 0) {
      messageFetchResult.setFetchType(FetchResult.FetchType.Add);
      messageFetchResult.setTypeIndex(-1);
    } else if (fromTime == 0 && toTime > 0) {
      messageFetchResult.setFetchType(FetchResult.FetchType.Add);
      messageFetchResult.setTypeIndex(0);
      if (messageList == null || messageList.size() < messagePageSize) {
        hasForward = false;
      }
      if (messageList != null
          && messageList.size() > 0
          && TextUtils.equals(
              messageList.get(messageList.size() - 1).getUuid(), forwardMessage.getUuid())) {
        messageList.remove(messageList.size() - 1);
      }
    }
    messageFetchResult.setData(messageList);
    messageLiveData.setValue(messageFetchResult);
  }

  public void queryMessageQuickComment(List<QChatMessageInfo> param) {
    QChatMessageRepo.getQuickComment(
        mServerId,
        mChannelId,
        param,
        new FetchCallback<QChatGetQuickCommentsResultInfo>() {
          @Override
          public void onSuccess(@Nullable QChatGetQuickCommentsResultInfo param) {
            ALog.d(TAG, "queryMessageQuickComment", "onSuccess");
            if (param != null && param.getMessageQuickCommentDetailMap() != null) {
              QChatCache.addQuickComment(param.getMessageQuickCommentDetailMap());
              List<Long> serIdList =
                  new ArrayList<>(param.getMessageQuickCommentDetailMap().keySet());
              FetchResult<List<Long>> fetchResult = new FetchResult<>(LoadStatus.Success);
              fetchResult.setData(serIdList);
              quickCommentLiveData.setValue(fetchResult);
            }
          }

          @Override
          public void onFailed(int code) {
            ALog.d(TAG, "queryMessageQuickComment", "onFailed:" + code);
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            String errorMsg = exception != null ? exception.getMessage() : "";
            ALog.d(TAG, "queryMessageQuickComment", "onException:" + errorMsg);
          }
        });
  }

  public boolean isHasForward() {
    return hasForward;
  }

  public QChatMessageInfo sendTextMessage(String content) {
    QChatSendMessageParam sendMessageInfo =
        new QChatSendMessageParam(mServerId, mChannelId, MsgTypeEnum.text);
    sendMessageInfo.setBody(content);
    return sendMessage(sendMessageInfo);
  }

  public QChatMessageInfo sendVoiceMessage(File audioFile, long audioLength) {
    QChatSendMessageParam messageParam =
        new QChatSendMessageParam(mServerId, mChannelId, MsgTypeEnum.audio);
    final AudioAttachment attachment = new AudioAttachment();
    attachment.setPath(audioFile.getPath());
    attachment.setSize(audioFile.length());
    attachment.setDuration(audioLength);
    messageParam.setAttachment(attachment);
    return sendMessage(messageParam);
  }

  public QChatMessageInfo sendImageMessage(ImageAttachment attachment) {
    QChatSendMessageParam sendMessageInfo =
        new QChatSendMessageParam(mServerId, mChannelId, MsgTypeEnum.image);
    sendMessageInfo.setAttachment(attachment);
    return sendMessage(sendMessageInfo);
  }

  private QChatMessageInfo sendMessage(QChatSendMessageParam sendMessageParam) {
    return QChatMessageRepo.sendMessage(
        sendMessageParam,
        new FetchCallback<QChatMessageInfo>() {
          @Override
          public void onSuccess(@Nullable QChatMessageInfo param) {
            ALog.d(TAG, "sendMessage", "onSuccess");
            sendMessageFetchResult.setLoadStatus(LoadStatus.Success);
            sendMessageFetchResult.setData(param);
            sendMessageLiveData.setValue(sendMessageFetchResult);
          }

          @Override
          public void onFailed(int code) {
            ALog.d(TAG, "sendMessage", "onFailed:" + code);
            sendMessageFetchResult.setError(code, R.string.qchat_channel_message_send_error);
            QChatMessageInfo messageInfo = new QChatMessageInfo(sendMessageParam.toQChatMessage());
            if (messageInfo.getMessage() != null) {
              messageInfo.getMessage().setSendMsgStatus(MsgStatusEnum.fail);
              sendMessageFetchResult.setData(messageInfo);
            }
            sendMessageLiveData.setValue(sendMessageFetchResult);
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            String errorMsg = exception != null ? exception.getMessage() : "";
            ALog.d(TAG, "sendMessage", "onException:" + errorMsg);
            sendMessageFetchResult.setError(
                QChatConstant.ERROR_CODE_SEND_MESSAGE, R.string.qchat_channel_message_send_error);
            QChatMessageInfo messageInfo = new QChatMessageInfo(sendMessageParam.toQChatMessage());
            if (messageInfo.getMessage() != null) {
              sendMessageFetchResult.setData(messageInfo);
            }
            sendMessageLiveData.setValue(sendMessageFetchResult);
          }
        });
  }

  public void resendMessage(QChatMessageInfo messageInfo) {
    QChatMessageRepo.resendMessage(
        messageInfo,
        new FetchCallback<QChatMessageInfo>() {
          @Override
          public void onSuccess(@Nullable QChatMessageInfo param) {
            ALog.d(TAG, "sendMessage", "onSuccess");
            sendMessageFetchResult.setLoadStatus(LoadStatus.Success);
            sendMessageFetchResult.setData(param);
            sendMessageLiveData.setValue(sendMessageFetchResult);
          }

          @Override
          public void onFailed(int code) {
            QChatUtils.operateError(code);
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            QChatUtils.operateError(-1);
          }
        });
  }

  public void addQuickComment(QChatMessageInfo messageInfo, int commentId) {
    QChatMessageRepo.addQuickComment(
        messageInfo,
        commentId,
        new FetchCallback<Void>() {
          @Override
          public void onSuccess(@Nullable Void param) {
            QChatQuickComment addQuickComment =
                new QChatQuickCommentImpl(
                    messageInfo.getQChatServerId(),
                    messageInfo.getQChatChannelId(),
                    messageInfo.getFromAccount(),
                    messageInfo.getMsgIdServer(),
                    messageInfo.getTime(),
                    commentId,
                    QChatKitClient.account(),
                    QChatQuickCommentOperateType.ADD);
            QChatCache.updateQuickComment(addQuickComment);
            List<Long> serIdList = new ArrayList<>();
            serIdList.add(messageInfo.getMsgIdServer());
            FetchResult<List<Long>> fetchResult = new FetchResult<>(LoadStatus.Success);
            fetchResult.setData(serIdList);
            quickCommentLiveData.setValue(fetchResult);
          }

          @Override
          public void onFailed(int code) {
            QChatUtils.operateError(code);
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            QChatUtils.operateError(-1);
          }
        });
  }

  public void removeQuickComment(QChatMessageInfo messageInfo, int commentId) {
    QChatMessageRepo.removeQuickComment(
        messageInfo,
        commentId,
        new FetchCallback<Void>() {
          @Override
          public void onSuccess(@Nullable Void param) {
            QChatQuickComment removeQuickComment =
                new QChatQuickCommentImpl(
                    messageInfo.getQChatServerId(),
                    messageInfo.getQChatChannelId(),
                    messageInfo.getFromAccount(),
                    messageInfo.getMsgIdServer(),
                    messageInfo.getTime(),
                    commentId,
                    QChatKitClient.account(),
                    QChatQuickCommentOperateType.REMOVE);
            QChatCache.updateQuickComment(removeQuickComment);
            List<Long> serIdList = new ArrayList<>();
            serIdList.add(messageInfo.getMsgIdServer());
            FetchResult<List<Long>> fetchResult = new FetchResult<>(LoadStatus.Success);
            fetchResult.setData(serIdList);
            quickCommentLiveData.setValue(fetchResult);
          }

          @Override
          public void onFailed(int code) {
            QChatUtils.operateError(code);
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            QChatUtils.operateError(-1);
          }
        });
  }

  public void makeMessageRead(QChatMessageInfo messageInfo) {
    QChatMessageRepo.markMessageRead(
        messageInfo.getQChatServerId(),
        messageInfo.getQChatChannelId(),
        messageInfo.getTime(),
        new FetchCallback<Void>() {
          @Override
          public void onSuccess(@Nullable Void param) {
            ALog.d(TAG, "makeMessageRead", "onSuccess");
          }

          @Override
          public void onFailed(int code) {
            ALog.d(TAG, "makeMessageRead", "onFailed:" + code);
            QChatUtils.operateError(code);
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            String errorMsg = exception != null ? exception.getMessage() : "";
            ALog.d(TAG, "makeMessageRead", "onException:" + errorMsg);
            QChatUtils.operateError(-1);
          }
        });
  }

  public void deleteMsg(QChatMessageInfo messageInfo) {
    if (messageInfo == null) {
      return;
    }
    if (messageInfo.getStatus() == MsgStatusEnum.sending
        || messageInfo.getStatus() == MsgStatusEnum.fail) {
      FetchResult<QChatMessageInfo> fetchResult = new FetchResult<>(LoadStatus.Success);
      fetchResult.setData(messageInfo);
      deleteMessageLiveData.setValue(fetchResult);
      return;
    }
    QChatMessageRepo.deleteMessage(
        messageInfo,
        new FetchCallback<QChatMessageInfo>() {
          @Override
          public void onSuccess(@Nullable QChatMessageInfo param) {
            ALog.d(LIB_TAG, TAG, "deleteMessage," + "onSuccess");
            EventCenter.notifyEvent(new QChatMsgEvent(QChatMsgEvent.EventType.Delete, param));
            FetchResult<QChatMessageInfo> fetchResult = new FetchResult<>(LoadStatus.Success);
            fetchResult.setData(param);
            deleteMessageLiveData.setValue(fetchResult);
          }

          @Override
          public void onFailed(int code) {
            ALog.d(LIB_TAG, TAG, "deleteMessage," + "onFailed:" + code);
            QChatUtils.operateError(code);
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            ALog.d(LIB_TAG, TAG, "deleteMessage," + "onException");
            QChatUtils.operateError(-1);
          }
        });
  }

  public void revokeMessage(QChatMessageInfo messageInfo) {
    ALog.d(LIB_TAG, TAG, "revokeMessage," + "start");
    QChatRevokeMessageParam revokeMessageParam = MessageUtil.buildRevokeParam(messageInfo);
    QChatMessageRepo.revokeMessage(
        revokeMessageParam,
        new FetchCallback<QChatMessageInfo>() {
          @Override
          public void onSuccess(@Nullable QChatMessageInfo param) {
            ALog.d(LIB_TAG, TAG, "revokeMessage," + "onSuccess");
            EventCenter.notifyEvent(new QChatMsgEvent(QChatMsgEvent.EventType.Revoke, param));
            FetchResult<QChatMessageInfo> fetchResult = new FetchResult<>(LoadStatus.Success);
            fetchResult.setData(param);
            revokeMessageLiveData.setValue(fetchResult);
          }

          @Override
          public void onFailed(int code) {
            ALog.d(LIB_TAG, TAG, "revokeMessage," + "onFailed:" + code);
            if (code == ResponseCode.RES_OVERDUE) {
              ToastX.showShortToast(R.string.qchat_message_revoke_over_time);
            } else {
              QChatUtils.operateError(code);
            }
          }

          @Override
          public void onException(@Nullable Throwable exception) {
            ALog.d(LIB_TAG, TAG, "revokeMessage," + "onException");
            QChatUtils.operateError(-1);
          }
        });
  }

  private final EventObserver<List<QChatMessageInfo>> receiveMessageObserver =
      new EventObserver<List<QChatMessageInfo>>() {
        @Override
        public void onEvent(@Nullable List<QChatMessageInfo> event) {
          // 此处也可以不使用 TimerCacheWithQChatMsg 处理接收到的消息，可以直接调用以下代码，区别是，
          // TimerCacheWithQChatMsg 处理后的消息会将在一段时间 x 内的收到的消息合并成一个消息列表，但是会产生 x 时间
          // 的延迟。
          timerCache.handle(event);
        }
      };

  private final EventObserver<QChatMessageDeleteEventInfo> deleteMessageObserver =
      new EventObserver<QChatMessageDeleteEventInfo>() {
        @Override
        public void onEvent(@Nullable QChatMessageDeleteEventInfo event) {
          ALog.d(LIB_TAG, TAG, "deleteMessageObserver," + "onEvent");
          FetchResult<QChatMessageInfo> fetchResult = new FetchResult<>(LoadStatus.Success);
          fetchResult.setData(event.getMessage());
          deleteMessageLiveData.setValue(fetchResult);
        }
      };

  private final EventObserver<QChatMessageRevokeEventInfo> revokeMessageObserver =
      new EventObserver<QChatMessageRevokeEventInfo>() {
        @Override
        public void onEvent(@Nullable QChatMessageRevokeEventInfo event) {
          ALog.d(LIB_TAG, TAG, "deleteMessageObserver," + "onEvent");
          FetchResult<QChatMessageInfo> fetchResult = new FetchResult<>(LoadStatus.Success);
          fetchResult.setData(event.getMessage());
          revokeMessageLiveData.setValue(fetchResult);
        }
      };

  private final EventObserver<List<QChatSystemNotificationInfo>> systemNotifyObserver =
      new EventObserver<List<QChatSystemNotificationInfo>>() {
        @Override
        public void onEvent(@Nullable List<QChatSystemNotificationInfo> event) {
          ALog.d(LIB_TAG, TAG, "systemNotifyObserver," + "onEvent");
          if (event != null) {
            List<Long> commengMgsIdList = new ArrayList<>();
            for (QChatSystemNotificationInfo item : event) {
              if (item.getServerId() == null || item.getServerId() != mServerId) {
                continue;
              }
              QChatSystemNotificationTypeInfo type = item.getType();
              if (item.getType() == ChannelUpdateQuickComment.INSTANCE) {
                QChatQuickCommentAttachment attachment =
                    (QChatQuickCommentAttachment) item.getAttachment();
                QChatQuickComment comment = attachment.getQuickComment();
                QChatCache.updateQuickComment(comment);
                commengMgsIdList.add(comment.getMsgIdServer());
              } else if (type == ServerRoleMemberAdd.INSTANCE) {
                QChatAddServerRoleMembersAttachment attachment =
                    (QChatSystemNotificationAttachmentImpl) item.getAttachment();
                updateMemberRole(
                    item.getServerId(), attachment.getRoleId(), attachment.getAddAccids());
              } else if (type == ServerEnterLeave.INSTANCE) {

                QChatServerEnterLeaveAttachment attachment =
                    (QChatServerEnterLeaveAttachment) item.getAttachment();
                QChatInOutType serverEnterLeave = attachment.getInOutType();
                FetchResult<Boolean> fetchResult = new FetchResult<>(LoadStatus.Success);
                if (serverEnterLeave == QChatInOutType.IN) {
                  fetchResult.setData(true);
                } else {
                  fetchResult.setData(false);
                }
                enterLeaveServerLiveData.setValue(fetchResult);

              } else if (type == ServerRoleMemberDelete.INSTANCE) {
                QChatDeleteServerRoleMembersAttachment attachment =
                    (QChatDeleteServerRoleMembersAttachment) item.getAttachment();
                updateMemberRole(
                    item.getServerId(), attachment.getRoleId(), attachment.getDeleteAccids());
              }
            }
            if (commengMgsIdList.size() > 0) {
              FetchResult<List<Long>> fetchResult = new FetchResult<>(LoadStatus.Success);
              fetchResult.setData(commengMgsIdList);
              quickCommentLiveData.setValue(fetchResult);
            }
          }
        }
      };

  private void updateMemberRole(Long serverId, Long roleId, List<String> accList) {
    if (roleId != null && accList != null) {
      boolean hasMe = false;
      for (String accId : accList) {
        if (TextUtils.equals(accId, QChatKitClient.account())) {
          hasMe = true;
          break;
        }
      }
      if (hasMe) {
        queryManager(serverId, QChatKitClient.account(), roleId);
      }
    }
  }
}
