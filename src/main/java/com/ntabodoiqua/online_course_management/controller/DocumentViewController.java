package com.ntabodoiqua.online_course_management.controller;

import com.ntabodoiqua.online_course_management.dto.request.ApiResponse;
import com.ntabodoiqua.online_course_management.service.ProgressService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/document-views")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DocumentViewController {
    
    ProgressService progressService;
    
    @PostMapping("/track")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<String> trackDocumentView(@RequestParam String documentId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        // Cần getUserId từ username - tạm thời pass username thay vì userId
        progressService.trackDocumentView(username, documentId);
        
        return ApiResponse.<String>builder()
                .message("Document view tracked successfully")
                .result("success")
                .build();
    }
} 