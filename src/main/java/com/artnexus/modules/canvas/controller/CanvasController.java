package com.artnexus.modules.canvas.controller;

import com.artnexus.common.PageResult;
import com.artnexus.common.Result;
import com.artnexus.modules.canvas.model.dto.*;
import com.artnexus.modules.canvas.service.CanvasService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/canvas")
@RequiredArgsConstructor
public class CanvasController {

    private final CanvasService canvasService;

    @PostMapping("/rooms")
    public Result<CanvasRoomVO> createRoom(@RequestAttribute("userId") Long userId,
                                            @Valid @RequestBody CanvasRoomCreateRequest request) {
        return Result.success(canvasService.createRoom(userId, request));
    }

    @GetMapping("/rooms")
    public Result<PageResult<CanvasRoomVO>> listRooms(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        return Result.success(canvasService.listRooms(page, size));
    }

    @GetMapping("/rooms/{roomId}")
    public Result<CanvasRoomVO> roomDetail(@PathVariable Long roomId) {
        return Result.success(canvasService.getRoomDetail(roomId));
    }

    @PostMapping("/rooms/{roomId}/join")
    public Result<CanvasRoomVO> joinRoom(@PathVariable Long roomId,
                                          @RequestAttribute("userId") Long userId) {
        return Result.success(canvasService.joinRoom(roomId, userId));
    }

    @PostMapping("/rooms/{roomId}/leave")
    public Result<Void> leaveRoom(@PathVariable Long roomId,
                                   @RequestAttribute("userId") Long userId) {
        canvasService.leaveRoom(roomId, userId);
        return Result.success();
    }

    @GetMapping("/rooms/{roomId}/snapshot")
    public Result<String> getSnapshot(@PathVariable Long roomId) {
        return Result.success(canvasService.getSnapshot(roomId));
    }

    @DeleteMapping("/rooms/{roomId}")
    public Result<Void> deleteRoom(@PathVariable Long roomId,
                                   @RequestAttribute("userId") Long userId) {
        canvasService.deleteRoom(roomId, userId);
        return Result.success();
    }
}
