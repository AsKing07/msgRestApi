package com.bschool.msgrestapi.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateGroupRequest(
        @Size(max = 100)
        String name,

        List<@NotNull Long> memberIds
) {
}
