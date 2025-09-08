/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.provider

import com.netease.nimlib.sdk.NIMClient
import com.netease.nimlib.sdk.qchat.QChatRoleService
import com.netease.nimlib.sdk.qchat.enums.QChatRoleOption
import com.netease.nimlib.sdk.qchat.enums.QChatRoleResource
import com.netease.nimlib.sdk.qchat.enums.QChatRoleType
import com.netease.nimlib.sdk.qchat.param.QChatAddChannelRoleParam
import com.netease.nimlib.sdk.qchat.param.QChatAddMemberRoleParam
import com.netease.nimlib.sdk.qchat.param.QChatAddMembersToServerRoleParam
import com.netease.nimlib.sdk.qchat.param.QChatCheckPermissionParam
import com.netease.nimlib.sdk.qchat.param.QChatCheckPermissionsParam
import com.netease.nimlib.sdk.qchat.param.QChatCreateServerRoleParam
import com.netease.nimlib.sdk.qchat.param.QChatDeleteServerRoleParam
import com.netease.nimlib.sdk.qchat.param.QChatGetChannelRolesParam
import com.netease.nimlib.sdk.qchat.param.QChatGetExistingAccidsInServerRoleParam
import com.netease.nimlib.sdk.qchat.param.QChatGetExistingAccidsOfMemberRolesParam
import com.netease.nimlib.sdk.qchat.param.QChatGetExistingChannelRolesByServerRoleIdsParam
import com.netease.nimlib.sdk.qchat.param.QChatGetExistingServerRolesByAccidsParam
import com.netease.nimlib.sdk.qchat.param.QChatGetMemberRolesParam
import com.netease.nimlib.sdk.qchat.param.QChatGetMembersFromServerRoleParam
import com.netease.nimlib.sdk.qchat.param.QChatGetServerRolesByAccidParam
import com.netease.nimlib.sdk.qchat.param.QChatGetServerRolesParam
import com.netease.nimlib.sdk.qchat.param.QChatRemoveChannelRoleParam
import com.netease.nimlib.sdk.qchat.param.QChatRemoveMemberRoleParam
import com.netease.nimlib.sdk.qchat.param.QChatRemoveMembersFromServerRoleParam
import com.netease.nimlib.sdk.qchat.param.QChatUpdateChannelRoleParam
import com.netease.nimlib.sdk.qchat.param.QChatUpdateMemberRoleParam
import com.netease.nimlib.sdk.qchat.param.QChatUpdateServerRoleParam
import com.netease.nimlib.sdk.qchat.param.QChatUpdateServerRolePrioritiesParam
import com.netease.nimlib.sdk.qchat.result.QChatAddChannelRoleResult
import com.netease.nimlib.sdk.qchat.result.QChatAddMemberRoleResult
import com.netease.nimlib.sdk.qchat.result.QChatAddMembersToServerRoleResult
import com.netease.nimlib.sdk.qchat.result.QChatCheckPermissionResult
import com.netease.nimlib.sdk.qchat.result.QChatCheckPermissionsResult
import com.netease.nimlib.sdk.qchat.result.QChatCreateServerRoleResult
import com.netease.nimlib.sdk.qchat.result.QChatGetChannelRolesResult
import com.netease.nimlib.sdk.qchat.result.QChatGetExistingAccidsInServerRoleResult
import com.netease.nimlib.sdk.qchat.result.QChatGetExistingAccidsOfMemberRolesResult
import com.netease.nimlib.sdk.qchat.result.QChatGetExistingChannelRolesByServerRoleIdsResult
import com.netease.nimlib.sdk.qchat.result.QChatGetExistingServerRolesByAccidsResult
import com.netease.nimlib.sdk.qchat.result.QChatGetMemberRolesResult
import com.netease.nimlib.sdk.qchat.result.QChatGetMembersFromServerRoleResult
import com.netease.nimlib.sdk.qchat.result.QChatGetServerRolesByAccidResult
import com.netease.nimlib.sdk.qchat.result.QChatGetServerRolesResult
import com.netease.nimlib.sdk.qchat.result.QChatRemoveMembersFromServerRoleResult
import com.netease.nimlib.sdk.qchat.result.QChatUpdateChannelRoleResult
import com.netease.nimlib.sdk.qchat.result.QChatUpdateMemberRoleResult
import com.netease.nimlib.sdk.qchat.result.QChatUpdateServerRolePrioritiesResult
import com.netease.nimlib.sdk.qchat.result.QChatUpdateServerRoleResult
import com.netease.yunxin.kit.corekit.model.ResultInfo
import com.netease.yunxin.kit.qchatkit.onResult
import kotlin.coroutines.suspendCoroutine

object QChatRoleProvider {

    private val chatRoleService by lazy {
        NIMClient.getService(QChatRoleService::class.java)
    }

