// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.app.qchat.network;

import android.text.TextUtils;
import com.netease.yunxin.app.qchat.network.model.QChatSquareResponse;
import com.netease.yunxin.app.qchat.network.model.QChatSquareServer;
import com.netease.yunxin.app.qchat.network.model.QChatSquareTitle;
import com.netease.yunxin.kit.corekit.im2.extend.FetchCallback;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerInfo;
import com.netease.yunxin.kit.qchatkit.ui.square.model.QChatSquarePageInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class QChatSquareNetRequester {
  private final Map<String, String> headerMap = new HashMap<>();
  private Retrofit retrofit;
  private String appKey;

  private QChatSquareNetRequester() {}

  private static final class Holder {
    private static final QChatSquareNetRequester INSTANCE = new QChatSquareNetRequester();
  }

  public static QChatSquareNetRequester getInstance() {
    return Holder.INSTANCE;
  }

  public void setup(String url, String appKey, String accountId, String signature) {
    if (TextUtils.isEmpty(url)) {
      return;
    }
    this.appKey = appKey;
    headerMap.put("appKey", appKey);
    headerMap.put("accountId", accountId);
    headerMap.put("accessToken", signature);
    headerMap.put("Content-Type", "application/json;charset=utf-8");

    OkHttpClient httpClient =
        new OkHttpClient.Builder()
            .connectTimeout(30L, TimeUnit.SECONDS)
            .readTimeout(5L, TimeUnit.SECONDS)
            .writeTimeout(5L, TimeUnit.SECONDS)
            .build();
    retrofit =
        new Retrofit.Builder()
            .baseUrl(url)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build();
  }

  public void requestSquareSearchTypeList(FetchCallback<List<QChatSquarePageInfo>> callback) {
    if (callback == null || retrofit == null) {
      return;
    }
    retrofit
        .create(QChatSquareAPI.class)
        .requestSquareSearchTypeList(headerMap)
        .enqueue(
            new Callback<QChatSquareResponse<List<QChatSquareTitle>>>() {
              @Override
              public void onResponse(
                  Call<QChatSquareResponse<List<QChatSquareTitle>>> call,
                  Response<QChatSquareResponse<List<QChatSquareTitle>>> response) {
                if (response.isSuccessful()) {
                  QChatSquareResponse<List<QChatSquareTitle>> body = response.body();
                  if (body != null && body.data != null) {
                    List<QChatSquarePageInfo> resultInfoList = new ArrayList<>();
                    for (QChatSquareTitle item : body.data) {
                      resultInfoList.add(new QChatSquarePageInfo(item.title, item.type));
                    }
                    callback.onSuccess(resultInfoList);
                  } else {
                    callback.onSuccess(Collections.emptyList());
                  }

                } else {
                  callback.onError(response.code(), response.message());
                }
              }

              @Override
              public void onFailure(
                  Call<QChatSquareResponse<List<QChatSquareTitle>>> call, Throwable t) {
                callback.onError(-1, t.getMessage());
              }
            });
  }

  public void requestSquareServerListByType(
      int searchType, FetchCallback<List<QChatServerInfo>> callback) {
    if (callback == null || retrofit == null) {
      return;
    }
    retrofit
        .create(QChatSquareAPI.class)
        .requestSquareServerListByType(headerMap, appKey, searchType)
        .enqueue(
            new Callback<QChatSquareResponse<List<QChatSquareServer>>>() {
              @Override
              public void onResponse(
                  Call<QChatSquareResponse<List<QChatSquareServer>>> call,
                  Response<QChatSquareResponse<List<QChatSquareServer>>> response) {
                if (response.isSuccessful()) {
                  QChatSquareResponse<List<QChatSquareServer>> body = response.body();
                  if (body != null && body.data != null) {
                    List<QChatServerInfo> resultInfoList = new ArrayList<>();
                    for (QChatSquareServer item : body.data) {
                      resultInfoList.add(
                          new QChatServerInfo(
                              item.serverId, item.serverName, item.icon, item.custom));
                    }
                    callback.onSuccess(resultInfoList);
                  } else {
                    callback.onSuccess(Collections.emptyList());
                  }

                } else {
                  callback.onError(response.code(), response.message());
                }
              }

              @Override
              public void onFailure(
                  Call<QChatSquareResponse<List<QChatSquareServer>>> call, Throwable t) {
                callback.onError(-1, t.getMessage());
              }
            });
  }
}
