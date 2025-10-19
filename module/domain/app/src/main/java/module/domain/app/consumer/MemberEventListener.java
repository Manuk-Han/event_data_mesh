package module.domain.app.consumer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static module.domain.app.config.KafkaTopicConfig.MEMBER_TOPIC;
import static module.domain.app.config.KafkaTopicConfig.MEMBER_DLT;

@Slf4j
@Component
public class MemberEventListener {

    private final Counter consumeSuccess;
    private final Counter consumeFailure;
    private final Counter consumeDlt;

    public MemberEventListener(MeterRegistry meter) {
        this.consumeSuccess = Counter.builder("member_consume_success_total").register(meter);
        this.consumeFailure = Counter.builder("member_consume_failure_total").register(meter);
        this.consumeDlt     = Counter.builder("member_consume_dlt_total").register(meter);
    }

    @KafkaListener(topics = MEMBER_TOPIC, containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(ConsumerRecord<String, String> rec) {
        try {
            log.info("consume key={}, value={}", rec.key(), rec.value());

            consumeSuccess.increment();
        } catch (Exception e) {
            consumeFailure.increment();
            throw e;
        }
    }

    @KafkaListener(topics = MEMBER_DLT, containerFactory = "kafkaListenerContainerFactory")
    public void onDlt(ConsumerRecord<String, String> rec) {
        log.warn("DLT received key={}, value={}, topic={}, partition={}, offset={}",
                rec.key(), rec.value(), rec.topic(), rec.partition(), rec.offset());
        consumeDlt.increment();
    }
}
