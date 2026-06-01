package com.artnexus.config.scheduler;

import com.artnexus.modules.relay.mapper.*;
import com.artnexus.modules.relay.model.entity.*;
import com.artnexus.modules.relay.websocket.RelayWebSocketHandler;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 接龙超时检测定时任务
 * 每分钟扫描 IN_PROGRESS 状态的房间，超时自动跳过当前轮次
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RelayTimeoutScheduler {

    private final RelayRoomMapper relayRoomMapper;
    private final RelayRoomMemberMapper relayRoomMemberMapper;
    private final RelayWebSocketHandler relayWebSocketHandler;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${relay.turn-timeout-minutes:30}")
    private int turnTimeoutMinutes;

    private static final String TURN_KEY_PREFIX = "relay:turn:";

    @Scheduled(fixedDelay = 60000) // 每分钟检查
    public void checkTimeout() {
        List<RelayRoom> rooms = relayRoomMapper.selectList(
                new LambdaQueryWrapper<RelayRoom>()
                        .eq(RelayRoom::getStatus, "IN_PROGRESS"));

        Duration timeout = Duration.ofMinutes(turnTimeoutMinutes);

        for (RelayRoom room : rooms) {
            try {
                Long turnStartTime = (Long) redisTemplate.opsForValue()
                        .get(TURN_KEY_PREFIX + room.getId());
                if (turnStartTime == null) {
                    // 没有记录开始时间，跳过（可能是刚迁移的数据）
                    continue;
                }

                long elapsed = System.currentTimeMillis() - turnStartTime;
                if (elapsed < timeout.toMillis()) continue;

                log.info("接龙房间 roomId={} 第{}轮超时，自动跳过", room.getId(), room.getCurrentTurn());

                int nextTurn = room.getCurrentTurn() + 1;
                if (nextTurn > room.getMaxMembers()) {
                    room.setStatus("FINISHED");
                    redisTemplate.delete(TURN_KEY_PREFIX + room.getId());
                    relayRoomMapper.updateById(room);
                    relayWebSocketHandler.sendToRoom(room.getId(), "ROOM_FINISHED", Map.of(
                            "message", "接龙已完成！(部分轮次超时跳过)",
                            "totalTurns", room.getMaxMembers()
                    ));
                } else {
                    room.setCurrentTurn(nextTurn);
                    relayRoomMapper.updateById(room);
                    // 重置计时
                    redisTemplate.opsForValue().set(TURN_KEY_PREFIX + room.getId(),
                            System.currentTimeMillis(), timeout.plusMinutes(5));

                    RelayRoomMember nextMember = relayRoomMemberMapper.selectOne(
                            new LambdaQueryWrapper<RelayRoomMember>()
                                    .eq(RelayRoomMember::getRoomId, room.getId())
                                    .eq(RelayRoomMember::getJoinOrder, nextTurn));

                    relayWebSocketHandler.sendToRoom(room.getId(), "TURN_TIMEOUT", Map.of(
                            "skippedTurn", room.getCurrentTurn() - 1,
                            "currentTurn", nextTurn,
                            "currentUserId", nextMember != null ? nextMember.getUserId() : null,
                            "message", "上一轮超时，自动跳过"
                    ));
                }
            } catch (Exception e) {
                log.error("接龙超时处理异常 roomId={}", room.getId(), e);
            }
        }
    }
}