    @JvmStatic
    @JvmOverloads
    suspend fun createServerRole(
        serverId: Long,
        name: String,
        type: QChatRoleType,
        icon: String? = null,
        extension: String? = null
    ): ResultInfo<QChatCreateServerRoleResult> {
        return suspendCoroutine {
            val param = QChatCreateServerRoleParam(serverId, name, type)
            param.icon = icon
            param.extension = extension
            chatRoleService.createServerRole(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun deleteServerRole(
        serverId: Long,
        roleId: Long
    ): ResultInfo<Void> {
        return suspendCoroutine {
            val param = QChatDeleteServerRoleParam(serverId, roleId)
            chatRoleService.deleteServerRole(param).onResult(it)
        }
    }

    @JvmStatic
    @JvmOverloads
    suspend fun updateServerRole(
        serverId: Long,
        roleId: Long,
        name: String? = null,
        icon: String? = null,
        extension: String? = null,
        auths: Map<QChatRoleResource, QChatRoleOption>? = null
    ): ResultInfo<QChatUpdateServerRoleResult> {
        return suspendCoroutine {
            val param = QChatUpdateServerRoleParam(serverId, roleId)
            name?.let { s ->
                param.name = s
            }
            icon?.let { i -> param.icon = i }
            extension?.let { e -> param.ext = e }
            auths?.let { c -> param.resourceAuths = c }
            chatRoleService.updateServerRole(param).onResult(it)
        }
    }

    @JvmStatic
    @JvmOverloads
    suspend fun fetchServerRoles(
        serverId: Long,
        timeTag: Long,
        limit: Int
    ): ResultInfo<QChatGetServerRolesResult> {
        return suspendCoroutine {
            val param = QChatGetServerRolesParam(serverId, timeTag, limit)
            chatRoleService.getServerRoles(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun getServerRoleMembers(
        serverId: Long,
        roleId: Long,
        timeTag: Long,
        limit: Int,
        accId: String?
    ): ResultInfo<QChatGetMembersFromServerRoleResult> {
        return suspendCoroutine {
            val param = QChatGetMembersFromServerRoleParam(serverId, roleId, timeTag, limit)
            param.accid = accId
            chatRoleService.getMembersFromServerRole(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun removeMembersFromServerRole(
        serverId: Long,
        roleId: Long,
        accIds: List<String>
    ): ResultInfo<QChatRemoveMembersFromServerRoleResult> {
        return suspendCoroutine {
            val param = QChatRemoveMembersFromServerRoleParam(serverId, roleId, accIds)
            chatRoleService.removeMembersFromServerRole(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun addMembersToServerRole(
        serverId: Long,
        roleId: Long,
        accIds: List<String>
    ): ResultInfo<QChatAddMembersToServerRoleResult> {
        return suspendCoroutine {
            val param = QChatAddMembersToServerRoleParam(serverId, roleId, accIds)
            chatRoleService.addMembersToServerRole(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun getServerRolesByAccId(
        serverId: Long,
        accId: String,
        timeTag: Long,
        limit: Int
    ): ResultInfo<QChatGetServerRolesByAccidResult> {
        return suspendCoroutine {
            val param = QChatGetServerRolesByAccidParam(serverId, accId, timeTag, limit)
            chatRoleService.getServerRolesByAccid(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun getExistingServerRolesByAccids(
        serverId: Long,
        accIdList: List<String>
    ): ResultInfo<QChatGetExistingServerRolesByAccidsResult> {
        return suspendCoroutine {
            val param = QChatGetExistingServerRolesByAccidsParam(serverId, accIdList)
            chatRoleService.getExistingServerRolesByAccids(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun fetchChannelRoles(
        serverId: Long,
        channelId: Long,
        timeTag: Long,
        limit: Int
    ): ResultInfo<QChatGetChannelRolesResult> {
        return suspendCoroutine {
            val param = QChatGetChannelRolesParam(serverId, channelId, timeTag, limit)
            chatRoleService.getChannelRoles(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun fetchChannelMemberRole(
        serverId: Long,
        channelId: Long,
        timeTag: Long,
        limit: Int
    ): ResultInfo<QChatGetMemberRolesResult> {
        return suspendCoroutine {
            val param = QChatGetMemberRolesParam(serverId, channelId, timeTag, limit)
            chatRoleService.getMemberRoles(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun queryExistingChannelRoles(
        serverId: Long,
        channelId: Long,
        roleIdList: List<Long>
    ): ResultInfo<QChatGetExistingChannelRolesByServerRoleIdsResult> {
        return suspendCoroutine {
            val param = QChatGetExistingChannelRolesByServerRoleIdsParam(
                serverId,
                channelId,
                roleIdList
            )
            chatRoleService.getExistingChannelRolesByServerRoleIds(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun getExistingAccidsOfMemberRoles(
        serverId: Long,
        channelId: Long,
        accIdList: List<String>
    ): ResultInfo<QChatGetExistingAccidsOfMemberRolesResult> {
        return suspendCoroutine {
            val param = QChatGetExistingAccidsOfMemberRolesParam(serverId, channelId, accIdList)
            chatRoleService.getExistingAccidsOfMemberRoles(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun addChannelRole(
        serverId: Long,
        channelId: Long,
        parentRoleId: Long
    ): ResultInfo<QChatAddChannelRoleResult> {
        return suspendCoroutine {
            val param = QChatAddChannelRoleParam(serverId, channelId, parentRoleId)
            chatRoleService.addChannelRole(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun addChannelMemberRole(
        serverId: Long,
        channelId: Long,
        accId: String
    ): ResultInfo<QChatAddMemberRoleResult> {
        return suspendCoroutine {
            val param = QChatAddMemberRoleParam(serverId, channelId, accId)
            chatRoleService.addMemberRole(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun updateChannelRole(
        serverId: Long,
        channelId: Long,
        roleId: Long,
        optionMap: MutableMap<QChatRoleResource, QChatRoleOption>
    ): ResultInfo<QChatUpdateChannelRoleResult> {
        return suspendCoroutine {
            val param = QChatUpdateChannelRoleParam(serverId, channelId, roleId, optionMap)
            chatRoleService.updateChannelRole(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun updateChannelMemberRole(
        serverId: Long,
        channelId: Long,
        accId: String,
        optionMap: MutableMap<QChatRoleResource, QChatRoleOption>
    ): ResultInfo<QChatUpdateMemberRoleResult> {
        return suspendCoroutine {
            val param = QChatUpdateMemberRoleParam(serverId, channelId, accId, optionMap)
            chatRoleService.updateMemberRole(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun deleteChannelMemberRole(
        serverId: Long,
        channelId: Long,
        accId: String
    ): ResultInfo<Void> {
        return suspendCoroutine {
            val param = QChatRemoveMemberRoleParam(serverId, channelId, accId)
            chatRoleService.removeMemberRole(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun deleteChannelRole(
        serverId: Long,
        channelId: Long,
        roleId: Long
    ): ResultInfo<Void> {
        return suspendCoroutine {
            val param = QChatRemoveChannelRoleParam(serverId, channelId, roleId)
            chatRoleService.removeChannelRole(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun getExistingAccidsInServerRole(
        serverId: Long,
        roleId: Long,
        accIds: List<String>
    ): ResultInfo<QChatGetExistingAccidsInServerRoleResult> {
        return suspendCoroutine {
            val param = QChatGetExistingAccidsInServerRoleParam(serverId, roleId, accIds)
            chatRoleService.getExistingAccidsInServerRole(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun updateServerRolePriorities(
        serverId: Long,
        roleIdPriorityMap: Map<Long, Long>
    ): ResultInfo<QChatUpdateServerRolePrioritiesResult> {
        return suspendCoroutine {
            val param = QChatUpdateServerRolePrioritiesParam(serverId, roleIdPriorityMap)
            chatRoleService.updateServerRolePriorities(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun checkPermission(
        serverId: Long,
        channelId: Long?,
        resource: QChatRoleResource
    ): ResultInfo<QChatCheckPermissionResult> {
        return suspendCoroutine {
            val param = if (channelId == null) {
                QChatCheckPermissionParam(serverId, resource)
            } else {
                QChatCheckPermissionParam(serverId, channelId, resource)
            }
            chatRoleService.checkPermission(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun checkPermissions(
        serverId: Long,
        channelId: Long?,
        resourceList: List<QChatRoleResource>
    ): ResultInfo<QChatCheckPermissionsResult> {
        return suspendCoroutine {
            val param = if (channelId == null) {
                QChatCheckPermissionsParam(serverId, resourceList)
            } else {
                QChatCheckPermissionsParam(serverId, channelId, resourceList)
            }
            chatRoleService.checkPermissions(param).onResult(it)
        }
    }

    @JvmStatic
    suspend fun addMemberRole(
        serverId: Long,
        channelId: Long,
        accId: String
    ): ResultInfo<QChatAddMemberRoleResult> {
        return suspendCoroutine {
            chatRoleService.addMemberRole(QChatAddMemberRoleParam(serverId, channelId, accId)).onResult(
                it
            )
        }
    }

    @JvmStatic
    suspend fun removeMemberRole(
        serverId: Long,
        channelId: Long,
        accId: String
    ): ResultInfo<Void> {
        return suspendCoroutine {
            chatRoleService.removeMemberRole(QChatRemoveMemberRoleParam(serverId, channelId, accId)).onResult(
                it
            )
        }
    }

    @JvmStatic
    suspend fun updateMemberRole(
        serverId: Long,
        channelId: Long,
        accId: String,
        auths: Map<QChatRoleResource, QChatRoleOption>
    ): ResultInfo<QChatUpdateMemberRoleResult> {
        return suspendCoroutine {
            chatRoleService.updateMemberRole(
                QChatUpdateMemberRoleParam(serverId, channelId, accId, auths)
            )
                .onResult(it)
        }
    }
}
