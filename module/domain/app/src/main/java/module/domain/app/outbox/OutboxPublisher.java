package module.domain.app.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import module.domain.app.governance.GovernanceService;
import module.domain.app.governance.QuarantineService;
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
    private final ObjectMapper om = new ObjectMapper();

    private final Counter publishSuccess;
    private final Counter publishFailure;
    private final Counter quarantined;

    public OutboxPublisher(OutboxRepository repo,
                           KafkaTemplate<String, String> kafka,
                           EventJournalRepository journalRepo,
                           GovernanceService governance,
                           QuarantineService quarantine,
                           MeterRegistry meter) {
        this.repo = repo;
        this.kafka = kafka;
        this.journalRepo = journalRepo;
        this.governance = governance;
        this.quarantine = quarantine;
        this.publishSuccess = Counter.builder("outbox_publish_success_total").register(meter);
        this.publishFailure = Counter.builder("outbox_publish_failure_total").register(meter);
        this.quarantined    = Counter.builder("outbox_quarantine_total").register(meter);
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.delay:PT1S}")
    @Transactional
    public void publish() {
        var batch = repo.findBatch(PageRequest.of(0, 100));
        if (batch.isEmpty()) return;

        var toSave = new ArrayList<Outbox>(batch.size());

        for (var o : batch) {
            try {
                // 1) JSON 파싱
                var root = om.readTree(o.getPayload());

                // 2) 정책 적용(예: 이메일 마스킹)
                final String DATASET = "member-events";
                root = governance.applyPolicies(DATASET, root);

                // 3) 필수 필드 검증 실패 → 격리
                if (governance.violatesRequired(root, "memberId", "email", "name")) {
                    quarantine.save(DATASET, root.toString(), "REQUIRED_FIELD_MISSING");
                    o.setPublished(true);      // 무한 재시도 방지
                    toSave.add(o);
                    quarantined.increment();
                    continue;
                }

                // 4) Kafka 전송 (마스킹 반영된 페이로드)
                var sendResult = kafka.send(MEMBER_TOPIC, o.getAggregateId(), root.toString())
                        .get(3, TimeUnit.SECONDS);

                // 5) 성공 처리
                o.setPublished(true);
                toSave.add(o);

                var meta = sendResult.getRecordMetadata();
                journalRepo.save(
                        EventJournal.builder()
                                .eventId(o.getId())
                                .correlationId(null)
                                .schemaVersion("v1")
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

        if (!toSave.isEmpty()) repo.saveAll(toSave);
    }
}
