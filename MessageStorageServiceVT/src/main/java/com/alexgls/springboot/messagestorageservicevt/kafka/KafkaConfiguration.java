package com.alexgls.springboot.messagestorageservicevt.kafka;

import com.alexgls.springboot.messagestorageservicevt.dto.messages.*;
import com.alexgls.springboot.messagestorageservicevt.dto.notifications.CreateNotificationRequest;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
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
public class KafkaConfiguration {

    private Map<String, Object> initializeProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "messaging-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    @Bean
    public ProducerFactory<String, MessageDto> messageProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        JacksonJsonSerializer<MessageDto> jsonSerializer = new JacksonJsonSerializer<>();
        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), jsonSerializer);
    }

    @Bean
    public ProducerFactory<String, ReadMessagePayload> readMessagePayloadProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        JacksonJsonSerializer<ReadMessagePayload> jsonSerializer = new JacksonJsonSerializer<>();
        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), jsonSerializer);
    }

    @Bean
    public ProducerFactory<String, DeleteMessageResponse> deleteMessageProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        JacksonJsonSerializer<DeleteMessageResponse> jsonSerializer = new JacksonJsonSerializer<>();
        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), jsonSerializer);
    }

    @Bean
    public ProducerFactory<String, CreateNotificationRequest> createNotificationRequestProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        JacksonJsonSerializer<CreateNotificationRequest> jsonSerializer = new JacksonJsonSerializer<>();
        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), jsonSerializer);
    }

    @Bean
    public ProducerFactory<String, MessageDto> updateMessageProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        JacksonJsonSerializer<MessageDto> jsonSerializer = new JacksonJsonSerializer<>();
        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), jsonSerializer);
    }

    @Bean
    public KafkaTemplate<String, MessageDto> updateMessageKafkaTemplate() {
        return new KafkaTemplate<>(updateMessageProducerFactory());
    }

    @Bean
    public KafkaTemplate<String, CreateNotificationRequest> createNotificationRequestKafkaTemplate() {
        return new KafkaTemplate<>(createNotificationRequestProducerFactory());
    }

    @Bean
    public KafkaTemplate<String, MessageDto> kafkaTemplate() {
        return new KafkaTemplate<>(messageProducerFactory());
    }

    @Bean
    public KafkaTemplate<String, ReadMessagePayload> readMessagePayloadKafkaTemplate() {
        return new KafkaTemplate<>(readMessagePayloadProducerFactory());
    }

    @Bean
    public KafkaTemplate<String, DeleteMessageResponse> deleteMessageKafkaTemplate() {
        return new KafkaTemplate<>(deleteMessageProducerFactory());
    }


    @Bean
    public NewTopic eventsTopic() {
        return TopicBuilder
                .name("events-message-created")
                .build();
    }

    @Bean
    public NewTopic updateMessageTopic() {
        return TopicBuilder
                .name("update-message-topic")
                .build();
    }

    @Bean
    public NewTopic readMessagesTopic() {
        return TopicBuilder
                .name("read-message-topic")
                .build();
    }

    @Bean
    public NewTopic deleteMessagesTopic() {
        return TopicBuilder
                .name("delete-message-topic")
                .build();
    }

}
