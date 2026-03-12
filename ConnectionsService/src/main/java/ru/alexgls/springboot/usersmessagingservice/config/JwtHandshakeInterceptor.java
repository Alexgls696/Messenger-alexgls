package ru.alexgls.springboot.usersmessagingservice.config;

import ru.alexgls.springboot.usersmessagingservice.client.AuthServiceClient;
import ru.alexgls.springboot.usersmessagingservice.dto.JwtValidationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

@RequiredArgsConstructor
@Slf4j
public class JwtHandshakeInterceptor  implements HandshakeInterceptor {

    private final AuthServiceClient authServiceClient;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        String token = extractToken(request);

        if (token == null || token.isEmpty()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        try{
            JwtValidationResponse jwtValidationResponse = authServiceClient.validateToken(token);
            if(!jwtValidationResponse.isValid()){
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    jwtValidationResponse.getUserId(),
                    null,
                    jwtValidationResponse.getRoles().stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList())
            );

            attributes.put("userId", jwtValidationResponse.getUserId());
            attributes.put("roles", jwtValidationResponse.getRoles());
            attributes.put("token", token);
            attributes.put(SPRING_SECURITY_CONTEXT_KEY,
                    new SecurityContextImpl(auth));
            return true;
        }catch (Exception e){
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }

    private String extractToken(ServerHttpRequest request) {
        if (request.getURI().getQuery() != null) {
            String[] queryParams = request.getURI().getQuery().split("&");
            for (String param : queryParams) {
                if (param.startsWith("token=")) {
                    return param.substring(6);
                }
            }
        }
        return null;
    }
}
