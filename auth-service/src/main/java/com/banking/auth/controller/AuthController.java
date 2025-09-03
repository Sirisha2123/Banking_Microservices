package com.banking.auth.controller;

import com.banking.auth.entity.UserEntity;
import com.banking.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService service;

    @Autowired
    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public Mono<Map<String, Object>> register(@RequestBody UserEntity user, @RequestParam String role) {
        return Mono.just(service.register(user, role))
                .map(result -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("message", "User registered successfully");
                    response.put("status", "success");
                    response.put("userId", result);
                    return response;
                });
    }

    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> initiateLogin(@RequestBody UserEntity credentials) {
        return Mono.just(service.initiateLogin(credentials.getUsername(), credentials.getPassword()))
                .map(result -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("message", result);
                    response.put("status", "success");
                    return response;
                });
    }

    @PostMapping(value = "/verify-otp", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> verifyOtp(@RequestParam String username, @RequestParam String otp) {
        return Mono.just(service.verifyOtpAndIssueTokens(username, otp))
                .map(result -> {
                    Map<String, Object> response = new HashMap<>();
                    if (result.containsKey("status") && "success".equals(result.get("status"))) {
                        response.put("message", "OTP verified successfully");
                        response.put("status", "success");
                        response.put("token", result.get("token"));
                        response.put("refreshToken", result.get("refreshToken"));
                    } else {
                        response.put("message", "Invalid OTP");
                        response.put("status", "failure");
                    }
                    return response;
                });
    }

    @PostMapping(value = "/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> refresh(@RequestParam String refreshToken) {
        return Mono.just(service.refreshToken(refreshToken))
                .map(result -> {
                    Map<String, Object> response = new HashMap<>();
                    if (result.containsKey("status") && "success".equals(result.get("status"))) {
                        response.put("message", "Token refreshed successfully");
                        response.put("status", "success");
                        response.put("token", result.get("token"));
                    } else {
                        response.put("message", "Invalid refresh token");
                        response.put("status", "failure");
                    }
                    return response;
                });
    }
}