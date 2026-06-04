package com.bschool.msgrestapi.controller;

import com.bschool.msgrestapi.dto.request.AddGroupMemberRequest;
import com.bschool.msgrestapi.dto.request.CreateGroupRequest;
import com.bschool.msgrestapi.dto.request.EditMessageRequest;
import com.bschool.msgrestapi.dto.request.SendMessageRequest;
import com.bschool.msgrestapi.dto.response.GroupMessageResponse;
import com.bschool.msgrestapi.dto.response.GroupResponse;
import com.bschool.msgrestapi.security.CurrentUserId;
import com.bschool.msgrestapi.service.GroupMessageService;
import com.bschool.msgrestapi.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@Tag(name = "Groupes", description = "Création et gestion des groupes")
@SecurityRequirement(name = "bearerAuth")
public class GroupController {

    private final GroupService groupService;
    private final GroupMessageService groupMessageService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer un groupe de 5 personnes maximum")
    public GroupResponse createGroup(
            @CurrentUserId Long userId,
            @Valid @RequestBody CreateGroupRequest request
    ) {
        return groupService.createGroup(userId, request.name(), request.memberIds());
    }

    @GetMapping
    @Operation(summary = "Lister mes groupes")
    public List<GroupResponse> listMyGroups(@CurrentUserId Long userId) {
        return groupService.listMyGroups(userId);
    }

    @PostMapping("/{groupId}/members")
    @Operation(summary = "Ajouter un ami au groupe")
    public GroupResponse addMember(
            @PathVariable Long groupId,
            @CurrentUserId Long userId,
            @Valid @RequestBody AddGroupMemberRequest request
    ) {
        return groupService.addMember(groupId, userId, request.userId());
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    @Operation(summary = "Supprimer un membre du groupe")
    public GroupResponse removeMember(
            @PathVariable Long groupId,
            @PathVariable Long memberId,
            @CurrentUserId Long userId
    ) {
        return groupService.removeMember(groupId, userId, memberId);
    }

    @GetMapping("/{groupId}/messages")
    @Operation(summary = "Lister les messages d'un groupe")
    public List<GroupMessageResponse> listMessages(
            @PathVariable Long groupId,
            @CurrentUserId Long userId
    ) {
        return groupMessageService.listMessages(groupId, userId);
    }

    @PostMapping("/{groupId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Envoyer un message dans un groupe")
    public GroupMessageResponse sendMessage(
            @PathVariable Long groupId,
            @CurrentUserId Long userId,
            @Valid @RequestBody SendMessageRequest request
    ) {
        return groupMessageService.sendMessage(groupId, userId, request.content());
    }

    @PutMapping("/{groupId}/messages/{messageId}")
    @Operation(summary = "Modifier son message de groupe")
    public GroupMessageResponse editMessage(
            @PathVariable Long groupId,
            @PathVariable Long messageId,
            @CurrentUserId Long userId,
            @Valid @RequestBody EditMessageRequest request
    ) {
        return groupMessageService.editMessage(groupId, messageId, userId, request.content());
    }

    @DeleteMapping("/{groupId}/messages/{messageId}")
    @Operation(summary = "Supprimer son message de groupe")
    public GroupMessageResponse deleteMessage(
            @PathVariable Long groupId,
            @PathVariable Long messageId,
            @CurrentUserId Long userId
    ) {
        return groupMessageService.deleteMessage(groupId, messageId, userId);
    }
}
