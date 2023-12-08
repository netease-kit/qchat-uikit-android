// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.app.qchat.network;

import com.netease.yunxin.app.qchat.network.model.QChatSquareResponse;
import com.netease.yunxin.app.qchat.network.model.QChatSquareServer;
import com.netease.yunxin.app.qchat.network.model.QChatSquareTitle;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.Path;

public interface QChatSquareAPI {

  @GET("im/group/searchType/list")
  Call<QChatSquareResponse<List<QChatSquareTitle>>> requestSquareSearchTypeList(
      @HeaderMap Map<String, String> header);

  @GET("im/group/server/{appKey}/{searchType}/list")
  Call<QChatSquareResponse<List<QChatSquareServer>>> requestSquareServerListByType(
      @HeaderMap Map<String, String> header,
      @Path("appKey") String appKey,
      @Path("searchType") int searchType);
}
