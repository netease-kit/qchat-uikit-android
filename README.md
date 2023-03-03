# qchat-uikit-android
圈组是网易云信IM即时通讯服务的全新能力，可用来帮助您构建“类 Discord 即时通讯社群”。圈组与单聊（点对点聊天）、群聊、聊天室一起构成网易云信IM即时通讯服务的四大能力模块。

## 快速跑通Demo

### 前提条件
在开始集成 IM UIKit 前，请确保您已：
 * 在云信控制台完成应用创建并获取APPKEY。
 * 注册云信 IM 账号，获取 accid 和 token。

准备如下开发环境/工具：
 * Android Studio Bumblebee
* Java 11
* Gradle 7.4.1
* Android Gradle Plugin 7.1.3


### 初始化
1、在AndroidManefest.xml中配置 APP Key。
如果需要使用“发送地理位置消息”的功能，还需在AndroidManefest.xml中配置高德地图 API Key 和定位服务（APSService）。
示例代码如下：
```xml
<!--在application节点里面增加<meta-data>配置云信APPKey等-->
    <application ...>
    <!-- 云信 APPKEY -->
    <meta-data
        android:name="com.netease.nim.appKey"
        android:value="your APPKey" /> 
    <!-- 高德地图API Key -->
    <meta-data
        android:name="com.amap.api.v2.apikey"
        android:value="apikey" />  
    <!-- 高德地图定位 -->
    <service android:name="com.amap.api.location.APSService" />
</application>
```
2、配置account和Token
在 `WelcomActivity`中 `startLogin`方法中填入您申请的account和token。代码如下
```java
private void startLogin() {
    ALog.d(Constant.PROJECT_TAG, TAG, "startLogin");

      //填入你的 account and token
      String account = "";
      String token = "";
      LoginInfo loginInfo = LoginInfo.LoginInfoBuilder.loginInfoDefault(account,token).build();

      if (!TextUtils.isEmpty(account) && !TextUtils.isEmpty(token)) {
        loginQChat(loginInfo);
      } else {
        activityWelcomeBinding.appDesc.setVisibility(View.GONE);
        activityWelcomeBinding.loginButton.setVisibility(View.VISIBLE);
        activityWelcomeBinding.loginButton.setOnClickListener(view -> launchLoginPage());
      }
  }

```

3、点击运行即可

### 其他
本工程包含Demo和圈组UIKit源码，Demo默认采用Maven的方式引入qchatKit-ui（圈组模块）、chatkit-ui（会话模块）等。如果想修改为源码依赖，请在app/build.
gradle文件中修改依赖方式即可。