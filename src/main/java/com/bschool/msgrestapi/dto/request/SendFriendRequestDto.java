package com.bschool.msgrestapi.dto.request;

import jakarta.validation.constraints.NotNull;

public record SendFriendRequestDto(
        @NotNull Long receiverId
) {
}
