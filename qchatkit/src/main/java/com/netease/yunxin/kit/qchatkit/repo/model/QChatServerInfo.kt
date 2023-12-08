/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.repo.model

import com.netease.yunxin.kit.alog.ALog
import java.io.Serializable
import java.lang.Exception
import org.json.JSONObject

class QChatServerInfo(
    val serverId: Long,
    val name: String,
    val iconUrl: String? = null,
    val owner: String,
    val memberCount: Int,
    val inviteMode: Int,
    val applyMode: Int,
    val valid: Boolean,
    val createTime: Long,
    val updateTime: Long,
    val channelNum: Int,
    val custom: String? = null,
    val searchType: Int = 0,
    val nextInfo: NextInfo? = null
) : Serializable {
    constructor(serverId: Long) : this(serverId, "", "", "", 0, 0, 0, true, 0L, 0L, 0, null, 0, null)

    constructor(
        serverId: Long,
        name: String,
        iconUrl: String? = null,
        custom: String? = null
    ) : this(serverId, name, iconUrl, "", 0, 0, 0, true, 0L, 0L, 0, custom, 0, null)

    private val logTag = "QChatServerInfo"

    var announcementInfo: AnnouncementInfo? = null
        private set

    var desc: String? = null
        private set

    init {
        custom?.run {
            try {
                val json = JSONObject(this)
                val announceObj = json.optJSONObject("announce")
                if (announceObj != null) {
                    val channelId = announceObj.optLong("channelId")
                    val managerRoleId = announceObj.optLong("roleId")
                    val emojiReplay = announceObj.optInt("emojiReplay") == 1
                    announcementInfo =
                        AnnouncementInfo(channelId, managerRoleId, emojiReplay)
                }
                desc = json.optString("topic")
            } catch (exception: Exception) {
                ALog.e(logTag, "parsing announcement and des is error. $exception")
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QChatServerInfo

        if (serverId != other.serverId) return false

        return true
    }

    override fun hashCode(): Int {
        return serverId.hashCode()
    }

    override fun toString(): String {
        return "QChatServerInfo(serverId=$serverId, name='$name', iconUrl=$iconUrl, owner='$owner', memberCount=$memberCount, inviteMode=$inviteMode, applyMode=$applyMode, valid=$valid, createTime=$createTime, updateTime=$updateTime, channelNum=$channelNum, custom=$custom, searchType=$searchType, nextInfo=$nextInfo)"
    }

    companion object {

        @JvmStatic
        fun generateCustom(announcementInfo: AnnouncementInfo?, desc: String?): String? {
            if (desc == null && announcementInfo == null) {
                return null
            }
            val customObj = JSONObject()
            desc?.run {
                customObj.put("topic", this)
            }
            if (announcementInfo != null) {
                val announceObj = JSONObject()
                announceObj.put("channelId", announcementInfo.channelId)
                announceObj.put("roleId", announcementInfo.managerRoleId)
                announceObj.put("emojiReplay", if (announcementInfo.emojiReplay == true) 1 else 0)
                customObj.put("announce", announceObj)
            }
            return customObj.toString()
        }
    }

    class AnnouncementInfo(
        val channelId: Long?,
        val managerRoleId: Long?,
        var emojiReplay: Boolean?
    ) : Serializable {

        fun isValid(): Boolean {
            return channelId != null && managerRoleId != null && channelId > 0L && managerRoleId >= 0L
        }

        fun toChannelInfo(serverInfo: QChatServerInfo): QChatChannelInfo? {
            return if (isValid()) {
                QChatChannelInfo(
                    channelId!!,
                    serverInfo.serverId,
                    serverInfo.name,
                    null,
                    QChatChannelTypeEnum.Message,
                    true,
                    serverInfo.createTime,
                    serverInfo.updateTime,
                    serverInfo.owner,
                    QChatChannelModeEnum.Public
                )
            } else {
                null
            }
        }

        override fun toString(): String {
            return "AnnouncementInfo(channelId=$channelId, managerRoleId=$managerRoleId, emojiReplay=$emojiReplay)"
        }
    }
}
