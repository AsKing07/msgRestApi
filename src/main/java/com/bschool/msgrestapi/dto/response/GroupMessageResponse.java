package com.bschool.msgrestapi.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class GroupMessageResponse {
    private Long id;
    private Long groupId;
    private Long senderId;
    private String senderFirstName;
    private String senderLastName;
    private String content;
    private String oldContent;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean edited;
    private boolean deleted;
    private Instant deletedAt;
}
