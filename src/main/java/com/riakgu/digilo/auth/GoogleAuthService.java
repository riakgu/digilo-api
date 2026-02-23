package com.riakgu.digilo.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.riakgu.digilo.auth.dto.AuthResponse;
import com.riakgu.digilo.auth.dto.GoogleAuthRequest;
import com.riakgu.digilo.common.exception.UnauthorizedException;
import com.riakgu.digilo.config.GoogleProperties;
import com.riakgu.digilo.user.Role;
import com.riakgu.digilo.user.User;
import com.riakgu.digilo.user.UserRepository;
import com.riakgu.digilo.user.UserStatus;
import com.riakgu.digilo.user.dto.UserResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final GoogleProperties googleProperties;
    private final UserRepository userRepository;
    private final UserAuthProviderRepository authProviderRepository;
    private final JwtService jwtService;

    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    public void init() {
        verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleProperties.clientId()))
                .build();
    }

    @Transactional
    public AuthResponse authenticate(GoogleAuthRequest request, String userAgent, String ip) {
        GoogleIdToken.Payload payload = verifyIdToken(request.getIdToken());

        String googleId = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        // Check if provider link exists
        UserAuthProvider existingProvider = authProviderRepository
                .findByProviderAndProviderId(AuthProvider.GOOGLE, googleId)
                .orElse(null);

        User user;
        if (existingProvider != null) {
            // User already linked with Google
            user = existingProvider.getUser();
            log.info("Google login for existing linked user: userId={}", user.getId());
        } else {
            // Check if user exists by email
            user = userRepository.findByEmail(email).orElse(null);

            if (user != null) {
                // Link Google to existing user
                createAuthProvider(user, googleId);
                log.info("Linked Google to existing user: userId={}", user.getId());
            } else {
                // Create new user
                user = createUser(email, name);
                createAuthProvider(user, googleId);
                log.info("Created new user via Google: userId={}", user.getId());
            }
        }

        // Check if suspended
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new UnauthorizedException("Your account has been suspended");
        }

        String sessionId = java.util.UUID.randomUUID().toString();
        return AuthResponse.builder()
                .user(UserResponse.fromEntity(user))
                .accessToken(jwtService.generateAccessToken(user.getId(), user.getRole().name(), sessionId))
                .refreshToken(jwtService.generateRefreshToken(user.getId(), user.getRole().name(), sessionId, userAgent, ip))
                .build();
    }

    private GoogleIdToken.Payload verifyIdToken(String idToken) {
        try {
            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                throw new UnauthorizedException("Invalid Google ID token");
            }
            return googleIdToken.getPayload();
        } catch (Exception e) {
            log.error("Failed to verify Google ID token: {}", e.getMessage());
            throw new UnauthorizedException("Failed to verify Google ID token");
        }
    }

    private User createUser(String email, String name) {
        User user = User.builder()
                .email(email)
                .name(name != null ? name : email.split("@")[0])
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .emailVerified(true) // Google email is verified
                .phoneVerified(false)
                .build();

        return userRepository.save(user);
    }

    private void createAuthProvider(User user, String googleId) {
        UserAuthProvider provider = UserAuthProvider.builder()
                .user(user)
                .provider(AuthProvider.GOOGLE)
                .providerId(googleId)
                .build();

        authProviderRepository.save(provider);
    }
}
