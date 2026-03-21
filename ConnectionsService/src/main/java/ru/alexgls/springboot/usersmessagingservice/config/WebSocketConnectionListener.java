package ru.alexgls.springboot.usersmessagingservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import ru.alexgls.springboot.usersmessagingservice.service.PresenceService;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketConnectionListener {

    private final PresenceService presenceService;

    @EventListener
    public void connect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> attributes = accessor.getSessionAttributes();

        if (attributes != null && attributes.containsKey("token")) {
            String token = (String) attributes.get("token");
            int userId = Integer.parseInt((String)attributes.get("userId"));
            presenceService.setOnline(userId);
        }
    }

    @EventListener
    public void disconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> attributes = accessor.getSessionAttributes();

        if (attributes != null && attributes.containsKey("token")) {
            int userId = Integer.parseInt((String)attributes.get("userId"));
            presenceService.setOffline(userId);
        }
    }
}