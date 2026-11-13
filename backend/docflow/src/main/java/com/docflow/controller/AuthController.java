
package com.docflow.controller;

import org.springframework.http.HttpStatus;
import com.docflow.dto.LoginRequest;
import com.docflow.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import com.docflow.dto.RegisterRequest;
import com.docflow.dto.AuthResponse;
import com.docflow.entity.User;
import com.docflow.service.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    
    public AuthController(
            UserService userService,
            JwtService jwtService) {

        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        User user = userService.registerUser(
                request.getName(),
                request.getEmail(),
                request.getPassword()
        );

        AuthResponse response = new AuthResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        User user = userService.authenticateUser(
                request.getEmail(),
                request.getPassword()
        );

        String token = jwtService.generateToken(user);

        AuthResponse response = new AuthResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                token
        );

        return ResponseEntity.ok(response);
    }
}
