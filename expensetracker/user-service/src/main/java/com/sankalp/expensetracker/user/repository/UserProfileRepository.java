package com.sankalp.expensetracker.user.repository;

import com.sankalp.expensetracker.user.entity.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    Optional<UserProfile> findByUserId(UUID userId);
    Optional<UserProfile> findByEmailIgnoreCase(String email);

    @Query("""
            select p from UserProfile p
            where (lower(p.fullName) like lower(concat('%', :q, '%'))
                or lower(p.email) like lower(concat('%', :q, '%')))
              and p.privacy <> com.sankalp.expensetracker.user.entity.UserProfile$PrivacyLevel.PRIVATE
            """)
    Page<UserProfile> searchVisible(@Param("q") String q, Pageable pageable);
}
