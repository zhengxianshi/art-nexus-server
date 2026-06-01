package com.artnexus.modules.common.controller;

import com.artnexus.common.ErrorCode;
import com.artnexus.common.Result;
import com.artnexus.util.OssUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final OssUtil ossUtil;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"
    );
    private static final long MAX_SIZE = 20 * 1024 * 1024; // 20MB

    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file,
                                       @RequestParam(defaultValue = "artwork") String dir) {
        if (file.isEmpty()) {
            return Result.error(ErrorCode.PARAM_ERROR.getCode(), "文件为空");
        }
        if (file.getSize() > MAX_SIZE) {
            return Result.error(ErrorCode.FILE_TOO_LARGE);
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            return Result.error(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
        }
        String url = ossUtil.upload(file, dir);
        return Result.success(url);
    }
}
