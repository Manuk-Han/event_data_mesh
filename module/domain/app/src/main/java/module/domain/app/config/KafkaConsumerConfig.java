package module.domain.app.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Bean
    public DeadLetterPublishingRecoverer dltRecoverer(KafkaTemplate<String, String> template,
                                                      MeterRegistry meter) {
        Counter dlt = Counter.builder("member_consume_dlt_total").register(meter);
        // DLT로 보낼 때 카운터 증가
        return new DeadLetterPublishingRecoverer(
                template,
                (record, ex) -> {
                    dlt.increment();
                    return new TopicPartition(record.topic() + ".DLT", record.partition());
                });
    }

    @Bean
    public DefaultErrorHandler defaultErrorHandler(DeadLetterPublishingRecoverer recoverer) {
        // 1초 간격 3회 재시도 후 DLT
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String,String> cf,
            KafkaTemplate<String,String> dltTemplate) {

        var factory = new ConcurrentKafkaListenerContainerFactory<String,String>();
        factory.setConsumerFactory(cf);

        var recoverer = new DeadLetterPublishingRecoverer(dltTemplate,
                (rec, ex) -> new TopicPartition(rec.topic() + ".DLT", rec.partition()));
        var eh = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
        eh.setCommitRecovered(true);
        eh.addNotRetryableExceptions(IllegalArgumentException.class);

        factory.setCommonErrorHandler(eh);
        factory.setConcurrency(2);
        return factory;
    }

}
