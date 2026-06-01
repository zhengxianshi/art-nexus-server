package com.artnexus.modules.canvas.service;

import com.artnexus.common.PageResult;
import com.artnexus.modules.canvas.model.dto.*;

public interface CanvasService {

    CanvasRoomVO createRoom(Long userId, CanvasRoomCreateRequest request);

    PageResult<CanvasRoomVO> listRooms(int page, int size);

    CanvasRoomVO getRoomDetail(Long roomId);

    CanvasRoomVO joinRoom(Long roomId, Long userId);

    void leaveRoom(Long roomId, Long userId);

    String getSnapshot(Long roomId);

    void deleteRoom(Long roomId, Long userId);
}
