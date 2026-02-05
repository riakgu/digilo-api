package com.riakgu.digilo.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    Optional<User> findByEmail(String email);

    Optional<User> findById(Long id);

    Page<User> findAll(Pageable pageable);

    long countByStatus(UserStatus status);

    @Query("SELECT u FROM User u WHERE " +
            "(:search IS NULL OR :search = '' OR LOWER(u.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) " +
            "AND (:role IS NULL OR u.role = :role) " +
            "AND (:status IS NULL OR u.status = :status)")
    Page<User> findAllWithFilters(
            @Param("search") String search,
            @Param("role") Role role,
            @Param("status") UserStatus status,
            Pageable pageable
    );
}

