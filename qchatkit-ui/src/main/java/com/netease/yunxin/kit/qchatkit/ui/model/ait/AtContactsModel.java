// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.model.ait;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

/** 所有@块的聚合模型，管理当前输入框中所有的@记录 */
public class AtContactsModel {

  /** @数据写入消息扩展字段时使用的 KEY */
  public static final String AIT_REMOTE_EXTENSION_KEY = "yxAitMsg";

  /** 内部存储：account → AitBlock */
  private final Map<String, AitBlock> aitBlocks = new HashMap<>();

  /**
   * 添加一个@记录
   * @param account  被@人账号
   * @param name     "@名字 "文本（已含末尾空格）
   * @param start    "@"符号在 EditText 中的起始索引
   */
  public void addAtMember(String account, String name, int start) {
    AitBlock block = aitBlocks.get(account);
    if (block == null) {
      block = new AitBlock(name, account);
      aitBlocks.put(account, block);
    }
    int end = start + name.length(); // end 是最后一个字符的索引（不含）
    block.addSegment(start, end);
  }

  /**
   * 返回所有有效的被@账号列表（供消息发送时填充 pushList）
   */
  public List<String> getAtMemberList() {
    List<String> result = new ArrayList<>();
    for (Map.Entry<String, AitBlock> entry : aitBlocks.entrySet()) {
      if (entry.getValue().valid()) {
        result.add(entry.getKey());
      }
    }
    return result;
  }

  /**
   * 遍历所有 AitBlock，找到终止位置匹配的 Segment（用于删除整块）
   * @param pos 终止位置
   * @return [account, segment] 或 null
   */
  public Object[] findAtSegmentByEndPos(int pos) {
    for (Map.Entry<String, AitBlock> entry : aitBlocks.entrySet()) {
      AitBlock.AitSegment segment = entry.getValue().findLastSegmentByEnd(pos);
      if (segment != null) {
        return new Object[]{entry.getKey(), segment};
      }
    }
    return null;
  }

  /**
   * 插入文字时，更新所有 AitBlock 的坐标
   * @param start      插入起始位置
   * @param changeText 插入内容
   */
  public void onInsertText(int start, String changeText) {
    for (AitBlock block : aitBlocks.values()) {
      block.moveRight(start, changeText);
    }
  }

  /**
   * 删除文字时，更新所有 AitBlock 的坐标，并移除失效块
   * @param start  删除起始位置
   * @param length 删除长度
   */
  public void onDeleteText(int start, int length) {
    for (AitBlock block : aitBlocks.values()) {
      block.moveLeft(start, length);
    }
    // 移除所有完全失效的 AitBlock
    Iterator<Map.Entry<String, AitBlock>> it = aitBlocks.entrySet().iterator();
    while (it.hasNext()) {
      if (!it.next().getValue().valid()) {
        it.remove();
      }
    }
  }

  /**
   * 序列化整个@模型为 JSONObject，写入消息扩展字段
   */
  public JSONObject getBlockJson() {
    try {
      JSONObject root = new JSONObject();
      for (Map.Entry<String, AitBlock> entry : aitBlocks.entrySet()) {
        if (entry.getValue().valid()) {
          JSONObject blockJson = entry.getValue().toJson();
          if (blockJson != null) {
            root.put(entry.getKey(), blockJson);
          }
        }
      }
      return root;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * 从 JSONObject 反序列化（接收方解析消息中的@信息）
   */
  public static AtContactsModel parseFromJson(JSONObject obj) {
    if (obj == null) return null;
    try {
      AtContactsModel model = new AtContactsModel();
      Iterator<String> keys = obj.keys();
      while (keys.hasNext()) {
        String key = keys.next();
        AitBlock block = AitBlock.parseFromJson(obj.getJSONObject(key));
        if (block != null) {
          model.aitBlocks.put(key, block);
        }
      }
      return model;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * 返回所有 AitBlock 列表（用于恢复编辑状态等）
   */
  public List<AitBlock> getAtBlockList() {
    return new ArrayList<>(aitBlocks.values());
  }

  /** 发送消息成功后，清空所有@记录 */
  public void reset() {
    aitBlocks.clear();
  }

  public boolean isEmpty() {
    return aitBlocks.isEmpty();
  }
}
