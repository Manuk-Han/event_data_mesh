package module.domain.app.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.util.backoff.FixedBackOff;

import static module.domain.app.config.KafkaTopicConfig.MEMBER_TOPIC;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String,String> kafkaListenerContainerFactory(
            ConsumerFactory<String,String> cf,
            KafkaTemplate<String,String> template) {

        var recoverer = new DeadLetterPublishingRecoverer(template, (cr, ex) -> {
            // <원본토픽>.DLT 로 라우팅
            return new org.apache.kafka.common.TopicPartition(cr.topic() + ".DLT", cr.partition());
        });
        var errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L)); // 1초 간격 3회 재시도

        var f = new ConcurrentKafkaListenerContainerFactory<String,String>();
        f.setConsumerFactory(cf);
        f.setCommonErrorHandler(errorHandler);
        f.setConcurrency(2);
        return f;
    }
}
