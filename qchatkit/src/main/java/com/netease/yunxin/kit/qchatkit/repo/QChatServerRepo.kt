/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

package com.netease.yunxin.kit.qchatkit.repo

import android.util.Pair
import com.netease.nimlib.sdk.qchat.enums.QChatChannelBlackWhiteType
import com.netease.nimlib.sdk.qchat.enums.QChatChannelMode
import com.netease.nimlib.sdk.qchat.enums.QChatChannelType
import com.netease.nimlib.sdk.qchat.enums.QChatRoleOption
import com.netease.nimlib.sdk.qchat.enums.QChatRoleResource
import com.netease.nimlib.sdk.qchat.enums.QChatRoleType
import com.netease.nimlib.sdk.qchat.result.QChatApplyServerJoinResult
import com.netease.nimlib.sdk.qchat.result.QChatUpdateServerRoleResult
import com.netease.yunxin.kit.alog.ALog
import com.netease.yunxin.kit.corekit.im.login.LoginService
import com.netease.yunxin.kit.corekit.im.provider.FetchCallback
import com.netease.yunxin.kit.corekit.im.provider.NosProvider
import com.netease.yunxin.kit.corekit.im.utils.toInform
import com.netease.yunxin.kit.corekit.model.ErrorMsg
import com.netease.yunxin.kit.corekit.model.ResultInfo
import com.netease.yunxin.kit.corekit.qchat.provider.QChatChannelProvider
import com.netease.yunxin.kit.corekit.qchat.provider.QChatRoleProvider
import com.netease.yunxin.kit.corekit.qchat.provider.QChatServerProvider
import com.netease.yunxin.kit.qchatkit.repo.model.NextInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatAnnounceMemberInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatChannelInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatPageResult
import com.netease.yunxin.kit.qchatkit.repo.model.QChatSearchResultInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerMemberInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerMemberWithRoleInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerRoleInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerRoleMemberInfo
import com.netease.yunxin.kit.qchatkit.repo.model.QChatServerWithSingleChannel
import java.io.File
import java.util.Collections
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/**
 * Qchat server repo
 *
 * @constructor Create empty Q chat server repo
 */
object QChatServerRepo {

    private const val LOG_TAG = "QChatServerRepo"

