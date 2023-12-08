/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.repo.model

import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import java.io.Serializable

class QChatServerRoleMemberInfo(
    val serverId: Long,
    val accId: String,
    val nick: String?,
    val avatarUrl: String? = null,
    val type: Int = 0,
    val joinTime: Long,
    val inviter: String,
    val createTime: Long,
    val updateTime: Long,
    val custom: String? = null
) : Serializable, Parcelable {
    var selected: Boolean = false

    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString() ?: "",
        parcel.readString(),
        parcel.readString(),
        parcel.readInt(),
        parcel.readLong(),
        parcel.readString() ?: "",
        parcel.readLong(),
        parcel.readLong(),
        parcel.readString()
    ) {
        selected = parcel.readByte() != 0.toByte()
    }

    fun getNickName(): String {
        return if (TextUtils.isEmpty(nick)) accId else nick!!
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) {
            return false
        }

        if (other === this) {
            return true
        }

        val that = other as QChatServerRoleMemberInfo
        return serverId == that.serverId && accId == that.accId
    }

    override fun hashCode(): Int {
        var result = serverId.hashCode()
        result = 31 * result + accId.hashCode()
        return result
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(serverId)
        parcel.writeString(accId)
        parcel.writeString(nick)
        parcel.writeString(avatarUrl)
        parcel.writeInt(type)
        parcel.writeLong(joinTime)
        parcel.writeString(inviter)
        parcel.writeLong(createTime)
        parcel.writeLong(updateTime)
        parcel.writeString(custom)
        parcel.writeByte(if (selected) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<QChatServerRoleMemberInfo> {
        override fun createFromParcel(parcel: Parcel): QChatServerRoleMemberInfo {
            return QChatServerRoleMemberInfo(parcel)
        }

        override fun newArray(size: Int): Array<QChatServerRoleMemberInfo?> {
            return arrayOfNulls(size)
        }
    }
}
