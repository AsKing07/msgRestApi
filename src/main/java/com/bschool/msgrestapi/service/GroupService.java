package com.bschool.msgrestapi.service;

import com.bschool.msgrestapi.dto.response.GroupResponse;

import java.util.List;

public interface GroupService {

    GroupResponse createGroup(Long ownerId, String name, List<Long> memberIds);

    List<GroupResponse> listMyGroups(Long userId);

    GroupResponse addMember(Long groupId, Long ownerId, Long memberId);

    GroupResponse removeMember(Long groupId, Long ownerId, Long memberId);
}
