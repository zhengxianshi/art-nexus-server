package com.artnexus.modules.artwork.service;

import com.artnexus.common.PageResult;
import com.artnexus.modules.artwork.model.dto.*;

public interface ArtworkService {

    ArtworkVO create(Long userId, ArtworkCreateRequest request);

    ArtworkVO getDetail(Long artworkId, Long currentUserId);

    PageResult<ArtworkVO> list(int page, int size, String keyword);

    PageResult<ArtworkVO> getUserArtworks(Long userId, int page, int size);

    void delete(Long artworkId, Long userId);

    ArtworkVO update(Long artworkId, Long userId, ArtworkUpdateRequest request);

    boolean toggleLike(Long artworkId, Long userId);

    PageResult<CommentVO> getComments(Long artworkId, int page, int size);

    CommentVO createComment(Long artworkId, Long userId, CommentCreateRequest request);

    void deleteComment(Long commentId, Long userId);
}
