package com.alexgls.springboot.contentanalysisservice;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.util.unit.DataSize;

@SpringBootApplication
public class ContentAnalysisServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentAnalysisServiceApplication.class, args);
    }

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();

        // Указываем размер (используем DataSize для надежности)
        // Максимальный размер одного файла
        factory.setMaxFileSize(DataSize.ofMegabytes(5));

        // Максимальный размер всего запроса (файлы + данные формы)
        factory.setMaxRequestSize(DataSize.ofMegabytes(5));

        return factory.createMultipartConfig();
    }

}
