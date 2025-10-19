package module.domain.app.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import module.domain.app.config.KafkaTopicConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberEventListener {

    private final ObjectMapper om;
    private final MeterRegistry meter;

    private Counter consumeSuccess;
    private Counter consumeFailure;
    private Counter consumeDLT;

    @PostConstruct
    void initCounters() {
        consumeSuccess = Counter.builder("member_consume_success_total").register(meter);
        consumeFailure = Counter.builder("member_consume_failure_total").register(meter);
        consumeDLT     = Counter.builder("member_consume_dlt_total").register(meter);
    }

    @KafkaListener(topics = KafkaTopicConfig.MEMBER_TOPIC) // containerFactory 기본 사용
    public void onMessage(ConsumerRecord<String, String> rec) throws Exception {
        try {
            JsonNode node = om.readTree(rec.value());
            // 실패를 유도해서 DLT로 가는지 확인 (name == "DLT" 이면 실패)
            if (node.hasNonNull("name") && "DLT".equals(node.get("name").asText())) {
                throw new RuntimeException("force DLT for test");
            }
            // 정상 처리
            log.info("consume ok key={} value={}", rec.key(), rec.value());
            consumeSuccess.increment();
        } catch (Exception e) {
            consumeFailure.increment(); // 실패 카운트 (재시도 포함)
            throw e;                    // 반드시 예외를 던져야 에러핸들러/재시도/DLT가 동작
        }
    }

    // DLT 토픽 별도 구독해서 DLT 카운터 올림
    @KafkaListener(topics = KafkaTopicConfig.MEMBER_TOPIC + ".DLT",
            groupId = "member-consumer-dlt")
    public void onDlt(String value) {
        log.warn("DLT received: {}", value);
        consumeDLT.increment();
    }
}
