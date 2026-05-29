package com.sankalp.expensetracker.user.entity;

import com.sankalp.expensetracker.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "user_profiles", indexes = {
        @Index(name = "idx_profile_user", columnList = "user_id", unique = true),
        @Index(name = "idx_profile_email", columnList = "email")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class UserProfile extends AuditableEntity {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true, columnDefinition = "uuid")
    private UUID userId;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(name = "full_name", length = 150)
    private String fullName;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "status_message", length = 280)
    private String statusMessage;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "preferred_currency", length = 3)
    @Builder.Default
    private String preferredCurrency = "USD";

    @Column(name = "preferred_language", length = 5)
    @Builder.Default
    private String preferredLanguage = "en";

    @Enumerated(EnumType.STRING)
    @Column(name = "privacy", length = 20, nullable = false)
    @Builder.Default
    private PrivacyLevel privacy = PrivacyLevel.PUBLIC;

    public enum PrivacyLevel { PUBLIC, FRIENDS_ONLY, PRIVATE }
}
