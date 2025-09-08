// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.announce.viewmodel;

import static com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant.LIB_TAG;

import android.text.TextUtils;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import com.netease.nimlib.sdk.qchat.enums.QChatRoleOption;
import com.netease.nimlib.sdk.qchat.enums.QChatRoleResource;
import com.netease.nimlib.sdk.qchat.model.QChatMemberRole;
import com.netease.yunxin.kit.alog.ALog;
import com.netease.yunxin.kit.common.ui.viewmodel.BaseViewModel;
import com.netease.yunxin.kit.common.ui.viewmodel.FetchResult;
import com.netease.yunxin.kit.common.ui.viewmodel.LoadStatus;
import com.netease.yunxin.kit.corekit.im2.extend.FetchCallback;
import com.netease.yunxin.kit.qchatkit.repo.QChatRoleRepo;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ManagerEditViewModel extends BaseViewModel {

  public final String TAG = ManagerEditViewModel.class.getSimpleName();
  private MutableLiveData<FetchResult<QChatMemberRole>> memberInfoLiveData =
      new MutableLiveData<>();
  private final MutableLiveData<FetchResult<Boolean>> kickMemberLiveData = new MutableLiveData<>();
  private final MutableLiveData<FetchResult<Boolean>> updateLiveData = new MutableLiveData<>();

  public MutableLiveData<FetchResult<QChatMemberRole>> getMemberInfoLiveData() {
    return memberInfoLiveData;
  }

  public MutableLiveData<FetchResult<Boolean>> getKickMemberLiveData() {
    return kickMemberLiveData;
  }

  public MutableLiveData<FetchResult<Boolean>> getUpdateLiveData() {
    return updateLiveData;
  }

  public void requestMemberInfo(long serverId, long channelId, String accId) {
    ALog.d(LIB_TAG, TAG, "requestMemberInfo serverId = " + serverId + " accId = " + accId);

    QChatRoleRepo.getMemberRoles(
        serverId,
        channelId,
        0,
        200,
        new FetchCallback<List<QChatMemberRole>>() {
          @Override
          public void onError(int code, @Nullable String s) {
            ALog.d(LIB_TAG, TAG, "requestMemberInfo result onFailed:" + code);
          }

          @Override
          public void onSuccess(@Nullable List<QChatMemberRole> param) {
            ALog.d(LIB_TAG, TAG, "requestMemberInfo result success");
            FetchResult<QChatMemberRole> result = new FetchResult<>(LoadStatus.Success);
            QChatMemberRole memberRole = null;
            if (param != null && param.size() > 0) {
              for (QChatMemberRole member : param) {
                if (TextUtils.equals(member.getAccid(), accId)) {
                  memberRole = member;
                  result.setData(member);
                  break;
                }
              }
            }
            if (memberRole == null) {
              QChatRoleRepo.addMemberRole(
                  serverId,
                  channelId,
                  accId,
                  new FetchCallback<QChatMemberRole>() {
                    @Override
                    public void onSuccess(@Nullable QChatMemberRole param) {
                      ALog.d(LIB_TAG, TAG, "addMemberRole result success");
                      result.setData(param);
                      memberInfoLiveData.setValue(result);
                    }

                    @Override
                    public void onError(int code, @Nullable String msg) {
                      ALog.d(LIB_TAG, TAG, "addMemberRole result onFailed:" + code);
                      updateLiveData.setValue(new FetchResult<>(LoadStatus.Success, false));
                    }
                  });
            } else {
              memberInfoLiveData.setValue(result);
            }
          }
        });
  }

  public void updateMemberRole(
      long serverId, long channelId, String accId, Map<QChatRoleResource, QChatRoleOption> option) {
    QChatRoleRepo.updateMemberRole(
        serverId,
        channelId,
        accId,
        option,
        new FetchCallback<QChatMemberRole>() {
          @Override
          public void onSuccess(@Nullable QChatMemberRole param) {
            ALog.d(LIB_TAG, TAG, "updateMemberRole result success");
            updateLiveData.setValue(new FetchResult<>(LoadStatus.Success, true));
          }

          @Override
          public void onError(int code, @Nullable String msg) {
            ALog.d(LIB_TAG, TAG, "updateMemberRole result onFailed:" + code);
            // 如果出现406代表参数没有修改，业务上按照成功处理
            if (code == QChatConstant.PARAM_ERROR_CODE) {
              updateLiveData.setValue(new FetchResult<>(LoadStatus.Success, true));
            } else {
              updateLiveData.setValue(new FetchResult<>(LoadStatus.Success, false));
            }
          }
        });
  }

  public void kickMember(long serverId, long roleId, long channel, String accId) {
    if (TextUtils.isEmpty(accId)) {
      return;
    }
    QChatRoleRepo.removeServerRoleMember(
        serverId,
        roleId,
        Collections.singletonList(accId),
        new FetchCallback<List<String>>() {
          @Override
          public void onSuccess(@Nullable List<String> param) {
            QChatRoleRepo.removeMemberRole(
                serverId,
                channel,
                accId,
                new FetchCallback<Void>() {
                  @Override
                  public void onSuccess(@Nullable Void param) {
                    ALog.d(LIB_TAG, TAG, "removeMemberRole result success");
                    kickMemberLiveData.setValue(new FetchResult<>(LoadStatus.Success, true));
                  }

                  @Override
                  public void onError(int code, @Nullable String msg) {
                    ALog.d(LIB_TAG, TAG, "removeMemberRole result onFailed:" + code);
                    kickMemberLiveData.setValue(new FetchResult<>(LoadStatus.Success, false));
                  }
                });
          }

          @Override
          public void onError(int code, @Nullable String msg) {
            ALog.d(LIB_TAG, TAG, "removeMemberRole result onFailed:" + code);
            kickMemberLiveData.setValue(new FetchResult<>(LoadStatus.Success, false));
          }
        });
  }
}
