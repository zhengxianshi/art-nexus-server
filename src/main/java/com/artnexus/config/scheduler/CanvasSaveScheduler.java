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

                // 读取 Redis 中已有的快照，将新操作追加进去（而非覆盖）
                String snapshotKey = SNAPSHOT_KEY_PREFIX + roomId;
                String existingRaw = (String) redisTemplate.opsForValue().get(snapshotKey);
                com.fasterxml.jackson.databind.JsonNode existingSnapshot;
                try {
                    existingSnapshot = existingRaw != null
                            ? objectMapper.readTree(existingRaw)
                            : objectMapper.readTree("{\"actions\":[]}");
                } catch (Exception e) {
                    existingSnapshot = objectMapper.readTree("{\"actions\":[]}");
                }

                com.fasterxml.jackson.databind.node.ArrayNode actions =
                        (com.fasterxml.jackson.databind.node.ArrayNode) existingSnapshot.get("actions");

                // 暂存新增的操作（用于 MySQL 持久化）
                StringBuilder newOpsBuilder = new StringBuilder("[");
                int idx = 0;
                while (redisTemplate.opsForList().size(opsKey) != null
                        && redisTemplate.opsForList().size(opsKey) > 0) {
                    Object op = redisTemplate.opsForList().leftPop(opsKey);
                    if (op != null) {
                        if (idx > 0) newOpsBuilder.append(",");
                        newOpsBuilder.append(op.toString());
                        // 追加到已有快照
                        try {
                            actions.add(objectMapper.readTree(op.toString()));
                        } catch (Exception e) {
                            log.warn("解析操作 JSON 失败", e);
                        }
                        idx++;
                    }
                }
                newOpsBuilder.append("]");

                if (idx == 0) continue;

                // MySQL 持久化（保存新增的这批操作）
                CanvasSnapshot snapshot = new CanvasSnapshot();
                snapshot.setRoomId(Long.parseLong(roomId));
                snapshot.setSnapshotData(newOpsBuilder.toString());
                snapshot.setLastOpSeq(currentSeq);
                canvasSnapshotMapper.insert(snapshot);

                // 更新 Redis 快照 = 已有操作 + 新增操作
                redisTemplate.opsForValue().set(snapshotKey,
                        objectMapper.writeValueAsString(existingSnapshot));

                log.debug("画布 roomId={} 自动保存: {} 条操作, seq={}", roomId, opCount, currentSeq);
            } catch (Exception e) {
                log.error("画布自动保存失败 key={}", opsKey, e);
            }
        }
    }
}
