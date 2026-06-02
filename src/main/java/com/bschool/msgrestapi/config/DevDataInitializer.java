package com.bschool.msgrestapi.config;

import com.bschool.msgrestapi.domain.entity.Conversation;
import com.bschool.msgrestapi.domain.entity.User;
import com.bschool.msgrestapi.repository.ConversationRepository;
import com.bschool.msgrestapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Profile("dev-seed")
@RequiredArgsConstructor
public class DevDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;

    @Override
    public void run(String... args) {
        User alice = userRepository.findByEmail("alice@example.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .firstName("Alice")
                        .lastName("Martin")
                        .email("alice@example.com")
                        .passwordHash("dev")
                        .createdAt(Instant.now())
                        .build()));

        User bob = userRepository.findByEmail("bob@example.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .firstName("Bob")
                        .lastName("Durand")
                        .email("bob@example.com")
                        .passwordHash("dev")
                        .createdAt(Instant.now())
                        .build()));

        User low = alice.getId() < bob.getId() ? alice : bob;
        User high = alice.getId() < bob.getId() ? bob : alice;

        conversationRepository.findByParticipantLowAndParticipantHigh(low, high)
                .orElseGet(() -> conversationRepository.save(Conversation.builder()
                        .participantLow(low)
                        .participantHigh(high)
                        .createdAt(Instant.now())
                        .lastActivityAt(Instant.now())
                        .build()));
    }
}
