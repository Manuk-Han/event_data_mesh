package module.domain.app.outbox;

import lombok.RequiredArgsConstructor;
import module.domain.db.entity.Outbox;
import module.domain.db.repository.OutboxRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static module.domain.app.config.KafkaTopicConfig.MEMBER_TOPIC;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxRepository repo;
    private final KafkaTemplate<String, String> kafka;

    @Scheduled(fixedDelayString = "PT1S")
    @Transactional
    public void publish() {
        List<Outbox> batch = repo.findBatch(PageRequest.of(0, 100));
        if (batch.isEmpty()) return;

        for (Outbox o : batch) {
            kafka.send(MEMBER_TOPIC, o.getAggregateId(), o.getPayload());
            o.setPublished(true);
        }
        repo.saveAll(batch);
    }
}
