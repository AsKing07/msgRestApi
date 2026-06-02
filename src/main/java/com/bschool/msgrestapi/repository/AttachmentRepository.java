package com.bschool.msgrestapi.repository;

import com.bschool.msgrestapi.domain.entity.Attachment;
import com.bschool.msgrestapi.domain.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByConversationAndDeletedFalseOrderByUploadedAtAsc(Conversation conversation);

    Optional<Attachment> findByIdAndConversation_Id(Long id, Long conversationId);
}
