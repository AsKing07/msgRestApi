package com.bschool.msgrestapi.service.impl;

import com.bschool.msgrestapi.domain.entity.User;
import com.bschool.msgrestapi.dto.request.LoginRequest;
import com.bschool.msgrestapi.dto.request.RegisterRequest;
import com.bschool.msgrestapi.dto.response.AuthResponse;
import com.bschool.msgrestapi.exception.BusinessException;
import com.bschool.msgrestapi.exception.UnauthorizedException;
import com.bschool.msgrestapi.repository.UserRepository;
import com.bschool.msgrestapi.security.JwtTokenProvider;
import com.bschool.msgrestapi.service.AuthService;

import com.bschool.msgrestapi.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Un compte existe déjà avec cet email");
        }

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();

        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Email ou mot de passe incorrect"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Email ou mot de passe incorrect");
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtTokenProvider.generateToken(user);
        return new AuthResponse(
                token,
                "Bearer",
                jwtProperties.expirationSeconds(),
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName()
        );
    }
}
