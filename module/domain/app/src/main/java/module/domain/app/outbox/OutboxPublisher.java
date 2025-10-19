package module.domain.app.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import module.domain.app.governance.GovernanceService;
import module.domain.app.governance.QuarantineService;
import module.domain.app.validator.EventGovernance;
import module.domain.db.entity.EventJournal;
import module.domain.db.entity.Outbox;
import module.domain.db.repository.EventJournalRepository;
import module.domain.db.repository.OutboxRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static module.domain.app.config.KafkaTopicConfig.MEMBER_TOPIC;

@Slf4j
@Component
public class OutboxPublisher {

    private final OutboxRepository repo;
    private final KafkaTemplate<String, String> kafka;
    private final EventJournalRepository journalRepo;
    private final GovernanceService governance;
    private final QuarantineService quarantine;

    private final EventGovernance eventGovernance;
    private final ObjectMapper om;

    private final Counter publishSuccess;
    private final Counter publishFailure;
    private final Counter quarantined;

    public OutboxPublisher(OutboxRepository repo,
                           KafkaTemplate<String, String> kafka,
                           EventJournalRepository journalRepo,
                           GovernanceService governance,
                           QuarantineService quarantine,
                           EventGovernance eventGovernance,
                           MeterRegistry meter,
                           ObjectMapper om) {
        this.repo = repo;
        this.kafka = kafka;
        this.journalRepo = journalRepo;
        this.governance = governance;
        this.quarantine = quarantine;
        this.eventGovernance = eventGovernance;
        this.om = om;
        this.publishSuccess = Counter.builder("outbox_publish_success_total").register(meter);
        this.publishFailure = Counter.builder("outbox_publish_failure_total").register(meter);
        this.quarantined    = Counter.builder("outbox_quarantine_total").register(meter);
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.delay:PT1S}")
    @Transactional
    public void publish() {
        List<Outbox> batch = repo.findBatch(PageRequest.of(0, 100));
        if (batch.isEmpty()) return;

        List<Outbox> toSave = new ArrayList<>(batch.size());

        for (Outbox o : batch) {
            try {
                // 0) JSON 파싱
                JsonNode payloadNode = om.readTree(o.getPayload());
                JsonNode headersNode = (o.getHeaders() != null) ? om.readTree(o.getHeaders()) : null;

                final String dataset = (headersNode != null && headersNode.hasNonNull("dataset"))
                        ? headersNode.get("dataset").asText()
                        : "member-events";
                final String schemaVersion = (headersNode != null && headersNode.hasNonNull("schemaVersion"))
                        ? headersNode.get("schemaVersion").asText()
                        : "v1";
                final String correlationId = (headersNode != null && headersNode.hasNonNull("correlationId"))
                        ? headersNode.get("correlationId").asText()
                        : null;

                // 1) 스키마/필수필드 검증 (원본 JSON 기준)
                eventGovernance.validateOrQuarantine(dataset, schemaVersion, o.getPayload()); // <- 오타 수정

                // (추가 방어: 필수 필드 빠짐 → 격리 후 스킵)
                if (governance.violatesRequired(payloadNode, "memberId", "email", "name")) {
                    quarantine.save(dataset, payloadNode.toString(), "REQUIRED_FIELD_MISSING");
                    o.setPublished(true); // 무한 재시도 방지
                    toSave.add(o);
                    quarantined.increment();
                    continue;
                }

                // 2) 정책 적용(마스킹 등) → 전송에 쓰는 최종 페이로드
                JsonNode masked = governance.applyPolicies(dataset, payloadNode);

                // 3) Kafka 전송
                var sendResult = kafka.send(MEMBER_TOPIC, o.getAggregateId(), masked.toString())
                        .get(3, TimeUnit.SECONDS);

                // 4) 성공 처리 & 저널 기록
                o.setPublished(true);
                toSave.add(o);

                var meta = sendResult.getRecordMetadata();
                journalRepo.save(
                        EventJournal.builder()
                                .eventId(o.getId())
                                .correlationId(correlationId)
                                .schemaVersion(schemaVersion)
                                .topic(MEMBER_TOPIC)
                                .partitionNo(meta.partition())
                                .offsetNo(meta.offset())
                                .recordedAt(OffsetDateTime.now())
                                .build()
                );

                publishSuccess.increment();

            } catch (Exception ex) {
                log.warn("Outbox publish failed id={}, reason={}", o.getId(), ex.toString());
                publishFailure.increment();
            }
        }

        if (!toSave.isEmpty()) {
            repo.saveAll(toSave);
        }
    }

}
