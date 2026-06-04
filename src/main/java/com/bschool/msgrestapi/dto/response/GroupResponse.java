package com.bschool.msgrestapi.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class GroupResponse {
    private Long id;
    private String name;
    private Long ownerId;
    private String ownerFirstName;
    private String ownerLastName;
    private Instant createdAt;
    private int memberCount;
    private List<GroupMemberResponse> members;
}
