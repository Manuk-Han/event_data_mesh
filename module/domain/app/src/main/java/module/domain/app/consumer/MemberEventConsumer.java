package module.domain.app.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static module.domain.app.config.KafkaTopicConfig.MEMBER_TOPIC;

@Slf4j
@Component
public class MemberEventConsumer {

    private final ObjectMapper om;
    private final Counter consumeSuccess;
    private final Counter consumeFailure;

    public MemberEventConsumer(MeterRegistry meter, ObjectMapper om) {
        this.om = om;
        this.consumeSuccess = Counter.builder("member_consume_success_total").register(meter);
        this.consumeFailure = Counter.builder("member_consume_failure_total").register(meter);
    }

    @KafkaListener(topics = MEMBER_TOPIC, groupId = "domain-sample")
    public void onMessage(String payload) throws JsonProcessingException {
        try {
            var root = om.readTree(payload);
            if ("DLT".equalsIgnoreCase(root.path("name").asText())) {
                throw new IllegalStateException("forced failure for DLT demo");
            }

            // TODO 실제 처리 로직
            consumeSuccess.increment();

        } catch (Exception e) {
            consumeFailure.increment();
            throw e;
        }
    }

}
