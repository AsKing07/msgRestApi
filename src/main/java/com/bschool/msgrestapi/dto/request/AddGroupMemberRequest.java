package com.bschool.msgrestapi.dto.request;

import jakarta.validation.constraints.NotNull;

public record AddGroupMemberRequest(
        @NotNull Long userId
) {
}
