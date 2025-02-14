package com.aivle.TermCompass.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import com.aivle.TermCompass.domain.User;
import com.aivle.TermCompass.repository.UserRepository;
import com.aivle.TermCompass.service.JwtTokenProvider;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final WSHandler wsHandler;

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry.addHandler(wsHandler, "/ws")
                .setAllowedOrigins("*")
                .addInterceptors(new WebSocketAuthInterceptor(wsHandler)); // JWT 인증을 위한 인터셉터 추가
    }

    // WebSocket 인증을 위한 인터셉터
    @Component
    public class WebSocketAuthInterceptor implements HandshakeInterceptor {

        private final WSHandler wsHandler;

        public WebSocketAuthInterceptor(WSHandler wsHandler) {
            this.wsHandler = wsHandler;
        }

        @Override
        public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes)
                throws Exception {
                    
            // URL에서 JWT 토큰 추출 (예: ws://localhost:8080/ws?token=JWT_TOKEN)
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            String token = jwtTokenProvider.getTokenFromCookie(servletRequest);

            // JWT 토큰에서 사용자 email 추출
            String email;
            Long id;

            // 로그인된 사용자의 경우 ( 토큰 정보 추출 성공 )
            try {
                email = jwtTokenProvider.getEmailFromToken(token);
                id = jwtTokenProvider.getIdFromToken(token);
                // 사용자 정보 DB에서 조회 (UserRepository는 JPA repository로 가정)
                User user = userRepository.findByEmail(email).orElse(null);
                if (user != null) {
                    System.out.println("User found in DB: " + user.getName());
                } else {
                    System.out.println("User not found in DB");
                }
            }
            
            // 비로그인 사용자의 경우 ( 토큰 정보 추출 실패시 )
            catch (IllegalArgumentException e) {
                email = "not@user";
                do {
                    id = (long) (Math.random() * 10000) + 10000; // 숫자 랜덤 생성
                } while (this.wsHandler.containsClientAndFastAPIId(id) || userRepository.existsById(id)); // Use the new method
            }

            // session에 정보 저장
            attributes.put("email", email);
            attributes.put("id", id);
            attributes.put("direction", "client"); // Client <-> Spring 세션임을 표시

            return true;
        }

        @Override
        public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                @NonNull WebSocketHandler wsHandler, @Nullable Exception exception) {

        }

    }

}
