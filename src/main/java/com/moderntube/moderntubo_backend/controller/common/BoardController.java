package com.moderntube.moderntubo_backend.controller.common;

import com.moderntube.moderntubo_backend.model.payload.response.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@AllArgsConstructor
@RequestMapping("/api/board")
public class BoardController {

    public ResponseEntity<?> boardList() {
        return ResponseEntity.ok(new ApiResponse(true, "제작중..."));
    }

    public ResponseEntity<?> boardView() {
        return ResponseEntity.ok(new ApiResponse(true, "제작중..."));
    }
}
