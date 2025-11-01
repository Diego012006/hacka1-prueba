package com.oreo.insightfactory.user;

import com.oreo.insightfactory.user.dto.LoginRequest;
import com.oreo.insightfactory.user.dto.LoginResponse;
import com.oreo.insightfactory.user.dto.RegisterRequest;
import com.oreo.insightfactory.user.dto.UserResponse;
import com.oreo.insightfactory.security.JwtService;
import jakarta.validation.ValidationException;
import java.time.Duration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ValidationException("Username already exists");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ValidationException("Email already exists");
        }
        if (request.role() == UserRole.BRANCH && (request.branch() == null || request.branch().isBlank())) {
            throw new ValidationException("Branch is required for branch users");
        }
        if (request.role() == UserRole.CENTRAL && request.branch() != null) {
            throw new ValidationException("Central users must not have a branch");
        }
        User user = new User(
                request.username(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.role(),
                request.role() == UserRole.BRANCH ? request.branch() : null
        );
        return UserResponse.from(userRepository.save(user));
    }

    public LoginResponse login(LoginRequest request) {
        var authToken = new UsernamePasswordAuthenticationToken(request.username(), request.password());
        authenticationManager.authenticate(authToken);
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ValidationException("Invalid credentials"));
        String token = jwtService.generateToken(user);
        long expiresIn = Duration.ofHours(1).toSeconds();
        return new LoginResponse(token, expiresIn, user.getRole(), user.getBranch());
    }
}
