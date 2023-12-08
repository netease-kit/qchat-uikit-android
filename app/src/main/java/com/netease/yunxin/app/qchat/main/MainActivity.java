// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.app.qchat.main;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.netease.lava.nertc.sdk.NERtcOption;
import com.netease.nimlib.sdk.avsignalling.constant.ChannelType;
import com.netease.yunxin.app.qchat.CustomConfig;
import com.netease.yunxin.app.qchat.R;
import com.netease.yunxin.app.qchat.databinding.ActivityMainBinding;
import com.netease.yunxin.app.qchat.main.mine.MineFragment;
import com.netease.yunxin.app.qchat.network.QChatSquareNetRequester;
import com.netease.yunxin.app.qchat.utils.Constant;
import com.netease.yunxin.app.qchat.utils.DataUtils;
import com.netease.yunxin.app.qchat.welcome.WelcomeActivity;
import com.netease.yunxin.kit.alog.ALog;
import com.netease.yunxin.kit.call.p2p.NECallEngine;
import com.netease.yunxin.kit.call.p2p.model.NECallInitRtcMode;
import com.netease.yunxin.kit.chatkit.repo.ContactRepo;
import com.netease.yunxin.kit.common.ui.activities.BaseActivity;
import com.netease.yunxin.kit.contactkit.ui.normal.contact.ContactFragment;
import com.netease.yunxin.kit.conversationkit.ui.normal.page.ConversationFragment;
import com.netease.yunxin.kit.corekit.im.IMKitClient;
import com.netease.yunxin.kit.corekit.im.model.UserInfo;
import com.netease.yunxin.kit.corekit.im.provider.FetchCallback;
import com.netease.yunxin.kit.corekit.im.provider.FetchCallbackImpl;
import com.netease.yunxin.kit.corekit.model.ErrorMsg;
import com.netease.yunxin.kit.corekit.model.ResultInfo;
import com.netease.yunxin.kit.corekit.qchat.QChatKitClient;
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerInfo;
import com.netease.yunxin.kit.qchatkit.ui.server.QChatServerFragment;
import com.netease.yunxin.kit.qchatkit.ui.square.QChatSquareFragment;
import com.netease.yunxin.kit.qchatkit.ui.square.SquareDataSourceHelper;
import com.netease.yunxin.kit.qchatkit.ui.square.model.QChatSquarePageInfo;
import com.netease.yunxin.nertc.ui.CallKitNotificationConfig;
import com.netease.yunxin.nertc.ui.CallKitUI;
import com.netease.yunxin.nertc.ui.CallKitUIOptions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** IM Main Page include four tab , message/contact/live/profile */
public class MainActivity extends BaseActivity {
  private static final String QCHAT_SQUARE_SERVER_URL_DEBUG = "https://yiyong-qa.netease.im/";
  private static final String QCHAT_SQUARE_SERVER_URL_RELEASE = "https://yiyong.netease.im/";
  private static final int START_INDEX = 0;
  private ActivityMainBinding activityMainBinding;
  private View mCurrentTab;
  private ContactFragment mContactFragment;
  private ConversationFragment mConversationFragment;
  private QChatServerFragment mQChatServerFragment;
  private QChatSquareFragment mQChatSquareFragment;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ALog.d(Constant.PROJECT_TAG, "MainActivity:onCreate");
    if (TextUtils.isEmpty(IMKitClient.account())) {
      Intent intent = new Intent(this, WelcomeActivity.class);
      startActivity(intent);
      finish();
      return;
    }
    activityMainBinding = ActivityMainBinding.inflate(getLayoutInflater());
    setContentView(activityMainBinding.getRoot());
    initView();

