package com.artnexus.modules.artwork.service.impl;

import com.artnexus.common.BusinessException;
import com.artnexus.common.ErrorCode;
import com.artnexus.common.PageResult;
import com.artnexus.modules.artwork.mapper.*;
import com.artnexus.modules.artwork.model.dto.*;
import com.artnexus.modules.artwork.model.entity.*;
import com.artnexus.modules.artwork.service.ArtworkService;
import com.artnexus.modules.user.mapper.UserMapper;
import com.artnexus.modules.user.model.entity.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArtworkServiceImpl implements ArtworkService {

    private final ArtworkMapper artworkMapper;
    private final CommentMapper commentMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public ArtworkVO create(Long userId, ArtworkCreateRequest request) {
        Artwork artwork = new Artwork();
        artwork.setUserId(userId);
        artwork.setTitle(request.getTitle());
        artwork.setDescription(request.getDescription());
        artwork.setImageUrl(request.getImageUrl());
        artwork.setThumbnailUrl(request.getThumbnailUrl());
        artwork.setWidth(request.getWidth());
        artwork.setHeight(request.getHeight());
        artwork.setImageSize(request.getImageSize());
        artwork.setLikeCount(0);
        artwork.setCommentCount(0);
        artwork.setViewCount(0);
        artwork.setStatus(1);
        artworkMapper.insert(artwork);
        return toVO(artwork, userId);
    }

    @Override
    public ArtworkVO getDetail(Long artworkId, Long currentUserId) {
        Artwork artwork = artworkMapper.selectById(artworkId);
        if (artwork == null || artwork.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.ARTWORK_NOT_FOUND);
        }
        // Redis HyperLogLog 去重统计 UV，DB 原子更新 PV
        String viewKey = "artwork:uv:" + artworkId;
        redisTemplate.opsForHyperLogLog().add(viewKey, currentUserId != null ? currentUserId.toString() : "0");
        artworkMapper.update(null, new LambdaUpdateWrapper<Artwork>()
                .eq(Artwork::getId, artworkId)
                .setSql("view_count = view_count + 1"));
        // 重新查询以获取最新数据
        artwork = artworkMapper.selectById(artworkId);
        return toVO(artwork, currentUserId);
    }

    @Override
    public PageResult<ArtworkVO> list(int page, int size, String keyword) {
        LambdaQueryWrapper<Artwork> wrapper = new LambdaQueryWrapper<Artwork>()
                .eq(Artwork::getStatus, 1)
                .orderByDesc(Artwork::getCreatedAt);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Artwork::getTitle, keyword);
        }
        Page<Artwork> pageResult = artworkMapper.selectPage(new Page<>(page, size), wrapper);
        List<ArtworkVO> records = pageResult.getRecords().stream()
                .map(a -> toVO(a, null))
                .collect(Collectors.toList());
        return PageResult.of(pageResult.getTotal(), page, size, records);
    }

    @Override
    public PageResult<ArtworkVO> getUserArtworks(Long userId, int page, int size) {
        LambdaQueryWrapper<Artwork> wrapper = new LambdaQueryWrapper<Artwork>()
                .eq(Artwork::getUserId, userId)
                .eq(Artwork::getStatus, 1)
                .orderByDesc(Artwork::getCreatedAt);
        Page<Artwork> pageResult = artworkMapper.selectPage(new Page<>(page, size), wrapper);
        List<ArtworkVO> records = pageResult.getRecords().stream()
                .map(a -> toVO(a, userId))
                .collect(Collectors.toList());
        return PageResult.of(pageResult.getTotal(), page, size, records);
    }

    @Override
    public ArtworkVO update(Long artworkId, Long userId, ArtworkUpdateRequest request) {
        Artwork artwork = artworkMapper.selectById(artworkId);
        if (artwork == null || artwork.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.ARTWORK_NOT_FOUND);
        }
        if (!artwork.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CANNOT_DELETE_OTHERS_ARTWORK);
        }
        artwork.setTitle(request.getTitle());
        artwork.setDescription(request.getDescription());
        artworkMapper.updateById(artwork);
        return toVO(artwork, userId);
    }

    @Override
    public void delete(Long artworkId, Long userId) {
        Artwork artwork = artworkMapper.selectById(artworkId);
        if (artwork == null || artwork.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.ARTWORK_NOT_FOUND);
        }
        if (!artwork.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CANNOT_DELETE_OTHERS_ARTWORK);
        }
        artworkMapper.deleteById(artworkId);
    }

    @Override
    @Transactional
    public boolean toggleLike(Long artworkId, Long userId) {
        LikeRecord existing = likeRecordMapper.selectOne(new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getArtworkId, artworkId)
                .eq(LikeRecord::getUserId, userId));
        if (existing != null) {
            likeRecordMapper.deleteById(existing.getId());
            // 原子更新：like_count - 1，且不小于0
            artworkMapper.update(null, new LambdaUpdateWrapper<Artwork>()
                    .eq(Artwork::getId, artworkId)
                    .setSql("like_count = GREATEST(like_count - 1, 0)"));
            return false;
        } else {
            LikeRecord record = new LikeRecord();
            record.setArtworkId(artworkId);
            record.setUserId(userId);
            likeRecordMapper.insert(record);
            // 原子更新：like_count + 1
            artworkMapper.update(null, new LambdaUpdateWrapper<Artwork>()
                    .eq(Artwork::getId, artworkId)
                    .setSql("like_count = like_count + 1"));
            return true;
        }
    }

    @Override
    public PageResult<CommentVO> getComments(Long artworkId, int page, int size) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<Comment>()
                .eq(Comment::getArtworkId, artworkId)
                .isNull(Comment::getParentId)
                .orderByDesc(Comment::getCreatedAt);
        Page<Comment> pageResult = commentMapper.selectPage(new Page<>(page, size), wrapper);
        List<CommentVO> records = pageResult.getRecords().stream()
                .map(this::toCommentVO)
                .collect(Collectors.toList());
        return PageResult.of(pageResult.getTotal(), page, size, records);
    }

    @Override
    @Transactional
    public CommentVO createComment(Long artworkId, Long userId, CommentCreateRequest request) {
        Comment comment = new Comment();
        comment.setArtworkId(artworkId);
        comment.setUserId(userId);
        comment.setContent(request.getContent());
        comment.setParentId(request.getParentId());
        comment.setReplyToUserId(request.getReplyToUserId());
        commentMapper.insert(comment);
        // 原子更新评论数
        artworkMapper.update(null, new LambdaUpdateWrapper<Artwork>()
                .eq(Artwork::getId, artworkId)
                .setSql("comment_count = comment_count + 1"));
        return toCommentVO(comment);
    }

    @Override
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        commentMapper.deleteById(commentId);
    }

    private ArtworkVO toVO(Artwork artwork, Long currentUserId) {
        ArtworkVO vo = new ArtworkVO();
        vo.setId(artwork.getId());
        vo.setUserId(artwork.getUserId());
        vo.setTitle(artwork.getTitle());
        vo.setDescription(artwork.getDescription());
        vo.setImageUrl(artwork.getImageUrl());
        vo.setThumbnailUrl(artwork.getThumbnailUrl());
        vo.setWidth(artwork.getWidth());
        vo.setHeight(artwork.getHeight());
        vo.setLikeCount(artwork.getLikeCount());
        vo.setCommentCount(artwork.getCommentCount());
        vo.setViewCount(artwork.getViewCount());
        vo.setCreatedAt(artwork.getCreatedAt());

        User user = userMapper.selectById(artwork.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setUserAvatar(user.getAvatarUrl());
        }

        // 当前用户是否点赞
        if (currentUserId != null) {
            Long count = likeRecordMapper.selectCount(new LambdaQueryWrapper<LikeRecord>()
                    .eq(LikeRecord::getArtworkId, artwork.getId())
                    .eq(LikeRecord::getUserId, currentUserId));
            vo.setLiked(count > 0);
        }
        return vo;
    }

    private CommentVO toCommentVO(Comment comment) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setUserId(comment.getUserId());
        vo.setContent(comment.getContent());
        vo.setReplyToUserId(comment.getReplyToUserId());
        vo.setCreatedAt(comment.getCreatedAt());

        User user = userMapper.selectById(comment.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setUserAvatar(user.getAvatarUrl());
        }
        if (comment.getReplyToUserId() != null) {
            User replyTo = userMapper.selectById(comment.getReplyToUserId());
            if (replyTo != null) {
                vo.setReplyToUsername(replyTo.getUsername());
            }
        }
        return vo;
    }
}
