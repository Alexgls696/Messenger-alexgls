package com.alexgls.springboot.notificationsservice.config;

import com.alexgls.springboot.notificationsservice.dto.CreateNotificationRequest;
import com.alexgls.springboot.notificationsservice.dto.NotificationDto;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic notificationsTopic() {
        return TopicBuilder
                .name("notifications-topic")
                .build();
    }

    @Bean
    public NewTopic createNotificationsTopic() {
        return TopicBuilder
                .name("create-notifications-topic")
                .build();
    }

    @Bean
    public ProducerFactory<String, NotificationDto> notificationsProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        JacksonJsonSerializer<NotificationDto>jsonSerializer = new JacksonJsonSerializer<>();
        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), jsonSerializer);
    }

    @Bean
    public KafkaTemplate<String, NotificationDto> notificationsKafkaTemplate() {
        return new KafkaTemplate<>(notificationsProducerFactory());
    }

    @Bean
    public ConsumerFactory<String, CreateNotificationRequest> notificationsConsumerFactory() {
        Map<String, Object>props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notifications-consumer");
        JacksonJsonDeserializer<CreateNotificationRequest> jsonDeserializer = new JacksonJsonDeserializer<>(CreateNotificationRequest.class);
        jsonDeserializer.addTrustedPackages("/**");
        jsonDeserializer.setRemoveTypeHeaders(false);
        jsonDeserializer.setUseTypeMapperForKey(true);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), jsonDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CreateNotificationRequest> notificationsKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, CreateNotificationRequest> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(notificationsConsumerFactory());
        return factory;
    }


}
