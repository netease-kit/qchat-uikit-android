// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.qchatkit.ui.model.ait;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 单个@块的位置记录
 * 记录了一个@某人在 EditText 中所有出现位置的区间列表（复制粘贴可能出现多个相同@块）
 */
public class AitBlock {

  public static final String KEY_TEXT = "text";
  public static final String KEY_ACCOUNT_ID = "accountId";
  public static final String KEY_SEGMENTS = "segments";

  /** "@名字 "（含前缀@和末尾空格） */
  private String text;
  /** 被@人的账号 */
  private String accountId;
  /** 该@文本在EditText中所有出现位置的区间列表 */
  private final List<AitSegment> segments = new ArrayList<>();

  public AitBlock() {}

  public AitBlock(String text, String accountId) {
    this.text = text;
    this.accountId = accountId;
  }

  public String getText() {
    return text;
  }

  public String getAccountId() {
    return accountId;
  }

  public List<AitSegment> getSegments() {
    return segments;
  }

  /** 新增一个@位置区间 */
  public void addSegment(int start, int end) {
    segments.add(new AitSegment(start, end));
  }

  /**
   * 用户在 start 位置之前插入文字时，右移该位置及之后的所有 Segment 坐标
   * @param start  插入位置
   * @param changeText 插入的文字
   */
  public void moveRight(int start, String changeText) {
    int length = changeText.length();
    for (AitSegment segment : segments) {
      if (segment.getStart() >= start) {
        segment.moveRight(length);
      }
    }
  }

  /**
   * 用户删除文字时，左移坐标；若删除命中 Segment 内部，标记 broken=true
   * @param start  删除起始位置
   * @param length 删除长度
   */
  public void moveLeft(int start, int length) {
    for (AitSegment segment : segments) {
      if (segment.getStart() > start) {
        // 删除位置在 Segment 之前，整体左移
        if (segment.getStart() - length < start) {
          // 删除范围覆盖了 Segment 起始位置，标记broken
          segment.setBroken(true);
        } else {
          segment.moveLeft(length);
        }
      } else if (start < segment.getEnd()) {
        // 删除位置在 Segment 内部，标记broken
        segment.setBroken(true);
      }
    }
  }

  /**
   * 通过终止位置反向查找 Segment（用于整体删除@块）
   * @param end 终止位置（包含）
   * @return 找到的 Segment，未找到返回 null
   */
  public AitSegment findLastSegmentByEnd(int end) {
    for (AitSegment segment : segments) {
      if (!segment.isBroken() && segment.getEnd() == end) {
        return segment;
      }
    }
    return null;
  }

  /** 判断该@块是否至少有一个未损坏的 Segment */
  public boolean valid() {
    for (AitSegment segment : segments) {
      if (!segment.isBroken()) {
        return true;
      }
    }
    return false;
  }

  /** 序列化为 JSONObject */
  public JSONObject toJson() {
    try {
      JSONObject obj = new JSONObject();
      obj.put(KEY_TEXT, text);
      obj.put(KEY_ACCOUNT_ID, accountId);
      JSONArray segArray = new JSONArray();
      for (AitSegment segment : segments) {
        if (!segment.isBroken()) {
          segArray.put(segment.toJson());
        }
      }
      obj.put(KEY_SEGMENTS, segArray);
      return obj;
    } catch (Exception e) {
      return null;
    }
  }

  /** 从 JSONObject 反序列化 */
  public static AitBlock parseFromJson(JSONObject obj) {
    if (obj == null) return null;
    try {
      AitBlock block = new AitBlock();
      block.text = obj.optString(KEY_TEXT);
      block.accountId = obj.optString(KEY_ACCOUNT_ID);
      JSONArray segArray = obj.optJSONArray(KEY_SEGMENTS);
      if (segArray != null) {
        for (int i = 0; i < segArray.length(); i++) {
          AitSegment segment = AitSegment.parseFromJson(segArray.getJSONObject(i));
          if (segment != null) {
            block.segments.add(segment);
          }
        }
      }
      return block;
    } catch (Exception e) {
      return null;
    }
  }

  // ----------------------------- AitSegment 内部类 ---------------------------------

  public static class AitSegment {

    public static final String KEY_START = "start";
    public static final String KEY_END = "end";
    public static final String KEY_BROKEN = "broken";

    private int start;
    private int end;
    private boolean broken;

    public AitSegment(int start, int end) {
      this.start = start;
      this.end = end;
      this.broken = false;
    }

    public int getStart() { return start; }
    public int getEnd() { return end; }
    public boolean isBroken() { return broken; }
    public void setBroken(boolean broken) { this.broken = broken; }

    public void moveRight(int length) {
      start += length;
      end += length;
    }

    public void moveLeft(int length) {
      start -= length;
      end -= length;
    }

    public JSONObject toJson() {
      try {
        JSONObject obj = new JSONObject();
        obj.put(KEY_START, start);
        obj.put(KEY_END, end);
        obj.put(KEY_BROKEN, broken);
        return obj;
      } catch (Exception e) {
        return null;
      }
    }

    public static AitSegment parseFromJson(JSONObject obj) {
      if (obj == null) return null;
      try {
        int start = obj.optInt(KEY_START);
        int end = obj.optInt(KEY_END);
        AitSegment segment = new AitSegment(start, end);
        segment.broken = obj.optBoolean(KEY_BROKEN, false);
        return segment;
      } catch (Exception e) {
        return null;
      }
    }
  }
}
