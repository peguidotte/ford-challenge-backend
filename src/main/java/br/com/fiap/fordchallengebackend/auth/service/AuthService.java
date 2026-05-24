package br.com.fiap.fordchallengebackend.auth.service;

import br.com.fiap.fordchallengebackend.auth.domain.Role;
import br.com.fiap.fordchallengebackend.auth.domain.User;
import br.com.fiap.fordchallengebackend.auth.dto.AuthResponse;
import br.com.fiap.fordchallengebackend.auth.dto.LoginRequest;
import br.com.fiap.fordchallengebackend.auth.dto.RefreshRequest;
import br.com.fiap.fordchallengebackend.auth.dto.RegisterRequest;
import br.com.fiap.fordchallengebackend.auth.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadCredentialsException("Erro no cadastro");
        }

        var role = parseRole(request.role());

        var user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(role);
        userRepository.save(user);

        var accessToken = jwtService.generateAccessToken(new org.springframework.security.core.userdetails.User(
            user.getEmail(), user.getPassword(), new java.util.ArrayList<>()));
        var refreshToken = jwtService.generateRefreshToken(new org.springframework.security.core.userdetails.User(
            user.getEmail(), user.getPassword(), new java.util.ArrayList<>()));

        LOGGER.info("REGISTER: email={} role={}", user.getEmail(), user.getRole());

        return new AuthResponse(
            accessToken,
            refreshToken,
            jwtService.getExpirationMs(),
            user.getEmail(),
            user.getRole().name()
        );
    }

    public AuthResponse login(LoginRequest request) {
        try {
            var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            var userDetails = (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

            var accessToken = jwtService.generateAccessToken(userDetails);
            var refreshToken = jwtService.generateRefreshToken(userDetails);

            var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciais invalidas"));

            LOGGER.info("LOGIN_SUCCESS: email={}", request.email());

            return new AuthResponse(
                accessToken,
                refreshToken,
                jwtService.getExpirationMs(),
                user.getEmail(),
                user.getRole().name()
            );
        } catch (BadCredentialsException e) {
            LOGGER.warn("LOGIN_FAILURE: email={}", request.email());
            throw e;
        }
    }

    public AuthResponse refresh(RefreshRequest request) {
        var newAccessToken = jwtService.refreshAccessToken(request.refreshToken());
        if (newAccessToken == null) {
            throw new BadCredentialsException("Token de refresh invalido");
        }

        var email = jwtService.extractEmail(newAccessToken);
        var user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BadCredentialsException("Token de refresh invalido"));

        LOGGER.info("TOKEN_REFRESH: email={}", email);

        return new AuthResponse(
            newAccessToken,
            request.refreshToken(),
            jwtService.getExpirationMs(),
            user.getEmail(),
            user.getRole().name()
        );
    }

    @PostConstruct
    public void seedAdminUser() {
        if (!userRepository.existsByEmail("admin@fordchallenge.com")) {
            var admin = new User();
            admin.setName("Admin");
            admin.setEmail("admin@fordchallenge.com");
            admin.setPassword(passwordEncoder.encode(System.getenv().getOrDefault("APP_ADMIN_PASSWORD", "admin123")));
            admin.setRole(Role.ROLE_ADMIN);
            userRepository.save(admin);
            LOGGER.info("Admin user created: admin@fordchallenge.com");
        }
    }

    private Role parseRole(String role) {
        if (role == null || role.isBlank()) {
            return Role.ROLE_USER;
        }
        try {
            return Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Role.ROLE_USER;
        }
    }
}
