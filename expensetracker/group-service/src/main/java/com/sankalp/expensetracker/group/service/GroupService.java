package com.sankalp.expensetracker.group.service;

import com.sankalp.expensetracker.common.events.Events;
import com.sankalp.expensetracker.common.events.KafkaTopics;
import com.sankalp.expensetracker.common.exception.BusinessException;
import com.sankalp.expensetracker.common.exception.NotFoundException;
import com.sankalp.expensetracker.group.dto.CreateGroupRequest;
import com.sankalp.expensetracker.group.dto.GroupResponse;
import com.sankalp.expensetracker.group.entity.ExpenseGroup;
import com.sankalp.expensetracker.group.entity.GroupMember;
import com.sankalp.expensetracker.group.repository.GroupMemberRepository;
import com.sankalp.expensetracker.group.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GroupService {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RAND = new SecureRandom();

    private final GroupRepository groupRepo;
    private final GroupMemberRepository memberRepo;
    private final KafkaTemplate<String, Object> kafka;

    @Transactional
    public GroupResponse createGroup(UUID ownerId, CreateGroupRequest req) {
        ExpenseGroup g = ExpenseGroup.builder()
                .name(req.name())
                .description(req.description())
                .avatarUrl(req.avatarUrl())
                .ownerId(ownerId)
                .defaultCurrency(req.defaultCurrency() == null ? "USD" : req.defaultCurrency())
                .inviteCode(generateInviteCode())
                .build();
        // owner becomes ADMIN
        GroupMember owner = GroupMember.builder()
                .group(g).userId(ownerId).role(GroupMember.MemberRole.ADMIN).build();
        g.getMembers().add(owner);
        // initial members (MEMBER role)
        if (req.initialMemberIds() != null) {
            for (UUID memberId : new LinkedHashSet<>(req.initialMemberIds())) {
                if (memberId.equals(ownerId)) continue;
                g.getMembers().add(GroupMember.builder()
                        .group(g).userId(memberId).role(GroupMember.MemberRole.MEMBER).build());
            }
        }
        groupRepo.save(g);

        List<UUID> memberIds = g.getMembers().stream().map(GroupMember::getUserId).toList();
        kafka.send(KafkaTopics.GROUP_CREATED,
                new Events.GroupCreatedEvent(g.getId(), g.getName(), ownerId, memberIds, Instant.now()));

        return toResponse(g);
    }

    @Transactional
    public GroupResponse get(UUID groupId, UUID requesterId) {
        ExpenseGroup g = groupRepo.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));
        if (!memberRepo.existsByGroupIdAndUserId(groupId, requesterId))
            throw new BusinessException("Not a member of this group");
        return toResponse(g);
    }


    @Transactional(readOnly = true)
    public List<GroupResponse> listMyGroups(UUID userId) {
        return groupRepo.findGroupsForUser(userId).stream().map(this::toResponse).toList();
    }


    @Transactional
    public GroupResponse addMember(UUID groupId, UUID requesterId, UUID newMemberId) {
        GroupMember requester = memberRepo.findByGroupIdAndUserId(groupId, requesterId)
                .orElseThrow(() -> new BusinessException("Not a member"));
        if (requester.getRole() != GroupMember.MemberRole.ADMIN)
            throw new BusinessException("Only admins can add members");
        ExpenseGroup g = groupRepo.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));
        if (memberRepo.existsByGroupIdAndUserId(groupId, newMemberId)) return toResponse(g);
        memberRepo.save(GroupMember.builder()
                .group(g).userId(newMemberId).role(GroupMember.MemberRole.MEMBER).build());
        return toResponse(groupRepo.findById(groupId).orElseThrow());
    }

    @Transactional
    public GroupResponse joinByInviteCode(UUID userId, String code) {
        ExpenseGroup g = groupRepo.findByInviteCode(code)
                .orElseThrow(() -> new NotFoundException("Invite code invalid"));
        if (!memberRepo.existsByGroupIdAndUserId(g.getId(), userId)) {
            memberRepo.save(GroupMember.builder()
                    .group(g).userId(userId).role(GroupMember.MemberRole.MEMBER).build());
        }
        return toResponse(groupRepo.findById(g.getId()).orElseThrow());
    }

    @Transactional
    public void removeMember(UUID groupId, UUID requesterId, UUID memberId) {
        GroupMember requester = memberRepo.findByGroupIdAndUserId(groupId, requesterId)
                .orElseThrow(() -> new BusinessException("Not a member"));
        boolean isSelf = requesterId.equals(memberId);
        if (!isSelf && requester.getRole() != GroupMember.MemberRole.ADMIN)
            throw new BusinessException("Only admins can remove others");
        GroupMember m = memberRepo.findByGroupIdAndUserId(groupId, memberId)
                .orElseThrow(() -> new NotFoundException("Member not found"));
        ExpenseGroup g = m.getGroup();
        if (g.getOwnerId().equals(memberId)) {
            throw new BusinessException("Group owner cannot be removed");
        }
        memberRepo.delete(m);
    }

    private GroupResponse toResponse(ExpenseGroup g) {
        List<GroupResponse.MemberInfo> infos = g.getMembers().stream()
                .map(m -> new GroupResponse.MemberInfo(m.getUserId(), m.getRole().name()))
                .toList();
        return GroupResponse.from(g, infos);
    }

    private String generateInviteCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 8; i++) sb.append(CODE_CHARS.charAt(RAND.nextInt(CODE_CHARS.length())));
            String code = sb.toString();
            if (!groupRepo.existsByInviteCode(code)) {
                return code;
            }
        }
        throw new BusinessException("Could not generate a unique invite code");
    }
}
