package com.moderntube.moderntubo_backend.controller.common;

import com.moderntube.moderntubo_backend.annotation.CurrentUser;
import com.moderntube.moderntubo_backend.model.CustomUserDetails;
import com.moderntube.moderntubo_backend.model.payload.request.CommentRequest;
import com.moderntube.moderntubo_backend.model.payload.response.ApiResponse;
import com.moderntube.moderntubo_backend.model.payload.response.CommentResponse;
import com.moderntube.moderntubo_backend.model.payload.response.PagedResponse;
import com.moderntube.moderntubo_backend.model.payload.response.VideoDetailResponse;
import com.moderntube.moderntubo_backend.model.payload.response.VideoUploadResponse;
import com.moderntube.moderntubo_backend.service.CommentService;
import com.moderntube.moderntubo_backend.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/videos")
@AllArgsConstructor
@Slf4j
public class VideoController {

    private final VideoService videoService;
    private final CommentService commentService;

    @Operation(summary = "동영상을 업로드")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequestBody(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> uploadVideo(
            @Parameter(description = "업로드할 동영상 파일", required = true)
            @RequestParam("video") MultipartFile file,
            @CurrentUser CustomUserDetails currentUser
    ) {
        VideoUploadResponse response = videoService.uploadVideo(file, currentUser);
        return ResponseEntity.ok(new ApiResponse(true, response));
    }

    @Operation(summary = "동영상 스트리밍 (Range 요청 기반)")
    @GetMapping("/{id}/stream")
    public ResponseEntity<ResourceRegion> streamVideo(
            @Parameter(description = "스트리밍할 동영상 ID", required = true)
            @PathVariable Long id,
            @RequestHeader HttpHeaders headers
    ) {
        ResourceRegion region = videoService.createRegion(id, headers);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaTypeFactory.getMediaType(region.getResource()).orElse(MediaType.APPLICATION_OCTET_STREAM))
                .body(region);
    }

    @Operation(summary = "동영상 상세 조회")
    @GetMapping("/{id}")
    public ResponseEntity<?> getVideoDetail(@PathVariable Long id) {
        VideoDetailResponse response = videoService.getVideoDetail(id);
        return ResponseEntity.ok(new ApiResponse(true, response));
    }

    @Operation(summary = "댓글 목록 조회")
    @GetMapping("/{id}/comments")
    public ResponseEntity<?> getComments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<CommentResponse> response = commentService.getComments(id, page, size);
        return ResponseEntity.ok(new ApiResponse(true, response));
    }

    @Operation(summary = "댓글 작성")
    @PostMapping("/{id}/comments")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> addComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentRequest request,
            @CurrentUser CustomUserDetails currentUser) {
        commentService.addComment(id, currentUser, request.getContent());
        return ResponseEntity.ok(new ApiResponse(true, "댓글이 등록되었습니다."));
    }

    @Operation(summary = "좋아요 토글")
    @PostMapping("/{id}/like")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> toggleLike(
            @PathVariable Long id,
            @CurrentUser CustomUserDetails currentUser) {
        boolean liked = videoService.toggleLike(id, currentUser);
        return ResponseEntity.ok(new ApiResponse(true, liked ? "좋아요를 눌렀습니다." : "좋아요를 취소했습니다."));
    }
}
