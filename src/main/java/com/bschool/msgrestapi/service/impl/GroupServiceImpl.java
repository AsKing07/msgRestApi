package com.bschool.msgrestapi.service.impl;

import com.bschool.msgrestapi.domain.entity.ChatGroup;
import com.bschool.msgrestapi.domain.entity.ChatGroupMember;
import com.bschool.msgrestapi.domain.entity.User;
import com.bschool.msgrestapi.domain.util.UserPairUtil;
import com.bschool.msgrestapi.dto.response.GroupMemberResponse;
import com.bschool.msgrestapi.dto.response.GroupResponse;
import com.bschool.msgrestapi.exception.BusinessException;
import com.bschool.msgrestapi.exception.ResourceNotFoundException;
import com.bschool.msgrestapi.repository.ChatGroupMemberRepository;
import com.bschool.msgrestapi.repository.ChatGroupRepository;
import com.bschool.msgrestapi.repository.FriendshipRepository;
import com.bschool.msgrestapi.repository.UserRepository;
import com.bschool.msgrestapi.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    private static final int MAX_GROUP_MEMBERS = 5;

    private final ChatGroupRepository chatGroupRepository;
    private final ChatGroupMemberRepository chatGroupMemberRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;

    @Override
    @Transactional
    public GroupResponse createGroup(Long ownerId, String name, List<Long> memberIds) {
        User owner = requireUser(ownerId);
        Set<Long> uniqueMemberIds = sanitizeMemberIds(memberIds);

        if (uniqueMemberIds.contains(ownerId)) {
            throw new BusinessException("Le propriétaire est déjà membre du groupe.");
        }
        if (uniqueMemberIds.size() + 1 > MAX_GROUP_MEMBERS) {
            throw new BusinessException("Un groupe ne peut pas dépasser 5 personnes.");
        }

        Instant now = Instant.now();
        ChatGroup group = chatGroupRepository.save(ChatGroup.builder()
                .name(normalizeGroupName(name))
                .owner(owner)
                .lastActivityAt(now)
                .build());

        chatGroupMemberRepository.save(ChatGroupMember.builder()
                .group(group)
                .user(owner)
                .build());

        for (Long memberId : uniqueMemberIds) {
            User member = requireUser(memberId);
            assertOwnerFriend(owner, member);
            chatGroupMemberRepository.save(ChatGroupMember.builder()
                    .group(group)
                    .user(member)
                    .build());
        }

        return toResponse(group);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupResponse> listMyGroups(Long userId) {
        User user = requireUser(userId);
        return chatGroupRepository.findAllByMemberOrderByLastActivityDesc(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public GroupResponse addMember(Long groupId, Long ownerId, Long memberId) {
        ChatGroup group = requireGroup(groupId);
        assertOwner(group, ownerId);

        if (ownerId.equals(memberId)) {
            throw new BusinessException("Le propriétaire est déjà membre du groupe.");
        }

        User member = requireUser(memberId);
        if (chatGroupMemberRepository.existsByGroupAndUser(group, member)) {
            throw new BusinessException("Cet utilisateur est déjà membre du groupe.");
        }
        if (chatGroupMemberRepository.countByGroup(group) >= MAX_GROUP_MEMBERS) {
            throw new BusinessException("Un groupe ne peut pas dépasser 5 personnes.");
        }

        assertOwnerFriend(group.getOwner(), member);
        chatGroupMemberRepository.save(ChatGroupMember.builder()
                .group(group)
                .user(member)
                .build());

        return toResponse(group);
    }

    @Override
    @Transactional
    public GroupResponse removeMember(Long groupId, Long ownerId, Long memberId) {
        ChatGroup group = requireGroup(groupId);
        assertOwner(group, ownerId);

        if (group.getOwner().getId().equals(memberId)) {
            throw new BusinessException("Le propriétaire ne peut pas être supprimé de son groupe.");
        }

        User member = requireUser(memberId);
        ChatGroupMember groupMember = chatGroupMemberRepository.findByGroupAndUser(group, member)
                .orElseThrow(() -> new ResourceNotFoundException("Membre du groupe introuvable."));

        chatGroupMemberRepository.delete(groupMember);
        return toResponse(group);
    }

    private Set<Long> sanitizeMemberIds(List<Long> memberIds) {
        if (memberIds == null) {
            return Set.of();
        }
        return new LinkedHashSet<>(memberIds);
    }

    private String normalizeGroupName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "Groupe";
        }
        return name.trim();
    }

    private void assertOwner(ChatGroup group, Long userId) {
        if (!group.getOwner().getId().equals(userId)) {
            throw new BusinessException("Seul le propriétaire du groupe peut effectuer cette action.");
        }
    }

    private void assertOwnerFriend(User owner, User member) {
        var orderedUsers = UserPairUtil.order(owner, member);
        if (friendshipRepository.findByUserLowAndUserHigh(orderedUsers.low(), orderedUsers.high()).isEmpty()) {
            throw new BusinessException("Le propriétaire peut uniquement ajouter ses amis au groupe.");
        }
    }

    private ChatGroup requireGroup(Long groupId) {
        return chatGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Groupe introuvable."));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));
    }

    private GroupResponse toResponse(ChatGroup group) {
        List<GroupMemberResponse> members = chatGroupMemberRepository.findAllByGroupOrderByJoinedAtAsc(group)
                .stream()
                .map(member -> {
                    User user = member.getUser();
                    return GroupMemberResponse.builder()
                            .userId(user.getId())
                            .firstName(user.getFirstName())
                            .lastName(user.getLastName())
                            .joinedAt(member.getJoinedAt())
                            .build();
                })
                .toList();

        User owner = group.getOwner();
        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .ownerId(owner.getId())
                .ownerFirstName(owner.getFirstName())
                .ownerLastName(owner.getLastName())
                .createdAt(group.getCreatedAt())
                .memberCount(members.size())
                .members(members)
                .build();
    }
}