    /**
     * 创建社区
     */
    @JvmStatic
    @JvmOverloads
    fun createServer(
        name: String,
        iconUrl: String? = null,
        callBack: FetchCallback<QChatServerInfo>?
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatServerProvider.createServer(name, iconUrl).apply {
                ALog.e(LOG_TAG, toString())
            }.toInform(callBack) { it?.server?.toInfo() }
        }
    }

    /**
     * 创建社区，同时在该社区下创建两个频道
     */
    @JvmStatic
    @JvmOverloads
    fun createServerAndCreateChannel(
        name: String,
        channelName1: String,
        channelName2: String,
        iconUrl: String? = null,
        callBack: FetchCallback<QChatServerWithSingleChannel>?
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            //  create channel
            suspend fun createChannel(serverId: Long, channelName: String): QChatChannelInfo? =
                QChatChannelProvider.createChannel(
                    serverId,
                    channelName,
                    "",
                    QChatChannelType.MessageChannel,
                    QChatChannelMode.PUBLIC
                ).value?.channel?.toInfo()

            QChatServerProvider.createServer(name, iconUrl).toInform(callBack) {
                it?.server?.toInfo()?.run {
                    // create channel 2.
                    createChannel(serverId, channelName2)
                    // return the result and create channel 1.
                    QChatServerWithSingleChannel(
                        this,
                        createChannel(serverId, channelName1)
                    )
                }
            }
        }
    }

    /**
     * 创建公告频道（参考官网说明，公告频道规则）
     */
    @JvmStatic
    @JvmOverloads
    fun createAnnouncementServer(
        name: String,
        iconUrl: String? = null,
        authMap: Map<QChatRoleResource, QChatRoleOption>?,
        callBack: FetchCallback<ResultInfo<QChatServerWithSingleChannel>>?
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            suspend fun createChannel(serverId: Long, channelName: String) = async {
                QChatChannelProvider.createChannel(
                    serverId,
                    channelName,
                    "",
                    QChatChannelType.MessageChannel,
                    QChatChannelMode.PUBLIC
                )
            }
            suspend fun updateServerRole(
                serverId: Long,
                roleId: Long,
                authMap: Map<QChatRoleResource, QChatRoleOption>? = null
            ): ResultInfo<QChatUpdateServerRoleResult> {
                return QChatRoleProvider.updateServerRole(
                    serverId,
                    roleId,
                    auths = authMap
                )
            }
            suspend fun createManagerServerRole(serverId: Long, channelName: String) = async {
                val createServerRoleResult = QChatRoleProvider.createServerRole(
                    serverId,
                    channelName,
                    QChatRoleType.CUSTOM
                )
                val roleId = createServerRoleResult.value?.role?.roleId
                if (!createServerRoleResult.success || roleId == null) {
                    return@async ResultInfo(
                        success = false,
                        msg = createServerRoleResult.msg ?: ErrorMsg(-1)
                    )
                }
                authMap?.run {
                    updateServerRole(serverId, roleId, this)
                }
            }
            val createCustom = QChatServerInfo.generateCustom(
                QChatServerInfo.AnnouncementInfo(
                    channelId = null,
                    managerRoleId = null,
                    emojiReplay = true
                ),
                null
            )
            val createServerResult = QChatServerProvider.createServer(
                name,
                iconUrl,
                custom = createCustom
            )
            val serverInfo = createServerResult.value?.server?.toInfo()
            if (serverInfo == null) {
                callBack?.onFailed(-1)
                return@launch
            }
            val channelResult = createChannel(serverInfo.serverId, name).await()
            val channelId = channelResult.value?.channel?.channelId
            if (!channelResult.success || channelId == null) {
                callBack?.onSuccess(
                    ResultInfo(
                        QChatServerWithSingleChannel(serverInfo, null),
                        success = false,
                        ErrorMsg(channelResult.msg?.code ?: -1)
                    )
                )
                return@launch
            }
            val createManagerServerRoleResult =
                createManagerServerRole(serverInfo.serverId, name).await()
            val roleId = createManagerServerRoleResult?.value?.role?.roleId
            if (createManagerServerRoleResult?.success != true || roleId == null) {
                callBack?.onSuccess(
                    ResultInfo(
                        QChatServerWithSingleChannel(serverInfo, null),
                        success = false,
                        ErrorMsg(createManagerServerRoleResult?.msg?.code ?: -1)
                    )
                )
                return@launch
            }
            val updateCustom = QChatServerInfo.generateCustom(
                QChatServerInfo.AnnouncementInfo(
                    channelId = channelId,
                    managerRoleId = roleId,
                    emojiReplay = serverInfo.announcementInfo?.emojiReplay ?: true
                ),
                null
            )
            QChatServerProvider.updateServer(serverInfo.serverId, custom = updateCustom).toInform(
                callBack
            ) {
                ResultInfo(
                    it?.server?.toInfo()?.run {
                        QChatServerWithSingleChannel(
                            this,
                            this.announcementInfo!!.toChannelInfo(this)
                        )
                    }
                )
            }
        }
    }

    /**
     * 修改社区信息
     */
    @JvmStatic
    @JvmOverloads
    fun updateServer(
        serverId: Long,
        name: String? = null,
        icon: String? = null,
        custom: String? = null,
        callBack: FetchCallback<Void>
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatServerProvider.updateServer(serverId, name, icon, custom).toInform(callBack) {
                null
            }
        }
    }

    /**
     * 删除社区
     */
    @JvmStatic
    fun deleteServer(
        serverId: Long,
        inform: FetchCallback<Void>?
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatServerProvider.deleteServer(serverId).toInform(inform) {
                null
            }
        }
    }

    /**
     * 主动离开社区
     */
    @JvmStatic
    fun leaveServer(
        serverId: Long,
        inform: FetchCallback<Void>
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatServerProvider.leaveServer(serverId).toInform(inform) {
                null
            }
        }
    }

    /**
     * 通过分页信息查询社区
     */
    @JvmStatic
    fun fetchServerList(
        timeTag: Long,
        limit: Int,
        callBack: FetchCallback<List<QChatServerInfo>>?
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatServerProvider.getServersByPage(timeTag, limit).toInform(callBack) { pageResult ->
                val lastIndex = (pageResult?.servers?.size ?: 0) - 1
                pageResult?.servers?.mapIndexed { index, qChatServer ->
                    qChatServer.toInfo(
                        if (index == lastIndex) {
                            NextInfo(
                                pageResult.isHasMore,
                                pageResult.nextTimeTag
                            )
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }

    /**
     * 通过ServerId列表查询社区
     */
    @JvmStatic
    fun getServers(serverIdList: List<Long>, callBack: FetchCallback<List<QChatServerInfo>>?) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatServerProvider.getServers(serverIdList).toInform(callBack) {
                it?.servers?.map { item ->
                    item.toInfo()
                }
            }
        }
    }

    /**
     * 通过ServerId查询社区
     */
    @JvmStatic
    fun getServer(serverId: Long, callBack: FetchCallback<QChatServerInfo>?) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatServerProvider.getServers(listOf(serverId)).toInform(callBack) {
                val server = it?.servers?.firstOrNull()
                server?.toInfo()
            }
        }
    }

    /**
     * 通过NOS上传图片文件，并返回URL地址
     */
    @JvmStatic
    fun uploadServerIcon(file: File, callBack: FetchCallback<String>?) {
        CoroutineScope(Dispatchers.Main).launch {
            NosProvider.uploadImage(file, "image/jpeg").toInform(callBack) { it }
        }
    }

    /**
     * 通过accid查询社区成员
     */
    @JvmStatic
    fun getServerMembers(
        dataList: List<Pair<Long, String>>,
        callBack: FetchCallback<List<QChatServerMemberInfo>>?
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatServerProvider.getServerMembers(dataList).toInform(callBack) {
                it?.serverMembers?.map { item ->
                    item.toInfo()
                }
            }
        }
    }

    /**
     * 根据社区ID 查询社区，并查询当前登录账号是否加入到该服务中
     */
    @JvmStatic
    fun searchServerById(
        serverId: Long,
        isAnnouncement: Boolean = false,
        callback: FetchCallback<List<QChatSearchResultInfo>>?
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val accId = LoginService.account()
            if (accId == null) {
                callback?.onException(IllegalStateException("not login."))
                return@launch
            }

            val channelInfoAsync = async {
                QChatChannelProvider.fetchChannelsByPage(serverId, 0, 1).value?.channels?.firstOrNull()
                    ?.toInfo()
            }

            val memberInfoResultAsync = async {
                QChatServerProvider.getServerMembers(
                    Collections.singletonList(
                        Pair(
                            serverId,
                            accId
                        )
                    )
                )
            }

            QChatServerProvider.getServers(Collections.singletonList(serverId))
                .toInform(callback) { searchResult ->
                    searchResult?.servers?.map {
                        it.toInfo()
                    }?.filter {
                        if (isAnnouncement) {
                            it.announcementInfo != null
                        } else {
                            it.announcementInfo == null
                        }
                    }?.map {
                        val state =
                            if (memberInfoResultAsync.await().value?.serverMembers?.isNotEmpty() == true) QChatSearchResultInfo.STATE_JOINED else QChatSearchResultInfo.STATE_NOT_JOIN
                        QChatSearchResultInfo(
                            it,
                            state,
                            if (state == QChatSearchResultInfo.STATE_JOINED) channelInfoAsync.await() else null
                        )
                    }
                }
        }
    }

    /**
     * 申请加入服务
     */
    @JvmStatic
    @JvmOverloads
    fun applyServerJoin(
        serverId: Long,
        postscript: String? = null,
        callback: FetchCallback<QChatApplyServerJoinResult>?
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatServerProvider.applyServerJoin(serverId, postscript).toInform(callback) { it }
        }
    }

    /**
     * 邀请加入服务
     */
    @JvmStatic
    @JvmOverloads
    fun inviteServerMembers(
        serverId: Long,
        accIdList: List<String>,
        postscript: String? = null,
        callback: FetchCallback<List<String>>?
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatServerProvider.inviteServerMembers(serverId, accIdList, postscript)
                .toInform(callback) {
                    it?.failedAccids
                }
        }
    }

    /**
     * 修改其他人的社区成员信息
     */
    @JvmStatic
    @JvmOverloads
    fun updateOtherMember(
        serverId: Long,
        accid: String,
        nick: String? = null,
        avatar: String? = null,
        inform: FetchCallback<Void>
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatServerProvider.updateServerMember(serverId, accid, nick, avatar).toInform(inform) {
                null
            }
        }
    }

    /**
     * 查询社区成员列表，并查询该成员加入的身份组列表
     */
    @JvmStatic
    fun fetchServerMemberInfoWithRolesList(
        serverId: Long,
        timeTag: Long,
        limit: Int,
        callBack: FetchCallback<List<QChatServerMemberWithRoleInfo>>?
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatServerProvider.getServerMembersByPage(serverId, timeTag, limit)
                .toInform(callBack) { pageResult ->
                    val lastIndex = (pageResult?.serverMembers?.size ?: 0) - 1
                    pageResult?.serverMembers?.mapIndexed { index, member ->
                        member.toInfo(
                            null,
                            if (index == lastIndex) {
                                NextInfo(
                                    pageResult.isHasMore,
                                    pageResult.nextTimeTag
                                )
                            } else {
                                null
                            }
                        )
                    }?.apply {
                        // get accId list
                        val list = map { info ->
                            info.accId
                        }
                        // fill in role info by accId
                        getRoleMap(serverId, list).also { roleMap ->
                            this.map { item ->
                                item.roleList = roleMap[item.accId]
                            }
                        }
                    }
                }
        }
    }

    /**
     * 查询服务中用户信息，并包括该用户的身份组信息
     */
    @JvmStatic
    fun fetchServerMemberInfoWithRolesByAccId(
        serverId: Long,
        accId: String,
        callBack: FetchCallback<QChatServerMemberWithRoleInfo>?
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            // get roles
            val roleList = async {
                getRoleMap(serverId, listOf(accId))[accId]
            }
            QChatServerProvider.getServerMembers(Collections.singletonList(Pair(serverId, accId)))
                .toInform(callBack) { memberResult ->
                    memberResult?.serverMembers?.firstOrNull()
                        ?.toInfo(roleList.await())
                }
        }
    }

    /**
     * 查询社区下，不在某个频道的所有成员信息
     */
    @JvmStatic
    fun fetchServerMemberWithoutChannel(
        serverId: Long,
        channelId: Long,
        timeTag: Long,
        limit: Int,
        callBack: FetchCallback<List<QChatServerMemberInfo>>?
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            suspend fun getExistingChannelMemberInfo(accIdList: List<String>): HashSet<String> {
                val result =
                    QChatRoleProvider.getExistingAccidsOfMemberRoles(serverId, channelId, accIdList)
                return HashSet<String>().apply {
                    result.value?.accidList?.map {
                        this.add(it)
                    }
                }
            }

            val result = QChatServerProvider.getServerMembersByPage(serverId, timeTag, limit)
            callBack.run {
                val hasMemberSet = mutableSetOf<String>()
                result.toInform(this) {
                    it?.serverMembers?.apply {
                        val list = map { info ->
                            info.accid
                        }
                        hasMemberSet.addAll(getExistingChannelMemberInfo(list))
                    }?.filterNot {
                        hasMemberSet.contains(it.accid)
                    }?.map { serverMember ->
                        serverMember.toInfo()
                    }
                }
            }
        }
    }

    private suspend fun getRoleMapWithId(
        serverId: Long,
        accIdList: List<String>
    ): Map<String, List<Long>> {
        val result = QChatRoleProvider.getExistingServerRolesByAccids(serverId, accIdList)
        return HashMap<String, List<Long>>().apply {
            result.value?.accidServerRolesMap?.map {
                this[it.key] = it.value.map { item ->
                    item.roleId
                }
            }
        }
    }

    private suspend fun getRoleMap(
        serverId: Long,
        accIdList: List<String>
    ): Map<String, List<QChatServerRoleInfo>> {
        val result = QChatRoleProvider.getExistingServerRolesByAccids(serverId, accIdList)
        return HashMap<String, List<QChatServerRoleInfo>>().apply {
            result.value?.accidServerRolesMap?.map {
                this[it.key] = it.value.map { item ->
                    item.toInfo()
                }
            }
        }
    }

    /**
     * 修改社区个人信息
     */
    @JvmStatic
    @JvmOverloads
    fun updateMyMember(
        serverId: Long,
        nick: String? = null,
        avatar: String? = null,
        custom: String? = null,
        inform: FetchCallback<Void>
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatServerProvider.updateMyServerMember(serverId, nick, avatar, custom)
                .toInform(inform) {
                    null
                }
        }
    }

    /**
     * 踢除社区成员
     */
    @JvmStatic
    fun kickMember(
        serverId: Long,
        accid: String,
        inform: FetchCallback<Void>
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatServerProvider.kickOutMember(serverId, accid).toInform(inform) {
                null
            }
        }
    }

    /**
     * 查询社区成员列表，并过滤掉某个身份组的成员
     */
    @JvmStatic
    fun fetchServerMembersWithRolesFilter(
        serverId: Long,
        roleId: Long,
        timeTag: Long,
        limit: Int,
        callBack: FetchCallback<QChatPageResult<QChatServerRoleMemberInfo>>
    ) {
        getServerMembersWithFilter(
            serverId,
            timeTag,
            limit,
            {
                QChatRoleProvider.getExistingAccidsInServerRole(
                    serverId,
                    roleId,
                    it
                ).value?.accidList
            },
            callBack
        )
    }

    /**
     * 查询社区成员列表，根据filter进行过滤
     */
    @JvmStatic
    @JvmOverloads
    fun getServerMembersWithFilter(
        serverId: Long,
        timeTag: Long,
        limit: Int,
        filter: (suspend (List<String>) -> List<String>?)? = null,
        callBack: FetchCallback<QChatPageResult<QChatServerRoleMemberInfo>>
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatServerProvider.getServerMembersByPage(serverId, timeTag, limit)
                .toInform(callBack) { it ->
                    val list = it?.serverMembers?.map { info ->
                        info.accid
                    }
                    if (list != null) {
                        val filterList = filter?.let { it(list) }
                        QChatPageResult<QChatServerRoleMemberInfo>(
                            it.serverMembers?.filterNot { member ->
                                filterList?.contains(member.accid) ?: false
                            }?.map { qChatServerMember ->
                                qChatServerMember.toMember()
                            },
                            it.isHasMore ?: false,
                            it.nextTimeTag ?: 0
                        )
                    } else {
                        null
                    }
                }
        }
    }

    /**
     * 查询社区成员列表，并过滤掉黑名单成员
     */
    @JvmStatic
    fun fetchServerMembersWithWhiteBlackFilter(
        serverId: Long,
        channelId: Long,
        channelType: Int,
        timeTag: Long,
        limit: Int,
        callBack: FetchCallback<QChatPageResult<QChatServerRoleMemberInfo>>
    ) {
        val resultList = mutableListOf<String>()
        val memberType = if (channelType == 0) QChatChannelBlackWhiteType.BLACK else QChatChannelBlackWhiteType.WHITE
        getServerMembersWithFilter(
            serverId,
            timeTag,
            limit,
            filter = {
                QChatChannelProvider.queryChannelBlackWhiteMembers(
                    serverId,
                    channelId,
                    memberType,
                    it
                ).value?.memberList?.map { serverMember ->
                    resultList.add(serverMember.accid)
                }
                resultList
            },
            callBack
        )
    }

    /**
     * 以游客身份加入社区
     */
    @JvmStatic
    fun enterAsVisitor(serverIdList: List<Long>, callBack: FetchCallback<List<Long>>?) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatServerProvider.enterAsVisitor(serverIdList).toInform(callBack) {
                it?.failedList
            }
        }
    }

    /**
     * 以游客身份离开社区
     */
    @JvmStatic
    fun leaveAsVisitor(serverIdList: List<Long>, callBack: FetchCallback<List<Long>>?) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatServerProvider.leaveAsVisitor(serverIdList).toInform(callBack) {
                it?.failedList
            }
        }
    }

    /**
     * 以游客身份订阅社区
     */
    @JvmStatic
    fun subscribeAsVisitor(
        serverIdList: List<Long>,
        register: Boolean, callBack: FetchCallback<List<Long>>?
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatServerProvider.subscribeAsVisitor(serverIdList, register).toInform(callBack) {
                it?.failedList
            }
        }
    }

    /**
     * 分页查询公告频道订阅者成员列表
     */
    @JvmStatic
    fun getAnnounceServerNormalMemberByPage(
        serverId: Long,
        timeTag: Long,
        limit: Int,
        roleId: Long,
        ownerId: String,
        callback: FetchCallback<Pair<List<QChatAnnounceMemberInfo>, List<String>>>?
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            QChatServerProvider.getServerMembersByPage(serverId, timeTag, limit)
                .toInform(callback) { pageResult ->
                    val lastIndex = (pageResult?.serverMembers?.size ?: 0) - 1
                    pageResult?.serverMembers?.mapIndexed { index, member ->
                        member.toAnnounceMemberInfo(
                            QChatAnnounceMemberInfo.USER_TYPE_NORMAL,
                            if (index == lastIndex) {
                                NextInfo(
                                    pageResult.isHasMore,
                                    pageResult.nextTimeTag
                                )
                            } else {
                                null
                            }
                        )
                    }?.run {
                        val filterAccIdList = mutableListOf<String>()
                        // get accId list
                        val list = map { info ->
                            info.accId
                        }
                        // fill in role info by accId
                        val roleIdMap = getRoleMapWithId(serverId, list)
                        val finalResult = this.filter {
                            (it.accId != ownerId && roleIdMap[it.accId]?.contains(roleId) != true).apply {
                                if (!this) {
                                    filterAccIdList.add(it.accId)
                                }
                            }
                        }
                        Pair(finalResult, filterAccIdList)
                    }
                }
        }
    }

    /**
     * 分页查询公告频道管理员成员列表
     */
    @JvmStatic
    @JvmOverloads
    fun getAnnounceServerManagerMemberByPage(
        serverId: Long,
        timeTag: Long,
        limit: Int,
        roleId: Long,
        ownerId: String? = null,
        anchorAccId: String? = null,
        callback: FetchCallback<List<QChatAnnounceMemberInfo>>?
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            suspend fun getOwnerInfo(serverId: Long, accId: String) =
                QChatServerProvider.getServerMembers(listOf(Pair(serverId, accId)))

            suspend fun getManagerRoleMemberList(
                serverId: Long,
                roleId: Long,
                timeTag: Long,
                pageSize: Int,
                accId: String?
            ) = QChatRoleProvider.getServerRoleMembers(serverId, roleId, timeTag, pageSize, accId)

            val result = mutableListOf<QChatAnnounceMemberInfo>()
            ownerId?.run {
                val ownerInfoResult = getOwnerInfo(serverId, this)
                if (!ownerInfoResult.success) {
                    ownerInfoResult.msg?.exception?.run {
                        callback?.onException(this)
                    } ?: run {
                        callback?.onFailed(ownerInfoResult.msg?.code ?: -1)
                    }
                    return@launch
                }
                ownerInfoResult.value?.serverMembers?.map {
                    it.toAnnounceMemberInfo(QChatAnnounceMemberInfo.USER_TYPE_OWNER)
                }?.apply {
                    result.addAll(this)
                }
            }
            val roleMemberListResult =
                getManagerRoleMemberList(serverId, roleId, timeTag, limit, anchorAccId)
            if (!roleMemberListResult.success) {
                roleMemberListResult.msg?.exception?.run {
                    callback?.onException(this)
                } ?: run {
                    callback?.onFailed(roleMemberListResult.msg?.code ?: -1)
                }
                return@launch
            }
            roleMemberListResult.value?.roleMemberList?.filter {
                it.accid != ownerId
            }?.map {
                it.toAnnounceMemberInfo(QChatAnnounceMemberInfo.USER_TYPE_MANAGER)
            }?.apply {
                result.addAll(this)
            }
            callback?.onSuccess(result)
        }
    }
}
