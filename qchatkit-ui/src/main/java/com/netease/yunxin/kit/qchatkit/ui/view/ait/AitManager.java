// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.view.ait;

import android.graphics.Color;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import com.netease.yunxin.kit.alog.ALog;
import com.netease.yunxin.kit.qchatkit.ui.model.ait.AitBlock;
import com.netease.yunxin.kit.qchatkit.ui.model.ait.AitUserInfo;
import com.netease.yunxin.kit.qchatkit.ui.model.ait.AtContactsModel;
import java.util.List;
import org.json.JSONObject;

/**
 * @功能核心控制器，实现 TextWatcher。
 * 挂接到 EditText 上，监听输入，识别@触发，管理@块坐标。
 * 弹窗显示由外部（Fragment）负责，通过 {@link AitTriggerListener} 通知。
 */
public class AitManager implements TextWatcher {

  private static final String TAG = "AitManager";

  /** 所有@块的聚合模型 */
  private AtContactsModel atContactsModel = new AtContactsModel();

  /** View 层 - 文字插入/删除回调 */
  private AitTextChangeListener aitTextChangeListener;

  /** @触发回调（通知 Fragment 弹出选人弹窗） */
  private AitTriggerListener aitTriggerListener;

  /** 当前光标位置 */
  private int curPos;

  /** 程序化修改 EditText 时设为 true，避免 TextWatcher 重入 */
  private boolean ignoreTextChange = false;

  /** 上一次 beforeTextChanged 中判断是否是删除操作 */
  private boolean delete = false;

  // onTextChanged 中记录的参数，供 afterTextChanged 使用
  private int editTextStart;
  private int editTextCount;
  private int editTextBefore;

  /**
   * 当用户输入"@"时，通知外部弹出选人界面
   */
  public interface AitTriggerListener {
    void onAtTrigger();
  }

  public void setAitTextChangeListener(AitTextChangeListener listener) {
    this.aitTextChangeListener = listener;
  }

  public void setAitTriggerListener(AitTriggerListener listener) {
    this.aitTriggerListener = listener;
  }

  public AtContactsModel getAitContactsModel() {
    return atContactsModel;
  }

  public void setAitContactsModel(AtContactsModel model) {
    this.atContactsModel = model;
  }

  // ========================== TextWatcher 实现 ==========================

  @Override
  public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    // 判断是否是删除操作：旧字符数(count) > 新字符数(after) 表示删除
    delete = (count > after);
  }

  @Override
  public void onTextChanged(CharSequence s, int start, int before, int count) {
    // 更新内部变量
    editTextStart = start;
    editTextCount = count;
    editTextBefore = before;
  }

  @Override
  public void afterTextChanged(Editable s) {
    if (ignoreTextChange) {
      return;
    }

    // 更新当前光标位置（插入后光标在插入内容之后）
    curPos = editTextStart + editTextCount;

    if (delete) {
      // ---- 删除操作 ----
      int deleteStart = editTextStart;
      int deleteLength = editTextBefore;

      // 尝试整块删除@块
      boolean deletedWholeBlock = deleteSegment(s, deleteStart, deleteLength);
      if (!deletedWholeBlock) {
        // 普通删除，只更新坐标
        atContactsModel.onDeleteText(deleteStart, deleteLength);
      }
    } else {
      // ---- 插入操作 ----
      if (editTextCount > 0) {
        String inserted = s.subSequence(editTextStart, editTextStart + editTextCount).toString();
        atContactsModel.onInsertText(editTextStart, inserted);

        // 检测输入的是否是"@"
        if ("@".equals(inserted)) {
          // 通知外部弹出选人弹窗
          if (aitTriggerListener != null) {
            aitTriggerListener.onAtTrigger();
          }
        }
      }
    }
  }

  // ========================== 选人回调 - 由 Fragment 在弹窗选择后调用 ==========================

  /**
   * Fragment 弹窗中用户选择了某个成员后，调用此方法插入@文字
   * （此时输入框已有"@"，只需插入"名字 "）
   *
   * @param item 被@的成员
   */
  public void onMemberSelected(AitUserInfo item) {
    if (item == null) return;
    insertAitMemberInner(item.getAccount(), item.getAitName(), curPos, false);
  }

  /**
   * 长按头像触发@（此时输入框无"@"，需要完整插入"@名字 "）
   *
   * @param account 被@账号
   * @param name    @显示名
   */
  public void insertReplyAit(String account, String name) {
    insertAitMemberInner(account, name, curPos, true);
  }

  /**
   * 插入@成员文字核心方法
   *
   * @param account          被@账号
   * @param name             @名字（不含@前缀，不含末尾空格）
   * @param start            当前光标位置
   * @param needInsertAtSign true=弹窗外触发（需补"@"前缀），false=弹窗内（输入框已有"@"）
   */
  private void insertAitMemberInner(String account, String name, int start, boolean needInsertAtSign) {
    String nameWithSpace = name + " ";
    String content = needInsertAtSign ? "@" + nameWithSpace : nameWithSpace;

    ignoreTextChange = true;
    if (aitTextChangeListener != null) {
      aitTextChangeListener.onTextAdd(content, start, content.length(), needInsertAtSign);
    }
    ignoreTextChange = false;

    // 更新坐标
    atContactsModel.onInsertText(start, content);

    // @符号索引
    int atIndex = needInsertAtSign ? start : start - 1;
    atContactsModel.addAtMember(account, "@" + nameWithSpace, atIndex);
  }

  // ========================== 删除@块 ==========================

  /**
   * 尝试整块删除@内容
   *
   * @param s           当前 Editable
   * @param deleteStart 删除起始位置
   * @param deleteLen   删除长度
   * @return 是否触发了整块删除
   */
  private boolean deleteSegment(Editable s, int deleteStart, int deleteLen) {
    // 找到结束位置紧接删除位置的@块
    int endPos = deleteStart + deleteLen;
    Object[] result = atContactsModel.findAtSegmentByEndPos(endPos);
    if (result == null) {
      return false;
    }

    String account = (String) result[0];
    AitBlock.AitSegment segment = (AitBlock.AitSegment) result[1];

    int blockStart = segment.getStart();
    int blockEnd = segment.getEnd();
    int blockLength = blockEnd - blockStart;

    // 输出详情
    ALog.d(TAG, "deleteSegment: whole block [" + blockStart + ", " + blockEnd + "]");

    // 先更新坐标（整块删除）
    atContactsModel.onDeleteText(blockStart, blockLength);

    // 通知 View 层删除整块
    ignoreTextChange = true;
    if (aitTextChangeListener != null) {
      aitTextChangeListener.onTextDelete(blockStart, blockLength);
    }
    ignoreTextChange = false;

    return true;
  }

  // ========================== 对外数据接口 ==========================

  /** 获取被@的账号列表 */
  public List<String> getAitTeamMember() {
    return atContactsModel.getAtMemberList();
  }

  /** 获取@数据 JSON（存入消息扩展字段 "yxAitMsg"） */
  public JSONObject getAitData() {
    return atContactsModel.getBlockJson();
  }

  /** 发送消息后重置 */
  public void reset() {
    atContactsModel.reset();
    curPos = 0;
  }

  /** 当前是否有@记录 */
  public boolean hasAitMember() {
    return !atContactsModel.isEmpty();
  }
}
