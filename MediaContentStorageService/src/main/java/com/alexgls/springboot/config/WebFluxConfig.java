package com.alexgls.springboot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;

import org.springframework.http.codec.multipart.DefaultPartHttpMessageReader;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;


public class WebFluxConfig implements WebFluxConfigurer {

//    @Override
//    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
//        configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024); // 16MB
//
//        DefaultPartHttpMessageReader partReader = new DefaultPartHttpMessageReader();
//        partReader.setMaxParts(1000);
//        partReader.setMaxDiskUsagePerPart(10 * 1024 * 1024L);
//        partReader.setEnableLoggingRequestDetails(true);
//
//        configurer.defaultCodecs().multipartReader(partReader);
//    }
}
