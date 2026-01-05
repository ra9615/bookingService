package com.example.booking.service;

import com.example.booking.dto.AuthRequestDto;
import com.example.booking.dto.AuthResponseDto;
import com.example.booking.model.User;
import com.example.booking.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final JwtTokenService tokenService;

    @Transactional
    public AuthResponseDto register(AuthRequestDto request) {
        if (repository.existsByUsername(request.username())) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(encoder.encode(request.password()));
        user.setRole("USER");
        repository.save(user);

        String token = tokenService.createToken(user.getUsername(), user.getRole());
        log.info("User registered: {}", user.getUsername());

        return new AuthResponseDto(token, user.getUsername(), user.getRole());
    }

    @Transactional
    public AuthResponseDto signIn(String username, String password) {

        User user = repository.findByUsername(username)
                .filter(u -> encoder.matches(password, u.getPasswordHash()))
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        log.info("Successful login: {}", user.getUsername());

        String token = tokenService.createToken(
                user.getUsername(),
                user.getRole()
        );
        return new AuthResponseDto(token, user.getUsername(), user.getRole());
    }
}
