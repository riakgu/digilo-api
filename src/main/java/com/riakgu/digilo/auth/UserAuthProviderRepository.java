package com.riakgu.digilo.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAuthProviderRepository extends JpaRepository<UserAuthProvider, Long> {

    Optional<UserAuthProvider> findByProviderAndProviderId(AuthProvider provider, String providerId);

    boolean existsByProviderAndProviderId(AuthProvider provider, String providerId);
}
