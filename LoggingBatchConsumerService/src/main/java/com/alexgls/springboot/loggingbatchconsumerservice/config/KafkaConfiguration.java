package com.alexgls.springboot.loggingbatchconsumerservice.config;

import com.alexgls.springboot.loggingbatchconsumerservice.dto.LogRecord;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfiguration {


    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public NewTopic loggingBatchConsumerTopic() {
        return TopicBuilder
                .name("logging-batch-consumer-topic")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public ConsumerFactory<String, LogRecord>loggingBatchConsumerFactory() {
        Map<String, Object> params = new HashMap<>();
        params.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        params.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        params.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        params.put(ConsumerConfig.GROUP_ID_CONFIG, "logging-batch-consumer-group");
        params.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        params.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        params.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);

        params.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 50 * 1024);
        params.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 5000);
        params.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, 5242880); // 5 МБ


        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule()); // Вот это лечит вашу ошибку
        om.findAndRegisterModules(); // На всякий случай для других модулей

        // Опционально: отключаем ошибку на неизвестных полях (полезно для логов)
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        om.configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false);
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);


        JsonDeserializer<LogRecord> jsonDeserializer = new JsonDeserializer<>(LogRecord.class, om);
        jsonDeserializer.addTrustedPackages("*");
        jsonDeserializer.setUseTypeHeaders(false);

        return new DefaultKafkaConsumerFactory<>(params, new StringDeserializer(), jsonDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, LogRecord> concurrentKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, LogRecord> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(loggingBatchConsumerFactory());
        factory.getContainerProperties().setPollTimeout(5000);
        factory.setConcurrency(3);
        factory.setBatchListener(true);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }
}
