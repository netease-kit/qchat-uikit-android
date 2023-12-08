// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.utils;

import android.content.Context;
import android.widget.Toast;
import com.netease.yunxin.kit.common.ui.utils.ToastX;
import com.netease.yunxin.kit.common.utils.NetworkUtils;
import com.netease.yunxin.kit.qchatkit.ui.R;
import com.netease.yunxin.kit.qchatkit.ui.model.QChatConstant;

public class QChatUtils {

  public static void isConnectedToastAndRun(Context context, Runnable runnable) {
    isConnectedToastAndRun(context, context.getString(R.string.qchat_network_error_tip), runnable);
  }

  public static void isConnectedToastAndRun(Context context, String toast, Runnable runnable) {
    isConnectedToastAndRun(
        runnable, () -> Toast.makeText(context, toast, Toast.LENGTH_SHORT).show());
  }

  public static void isConnectedToastAndRun(Runnable SuccessRunnable, Runnable failRunnable) {
    if (NetworkUtils.isConnected()) {
      if (SuccessRunnable != null) {
        SuccessRunnable.run();
      }
    } else {
      if (failRunnable != null) {
        failRunnable.run();
      }
    }
  }

  public static String generateNumberText(int number) {
    if (number < 1000) {
      return String.valueOf(number);
    } else {
      int knum = number / 1000;
      if (knum < 1000) {
        return knum + "k";
      } else {
        int result = knum / 1000;
        return result + "m";
      }
    }
  }

  public static boolean checkNetworkAndToast() {
    if (NetworkUtils.isConnected()) {
      return true;
    } else {
      ToastX.showShortToast(R.string.qchat_network_error_tip);
      return false;
    }
  }

  public static void operateError(int code) {
    if (code == QChatConstant.NO_PERMISSION_CODE) {
      ToastX.showShortToast(R.string.qchat_message_no_permission);
    } else if (code == QChatConstant.NO_INTERNET_CODE) {
      ToastX.showShortToast(R.string.qchat_network_error);
    } else {
      ToastX.showShortToast(R.string.qchat_server_request_fail);
    }
  }
}
