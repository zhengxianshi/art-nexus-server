package com.artnexus.modules.canvas.service.impl;

import com.artnexus.common.BusinessException;
import com.artnexus.common.ErrorCode;
import com.artnexus.common.PageResult;
import com.artnexus.modules.canvas.mapper.*;
import com.artnexus.modules.canvas.model.dto.*;
import com.artnexus.modules.canvas.model.entity.*;
import com.artnexus.modules.canvas.service.CanvasService;
import com.artnexus.modules.user.mapper.UserMapper;
import com.artnexus.modules.user.model.entity.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CanvasServiceImpl implements CanvasService {

    private final CanvasRoomMapper canvasRoomMapper;
    private final CanvasRoomMemberMapper canvasRoomMemberMapper;
    private final CanvasSnapshotMapper canvasSnapshotMapper;
    private final UserMapper userMapper;

    private static final int MAX_MEMBERS = 5;

    @Override
    @Transactional
    public CanvasRoomVO createRoom(Long userId, CanvasRoomCreateRequest request) {
        CanvasRoom room = new CanvasRoom();
        room.setCreatorId(userId);
        room.setTitle(request.getTitle());
        room.setMaxMembers(MAX_MEMBERS);
        room.setCurrentCount(1);
        room.setStatus("WAITING");
        canvasRoomMapper.insert(room);

        CanvasRoomMember member = new CanvasRoomMember();
        member.setRoomId(room.getId());
        member.setUserId(userId);
        canvasRoomMemberMapper.insert(member);

        room.setStatus("ACTIVE");
        canvasRoomMapper.updateById(room);

        return toVO(room);
    }

    @Override
    public PageResult<CanvasRoomVO> listRooms(int page, int size) {
        Page<CanvasRoom> pageResult = canvasRoomMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<CanvasRoom>().orderByDesc(CanvasRoom::getCreatedAt));
        List<CanvasRoomVO> records = pageResult.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(pageResult.getTotal(), page, size, records);
    }

    @Override
    public CanvasRoomVO getRoomDetail(Long roomId) {
        CanvasRoom room = canvasRoomMapper.selectById(roomId);
        if (room == null) {
            throw new BusinessException(ErrorCode.CANVAS_ROOM_NOT_FOUND);
        }
        return toVO(room);
    }

    @Override
    @Transactional
    public CanvasRoomVO joinRoom(Long roomId, Long userId) {
        CanvasRoom room = canvasRoomMapper.selectById(roomId);
        if (room == null) {
            throw new BusinessException(ErrorCode.CANVAS_ROOM_NOT_FOUND);
        }
        if (room.getCurrentCount() >= room.getMaxMembers()) {
            throw new BusinessException(ErrorCode.CANVAS_ROOM_FULL);
        }
        Long count = canvasRoomMemberMapper.selectCount(new LambdaQueryWrapper<CanvasRoomMember>()
                .eq(CanvasRoomMember::getRoomId, roomId)
                .eq(CanvasRoomMember::getUserId, userId));
        if (count > 0) {
            throw new BusinessException(ErrorCode.CANVAS_ALREADY_IN_ROOM);
        }

        CanvasRoomMember member = new CanvasRoomMember();
        member.setRoomId(roomId);
        member.setUserId(userId);
        canvasRoomMemberMapper.insert(member);

        room.setCurrentCount(room.getCurrentCount() + 1);
        canvasRoomMapper.updateById(room);

        return toVO(room);
    }

    @Override
    public void leaveRoom(Long roomId, Long userId) {
        CanvasRoomMember member = canvasRoomMemberMapper.selectOne(
                new LambdaQueryWrapper<CanvasRoomMember>()
                        .eq(CanvasRoomMember::getRoomId, roomId)
                        .eq(CanvasRoomMember::getUserId, userId));
        if (member != null) {
            canvasRoomMemberMapper.deleteById(member.getId());
            CanvasRoom room = canvasRoomMapper.selectById(roomId);
            room.setCurrentCount(Math.max(0, room.getCurrentCount() - 1));
            if (room.getCurrentCount() == 0) {
                room.setStatus("CLOSED");
            }
            canvasRoomMapper.updateById(room);
        }
    }

    @Override
    @Transactional
    public void deleteRoom(Long roomId, Long userId) {
        CanvasRoom room = canvasRoomMapper.selectById(roomId);
        if (room == null) {
            throw new BusinessException(ErrorCode.CANVAS_ROOM_NOT_FOUND);
        }
        if (!room.getCreatorId().equals(userId)) {
            throw new BusinessException(ErrorCode.CANVAS_NOT_CREATOR);
        }
        canvasRoomMapper.deleteById(roomId);
    }

    @Override
    public String getSnapshot(Long roomId) {
        CanvasSnapshot snapshot = canvasSnapshotMapper.selectOne(
                new LambdaQueryWrapper<CanvasSnapshot>()
                        .eq(CanvasSnapshot::getRoomId, roomId)
                        .orderByDesc(CanvasSnapshot::getCreatedAt)
                        .last("LIMIT 1"));
        return snapshot != null ? snapshot.getSnapshotData() : "{}";
    }

    private CanvasRoomVO toVO(CanvasRoom room) {
        CanvasRoomVO vo = new CanvasRoomVO();
        vo.setId(room.getId());
        vo.setCreatorId(room.getCreatorId());
        vo.setTitle(room.getTitle());
        vo.setMaxMembers(room.getMaxMembers());
        vo.setCurrentCount(room.getCurrentCount());
        vo.setStatus(room.getStatus());
        vo.setCreatedAt(room.getCreatedAt());

        User creator = userMapper.selectById(room.getCreatorId());
        if (creator != null) {
            vo.setCreatorName(creator.getUsername());
        }

        List<CanvasRoomMember> members = canvasRoomMemberMapper.selectList(
                new LambdaQueryWrapper<CanvasRoomMember>()
                        .eq(CanvasRoomMember::getRoomId, room.getId()));
        vo.setMembers(members.stream().map(m -> {
            CanvasMemberVO mvo = new CanvasMemberVO();
            mvo.setUserId(m.getUserId());
            mvo.setJoinedAt(m.getJoinedAt());
            User u = userMapper.selectById(m.getUserId());
            if (u != null) {
                mvo.setUsername(u.getUsername());
                mvo.setAvatarUrl(u.getAvatarUrl());
            }
            return mvo;
        }).collect(Collectors.toList()));

        return vo;
    }
}
