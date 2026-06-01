-- =====================================================
-- 艺术纽带 (Art Nexus) 数据库初始化脚本
-- MySQL 8.0+
-- =====================================================

CREATE DATABASE IF NOT EXISTS art_nexus
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE art_nexus;

-- =====================================================
-- 1. 用户表
-- =====================================================
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(32) NOT NULL COMMENT '用户名',
    `password` VARCHAR(128) NOT NULL COMMENT 'BCrypt加密后的密码',
    `nickname` VARCHAR(32) DEFAULT NULL COMMENT '昵称',
    `avatar_url` VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
    `bio` VARCHAR(255) DEFAULT NULL COMMENT '个人简介',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1=正常 0=禁用',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=未删除 1=已删除',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- =====================================================
-- 2. 作品表
-- =====================================================
CREATE TABLE IF NOT EXISTS `artwork` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '作者ID',
    `title` VARCHAR(100) NOT NULL COMMENT '作品标题',
    `description` VARCHAR(2000) DEFAULT NULL COMMENT '作品描述',
    `image_url` VARCHAR(512) NOT NULL COMMENT '原图URL',
    `thumbnail_url` VARCHAR(512) DEFAULT NULL COMMENT '缩略图URL',
    `width` INT DEFAULT NULL COMMENT '图片宽度',
    `height` INT DEFAULT NULL COMMENT '图片高度',
    `image_size` BIGINT DEFAULT NULL COMMENT '文件大小(字节)',
    `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    `comment_count` INT NOT NULL DEFAULT 0 COMMENT '评论数',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览数',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1=公开 0=隐藏',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_like_count` (`like_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作品表';

-- =====================================================
-- 3. 评论表
-- =====================================================
CREATE TABLE IF NOT EXISTS `comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `artwork_id` BIGINT NOT NULL COMMENT '作品ID',
    `user_id` BIGINT NOT NULL COMMENT '评论者ID',
    `content` VARCHAR(500) NOT NULL COMMENT '评论内容',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父评论ID(楼中楼)',
    `reply_to_user_id` BIGINT DEFAULT NULL COMMENT '回复的用户ID',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_artwork_id` (`artwork_id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

-- =====================================================
-- 4. 点赞记录表
-- =====================================================
CREATE TABLE IF NOT EXISTS `like_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `artwork_id` BIGINT NOT NULL COMMENT '作品ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_artwork` (`user_id`, `artwork_id`),
    KEY `idx_artwork_id` (`artwork_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点赞记录表';

-- =====================================================
-- 5. 接龙房间表
-- =====================================================
CREATE TABLE IF NOT EXISTS `relay_room` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `creator_id` BIGINT NOT NULL COMMENT '房主ID',
    `title` VARCHAR(50) NOT NULL COMMENT '房间标题',
    `cover_url` VARCHAR(512) DEFAULT NULL COMMENT '封面图URL',
    `max_members` INT NOT NULL COMMENT '人数上限(5-10)',
    `current_count` INT NOT NULL DEFAULT 0 COMMENT '当前人数',
    `status` VARCHAR(20) NOT NULL DEFAULT 'WAITING' COMMENT '状态: WAITING/IN_PROGRESS/FINISHED',
    `current_turn` INT NOT NULL DEFAULT 0 COMMENT '当前轮次(1-based)',
    `prompt` VARCHAR(200) DEFAULT NULL COMMENT '初始提示词',
    `deleted` TINYINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_creator_id` (`creator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='接龙房间表';

-- =====================================================
-- 6. 接龙房间成员表
-- =====================================================
CREATE TABLE IF NOT EXISTS `relay_room_member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `room_id` BIGINT NOT NULL COMMENT '房间ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `join_order` INT NOT NULL COMMENT '加入顺序(1-based)',
    `submitted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已提交本轮作品',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_room_user` (`room_id`, `user_id`),
    KEY `idx_room_id` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='接龙房间成员表';

-- =====================================================
-- 7. 接龙片段表
-- =====================================================
CREATE TABLE IF NOT EXISTS `relay_segment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `room_id` BIGINT NOT NULL COMMENT '房间ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `turn_number` INT NOT NULL COMMENT '第几轮',
    `image_url` VARCHAR(512) NOT NULL COMMENT '绘画图片URL',
    `hint` VARCHAR(200) DEFAULT NULL COMMENT '留给下一人的提示词',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_room_id` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='接龙片段表';

-- =====================================================
-- 8. 画布房间表
-- =====================================================
CREATE TABLE IF NOT EXISTS `canvas_room` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `creator_id` BIGINT NOT NULL COMMENT '房主ID',
    `title` VARCHAR(50) NOT NULL COMMENT '房间标题',
    `max_members` INT NOT NULL DEFAULT 5 COMMENT '人数上限(最多5)',
    `current_count` INT NOT NULL DEFAULT 0 COMMENT '当前人数',
    `status` VARCHAR(20) NOT NULL DEFAULT 'WAITING' COMMENT '状态: WAITING/ACTIVE/CLOSED',
    `last_snapshot_id` BIGINT DEFAULT NULL COMMENT '最近一次快照ID',
    `deleted` TINYINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_creator_id` (`creator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='画布房间表';

-- =====================================================
-- 9. 画布房间成员表
-- =====================================================
CREATE TABLE IF NOT EXISTS `canvas_room_member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `room_id` BIGINT NOT NULL COMMENT '房间ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `joined_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_room_user` (`room_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='画布房间成员表';

-- =====================================================
-- 10. 画布快照表
-- =====================================================
CREATE TABLE IF NOT EXISTS `canvas_snapshot` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `room_id` BIGINT NOT NULL COMMENT '房间ID',
    `snapshot_data` JSON NOT NULL COMMENT '快照JSON数据 {layers:[...]}',
    `last_op_seq` BIGINT NOT NULL DEFAULT 0 COMMENT '生成快照时的操作序号',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_room_id` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='画布快照表';
