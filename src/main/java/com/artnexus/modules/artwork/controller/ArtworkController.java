package com.artnexus.modules.artwork.controller;

import com.artnexus.common.PageResult;
import com.artnexus.common.Result;
import com.artnexus.modules.artwork.model.dto.*;
import com.artnexus.modules.artwork.service.ArtworkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/artwork")
@RequiredArgsConstructor
public class ArtworkController {

    private final ArtworkService artworkService;

    @PostMapping
    public Result<ArtworkVO> create(@RequestAttribute("userId") Long userId,
                                     @Valid @RequestBody ArtworkCreateRequest request) {
        return Result.success(artworkService.create(userId, request));
    }

    @GetMapping("/list")
    public Result<PageResult<ArtworkVO>> list(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int size,
                                               @RequestParam(required = false) String keyword) {
        return Result.success(artworkService.list(page, size, keyword));
    }

    @GetMapping("/{artworkId}")
    public Result<ArtworkVO> detail(@PathVariable Long artworkId,
                                     @RequestAttribute(value = "userId", required = false) Long userId) {
        return Result.success(artworkService.getDetail(artworkId, userId));
    }

    @GetMapping("/user/{userId}")
    public Result<PageResult<ArtworkVO>> userArtworks(@PathVariable Long userId,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        return Result.success(artworkService.getUserArtworks(userId, page, size));
    }

    @PutMapping("/{artworkId}")
    public Result<ArtworkVO> update(@PathVariable Long artworkId,
                                     @RequestAttribute("userId") Long userId,
                                     @Valid @RequestBody ArtworkUpdateRequest request) {
        return Result.success(artworkService.update(artworkId, userId, request));
    }

    @DeleteMapping("/{artworkId}")
    public Result<Void> delete(@PathVariable Long artworkId,
                                @RequestAttribute("userId") Long userId) {
        artworkService.delete(artworkId, userId);
        return Result.success();
    }

    @PostMapping("/{artworkId}/like")
    public Result<Boolean> toggleLike(@PathVariable Long artworkId,
                                       @RequestAttribute("userId") Long userId) {
        return Result.success(artworkService.toggleLike(artworkId, userId));
    }

    @GetMapping("/{artworkId}/comments")
    public Result<PageResult<CommentVO>> comments(@PathVariable Long artworkId,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return Result.success(artworkService.getComments(artworkId, page, size));
    }

    @PostMapping("/{artworkId}/comments")
    public Result<CommentVO> createComment(@PathVariable Long artworkId,
                                            @RequestAttribute("userId") Long userId,
                                            @Valid @RequestBody CommentCreateRequest request) {
        return Result.success(artworkService.createComment(artworkId, userId, request));
    }

    @DeleteMapping("/comments/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long commentId,
                                       @RequestAttribute("userId") Long userId) {
        artworkService.deleteComment(commentId, userId);
        return Result.success();
    }
}
