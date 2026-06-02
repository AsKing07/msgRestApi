package com.bschool.msgrestapi.service;

import com.bschool.msgrestapi.dto.request.LoginRequest;
import com.bschool.msgrestapi.dto.request.RegisterRequest;
import com.bschool.msgrestapi.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
