package module.domain.app.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import module.domain.db.repository.OutboxRepository;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MetricsConfig {
    private final MeterRegistry meterRegistry;
    private final OutboxRepository outboxRepository;

    @PostConstruct
    public MetricsConfig init() {
        Gauge.builder("outbox_backlog", outboxRepository, repo -> repo.countByPublishedFalse())
                .description("Count of unpublished outbox records")
                .register(meterRegistry);
        return this;
    }
}
