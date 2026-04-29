package com.restaurant.identity.service;

import com.restaurant.identity.dto.AuthResponse;
import com.restaurant.identity.dto.LoginRequest;
import com.restaurant.identity.dto.RegisterRequest;
import com.restaurant.identity.dto.UserResponse;
import com.restaurant.identity.entity.Role;
import com.restaurant.identity.entity.User;
import com.restaurant.identity.exception.EmailAlreadyExistsException;
import com.restaurant.identity.exception.InvalidCredentialsException;
import com.restaurant.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new EmailAlreadyExistsException(req.email());
        }
        User user = User.builder()
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .firstName(req.firstName())
                .lastName(req.lastName())
                .phone(req.phone())
                .role(Role.CLIENT)
                .enabled(true)
                .build();
        user = userRepository.save(user);
        String token = jwtService.generateAccessToken(user);
        return AuthResponse.bearer(token, jwtService.getExpirationSeconds(), UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new InvalidCredentialsException();
        }
        String token = jwtService.generateAccessToken(user);
        return AuthResponse.bearer(token, jwtService.getExpirationSeconds(), UserResponse.from(user));
    }
}