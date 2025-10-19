package module.domain.app.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import module.domain.db.repository.ConsumerCheckpointRepository;
import module.domain.db.repository.MemberProductRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberProjector {
    private final MemberProductRepository memberProductRepository;
    private final ConsumerCheckpointRepository consumerCheckpointRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "member-events", groupId = "member-projector")
    public void onMessage(String payload,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                          @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                          @Header(KafkaHeaders.OFFSET) long offset) throws Exception {

        if (consumerCheckpointRepository.exists(topic, partition, offset)) return;

        var p = objectMapper.readTree(payload);
        if ("MemberRegistered".equals(p.get("type").asText())) {
            var memberId = p.get("memberId").asText();
            var email    = p.get("email").asText();
            var name     = p.get("name").asText();
            memberProductRepository.upsert(memberId, email, name);
        }
        consumerCheckpointRepository.save(topic, partition, offset); // 마지막에 기록
    }
}
