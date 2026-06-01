package com.artnexus.config.scheduler;

import com.artnexus.modules.canvas.mapper.CanvasSnapshotMapper;
import com.artnexus.modules.canvas.model.entity.CanvasSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 画布自动保存定时任务
 * 每隔 30s 扫描 Redis 中的画布操作队列，批量持久化到 MySQL
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CanvasSaveScheduler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CanvasSnapshotMapper canvasSnapshotMapper;
    private final ObjectMapper objectMapper;

    private static final String OPS_KEY_PREFIX = "canvas:ops:";
    private static final String SEQ_KEY_PREFIX = "canvas:seq:";
    private static final String SNAPSHOT_KEY_PREFIX = "canvas:snapshot:";

    @Scheduled(fixedDelayString = "${canvas.auto-save-interval:30000}")
    public void saveCanvasOperations() {
        // 扫描所有 canvas:ops:* 的 key
        Set<String> keys = redisTemplate.keys(OPS_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) return;

        for (String opsKey : keys) {
            try {
                String roomId = opsKey.substring(OPS_KEY_PREFIX.length());
                // 取出队列中所有待持久化的操作
                Long opCount = redisTemplate.opsForList().size(opsKey);
                if (opCount == null || opCount == 0) continue;

                // 读取当前序号
                String seqKey = SEQ_KEY_PREFIX + roomId;
                Long currentSeq = redisTemplate.opsForValue().increment(seqKey, opCount);
                if (currentSeq == null) continue;

                // 构建快照 JSON（只保存操作列表，前端根据操作重放）
                StringBuilder opsBuilder = new StringBuilder("[");
                for (int i = 0; i < opCount; i++) {
                    Object op = redisTemplate.opsForList().leftPop(opsKey);
                    if (op != null) {
                        if (i > 0) opsBuilder.append(",");
                        opsBuilder.append(op.toString());
                    }
                }
                opsBuilder.append("]");

                CanvasSnapshot snapshot = new CanvasSnapshot();
                snapshot.setRoomId(Long.parseLong(roomId));
                snapshot.setSnapshotData(opsBuilder.toString());
                snapshot.setLastOpSeq(currentSeq);
                canvasSnapshotMapper.insert(snapshot);

                // 更新 Redis 中最新的完整快照
                redisTemplate.opsForValue().set(SNAPSHOT_KEY_PREFIX + roomId,
                        opsBuilder.toString());

                log.debug("画布 roomId={} 自动保存: {} 条操作, seq={}", roomId, opCount, currentSeq);
            } catch (Exception e) {
                log.error("画布自动保存失败 key={}", opsKey, e);
            }
        }
    }
}
