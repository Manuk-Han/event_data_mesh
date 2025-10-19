package module.domain.app.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import module.domain.db.entity.QuarantineEvent;
import module.domain.db.repository.QuarantineEventRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberEventConsumer {

    private final ObjectMapper om = new ObjectMapper();
    private final QuarantineEventRepository quarantineRepo;

    @KafkaListener(
            topics = "domain.sample.member.events",
            groupId = "member-consumer"
    )
    @Transactional
    public void onMessage(
            ConsumerRecord<String, String> rec,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(name = "correlationId", required = false) String cid,
            @Header(name = "schemaVersion", required = false) String sv,
            @Header(name = "eventType", required = false) String et
    ) {
        String payload = rec.value();
        try {
            JsonNode root = om.readTree(payload);
            // 최소 검증: 필수 필드 존재
            List<String> required = List.of("memberId", "email", "name");
            for (String f : required) {
                if (!root.hasNonNull(f)) {
                    String reason = "missing field: " + f;
                    quarantine(topic, payload, reason);
                    log.warn("QUARANTINE reason={}, key={}, offset={}", reason, rec.key(), rec.offset());
                    return; // 격리하고 소비 끝
                }
            }
            // 통과
            log.info("CONSUME OK topic={}, key={}, offset={}, payload={}",
                    topic, rec.key(), rec.offset(), payload);

        } catch (Exception ex) {
            String reason = "invalid json: " + ex.getClass().getSimpleName();
            quarantine(topic, payload, reason);
            log.warn("QUARANTINE reason={}, key={}, offset={}, err={}", reason, rec.key(), rec.offset(), ex.toString());
        }
    }

    private void quarantine(String dataset, String payload, String reason) {
        quarantineRepo.save(
                QuarantineEvent.builder()
                        .datasetName(dataset)
                        .payload(payload)
                        .reason(reason)
                        .createdAt(OffsetDateTime.now())
                        .build()
        );
    }
}
