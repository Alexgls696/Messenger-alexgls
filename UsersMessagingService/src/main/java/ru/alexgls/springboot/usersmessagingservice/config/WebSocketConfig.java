package ru.alexgls.springboot.usersmessagingservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import ru.alexgls.springboot.usersmessagingservice.client.AuthServiceClient;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.net.InetAddress;
import java.net.UnknownHostException;


@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {


    private AuthServiceClient authServiceClient;

    private final String localHostAddress;

    @Autowired
    public WebSocketConfig(AuthServiceClient authServiceClient, @Value("${frontend.port}") Integer frontendPort) throws UnknownHostException {
        this.authServiceClient = authServiceClient;
        try {
            localHostAddress = "http://" + InetAddress.getLocalHost().getHostAddress() + ":" + frontendPort;
        } catch (UnknownHostException exception) {
            throw new UnknownHostException("Не удалось определить адрес хоста");
        }
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue", "/topic");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat")
                .setAllowedOrigins("http://localhost:8090", localHostAddress)
                .addInterceptors(new JwtHandshakeInterceptor(authServiceClient))
                .setHandshakeHandler(new UserHandshakeHandler())
                .withSockJS();
    }
}