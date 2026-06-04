package com.bschool.msgrestapi.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class GroupMemberResponse {
    private Long userId;
    private String firstName;
    private String lastName;
    private Instant joinedAt;
}
