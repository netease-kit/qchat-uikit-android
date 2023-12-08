/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.repo

import com.netease.nimlib.sdk.qchat.enums.QChatRoleOption
import com.netease.nimlib.sdk.qchat.enums.QChatRoleResource
import com.netease.nimlib.sdk.qchat.enums.QChatRoleType
import com.netease.nimlib.sdk.qchat.model.QChatMemberRole
import com.netease.nimlib.sdk.qchat.result.QChatCreateServerRoleResult
import com.netease.nimlib.sdk.qchat.result.QChatRemoveMembersFromServerRoleResult
import com.netease.yunxin.kit.alog.ALog
import com.netease.yunxin.kit.corekit.im.provider.FetchCallback
import com.netease.yunxin.kit.corekit.im.utils.toInform
import com.netease.yunxin.kit.corekit.model.ResultInfo
import com.netease.yunxin.kit.corekit.qchat.provider.QChatRoleProvider
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelMember
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelRoleInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatRoleOptionEnum
import com.netease.yunxin.kit.qchatkit.repo.model.QChatRoleResourceEnum
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerRoleInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerRoleMemberInfo
import com.netease.yunxin.kit.qchatkit.repo.model.ServerRoleResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object QChatRoleRepo {

    const val MAX_ROLE_PAGE_SIZE = 200

    /**
     * Q chat role repo
     *
     * @constructor Create empty Q chat role repo
     */

    private const val LOG_TAG = "QChatRoleRepo"

    /**
     * 新增服务器身份组，并将成员加入
     */
    @JvmStatic
    @JvmOverloads
    fun createRoleWithMember(
        serverId: Long,
        name: String,
        type: QChatRoleType = QChatRoleType.CUSTOM,
        members: List<String>,
        icon: String? = null,
        extension: String? = null,
        inform: ((ResultInfo<QChatCreateServerRoleResult>) -> Unit)
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = QChatRoleProvider.createServerRole(serverId, name, type, icon, extension)
            ALog.e(LOG_TAG, result.toString())
            if (result.success) {
                QChatRoleProvider.addMembersToServerRole(
                    serverId,
                    result.value!!.role.roleId,
                    members
                )
            }
            inform.invoke(result)
        }
    }

    /**
     * 新增服务器身份组
     */
    @JvmStatic
    @JvmOverloads
    fun createRole(
        serverId: Long,
        name: String,
        type: QChatRoleType = QChatRoleType.CUSTOM,
        icon: String? = null,
        extension: String? = null,
        inform: ((ResultInfo<QChatCreateServerRoleResult>) -> Unit)
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = QChatRoleProvider.createServerRole(serverId, name, type, icon, extension)
            ALog.e(LOG_TAG, result.toString())
            inform.invoke(result)
        }
    }

    /**
     * 查询某服务器下某身份组下的成员列表
     */
    @JvmStatic
    @JvmOverloads
    fun fetchServerRoleMember(
        serverId: Long,
        roleId: Long,
        timeTag: Long,
        limit: Int,
        accId: String? = null,
        callback: FetchCallback<List<QChatServerRoleMemberInfo>>
    ) {
        CoroutineScope((Dispatchers.Main)).launch {
            QChatRoleProvider.getServerRoleMembers(serverId, roleId, timeTag, limit, accId)
                .toInform(callback) {
                    it?.roleMemberList?.map { qChatServerRoleMember ->
                        qChatServerRoleMember.toInfo()
                    }
                }
        }
    }

    /**
     * 将某些人移出某服务器身份组
     */
    @JvmStatic
    fun removeServerRoleMember(
        serverId: Long,
        roleId: Long,
        accIds: List<String>,
        callback: FetchCallback<List<String>>
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatRoleProvider.removeMembersFromServerRole(serverId, roleId, accIds)
                .toInform(callback) {
                    it?.successAccids
                }
        }
    }

    /**
     * 将某些人移出某服务器身份组
     */
    @JvmStatic
    fun removeServerRoleMemberForResult(
        serverId: Long,
        roleId: Long,
        accIds: List<String>,
        callback: FetchCallback<QChatRemoveMembersFromServerRoleResult>
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatRoleProvider.removeMembersFromServerRole(serverId, roleId, accIds)
                .toInform(callback) { it }
        }
    }

    /**
     * 将某些人加入某服务器身份组
     */
    @JvmStatic
    fun addServerRoleMember(
        serverId: Long,
        roleId: Long,
        accIds: List<String>,
        callback: FetchCallback<List<String>>
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatRoleProvider.addMembersToServerRole(serverId, roleId, accIds).toInform(callback) {
                it?.successAccids
            }
        }
    }

    /**
     * 新增Channel身份组
     */
    @JvmStatic
    fun addChannelRole(
        serverId: Long,
        channelId: Long,
        parentRoleId: Long,
        callback: FetchCallback<QChatChannelRoleInfo>
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = QChatRoleProvider.addChannelRole(serverId, channelId, parentRoleId)
            callback.run {
                result.toInform(this) {
                    it?.role?.toInfo()
                }
            }
        }
    }

    /**
     * 修改频道下某身份组的权限
     */
    @JvmStatic
    fun updateChannelRole(
        serverId: Long,
        channelId: Long,
        roleId: Long,
        option: Map<QChatRoleResourceEnum, QChatRoleOptionEnum>,
        callback: FetchCallback<QChatChannelRoleInfo>
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val optionMap = mutableMapOf<QChatRoleResource, QChatRoleOption>()
            option.map {
                optionMap.put(
                    QChatConvert.convertToRoleResource(it.key),
                    QChatConvert.convertToRoleOption(it.value)
                )
            }
            val result = QChatRoleProvider.updateChannelRole(serverId, channelId, roleId, optionMap)
            callback.run {
                result.toInform(this) {
                    it?.role?.toInfo()
                }
            }
        }
    }

    /**
     * 删除频道身份组
     */
    @JvmStatic
    fun deleteChannelRole(
        serverId: Long,
        channelId: Long,
        roleId: Long,
        callback: FetchCallback<Void>
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = QChatRoleProvider.deleteChannelRole(serverId, channelId, roleId)
            callback.run {
                result.toInform(this) { it }
            }
        }
    }

    /**
     * 为某个人定制某频道的权限
     */
    @JvmStatic
    fun addChannelMemberRole(
        serverId: Long,
        channelId: Long,
        accId: String,
        callback: FetchCallback<QChatChannelMember>
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = QChatRoleProvider.addChannelMemberRole(serverId, channelId, accId)
            callback.run {
                result.toInform(this) {
                    it?.role?.toInfo()
                }
            }
        }
    }

    /**
     * 修改某人的定制权限
     */
    @JvmStatic
    fun updateChannelMember(
        serverId: Long,
        channelId: Long,
        accId: String,
        option: Map<QChatRoleResourceEnum, QChatRoleOptionEnum>,
        callback: FetchCallback<QChatChannelMember>
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val optionMap = mutableMapOf<QChatRoleResource, QChatRoleOption>()
            option.map {
                optionMap.put(
                    QChatConvert.convertToRoleResource(it.key),
                    QChatConvert.convertToRoleOption(it.value)
                )
            }
            val result =
                QChatRoleProvider.updateChannelMemberRole(serverId, channelId, accId, optionMap)
            callback.run {
                result.toInform(this) {
                    it?.role?.toInfo()
                }
            }
        }
    }

    /**
     * 删除频道下某人的定制权限
     */
    @JvmStatic
    fun deleteChannelMemberRole(
        serverId: Long,
        channelId: Long,
        accId: String,
        callback: FetchCallback<Void>
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = QChatRoleProvider.deleteChannelMemberRole(serverId, channelId, accId)
            callback.run {
                result.toInform(this) { it }
            }
        }
    }

    /**
     * 修改服务器身份组信息
     */
    @JvmStatic
    @JvmOverloads
    fun updateRole(
        serverId: Long,
        roleId: Long,
        name: String? = null,
        icon: String? = null,
        extension: String? = null,
        auths: Map<QChatRoleResourceEnum, QChatRoleOptionEnum>? = null,
        inform: FetchCallback<Void>
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val optionMap = mutableMapOf<QChatRoleResource, QChatRoleOption>()
            auths?.map {
                optionMap.put(
                    QChatConvert.convertToRoleResource(it.key),
                    QChatConvert.convertToRoleOption(it.value)
                )
            }
            QChatRoleProvider.updateServerRole(serverId, roleId, name, icon, extension, optionMap)
                .toInform(inform) {
                    ALog.e(LOG_TAG, it.toString())
                    null
                }
        }
    }

    /**
     * 查询服务器下身份组列表，第一页返回结果额外包含everyone身份组，自定义身份组数量充足的情况下会返回limit+1个身份组
     */
    @JvmStatic
    @JvmOverloads
    fun fetchServerRoles(
        serverId: Long,
        timeTag: Long,
        limit: Int,
        inform: FetchCallback<ServerRoleResult>
    ) {
        CoroutineScope((Dispatchers.Main)).launch {
            QChatRoleProvider.fetchServerRoles(serverId, timeTag, limit)
                .toInform(inform) { qChatGetServerRolesResult ->
                    ServerRoleResult(
                        qChatGetServerRolesResult?.roleList?.map { qChatServerRole ->
                            qChatServerRole.toInfo()
                        },
                        qChatGetServerRolesResult?.isMemberSet
                    )
                }
        }
    }

    /**
     * 查询服务器下身份组列表，第一页返回结果额外包含everyone身份组，并移除频道下身份组
     */
    @JvmStatic
    @JvmOverloads
    fun fetchServerRolesWithoutChannel(
        serverId: Long,
        channelId: Long,
        timeTag: Long,
        limit: Int,
        inform: FetchCallback<List<QChatServerRoleInfo>>
    ) {
        CoroutineScope((Dispatchers.Main)).launch {
            QChatRoleProvider.fetchServerRoles(serverId, timeTag, limit)
                .toInform(inform) { qChatGetServerRolesResult ->
                    val hasRoleSet = mutableSetOf<Long>()
                    qChatGetServerRolesResult?.roleList?.apply {
                        val roleList = map { serverRole ->
                            serverRole.roleId
                        }
                        queryChannelRole(serverId, channelId, roleList).also { resultSet ->
                            hasRoleSet.addAll(resultSet)
                        }
                    }?.filterNot {
                        hasRoleSet.contains(it.roleId)
                    }?.map { qChatServerRole ->
                        qChatServerRole.toInfo()
                    }
                }
        }
    }

    private suspend fun queryChannelRole(
        serverId: Long,
        channelId: Long,
        roleIdList: List<Long>
    ): Set<Long> {
        val result = QChatRoleProvider.queryExistingChannelRoles(serverId, channelId, roleIdList)
        return mutableSetOf<Long>().apply {
            result.value?.roleList?.map {
                this.add(it.parentRoleId)
            }
        }
    }

    /**
     * 查询ACCID用户加入的身份组列表，结果只有自定义身份组，不包含everyone身份组
     */
    @JvmStatic
    @JvmOverloads
    fun fetchMemberJoinedRoles(
        serverId: Long,
        accId: String,
        inform: FetchCallback<List<QChatServerRoleInfo>>
    ) {
        CoroutineScope((Dispatchers.Main)).launch {
            QChatRoleProvider.getExistingServerRolesByAccids(
                serverId,
                listOf(accId)
            ).toInform(inform) { qChatGetServerRolesByAccidResult ->
                qChatGetServerRolesByAccidResult?.accidServerRolesMap?.get(accId)
                    ?.map { qChatServerRole ->
                        qChatServerRole.toInfo()
                    }
            }
        }
    }

    /**
     * 移除服务器身份组
     */
    @JvmStatic
    fun deleteServerRole(
        serverId: Long,
        roleId: Long,
        callback: FetchCallback<Void>
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = QChatRoleProvider.deleteServerRole(serverId, roleId)
            callback.run {
                result.toInform(this) { it }
            }
        }
    }

    /**
     * 批量修改服务器身份组优先级
     */
    @JvmStatic
    fun updateRolesPriorities(
        serverId: Long,
        topPriority: Long,
        rolesList: List<QChatServerRoleInfo>,
        callback: FetchCallback<Void>
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val map = mutableMapOf<Long, Long>()
            var top = topPriority
            rolesList.map {
                map[it.roleId] = top++
            }
            QChatRoleProvider.updateServerRolePriorities(serverId, map).toInform(callback) {
                null
            }
        }
    }

    /**
     * 查询自己是否拥有某个权限
     */
    @JvmStatic
    @JvmOverloads
    fun checkPermission(
        serverId: Long,
        channelId: Long? = null,
        resource: QChatRoleResource,
        callback: FetchCallback<Boolean>
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatRoleProvider.checkPermission(serverId, channelId, resource).toInform(callback) {
                it?.isHasPermission
            }
        }
    }

    /**
     * 查询自己是否拥有某些权限
     */
    @JvmStatic
    @JvmOverloads
    fun checkPermissions(
        serverId: Long,
        channelId: Long? = null,
        resourceList: List<QChatRoleResource>,
        callback: FetchCallback<Map<QChatRoleResource, QChatRoleOption>>
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatRoleProvider.checkPermissions(serverId, channelId, resourceList)
                .toInform(callback) {
                    it?.permissions
                }
        }
    }

    /**
     * 查询channel下某人的定制权限
     */
    @JvmStatic
    fun getMemberRoles(
        serverId: Long,
        channelId: Long,
        timeTag: Long,
        limit: Int,
        callback: FetchCallback<List<QChatMemberRole>>
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatRoleProvider.fetchChannelMemberRole(
                serverId,
                channelId,
                timeTag,
                limit
            ).toInform(callback) {
                it?.roleList
            }
        }
    }

    /**
     * 为某个人定制某频道的权限
     */
    @JvmStatic
    fun addMemberRole(
        serverId: Long,
        channelId: Long,
        accId: String,
        callback: FetchCallback<QChatMemberRole>?
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatRoleProvider.addMemberRole(serverId, channelId, accId)
                .toInform(callback) {
                    it?.role
                }
        }
    }

    /**
     * 删除频道下某人的定制权限
     */
    @JvmStatic
    fun removeMemberRole(
        serverId: Long,
        channelId: Long,
        accId: String,
        callback: FetchCallback<Void>?
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatRoleProvider.removeMemberRole(serverId, channelId, accId)
                .toInform(callback) {
                    it
                }
        }
    }

    /**
     * 修改某人的定制权限
     */
    @JvmStatic
    fun updateMemberRole(
        serverId: Long,
        channelId: Long,
        accId: String,
        auths: Map<QChatRoleResource, QChatRoleOption>,
        callback: FetchCallback<QChatMemberRole>?
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatRoleProvider.updateMemberRole(serverId, channelId, accId, auths)
                .toInform(callback) {
                    it?.role
                }
        }
    }

    /**
     * 更新公告频道订阅者的权限
     */
    @JvmStatic
    fun updateEveryonePermissionForAnnounce(serverId: Long, authMap: Map<QChatRoleResource, QChatRoleOption>, callback: FetchCallback<Void>?) {
        CoroutineScope(Dispatchers.Main).launch {
            val everyoneResult =
                QChatRoleProvider.fetchServerRoles(serverId, 0, 2).value?.roleList?.find {
                    it.type == QChatRoleType.EVERYONE
                }
            val roleId = everyoneResult?.roleId
            if (everyoneResult == null || roleId == null) {
                callback?.onFailed(-1)
                return@launch
            }
            QChatRoleProvider.updateServerRole(
                serverId,
                roleId,
                auths = authMap
            ).toInform(callback) {
                null
            }
        }
    }
}
