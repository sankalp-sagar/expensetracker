package com.sankalp.expensetracker.notification.service;

import com.sankalp.expensetracker.common.events.Events;
import com.sankalp.expensetracker.common.events.KafkaTopics;
import com.sankalp.expensetracker.notification.entity.Notification;
import com.sankalp.expensetracker.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationRepository notifRepo;
    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from}")
    private String mailFrom;

    @KafkaListener(topics = KafkaTopics.GROUP_CREATED, groupId = "notification-service")
    @Transactional
    public void onGroupCreated(Events.GroupCreatedEvent e) {
        log.info("Notifying {} members about new group {}", e.memberIds().size(), e.groupId());
        for (UUID member : e.memberIds()) {
            saveAndEmail(member, Notification.NotificationType.GROUP_CREATED,
                    "Added to group: " + e.groupName(),
                    "You have been added to the group \"" + e.groupName() + "\"",
                    e.groupId(), null);
        }
    }

    @KafkaListener(topics = KafkaTopics.EXPENSE_CREATED, groupId = "notification-service")
    @Transactional
    public void onExpenseCreated(Events.ExpenseCreatedEvent e) {
        for (var s : e.splits()) {
            if (s.userId().equals(e.payerId())) continue;
            saveAndEmail(s.userId(), Notification.NotificationType.EXPENSE_CREATED,
                    "New expense: " + e.description(),
                    "You owe " + s.share() + " " + e.currency() + " for \"" + e.description() + "\"",
                    e.expenseId(), null);
        }
    }

    @KafkaListener(topics = KafkaTopics.SETTLEMENT_COMPLETED, groupId = "notification-service")
    @Transactional
    public void onSettlementCompleted(Events.SettlementCompletedEvent e) {
        saveAndEmail(e.payeeId(), Notification.NotificationType.SETTLEMENT_COMPLETED,
                "Payment received",
                "You received " + e.amount() + " " + e.currency(),
                e.settlementId(), null);
        saveAndEmail(e.payerId(), Notification.NotificationType.SETTLEMENT_COMPLETED,
                "Payment recorded",
                "You paid " + e.amount() + " " + e.currency(),
                e.settlementId(), null);
    }

    @KafkaListener(topics = KafkaTopics.USER_INVITED, groupId = "notification-service")
    @Transactional
    public void onUserInvited(Events.UserInvitedEvent e) {
        // For unregistered invitees we only send email; for existing users we'd resolve via user-service.
        if (mailEnabled) sendEmail(e.inviteeEmail(), "You're invited to " + e.groupName(),
                "You have been invited to join the expense group \"" + e.groupName() + "\".");
    }

    private void saveAndEmail(UUID userId, Notification.NotificationType type,
                              String title, String body, UUID relatedId, String email) {
        Notification n = Notification.builder()
                .userId(userId).type(type).title(title).body(body).relatedId(relatedId).build();
        notifRepo.save(n);
        if (mailEnabled && email != null && !email.isBlank()) sendEmail(email, title, body);
    }

    private void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(mailFrom);
            m.setTo(to);
            m.setSubject(subject);
            m.setText(text);
            mailSender.send(m);
        } catch (Exception ex) {
            log.warn("Email send skipped: {}", ex.getMessage());
        }
    }

    /** API helper: list paginated. */
    public List<Notification> recent(UUID userId) {
        return notifRepo.findByUserIdOrderByCreatedAtDesc(userId,
                org.springframework.data.domain.PageRequest.of(0, 50)).getContent();
    }

    @Transactional
    public void markRead(UUID userId, UUID notificationId) {
        notifRepo.findById(notificationId).ifPresent(n -> {
            if (n.getUserId().equals(userId)) {
                n.setRead(true);
                notifRepo.save(n);
            }
        });
    }

    public long unreadCount(UUID userId) {
        return notifRepo.countByUserIdAndReadFalse(userId);
    }
}
