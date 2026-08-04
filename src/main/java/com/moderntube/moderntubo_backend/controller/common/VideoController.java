package com.moderntube.moderntubo_backend.controller.common;

import com.moderntube.moderntubo_backend.annotation.CurrentUser;
import com.moderntube.moderntubo_backend.model.CustomUserDetails;
import com.moderntube.moderntubo_backend.model.payload.response.ApiResponse;
import com.moderntube.moderntubo_backend.model.payload.response.VideoUploadResponse;
import com.moderntube.moderntubo_backend.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/videos")
@AllArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @Operation(summary = "동영상을 업로드")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequestBody(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> uploadVideo(
            @Parameter(description = "업로드할 동영상 파일", required = true)
            @RequestParam("video") MultipartFile file,
            @CurrentUser CustomUserDetails currentUser) {
        VideoUploadResponse response = videoService.uploadVideo(file, currentUser);
        return ResponseEntity.ok(new ApiResponse(true, response));
    }
}
