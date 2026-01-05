package com.example.booking.service;

import com.example.booking.model.User;
import com.example.booking.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCrypt;
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
    public User register(String username, String password, boolean admin) {
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
        u.setRole(admin ? "ADMIN" : "USER");
        return repository.save(u);
    }

    @Transactional
    public String signIn(String username, String password) {

        User user = repository.findByUsername(username)
                .filter(u -> encoder.matches(password, u.getPasswordHash()))
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        log.info("Successful login: {}", user.getUsername());

        return tokenService.createToken(
                user.getUsername(),
                user.getRole()
        );
    }
}
