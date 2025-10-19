package module.domain.app.outbox;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import module.domain.db.entity.EventJournal;
import module.domain.db.entity.Outbox;
import module.domain.db.repository.EventJournalRepository;
import module.domain.db.repository.OutboxRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static module.domain.app.config.KafkaTopicConfig.MEMBER_TOPIC;

@Slf4j
@Component
public class OutboxPublisher {

    private final OutboxRepository repo;
    private final KafkaTemplate<String, String> kafka;
    private final EventJournalRepository journalRepo;

    // Micrometer counters
    private final Counter publishSuccess;
    private final Counter publishFailure;

    // MeterRegistry 주입 + 카운터 등록용 생성자
    public OutboxPublisher(OutboxRepository repo,
                           KafkaTemplate<String, String> kafka,
                           EventJournalRepository journalRepo,
                           MeterRegistry meter) {
        this.repo = repo;
        this.kafka = kafka;
        this.journalRepo = journalRepo;
        this.publishSuccess = Counter.builder("outbox_publish_success_total").register(meter);
        this.publishFailure = Counter.builder("outbox_publish_failure_total").register(meter);
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.delay:PT1S}")
    @Transactional
    public void publish() {
        List<Outbox> batch = repo.findBatch(PageRequest.of(0, 100));
        if (batch.isEmpty()) return;

        List<Outbox> toSave = new ArrayList<>(batch.size());

        for (Outbox o : batch) {
            try {
                // 상관아이디 생성 + 헤더 준비
                String correlationId = UUID.randomUUID().toString();

                ProducerRecord<String, String> rec =
                        new ProducerRecord<>(MEMBER_TOPIC, o.getAggregateId(), o.getPayload());
                rec.headers()
                        .add("correlationId", correlationId.getBytes())
                        .add("schemaVersion", "v1".getBytes())
                        .add("eventType", o.getEventType().getBytes());

                // 동기 전송(성공/실패 명확화)
                var sendResult = kafka.send(rec).get(3, TimeUnit.SECONDS);

                // 성공 처리: published=true, 저널 적재
                o.setPublished(true);
                toSave.add(o);

                var meta = sendResult.getRecordMetadata();
                journalRepo.save(
                        EventJournal.builder()
                                .eventId(o.getId())
                                .correlationId(correlationId)
                                .schemaVersion("v1")
                                .topic(MEMBER_TOPIC)
                                .partitionNo(meta.partition())
                                .offsetNo(meta.offset())
                                .recordedAt(OffsetDateTime.now())
                                .build()
                );

                publishSuccess.increment();
            } catch (Exception ex) {
                // 실패: 다음 주기에 재시도(published=false 유지)
                log.warn("Outbox publish failed id={}, reason={}", o.getId(), ex.toString());
                publishFailure.increment();
            }
        }

        if (!toSave.isEmpty()) {
            repo.saveAll(toSave);
        }
    }
}
