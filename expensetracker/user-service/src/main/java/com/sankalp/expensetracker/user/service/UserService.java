package com.sankalp.expensetracker.user.service;

import com.sankalp.expensetracker.common.events.Events;
import com.sankalp.expensetracker.common.events.KafkaTopics;
import com.sankalp.expensetracker.common.exception.BusinessException;
import com.sankalp.expensetracker.common.exception.NotFoundException;
import com.sankalp.expensetracker.user.dto.UpdateProfileRequest;
import com.sankalp.expensetracker.user.dto.UserProfileResponse;
import com.sankalp.expensetracker.user.dto.UserSummaryResponse;
import com.sankalp.expensetracker.user.entity.Friendship;
import com.sankalp.expensetracker.user.entity.UserProfile;
import com.sankalp.expensetracker.user.repository.FriendshipRepository;
import com.sankalp.expensetracker.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserProfileRepository profileRepo;
    private final FriendshipRepository friendshipRepo;

    @KafkaListener(topics = KafkaTopics.USER_REGISTERED, groupId = "user-service")
    @Transactional
    public void onUserRegistered(Events.UserRegisteredEvent event) {
        log.info("Provisioning profile for user {}", event.userId());
        profileRepo.findByUserId(event.userId())
                .ifPresentOrElse(
                        profile -> syncSignupFields(profile, event.email(), event.fullName()),
                        () -> profileRepo.save(newProfile(event.userId(), event.email(), event.fullName()))
                );
    }

    @Cacheable(value = "userProfiles", key = "#userId")
    @Transactional
    public UserProfileResponse getProfile(UUID userId, String email, String fullName) {
        return UserProfileResponse.from(findOrCreateProfile(userId, email, fullName));
    }

    public List<UserSummaryResponse> lookupByUserIds(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) return List.of();
        LinkedHashSet<UUID> uniqueIds = new LinkedHashSet<>(userIds);
        return profileRepo.findByUserIdIn(uniqueIds).stream()
                .map(UserSummaryResponse::from)
                .toList();
    }

    @Transactional
    @CacheEvict(value = "userProfiles", key = "#userId")
    public UserProfileResponse updateProfile(UUID userId, String email, String fullName, UpdateProfileRequest req) {
        UserProfile p = findOrCreateProfile(userId, email, fullName);
        if (req.avatarUrl() != null) p.setAvatarUrl(req.avatarUrl());
        if (req.statusMessage() != null) p.setStatusMessage(req.statusMessage());
        if (req.phone() != null) p.setPhone(req.phone());
        if (req.preferredCurrency() != null) p.setPreferredCurrency(req.preferredCurrency());
        if (req.preferredLanguage() != null) p.setPreferredLanguage(req.preferredLanguage());
        if (req.privacy() != null) p.setPrivacy(UserProfile.PrivacyLevel.valueOf(req.privacy()));
        return UserProfileResponse.from(profileRepo.save(p));
    }

    public Page<UserProfileResponse> search(String q, Pageable pageable) {
        return profileRepo.searchVisible(q == null ? "" : q, pageable)
                .map(UserProfileResponse::from);
    }

    public UserProfileResponse getById(UUID profileId) {
        return profileRepo.findById(profileId).map(UserProfileResponse::from)
                .orElseThrow(() -> new NotFoundException("Profile not found"));
    }

    @Transactional
    public Friendship sendFriendRequest(UUID requesterId, UUID addresseeId) {
        if (requesterId.equals(addresseeId)) throw new BusinessException("Cannot friend yourself");
        if (profileRepo.findByUserId(addresseeId).isEmpty()) {
            throw new NotFoundException("Addressee profile not found");
        }
        List<Friendship> existingLinks = friendshipRepo.findBetweenUsers(requesterId, addresseeId);
        return existingLinks.stream().findFirst()
                .map(existing -> {
                    if (existing.getStatus() == Friendship.Status.BLOCKED) {
                        throw new BusinessException("Friend request is blocked");
                    }
                    if (existing.getStatus() == Friendship.Status.REJECTED) {
                        existing.setRequesterId(requesterId);
                        existing.setAddresseeId(addresseeId);
                        existing.setStatus(Friendship.Status.PENDING);
                        return friendshipRepo.save(existing);
                    }
                    if (existing.getStatus() == Friendship.Status.PENDING
                            && existing.getRequesterId().equals(addresseeId)
                            && existing.getAddresseeId().equals(requesterId)) {
                        existing.setStatus(Friendship.Status.ACCEPTED);
                        return friendshipRepo.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> friendshipRepo.save(Friendship.builder()
                        .requesterId(requesterId)
                        .addresseeId(addresseeId)
                        .status(Friendship.Status.PENDING)
                        .build()));
    }

    @Transactional
    public Friendship respondToFriendRequest(UUID friendshipId, UUID userId, boolean accept) {
        Friendship f = friendshipRepo.findById(friendshipId)
                .orElseThrow(() -> new NotFoundException("Friend request not found"));
        if (!f.getAddresseeId().equals(userId))
            throw new BusinessException("Only the addressee can respond to this request");
        f.setStatus(accept ? Friendship.Status.ACCEPTED : Friendship.Status.REJECTED);
        return friendshipRepo.save(f);
    }

    public List<Friendship> listFriends(UUID userId) {
        return friendshipRepo.findAcceptedFriendsOf(userId);
    }

    public List<Friendship> listPendingRequests(UUID userId) {
        return friendshipRepo.findPendingFor(userId);
    }

    private UserProfile findOrCreateProfile(UUID userId, String email, String fullName) {
        return profileRepo.findByUserId(userId)
                .map(profile -> syncSignupFields(profile, email, fullName))
                .orElseGet(() -> profileRepo.save(newProfile(userId, email, fullName)));
    }

    private UserProfile newProfile(UUID userId, String email, String fullName) {
        String normalizedEmail = normalizeRequiredEmail(email);
        return UserProfile.builder()
                .userId(userId)
                .email(normalizedEmail)
                .fullName(normalizeName(fullName, normalizedEmail))
                .build();
    }

    private UserProfile syncSignupFields(UserProfile profile, String email, String fullName) {
        boolean changed = false;
        String normalizedEmail = normalizeOptional(email);
        if (!hasText(profile.getEmail()) && hasText(normalizedEmail)) {
            profile.setEmail(normalizedEmail.toLowerCase());
            changed = true;
        }
        if (!hasText(profile.getFullName())) {
            String normalizedName = normalizeName(fullName, profile.getEmail());
            if (hasText(normalizedName)) {
                profile.setFullName(normalizedName);
                changed = true;
            }
        }
        return changed ? profileRepo.save(profile) : profile;
    }

    private String normalizeRequiredEmail(String email) {
        String normalized = normalizeOptional(email);
        if (!hasText(normalized)) {
            throw new NotFoundException("Profile not found");
        }
        return normalized.toLowerCase();
    }

    private String normalizeName(String fullName, String fallbackEmail) {
        String normalized = normalizeOptional(fullName);
        return hasText(normalized) ? normalized : normalizeOptional(fallbackEmail);
    }

    private String normalizeOptional(String value) {
        return value == null ? null : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
