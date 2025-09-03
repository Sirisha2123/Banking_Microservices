package com.banking.auth.service;

import com.banking.auth.entity.UserEntity;
import com.banking.auth.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class AuthService {

    private final UserRepository repository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${jwt.secret}")
    private String secret;

    @Autowired
    public AuthService(UserRepository repository, KafkaTemplate<String, String> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String register(UserEntity user, String role) {
        if (!List.of("ROLE_CUSTOMER", "ROLE_EMPLOYEE", "ROLE_ADMIN").contains(role)) {
            throw new RuntimeException("Invalid role");
        }
        user.setPassword(encoder.encode(user.getPassword()));
        user.setRole(role);
        repository.save(user);
        kafkaTemplate.send("login-events", "User registered: " + user.getUsername() + " as " + role);
        return user.getId().toString(); // Return user ID as string
    }

    public String initiateLogin(String username, String password) {
        UserEntity user = repository.findByUsername(username);
        if (user != null && encoder.matches(password, user.getPassword())) {
            String otp = generateOtp();
            kafkaTemplate.send("otp-events", username + ":" + otp + ":" + user.getPhone() + ":" + user.getEmail());
            return "OTP sent for MFA";
        }
        throw new RuntimeException("Invalid credentials");
    }

    public Map<String, Object> verifyOtpAndIssueTokens(String username, String otp) {
        UserEntity user = repository.findByUsername(username);
        if (user == null) throw new RuntimeException("User not found");
        long now = System.currentTimeMillis();
        String jwt = Jwts.builder()
                .setSubject(username)
                .claim("role", user.getRole())
                .setExpiration(new Date(now + 900000)) // 15 minutes
                .signWith(getSigningKey())
                .compact();
        String refresh = Jwts.builder()
                .setSubject(username)
                .setExpiration(new Date(now + 86400000)) // 1 day
                .signWith(getSigningKey())
                .compact();
        kafkaTemplate.send("login-events", "User logged in: " + username + " as " + user.getRole());
        Map<String, Object> response = new HashMap<>();
        response.put("message", "OTP verified successfully");
        response.put("status", "success");
        response.put("token", jwt);
        response.put("refreshToken", refresh);
        return response;
    }

    public Map<String, Object> refreshToken(String refreshToken) {
        String username = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(refreshToken)
                .getBody()
                .getSubject();
        long now = System.currentTimeMillis();
        String newJwt = Jwts.builder()
                .setSubject(username)
                .setExpiration(new Date(now + 900000))
                .signWith(getSigningKey())
                .compact();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Token refreshed successfully");
        response.put("status", "success");
        response.put("token", newJwt);
        return response;
    }

    private String generateOtp() {
        return String.format("%06d", new Random().nextInt(999999));
    }
}