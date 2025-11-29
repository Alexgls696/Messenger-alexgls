package com.alexgls.springboot.contentanalysisservice.config;


import com.alexgls.springboot.contentanalysisservice.dto.ElasticSearchStorageServiceRequest;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic metadataTopic() {
        return TopicBuilder
                .name("metadata-topic")
                .build();
    }

    @Bean
    public KafkaTemplate<String, ElasticSearchStorageServiceRequest> metadataKafkaTemplate() {
        return new KafkaTemplate<>(metadataProducerFactory());
    }

    @Bean
    public ProducerFactory<String, ElasticSearchStorageServiceRequest> metadataProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        JsonSerializer<ElasticSearchStorageServiceRequest> jsonSerializer = new JsonSerializer<>();
        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), jsonSerializer);
    }
}
