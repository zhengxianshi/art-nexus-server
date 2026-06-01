package com.artnexus.modules.canvas.websocket;

import com.artnexus.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 共享画布 WebSocket 处理器
 *
 * 协议说明：
 * - 客户端连接: ws://host/ws/canvas/{roomId}?token=xxx
 * - 消息格式: { "type": "...", "data": {...}, "timestamp": 1234567890 }
 *
 * 消息类型:
 * - DRAW_ACTION: 绘制操作（画笔/橡皮擦/形状）
 * - DRAW_BROADCAST: 服务端广播绘制操作给房间内其他用户
 * - CURSOR_MOVE: 鼠标光标位置同步
 * - USER_JOIN / USER_LEAVE: 用户进出通知
 * - SYNC_REQUEST / SYNC_RESPONSE: 画布状态同步
 * - CLEAR_CANVAS: 清空画布
 * - UNDO_ACTION: 撤销操作
 * - HEARTBEAT / PONG: 心跳
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CanvasWebSocketHandler extends TextWebSocketHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /** roomId -> sessionId -> session */
    private static final Map<Long, Map<String, WebSocketSession>> ROOMS = new ConcurrentHashMap<>();

    /** sessionId -> { userId, roomId, username } */
    private static final Map<String, Map<String, Object>> SESSION_INFO = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        try {
            // 从 URL 中提取 roomId
            Long roomId = extractRoomId(session);
            // 从 URL 参数中提取 token 并认证
            String token = extractToken(session);
            if (token == null || !jwtTokenProvider.validateToken(token)) {
                session.close(CloseStatus.POLICY_VIOLATION);
                return;
            }
            Long userId = jwtTokenProvider.getUserId(token);
            String username = jwtTokenProvider.getUsername(token);

            // 记录会话信息
            Map<String, Object> info = new ConcurrentHashMap<>();
            info.put("userId", userId);
            info.put("roomId", roomId);
            info.put("username", username);
            SESSION_INFO.put(session.getId(), info);

            // 加入房间
            ROOMS.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
                 .put(session.getId(), session);

            // 广播用户加入
            broadcastToRoom(roomId, session.getId(), toJson(buildMessage("USER_JOIN", Map.of(
                    "userId", userId,
                    "username", username,
                    "onlineCount", ROOMS.get(roomId).size()
            ))));

            // 发送房间在线人数
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                    buildMessage("ROOM_INFO", Map.of(
                            "onlineCount", ROOMS.get(roomId).size(),
                            "roomId", roomId
                    ))
            )));

            log.info("用户 {} 加入画布房间 roomId={}, 当前在线: {}", username, roomId, ROOMS.get(roomId).size());
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
            Map<String, Object> info = SESSION_INFO.get(session.getId());
            if (info == null) return;

            Long roomId = (Long) info.get("roomId");

            switch (type) {
                case "DRAW_ACTION":
                    // 转发给房间内其他用户
                    broadcastToRoomExcept(roomId, session.getId(), message.getPayload());
                    // 将操作写入 Redis 队列用于持久化
                    redisTemplate.opsForList().rightPush(
                            "canvas:ops:" + roomId, message.getPayload());
                    // 更新快照：让迟来者也能看到完整画布
                    updateSnapshot(roomId, node.get("data"));
                    break;

                case "CURSOR_MOVE":
                case "UNDO_ACTION":
                    // 转发给房间内其他用户
                    broadcastToRoomExcept(roomId, session.getId(), message.getPayload());
                    break;

                case "CLEAR_CANVAS":
                    // 转发给房间内其他用户
                    broadcastToRoomExcept(roomId, session.getId(), message.getPayload());
                    // 清空操作记录和快照
                    redisTemplate.delete("canvas:ops:" + roomId);
                    redisTemplate.opsForValue().set("canvas:snapshot:" + roomId,
                            "{\"actions\":[]}");
                    break;

                case "SYNC_REQUEST":
                    // 发送最新画布快照
                    String snapshot = (String) redisTemplate.opsForValue()
                            .get("canvas:snapshot:" + roomId);
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                            buildMessage("SYNC_RESPONSE", Map.of("snapshot", snapshot != null ? snapshot : "{\"actions\":[]}"))
                    )));
                    break;

                case "HEARTBEAT":
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                            buildMessage("PONG", Map.of())
                    )));
                    break;

                default:
                    log.warn("未知消息类型: {}", type);
            }
        } catch (Exception e) {
            log.error("处理 WebSocket 消息异常", e);
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
                if (room.isEmpty()) {
                    ROOMS.remove(roomId);
                }
            }
            broadcastToRoom(roomId, null, toJson(buildMessage("USER_LEAVE", Map.of(
                    "userId", info.get("userId"),
                    "username", info.get("username"),
                    "onlineCount", ROOMS.containsKey(roomId) ? ROOMS.get(roomId).size() : 0
            ))));
            log.info("用户 {} 离开画布房间 roomId={}", info.get("username"), roomId);
        }
    }

    private Long extractRoomId(WebSocketSession session) {
        URI uri = session.getUri();
        String path = uri.getPath(); // /ws/canvas/{roomId}
        String[] parts = path.split("/");
        return Long.parseLong(parts[parts.length - 1]);
    }

    private String extractToken(WebSocketSession session) {
        URI uri = session.getUri();
        String query = uri.getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if ("token".equals(kv[0]) && kv.length > 1) {
                    return kv[1];
                }
            }
        }
        return null;
    }

    private void broadcastToRoom(Long roomId, String excludeSessionId, String message) {
        Map<String, WebSocketSession> room = ROOMS.get(roomId);
        if (room != null) {
            room.forEach((sid, s) -> {
                if (!sid.equals(excludeSessionId) && s.isOpen()) {
                    try {
                        s.sendMessage(new TextMessage(message));
                    } catch (Exception e) {
                        log.error("广播消息失败 sessionId={}", sid, e);
                    }
                }
            });
        }
    }

    private void broadcastToRoomExcept(Long roomId, String excludeSessionId, String message) {
        broadcastToRoom(roomId, excludeSessionId, message);
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); } catch (Exception e) { return "{}"; }
    }

    private Map<String, Object> buildMessage(String type, Map<String, Object> data) {
        Map<String, Object> msg = new ConcurrentHashMap<>();
        msg.put("type", type);
        msg.put("data", data);
        msg.put("timestamp", System.currentTimeMillis());
        return msg;
    }

    /**
     * 将绘图动作追加到快照，使迟来的用户也能看到完整画布
     */
    private void updateSnapshot(Long roomId, JsonNode actionData) {
        String key = "canvas:snapshot:" + roomId;
        String raw = (String) redisTemplate.opsForValue().get(key);
        try {
            JsonNode snapshot = raw != null ? objectMapper.readTree(raw) : objectMapper.createObjectNode();
            // snapshot = { "actions": [...] }
            if (!snapshot.has("actions")) {
                snapshot = objectMapper.readTree("{\"actions\":[]}");
            }
            // 只保留 start/draw/end 绘图动作（过滤掉光标同步等非绘图消息）
            JsonNode actionType = actionData.get("action");
            if (actionType != null) {
                // 换行用 += 追加
                ((com.fasterxml.jackson.databind.node.ArrayNode) snapshot.get("actions")).add(actionData);
                redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(snapshot));
            }
        } catch (Exception e) {
            log.error("更新画布快照失败 roomId={}", roomId, e);
        }
    }
}
