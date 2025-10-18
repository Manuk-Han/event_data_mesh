package module.domain.app.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {
    public static final String MEMBER_TOPIC = "domain.sample.member.events";
    @Bean
    NewTopic memberEventsTopic() {
        return new NewTopic(MEMBER_TOPIC, 3, (short) 1);
    }
}
