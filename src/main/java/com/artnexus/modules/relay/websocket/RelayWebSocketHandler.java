package com.artnexus.modules.relay.websocket;

import com.artnexus.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 绘画接龙 WebSocket 处理器
 *
 * 协议说明：
 * - 客户端连接: ws://host/ws/relay/{roomId}?token=xxx
 *
 * 消息类型:
 * - ROOM_START: 房间满员，接龙开始
 * - TURN_CHANGE: 轮次变更，通知当前轮到谁
 * - TURN_START: 当前轮次开始（含上一人留下的提示词）
 * - SEGMENT_SUBMITTED: 某人提交了片段
 * - TIME_WARNING: 超时警告
 * - TURN_TIMEOUT: 当前轮次超时（自动跳过）
 * - ROOM_FINISHED: 接龙完成
 * - USER_JOIN / USER_LEAVE: 用户进出
 * - HEARTBEAT / PONG: 心跳
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RelayWebSocketHandler extends TextWebSocketHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    /** roomId -> sessionId -> session */
    private static final Map<Long, Map<String, WebSocketSession>> ROOMS = new ConcurrentHashMap<>();

    /** sessionId -> { userId, roomId, username } */
    private static final Map<String, Map<String, Object>> SESSION_INFO = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        try {
            Long roomId = extractRoomId(session);
            String token = extractToken(session);
            if (token == null || !jwtTokenProvider.validateToken(token)) {
                session.close(CloseStatus.POLICY_VIOLATION);
                return;
            }
            Long userId = jwtTokenProvider.getUserId(token);
            String username = jwtTokenProvider.getUsername(token);

            Map<String, Object> info = new ConcurrentHashMap<>();
            info.put("userId", userId);
            info.put("roomId", roomId);
            info.put("username", username);
            SESSION_INFO.put(session.getId(), info);

            ROOMS.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
                 .put(session.getId(), session);

            broadcastToRoom(roomId, session.getId(), toJson(buildMessage("USER_JOIN", Map.of(
                    "userId", userId,
                    "username", username,
                    "onlineCount", ROOMS.get(roomId).size()
            ))));

            log.info("用户 {} 加入接龙房间 roomId={}", username, roomId);
        } catch (Exception e) {
            log.error("WebSocket 连接建立失败", e);
            try { session.close(); } catch (Exception ignored) {}
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode node = objectMapper.readTree(message.getPayload());
            String type = node.get("type").asText();

            switch (type) {
                case "HEARTBEAT":
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                            buildMessage("PONG", Map.of())
                    )));
                    break;
                default:
                    log.warn("接龙 WebSocket 未知消息类型: {}", type);
            }
        } catch (Exception e) {
            log.error("处理接龙 WebSocket 消息异常", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Map<String, Object> info = SESSION_INFO.remove(session.getId());
        if (info != null) {
            Long roomId = (Long) info.get("roomId");
            Map<String, WebSocketSession> room = ROOMS.get(roomId);
            if (room != null) {
                room.remove(session.getId());
                if (room.isEmpty()) ROOMS.remove(roomId);
            }
            broadcastToRoom(roomId, null, toJson(buildMessage("USER_LEAVE", Map.of(
                    "userId", info.get("userId"),
                    "username", info.get("username")
            ))));
        }
    }

    /**
     * 服务端主动推送消息给房间
     */
    public void sendToRoom(Long roomId, String type, Map<String, Object> data) {
        Map<String, WebSocketSession> room = ROOMS.get(roomId);
        if (room != null) {
            String msg = toJson(buildMessage(type, data));
            room.forEach((sid, s) -> {
                if (s.isOpen()) {
                    try { s.sendMessage(new TextMessage(msg)); } catch (Exception e) {
                        log.error("推送消息失败", e);
                    }
                }
            });
        }
    }

    private Long extractRoomId(WebSocketSession session) {
        String path = session.getUri().getPath();
        String[] parts = path.split("/");
        return Long.parseLong(parts[parts.length - 1]);
    }

    private String extractToken(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if ("token".equals(kv[0]) && kv.length > 1) return kv[1];
            }
        }
        return null;
    }

    private void broadcastToRoom(Long roomId, String excludeSessionId, String jsonMessage) {
        Map<String, WebSocketSession> room = ROOMS.get(roomId);
        if (room != null) {
            room.forEach((sid, s) -> {
                if (!sid.equals(excludeSessionId) && s.isOpen()) {
                    try { s.sendMessage(new TextMessage(jsonMessage)); } catch (Exception ignored) {}
                }
            });
        }
    }

    private Map<String, Object> buildMessage(String type, Map<String, Object> data) {
        Map<String, Object> msg = new ConcurrentHashMap<>();
        msg.put("type", type);
        msg.put("data", data);
        msg.put("timestamp", System.currentTimeMillis());
        return msg;
    }

    private String toJson(Map<String, Object> map) {
        try { return objectMapper.writeValueAsString(map); } catch (Exception e) { return "{}"; }
    }
}
