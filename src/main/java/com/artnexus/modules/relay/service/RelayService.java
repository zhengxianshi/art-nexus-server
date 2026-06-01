package com.artnexus.modules.relay.service;

import com.artnexus.common.PageResult;
import com.artnexus.modules.relay.model.dto.*;

import java.util.List;

public interface RelayService {

    RelayRoomVO createRoom(Long userId, RelayRoomCreateRequest request);

    PageResult<RelayRoomVO> listRooms(int page, int size, String status);

    RelayRoomVO getRoomDetail(Long roomId);

    void joinRoom(Long roomId, Long userId);

    void leaveRoom(Long roomId, Long userId);

    void submitSegment(Long roomId, Long userId, String imageUrl, String hint);

    List<RelaySegmentVO> getGallery(Long roomId);

    void deleteRoom(Long roomId, Long userId);

    RelaySegmentVO getLastSegment(Long roomId);
}
