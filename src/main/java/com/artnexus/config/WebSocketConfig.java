package com.artnexus.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.artnexus.modules.canvas.websocket.CanvasWebSocketHandler;
import com.artnexus.modules.relay.websocket.RelayWebSocketHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final CanvasWebSocketHandler canvasWebSocketHandler;
    private final RelayWebSocketHandler relayWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(canvasWebSocketHandler, "/ws/canvas/{roomId}")
                .setAllowedOrigins("*");
        registry.addHandler(relayWebSocketHandler, "/ws/relay/{roomId}")
                .setAllowedOrigins("*");
    }
}
