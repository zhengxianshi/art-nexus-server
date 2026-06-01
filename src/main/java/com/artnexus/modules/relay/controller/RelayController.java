package com.artnexus.modules.relay.controller;

import com.artnexus.common.PageResult;
import com.artnexus.common.Result;
import com.artnexus.modules.relay.model.dto.*;
import com.artnexus.modules.relay.service.RelayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/relay")
@RequiredArgsConstructor
public class RelayController {

    private final RelayService relayService;

    @PostMapping("/rooms")
    public Result<RelayRoomVO> createRoom(@RequestAttribute("userId") Long userId,
                                           @Valid @RequestBody RelayRoomCreateRequest request) {
        return Result.success(relayService.createRoom(userId, request));
    }

    @GetMapping("/rooms")
    public Result<PageResult<RelayRoomVO>> listRooms(@RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "20") int size,
                                                      @RequestParam(required = false) String status) {
        return Result.success(relayService.listRooms(page, size, status));
    }

    @GetMapping("/rooms/{roomId}")
    public Result<RelayRoomVO> roomDetail(@PathVariable Long roomId) {
        return Result.success(relayService.getRoomDetail(roomId));
    }

    @PostMapping("/rooms/{roomId}/join")
    public Result<Void> joinRoom(@PathVariable Long roomId,
                                  @RequestAttribute("userId") Long userId) {
        relayService.joinRoom(roomId, userId);
        return Result.success();
    }

    @PostMapping("/rooms/{roomId}/leave")
    public Result<Void> leaveRoom(@PathVariable Long roomId,
                                   @RequestAttribute("userId") Long userId) {
        relayService.leaveRoom(roomId, userId);
        return Result.success();
    }

    @PostMapping("/rooms/{roomId}/submit")
    public Result<Void> submitSegment(@PathVariable Long roomId,
                                       @RequestAttribute("userId") Long userId,
                                       @RequestBody RelaySegmentSubmitRequest request) {
        relayService.submitSegment(roomId, userId, request.getImageUrl(), request.getHint());
        return Result.success();
    }

    @GetMapping("/rooms/{roomId}/gallery")
    public Result<List<RelaySegmentVO>> gallery(@PathVariable Long roomId) {
        return Result.success(relayService.getGallery(roomId));
    }

    @DeleteMapping("/rooms/{roomId}")
    public Result<Void> deleteRoom(@PathVariable Long roomId,
                                   @RequestAttribute("userId") Long userId) {
        relayService.deleteRoom(roomId, userId);
        return Result.success();
    }

    @GetMapping("/rooms/{roomId}/last-segment")
    public Result<RelaySegmentVO> getLastSegment(@PathVariable Long roomId) {
        return Result.success(relayService.getLastSegment(roomId));
    }
}
