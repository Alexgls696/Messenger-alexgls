package com.alexgls.springboot.metadatastorageservice.config;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;

@Configuration
public class ElasticConfig extends ElasticsearchConfiguration {

    @Value("${spring.elasticsearch.uris}")
    private String elasticsearchHost;

    @Value("${spring.elasticsearch.username}")
    private String elasticsearchUsername;

    @Value("${spring.elasticsearch.password}")
    private String elasticsearchPassword;

    @Override
    @NonNull
    public ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder()
                .connectedTo(elasticsearchHost)
                .withBasicAuth(elasticsearchUsername, elasticsearchPassword)
                .withConnectTimeout(10000)
                .withSocketTimeout(30000)
                .build();
    }
}
