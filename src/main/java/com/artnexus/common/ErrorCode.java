package com.artnexus.common;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // 通用
    SUCCESS(200, "操作成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或Token已过期"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 认证 (1000-1099)
    USERNAME_EXISTS(1001, "用户名已存在"),
    WRONG_PASSWORD(1002, "用户名或密码错误"),
    ACCOUNT_DISABLED(1003, "账号已被禁用"),
    TOKEN_EXPIRED(1004, "Token已过期，请重新登录"),
    REFRESH_TOKEN_INVALID(1005, "刷新Token无效"),

    // 用户 (1100-1199)
    USER_NOT_FOUND(1101, "用户不存在"),

    // 作品 (1200-1299)
    ARTWORK_NOT_FOUND(1201, "作品不存在"),
    CANNOT_DELETE_OTHERS_ARTWORK(1202, "不能删除他人的作品"),

    // 接龙 (1300-1399)
    RELAY_ROOM_NOT_FOUND(1301, "接龙房间不存在"),
    RELAY_ROOM_FULL(1302, "房间已满"),
    RELAY_ROOM_IN_PROGRESS(1303, "房间接龙已开始"),
    RELAY_ALREADY_IN_ROOM(1304, "你已在该房间中"),
    RELAY_NOT_IN_ROOM(1305, "你不在该房间中"),
    RELAY_NOT_YOUR_TURN(1306, "还没轮到你"),
    RELAY_SEGMENT_NOT_FOUND(1307, "接龙片段不存在"),
    RELAY_NOT_CREATOR(1308, "只有房主才能删除房间"),
    RELAY_NO_SEGMENTS(1309, "还没有人提交作品"),

    // 画布 (1400-1499)
    CANVAS_ROOM_NOT_FOUND(1401, "画布房间不存在"),
    CANVAS_ROOM_FULL(1402, "画布房间已满"),
    CANVAS_ALREADY_IN_ROOM(1403, "你已在该房间中"),
    CANVAS_NOT_IN_ROOM(1404, "你不在该房间中"),
    CANVAS_NOT_CREATOR(1405, "只有房主才能删除房间"),

    // 上传 (1500-1599)
    FILE_TOO_LARGE(1501, "文件大小超出限制"),
    FILE_TYPE_NOT_SUPPORTED(1502, "不支持的文件类型"),
    UPLOAD_FAILED(1503, "文件上传失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
