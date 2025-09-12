package ru.alexgls.springboot.usersmessagingservice.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import ru.alexgls.springboot.usersmessagingservice.dto.CreateMessagePayload;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonSerializer;
import ru.alexgls.springboot.usersmessagingservice.dto.DeleteMessageResponse;
import ru.alexgls.springboot.usersmessagingservice.dto.MessageDto;
import ru.alexgls.springboot.usersmessagingservice.dto.ReadMessagePayload;


import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic chatTopic() {
        return TopicBuilder
                .name("chat-topic")
                .build();
    }

    @Bean
    public NewTopic messagesStorageTopic() {
        return TopicBuilder
                .name("messaging-topic")
                .build();
    }

    @Bean
    public ProducerFactory<String, CreateMessagePayload> producerFactory() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        JsonSerializer<CreateMessagePayload> jsonSerializer = new JsonSerializer<>();
        return new DefaultKafkaProducerFactory<>(properties, new StringSerializer(), jsonSerializer);
    }

    @Bean
    public KafkaTemplate<String, CreateMessagePayload> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, MessageDto> messageConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        JsonDeserializer<MessageDto> jsonDeserializer = new JsonDeserializer<>(MessageDto.class);
        jsonDeserializer.addTrustedPackages("/**");
        jsonDeserializer.setUseTypeHeaders(false);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), jsonDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MessageDto> kafkaMessageListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, MessageDto> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(messageConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, ReadMessagePayload> readMessagesConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "message-read-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        JsonDeserializer<ReadMessagePayload> deserializer = new JsonDeserializer<>(ReadMessagePayload.class);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("/**");
        deserializer.setUseTypeMapperForKey(true);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ReadMessagePayload> kafkaReadMessagesConsumerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ReadMessagePayload> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(readMessagesConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, DeleteMessageResponse> deleteMessagesKafkaConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "delete-message-group");
        JsonDeserializer<DeleteMessageResponse> deserializer = new JsonDeserializer<>(DeleteMessageResponse.class);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("/**");
        deserializer.setUseTypeMapperForKey(true);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DeleteMessageResponse> kafkaDeleteMessagesConsumerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, DeleteMessageResponse> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(deleteMessagesKafkaConsumerFactory());
        return factory;
    }
}
