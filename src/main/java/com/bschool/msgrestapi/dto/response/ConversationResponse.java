package com.bschool.msgrestapi.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.time.Instant;

@Getter
@Builder
public class ConversationResponse {
    private Long id;
    private Long friendId;
    private String friendFirstName;
    private String friendLastName;
    private Instant lastActivityAt;
}