    // 配置圈组广场数据请求
//    if (QChatKitClient.account() != null) {
//      QChatSquareNetRequester.getInstance()
//          .setup(
//              BuildConfig.DEBUG ? QCHAT_SQUARE_SERVER_URL_DEBUG : QCHAT_SQUARE_SERVER_URL_RELEASE,
//              DataUtils.readAppKey(this),
//                  QChatKitClient.account(),
//              "");
//    }
  }

  @Override
  protected void onPostResume() {
    super.onPostResume();
    // 部分Android机型在页面进入onResume前启动其他页面会取消当前页面流程，避免组件初始化后立即展示来电页面将初始化的逻辑滞后
    if (!CallKitUI.INSTANCE.getInit()) {
      configCallKit();
    }
  }

  private void initView() {
    ALog.d(Constant.PROJECT_TAG, "MainActivity:initView");
    //    loadConfig();
    List<Fragment> fragments = new ArrayList<>();
    // Conversation
    mConversationFragment = new ConversationFragment();
    fragments.add(mConversationFragment);
    // QChat
    mQChatServerFragment = new QChatServerFragment();
    fragments.add(mQChatServerFragment);
    // Square
    mQChatSquareFragment = new QChatSquareFragment();
    fragments.add(mQChatSquareFragment);
    // Contact
    mContactFragment = new ContactFragment();
    fragments.add(mContactFragment);
    // Mine
    fragments.add(new MineFragment());

    FragmentAdapter fragmentAdapter = new FragmentAdapter(this);
    fragmentAdapter.setFragmentList(fragments);
    activityMainBinding.viewPager.setUserInputEnabled(false);
    activityMainBinding.viewPager.setAdapter(fragmentAdapter);
    activityMainBinding.viewPager.setCurrentItem(START_INDEX, false);
    activityMainBinding.viewPager.setOffscreenPageLimit(fragments.size());
    mCurrentTab = activityMainBinding.conversationBtnGroup;
    changeStatusBarColor(R.color.color_white);
  }

  @Override
  protected void onResume() {
    super.onResume();
    initContactFragment(mContactFragment);
    initConversationFragment(mConversationFragment);
    initQChatFragment(mQChatServerFragment);
    initQChatSquareFragment(mQChatSquareFragment);
  }

  @SuppressLint("UseCompatLoadingForDrawables")
  public void tabClick(View view) {

    if (mCurrentTab != null && mCurrentTab == view) {
      return;
    }
    resetTabStyle();
    mCurrentTab = view;
    if (mCurrentTab == activityMainBinding.contactBtnGroup) {
      activityMainBinding.viewPager.setCurrentItem(3, false);
      activityMainBinding.contact.setTextColor(getResources().getColor(R.color.tab_checked_color));
      activityMainBinding.contact.setCompoundDrawablesWithIntrinsicBounds(
          null, getResources().getDrawable(R.mipmap.ic_contact_tab_checked), null, null);
      changeStatusBarColor(R.color.color_white);
    } else if (mCurrentTab == activityMainBinding.myselfBtnGroup) {
      activityMainBinding.viewPager.setCurrentItem(4, false);
      activityMainBinding.mine.setTextColor(getResources().getColor(R.color.tab_checked_color));
      activityMainBinding.mine.setCompoundDrawablesWithIntrinsicBounds(
          null, getResources().getDrawable(R.mipmap.ic_mine_tab_checked), null, null);
      changeStatusBarColor(R.color.color_white);
    } else if (mCurrentTab == activityMainBinding.qchatBtnGroup) {
      activityMainBinding.viewPager.setCurrentItem(1, false);
      activityMainBinding.qchat.setTextColor(getResources().getColor(R.color.tab_checked_color));
      activityMainBinding.qchat.setCompoundDrawablesWithIntrinsicBounds(
          null, getResources().getDrawable(R.drawable.ic_qchat_checked), null, null);
      changeStatusBarColor(R.color.color_eff1f4);
    } else if (mCurrentTab == activityMainBinding.conversationBtnGroup) {
      activityMainBinding.viewPager.setCurrentItem(0, false);
      activityMainBinding.conversation.setTextColor(
          getResources().getColor(R.color.tab_checked_color));
      activityMainBinding.conversation.setCompoundDrawablesWithIntrinsicBounds(
          null, getResources().getDrawable(R.mipmap.ic_conversation_tab_checked), null, null);
      changeStatusBarColor(R.color.color_white);
    } else if (mCurrentTab == activityMainBinding.qchatSquareBtnGroup) {
      activityMainBinding.viewPager.setCurrentItem(2, false);
      activityMainBinding.qchatSquare.setTextColor(
          getResources().getColor(R.color.tab_checked_color));
      activityMainBinding.qchatSquare.setCompoundDrawablesWithIntrinsicBounds(
          null, getResources().getDrawable(R.drawable.ic_qchat_square_checked), null, null);
      changeStatusBarColor(R.color.color_eff1f4);
    }
  }

  private void initConversationFragment(ConversationFragment conversationFragment) {
    if (conversationFragment != null) {
      conversationFragment.setConversationCallback(
          count -> {
            if (count > 0) {
              activityMainBinding.conversationDot.setVisibility(View.VISIBLE);
            } else {
              activityMainBinding.conversationDot.setVisibility(View.GONE);
            }
          });
    }
  }

  private void initContactFragment(ContactFragment contactFragment) {
    if (contactFragment != null) {
      contactFragment.setContactCallback(
          count -> {
            if (count > 0) {
              activityMainBinding.contactDot.setVisibility(View.VISIBLE);
            } else {
              activityMainBinding.contactDot.setVisibility(View.GONE);
            }
          });
    }
  }

  private void initQChatFragment(QChatServerFragment qChatServerFragment) {
    if (qChatServerFragment != null) {
      qChatServerFragment.setQChatServerCallback(
          count -> {
            if (activityMainBinding == null) {
              return;
            }
            activityMainBinding.qchatDot.post(
                () -> {
                  if (count > 0) {
                    String countStr;
                    if (count > 99) {
                      countStr = "99+";
                    } else {
                      countStr = String.valueOf(count);
                    }
                    activityMainBinding.qchatDot.setText(countStr);
                    activityMainBinding.qchatDot.setVisibility(View.VISIBLE);
                  } else {
                    activityMainBinding.qchatDot.setVisibility(View.GONE);
                  }
                });
          });
    }
  }

    private void initQChatSquareFragment(QChatSquareFragment qChatSquareFragment) {
        if (qChatSquareFragment != null) {
            qChatSquareFragment.setQChatSquareCallback(
                    (serverInfo, joined) -> {
                        if (mQChatServerFragment != null) {
                            mQChatServerFragment.enterQChatServer(
                                    serverInfo,
                                    new FetchCallbackImpl<Boolean>() {
                                        @Override
                                        public void onSuccess(@Nullable Boolean param) {
                                            if (param!=null&&param){
                                                activityMainBinding.qchatBtnGroup.performClick();
                                            } else {
                                                Toast.makeText(getApplicationContext(), "进入社区失败", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        }
                    });
            // 配置圈组广场数据请求
            SquareDataSourceHelper.getInstance()
                    .configRequester(
                            new SquareDataSourceHelper.RequesterForSquareInfo() {
                                @Override
                                public void requestSquareSearchType(
                                        FetchCallback<ResultInfo<List<QChatSquarePageInfo>>> callback) {
                                    // 获取圈组广场顶部 title 信息
                                    callback.onSuccess(new ResultInfo<>(Arrays.asList(
                                            new QChatSquarePageInfo("推荐",1),
                                            new QChatSquarePageInfo("体育",2)
                                    )));
                                }

                                @Override
                                public void requestServerInfoForSearchType(
                                        int searchType, FetchCallback<ResultInfo<List<QChatServerInfo>>> callback) {
                                    // 获取圈组广场具体 type 下的服务器列表数据
                                    List<QChatServerInfo> serverInfoList = new ArrayList<>();
                                    if (searchType == 1) {
                                        //这里是一个讨论和分享这款桌游的平台，你可以在这里和其他玩家交流游戏策略、分享游戏心得、提出问题等等。我们鼓励玩家之间互相帮助，一起解开城堡中的谜团，找出凶手。让我们一起探索这个充满恐怖和谋杀的城堡，享受逐步解谜的乐趣！
                                        serverInfoList.add(new QChatServerInfo(0,"血染钟楼","https://yx-web-nosdn.netease.im/common/bcbb1f2d82e8e271070994faf650b231/社区5.webp","{\"topic\":\"这里是一个讨论和分享这款桌游的平台，你可以在这里和其他玩家交流游戏策略、分享游戏心得、提出问题等等。我们鼓励玩家之间互相帮助，一起解开城堡中的谜团，找出凶手。让我们一起探索这个充满恐怖和谋杀的城堡，享受逐步解谜的乐趣！\"}"));
                                        serverInfoList.add(new QChatServerInfo(1,"韩流看这里","https://yx-web-nosdn.netease.im/common/9c7c4be8a8aa9cafd2f8de75b7debeaa/社区4.jpeg","{\"topic\":\"这里提供最新的韩国明星新闻、音乐、综艺节目和流行文化等内容。无论你是韩国文化的忠实粉丝还是初学者，我们都欢迎你的加入。与其他韩流迷一起分享你的热爱，探索韩国文化的无限魅力！\"}"));
                                    } else if (searchType == 2) {
                                        serverInfoList.add(new QChatServerInfo(2,"球迷之家","https://yx-web-nosdn.netease.im/common/58b014ae2b11696018e617ae950629a8/社区1.jpeg","{\"topic\":\"不管篮球足球羽毛球，不论男女老少，是球迷你就来\"}"));
                                        serverInfoList.add(new QChatServerInfo(3,"NBA ","https://yx-web-nosdn.netease.im/common/247c31d7e6741c7d2d983ab940ddc187/社区2.jpeg","{\"topic\":\"欢迎加入我们，这里是全球最热门的篮球迷交流平台之一。我们提供最新的NBA新闻、比赛分析、球员数据以及球迷讨论区等丰富内容，让您与全球篮球迷一起分享您的看法和热情。成为NBA社区的一员，与全球球迷一起探讨篮球的魅力，感受运动的激情！\"}"));
                                    }
                                    callback.onSuccess(new ResultInfo<>(serverInfoList));
                                }
                            });
        }
    }

  @SuppressLint("UseCompatLoadingForDrawables")
  private void resetTabStyle() {

    activityMainBinding.conversation.setTextColor(
        getResources().getColor(R.color.tab_unchecked_color));
    activityMainBinding.conversation.setCompoundDrawablesWithIntrinsicBounds(
        null, getResources().getDrawable(R.mipmap.ic_conversation_tab_unchecked), null, null);

    activityMainBinding.contact.setTextColor(getResources().getColor(R.color.tab_unchecked_color));
    activityMainBinding.contact.setCompoundDrawablesWithIntrinsicBounds(
        null, getResources().getDrawable(R.mipmap.ic_contact_tab_unchecked), null, null);

    activityMainBinding.mine.setTextColor(getResources().getColor(R.color.tab_unchecked_color));
    activityMainBinding.mine.setCompoundDrawablesWithIntrinsicBounds(
        null, getResources().getDrawable(R.mipmap.ic_mine_tab_unchecked), null, null);

    activityMainBinding.qchat.setTextColor(getResources().getColor(R.color.tab_unchecked_color));
    activityMainBinding.qchat.setCompoundDrawablesWithIntrinsicBounds(
        null, getResources().getDrawable(R.drawable.ic_qchat_unchecked), null, null);

    activityMainBinding.qchatSquare.setTextColor(
        getResources().getColor(R.color.tab_unchecked_color));
    activityMainBinding.qchatSquare.setCompoundDrawablesWithIntrinsicBounds(
        null, getResources().getDrawable(R.drawable.ic_qchat_square_unchecked), null, null);
  }

  private void configCallKit() {
    CallKitUIOptions options =
        new CallKitUIOptions.Builder()
            // 必要：音视频通话 sdk appKey，用于通话中使用
            .rtcAppKey(DataUtils.readAppKey(this))
            // 必要：当前用户 AccId
            .currentUserAccId(QChatKitClient.account())
            // 通话接听成功的超时时间单位 毫秒，默认30s
            .timeOutMillisecond(30 * 1000L)
            // 此处为 收到来电时展示的 notification 相关配置，如图标，提示语等。
            .notificationConfigFetcher(
                invitedInfo -> {
                    UserInfo info = ContactRepo.getUserInfoFromLocal(invitedInfo.callerAccId);
                    String content =
                            (info != null ? info.getUserInfoName() : invitedInfo.callerAccId)
                                    + (invitedInfo.callType == ChannelType.AUDIO.getValue()
                                    ? getString(R.string.incoming_call_notify_audio)
                                    : getString(R.string.incoming_call_notify_video));
                    ALog.d("=======" + content);
                    return new CallKitNotificationConfig(R.mipmap.ic_logo, null, null, content);
                })
                // 收到被叫时若 app 在后台，在恢复到前台时是否自动唤起被叫页面，默认为 true
                .resumeBGInvitation(true)
                // 请求 rtc token 服务，若非安全模式则不需设置(V1.8.0版本之前需要配置，V1.8.0及之后版本无需配置)
                //.rtcTokenService((uid, callback) -> requestRtcToken(appKey, uid, callback)) // 自己实现的 token 请求方法
                // 设置初始化 rtc sdk 相关配置，按照所需进行配置
                .rtcSdkOption(new NERtcOption())
                // 呼叫组件初始化 rtc 范围，NECallInitRtcMode.GLOBAL-全局初始化，
                // NECallInitRtcMode.IN_NEED-每次通话进行初始化以及销毁，全局初始化有助于更快进入首帧页面，
                // 当结合其他组件使用时存在rtc初始化冲突可设置NECallInitRtcMode.IN_NEED
                // 或当结合其他组件使用时存在rtc初始化冲突可设置NECallInitRtcMode.IN_NEED_DELAY_TO_ACCEPT
                .initRtcMode(NECallInitRtcMode.GLOBAL)
                .build();
      // 设置自定义话单消息发送
      NECallEngine.sharedInstance().setCallRecordProvider(new CustomCallOrderProvider());
      // 若重复初始化会销毁之前的初始化实例，重新初始化
      CallKitUI.init(getApplicationContext(), options);
    QChatKitClient.getAuthServiceObserver().observeOnlineStatus(
            onlineStatus -> {
              if (onlineStatus.wontAutoLogin()) {
                CallKitUI.destroy();
              }
            },true);
  }

  private void loadConfig() {
    CustomConfig.configContactKit(this);
    CustomConfig.configConversation(this);
    CustomConfig.configChatKit(this);
  }
}
