package com.beyond.hodadoc.chat.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // Broker -> stomp
public class StompWebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final StompHandler stompHandler;
    @Autowired
    public StompWebSocketConfig(StompHandler stompHandler) {
        this.stompHandler = stompHandler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/connect")
                .setAllowedOriginPatterns("http://localhost:*", "http://192.168.*:*", "https://www.hodadoc.littleniddle.store", "https://www.hodadoc.admin.littleniddle.store")
//                ws://가 아닌 http:// 엔드포인트를 사용할 수 있게 해주는 sockJs라이브러리를 통한 요청을 허용하는 설정.
                .withSockJS();

    }

    public void configureMessageBroker(MessageBrokerRegistry registry) {
//        /publish/1형태로 메시지 발행해야 함을 설정
//        /publish로 시작하는 url패턴으로 메시지가 발행되면 @Controller 객체의 @MessageMapping메서드로 라우팅
        registry.setApplicationDestinationPrefixes("/publish");

//        /topic/1형태로 메시지를 수신(subscribe)해야 함을 설정
        registry.enableSimpleBroker("/topic");
    }

//    웹소켓요청(connect, subscribe, disconnect)등의 요청 시에는 http header등 http메시지를 넣어올 수 있고, 이를 통해 가로채 토큰 등을 검증할 수 있음.
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompHandler);
    }

}
