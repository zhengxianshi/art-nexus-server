package com.artnexus.modules.relay.service.impl;

import com.artnexus.common.BusinessException;
import com.artnexus.common.ErrorCode;
import com.artnexus.common.PageResult;
import com.artnexus.modules.relay.mapper.*;
import com.artnexus.modules.relay.model.dto.*;
import com.artnexus.modules.relay.model.entity.*;
import com.artnexus.modules.relay.service.RelayService;
import com.artnexus.modules.relay.websocket.RelayWebSocketHandler;
import com.artnexus.modules.user.mapper.UserMapper;
import com.artnexus.modules.user.model.entity.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RelayServiceImpl implements RelayService {

    private final RelayRoomMapper relayRoomMapper;
    private final RelayRoomMemberMapper relayRoomMemberMapper;
    private final RelaySegmentMapper relaySegmentMapper;
    private final UserMapper userMapper;
    private final RelayWebSocketHandler relayWebSocketHandler;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final Duration TURN_TIMEOUT = Duration.ofMinutes(30);

    @Override
    @Transactional
    public RelayRoomVO createRoom(Long userId, RelayRoomCreateRequest request) {
        RelayRoom room = new RelayRoom();
        room.setCreatorId(userId);
        room.setTitle(request.getTitle());
        room.setMaxMembers(request.getMaxMembers());
        room.setCurrentCount(1);
        room.setStatus("WAITING");
        room.setCurrentTurn(0);
        room.setPrompt(request.getPrompt());
        relayRoomMapper.insert(room);

        // 创建者自动加入
        RelayRoomMember member = new RelayRoomMember();
        member.setRoomId(room.getId());
        member.setUserId(userId);
        member.setJoinOrder(1);
        member.setSubmitted(false);
        relayRoomMemberMapper.insert(member);

        return toVO(room);
    }

    @Override
    public PageResult<RelayRoomVO> listRooms(int page, int size, String status) {
        LambdaQueryWrapper<RelayRoom> wrapper = new LambdaQueryWrapper<RelayRoom>()
                .orderByDesc(RelayRoom::getCreatedAt);
        if (status != null && !status.isBlank()) {
            wrapper.eq(RelayRoom::getStatus, status);
        }
        Page<RelayRoom> pageResult = relayRoomMapper.selectPage(new Page<>(page, size), wrapper);
        List<RelayRoomVO> records = pageResult.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(pageResult.getTotal(), page, size, records);
    }

    @Override
    public RelayRoomVO getRoomDetail(Long roomId) {
        RelayRoom room = relayRoomMapper.selectById(roomId);
        if (room == null) {
            throw new BusinessException(ErrorCode.RELAY_ROOM_NOT_FOUND);
        }
        return toVO(room);
    }

    @Override
    @Transactional
    public void joinRoom(Long roomId, Long userId) {
        RelayRoom room = relayRoomMapper.selectById(roomId);
        if (room == null) {
            throw new BusinessException(ErrorCode.RELAY_ROOM_NOT_FOUND);
        }
        if (!"WAITING".equals(room.getStatus())) {
            throw new BusinessException(ErrorCode.RELAY_ROOM_IN_PROGRESS);
        }
        if (room.getCurrentCount() >= room.getMaxMembers()) {
            throw new BusinessException(ErrorCode.RELAY_ROOM_FULL);
        }
        // 检查是否已在房间中
        Long count = relayRoomMemberMapper.selectCount(new LambdaQueryWrapper<RelayRoomMember>()
                .eq(RelayRoomMember::getRoomId, roomId)
                .eq(RelayRoomMember::getUserId, userId));
        if (count > 0) {
            throw new BusinessException(ErrorCode.RELAY_ALREADY_IN_ROOM);
        }

        RelayRoomMember member = new RelayRoomMember();
        member.setRoomId(roomId);
        member.setUserId(userId);
        member.setJoinOrder(room.getCurrentCount() + 1);
        member.setSubmitted(false);
        relayRoomMemberMapper.insert(member);

        room.setCurrentCount(room.getCurrentCount() + 1);
        // 满员即开
        if (room.getCurrentCount() >= room.getMaxMembers()) {
            room.setStatus("IN_PROGRESS");
            room.setCurrentTurn(1);
            // 记录当前轮次开始时间（Redis），用于超时检测
            redisTemplate.opsForValue().set("relay:turn:" + roomId,
                    System.currentTimeMillis(), TURN_TIMEOUT.plusMinutes(5));
        }
        relayRoomMapper.updateById(room);

        // WebSocket 推送
        if ("IN_PROGRESS".equals(room.getStatus())) {
            // 查找第一轮的成员
            RelayRoomMember firstMember = relayRoomMemberMapper.selectOne(
                    new LambdaQueryWrapper<RelayRoomMember>()
                            .eq(RelayRoomMember::getRoomId, roomId)
                            .eq(RelayRoomMember::getJoinOrder, 1));
            relayWebSocketHandler.sendToRoom(roomId, "ROOM_START", Map.of(
                    "message", "接龙开始！",
                    "currentTurn", 1,
                    "currentUserId", firstMember != null ? firstMember.getUserId() : null,
                    "prompt", room.getPrompt()
            ));
        }
        relayWebSocketHandler.sendToRoom(roomId, "USER_JOIN", Map.of(
                "userId", userId,
                "currentCount", room.getCurrentCount(),
                "maxMembers", room.getMaxMembers()
        ));
    }

    @Override
    public void leaveRoom(Long roomId, Long userId) {
        RelayRoomMember member = relayRoomMemberMapper.selectOne(new LambdaQueryWrapper<RelayRoomMember>()
                .eq(RelayRoomMember::getRoomId, roomId)
                .eq(RelayRoomMember::getUserId, userId));
        if (member == null) {
            throw new BusinessException(ErrorCode.RELAY_NOT_IN_ROOM);
        }
        relayRoomMemberMapper.deleteById(member.getId());
        RelayRoom room = relayRoomMapper.selectById(roomId);
        room.setCurrentCount(room.getCurrentCount() - 1);
        // 如果等待中有人离开导致人数<5，暂且保持 WAITING 状态
        relayRoomMapper.updateById(room);
        relayWebSocketHandler.sendToRoom(roomId, "USER_LEAVE", Map.of(
                "userId", userId,
                "currentCount", room.getCurrentCount()
        ));
    }

    @Override
    @Transactional
    public void submitSegment(Long roomId, Long userId, String imageUrl, String hint) {
        RelayRoom room = relayRoomMapper.selectById(roomId);
        if (room == null) {
            throw new BusinessException(ErrorCode.RELAY_ROOM_NOT_FOUND);
        }
        // 检查是否为当前轮次用户的回合
        RelayRoomMember member = relayRoomMemberMapper.selectOne(new LambdaQueryWrapper<RelayRoomMember>()
                .eq(RelayRoomMember::getRoomId, roomId)
                .eq(RelayRoomMember::getUserId, userId));
        if (member == null) {
            throw new BusinessException(ErrorCode.RELAY_NOT_IN_ROOM);
        }
        if (!member.getJoinOrder().equals(room.getCurrentTurn())) {
            throw new BusinessException(ErrorCode.RELAY_NOT_YOUR_TURN);
        }

        RelaySegment segment = new RelaySegment();
        segment.setRoomId(roomId);
        segment.setUserId(userId);
        segment.setTurnNumber(room.getCurrentTurn());
        segment.setImageUrl(imageUrl);
        segment.setHint(hint);
        relaySegmentMapper.insert(segment);

        member.setSubmitted(true);
        relayRoomMemberMapper.updateById(member);

        // 更新房间封面为最新提交的图片
        room.setCoverUrl(imageUrl);
        relayRoomMapper.updateById(room);

        // 推进轮次
        int nextTurn = room.getCurrentTurn() + 1;
        if (nextTurn > room.getMaxMembers()) {
            room.setStatus("FINISHED");
            relayRoomMapper.updateById(room);
            // 清除轮次计时
            redisTemplate.delete("relay:turn:" + roomId);
            // 广播接龙完成
            relayWebSocketHandler.sendToRoom(roomId, "ROOM_FINISHED", Map.of(
                    "message", "接龙已完成！",
                    "totalTurns", room.getMaxMembers()
            ));
        } else {
            room.setCurrentTurn(nextTurn);
            relayRoomMapper.updateById(room);
            // 更新轮次计时
            redisTemplate.opsForValue().set("relay:turn:" + roomId,
                    System.currentTimeMillis(), TURN_TIMEOUT.plusMinutes(5));
            // 通知下一轮用户
            RelayRoomMember nextMember = relayRoomMemberMapper.selectOne(
                    new LambdaQueryWrapper<RelayRoomMember>()
                            .eq(RelayRoomMember::getRoomId, roomId)
                            .eq(RelayRoomMember::getJoinOrder, nextTurn));
            relayWebSocketHandler.sendToRoom(roomId, "TURN_CHANGE", Map.of(
                    "currentTurn", nextTurn,
                    "currentUserId", nextMember != null ? nextMember.getUserId() : null,
                    "hint", hint
            ));
        }
        // 广播片段已提交
        relayWebSocketHandler.sendToRoom(roomId, "SEGMENT_SUBMITTED", Map.of(
                "turnNumber", room.getCurrentTurn(),
                "userId", userId
        ));
    }

    @Override
    @Transactional
    public void deleteRoom(Long roomId, Long userId) {
        RelayRoom room = relayRoomMapper.selectById(roomId);
        if (room == null) {
            throw new BusinessException(ErrorCode.RELAY_ROOM_NOT_FOUND);
        }
        if (!room.getCreatorId().equals(userId)) {
            throw new BusinessException(ErrorCode.RELAY_NOT_CREATOR);
        }
        relayRoomMapper.deleteById(roomId);
    }

    @Override
    public RelaySegmentVO getLastSegment(Long roomId) {
        RelaySegment segment = relaySegmentMapper.selectOne(
                new LambdaQueryWrapper<RelaySegment>()
                        .eq(RelaySegment::getRoomId, roomId)
                        .orderByDesc(RelaySegment::getTurnNumber)
                        .last("LIMIT 1"));
        if (segment == null) {
            throw new BusinessException(ErrorCode.RELAY_NO_SEGMENTS);
        }
        return toSegmentVO(segment);
    }

    @Override
    public List<RelaySegmentVO> getGallery(Long roomId) {
        List<RelaySegment> segments = relaySegmentMapper.selectList(
                new LambdaQueryWrapper<RelaySegment>()
                        .eq(RelaySegment::getRoomId, roomId)
                        .orderByAsc(RelaySegment::getTurnNumber));
        return segments.stream().map(this::toSegmentVO).collect(Collectors.toList());
    }

    private RelayRoomVO toVO(RelayRoom room) {
        RelayRoomVO vo = new RelayRoomVO();
        vo.setId(room.getId());
        vo.setCreatorId(room.getCreatorId());
        vo.setTitle(room.getTitle());
        vo.setCoverUrl(room.getCoverUrl());
        vo.setMaxMembers(room.getMaxMembers());
        vo.setCurrentCount(room.getCurrentCount());
        vo.setStatus(room.getStatus());
        vo.setCurrentTurn(room.getCurrentTurn());
        vo.setPrompt(room.getPrompt());
        vo.setCreatedAt(room.getCreatedAt());

        User creator = userMapper.selectById(room.getCreatorId());
        if (creator != null) {
            vo.setCreatorName(creator.getUsername());
        }

        List<RelayRoomMember> members = relayRoomMemberMapper.selectList(
                new LambdaQueryWrapper<RelayRoomMember>()
                        .eq(RelayRoomMember::getRoomId, room.getId())
                        .orderByAsc(RelayRoomMember::getJoinOrder));
        vo.setMembers(members.stream().map(m -> {
            RelayMemberVO mvo = new RelayMemberVO();
            mvo.setUserId(m.getUserId());
            mvo.setJoinOrder(m.getJoinOrder());
            mvo.setSubmitted(m.getSubmitted());
            User u = userMapper.selectById(m.getUserId());
            if (u != null) {
                mvo.setUsername(u.getUsername());
                mvo.setAvatarUrl(u.getAvatarUrl());
            }
            return mvo;
        }).collect(Collectors.toList()));

        return vo;
    }

    private RelaySegmentVO toSegmentVO(RelaySegment segment) {
        RelaySegmentVO vo = new RelaySegmentVO();
        vo.setId(segment.getId());
        vo.setTurnNumber(segment.getTurnNumber());
        vo.setUserId(segment.getUserId());
        vo.setImageUrl(segment.getImageUrl());
        vo.setHint(segment.getHint());
        vo.setCreatedAt(segment.getCreatedAt());
        User user = userMapper.selectById(segment.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setAvatarUrl(user.getAvatarUrl());
        }
        return vo;
    }
}
