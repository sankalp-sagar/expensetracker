package com.sankalp.expensetracker.auth.config;

import com.sankalp.expensetracker.common.events.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {
    @Bean public NewTopic userRegistered() { return new NewTopic(KafkaTopics.USER_REGISTERED, 3, (short) 1); }
}
