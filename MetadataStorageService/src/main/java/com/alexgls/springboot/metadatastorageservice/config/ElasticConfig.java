package com.alexgls.springboot.metadatastorageservice.config;

import lombok.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;

@Configuration
public class ElasticConfig extends ElasticsearchConfiguration {

    @Override
    @NonNull
    public ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder()
                .connectedTo("localhost:9200")
                .withBasicAuth("elastic", "elastic") // Явно задаем логин/пароль
                .withConnectTimeout(10000)
                .withSocketTimeout(30000)
                .build();
    }
}
