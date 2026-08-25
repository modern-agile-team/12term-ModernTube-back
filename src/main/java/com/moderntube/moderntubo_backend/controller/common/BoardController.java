package com.moderntube.moderntubo_backend.controller.common;

import com.moderntube.moderntubo_backend.model.payload.response.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@AllArgsConstructor
@RequestMapping("/api/board")
public class BoardController {

    @GetMapping("/list")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> boardList() {
        return ResponseEntity.ok(new ApiResponse(true, "제작중..."));
    }

    @GetMapping("/view")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> boardView() {
        
        return ResponseEntity.ok(new ApiResponse(true, "제작중..."));
    }
}
