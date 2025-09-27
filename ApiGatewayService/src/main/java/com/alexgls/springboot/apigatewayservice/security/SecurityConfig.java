package com.alexgls.springboot.apigatewayservice.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;


@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {


    private final String localHostAddress;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    public SecurityConfig(@Value("${frontend.port}") Integer frontendPort) throws UnknownHostException {
        try {
            localHostAddress = "http://" + InetAddress.getLocalHost().getHostAddress() + ":" + frontendPort;
        } catch (UnknownHostException exception) {
            throw new UnknownHostException("Не удалось определить адрес хоста");
        }
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)


                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .pathMatchers(
                                "/auth/**",
                                "/.well-known/jwks.json",
                                "/ws-chat/**",
                                "/api/storage/proxy/download/**",
                                "/api/verification/**"
                        ).permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(server -> server.jwt(jwt -> jwt
                        .jwkSetUri(jwkSetUri)
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:8090", localHostAddress));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public Converter<Jwt, ? extends Mono<? extends AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new ReactiveGrantedAuthoritiesExtractor());
        return converter;
    }

    public static class ReactiveGrantedAuthoritiesExtractor implements Converter<Jwt, Flux<GrantedAuthority>> {
        @Override
        public Flux<GrantedAuthority> convert(Jwt jwt) {
            Object roles = jwt.getClaims().get("roles");
            if (!(roles instanceof Collection)) {
                return Flux.empty();
            }
            Collection<String> roleStrings = (Collection<String>) roles;
            return Flux.fromIterable(roleStrings)
                    .map(SimpleGrantedAuthority::new);
        }
    }
}


