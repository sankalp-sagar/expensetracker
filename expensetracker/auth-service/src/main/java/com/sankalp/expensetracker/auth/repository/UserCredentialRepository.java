package com.sankalp.expensetracker.auth.repository;

import com.sankalp.expensetracker.auth.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserCredentialRepository extends JpaRepository<UserCredential, UUID> {
    Optional<UserCredential> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
