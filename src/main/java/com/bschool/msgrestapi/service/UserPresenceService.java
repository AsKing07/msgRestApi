package com.bschool.msgrestapi.service;

import com.bschool.msgrestapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserPresenceService {

    private final UserRepository userRepository;

    @Transactional
    public void markActive(Long userId) {
        userRepository.updateLastActiveAt(userId, Instant.now());
    }
}
