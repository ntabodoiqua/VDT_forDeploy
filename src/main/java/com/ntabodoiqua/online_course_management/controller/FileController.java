package com.ntabodoiqua.online_course_management.controller;

import com.ntabodoiqua.online_course_management.dto.request.ApiResponse;
import com.ntabodoiqua.online_course_management.dto.response.document.FileUsageResponse;
import com.ntabodoiqua.online_course_management.entity.UploadedFile;
import com.ntabodoiqua.online_course_management.entity.User;
import com.ntabodoiqua.online_course_management.exception.AppException;
import com.ntabodoiqua.online_course_management.exception.ErrorCode;
import com.ntabodoiqua.online_course_management.repository.UploadedFileRepository;
import com.ntabodoiqua.online_course_management.repository.UserRepository;
import com.ntabodoiqua.online_course_management.service.UserService;
import com.ntabodoiqua.online_course_management.service.file.FileStorageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class FileController {
    UploadedFileRepository uploadedFileRepository;
    UserRepository userRepository;
    FileStorageService fileStorageService;
    UserService userService;

    @PostMapping("/upload")
    public ApiResponse<String> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam boolean isPublic) {
        UploadedFile uploadedFile = fileStorageService.storeFile(file, isPublic);
        log.info("File upload process initiated for: {}", uploadedFile.getFileName());
        return ApiResponse.<String>builder()
                .result("File uploaded successfully: " + uploadedFile.getFileName())
                .build();
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        UploadedFile file = uploadedFileRepository.findByFileName(fileName)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));

        Resource resource = fileStorageService.loadFile(fileName, file.isPublic());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .body(resource);
    }

    // Controller chuyển file từ private sang public
    @PutMapping("/make-public/{fileName}")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    public ApiResponse<String> makeFilePublic(@PathVariable String fileName) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        String result = fileStorageService.makeFilePublic(fileName, username);

        return ApiResponse.<String>builder()
                .result(result)
                .build();
    }

    // Controller lấy tất cả ảnh công khai của người dùng
    @GetMapping("/all-images-of-user")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    public ApiResponse<List<String>> getAllImagesOfUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<String> imageUrls = fileStorageService.getAllPublicImagesOfUser(username);

        return ApiResponse.<List<String>>builder()
                .message("Public images fetched successfully")
                .result(imageUrls)
                .build();
    }

    @DeleteMapping("/{fileName}")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    public ApiResponse<Void> deleteFile(@PathVariable String fileName) {
        fileStorageService.deleteFile(fileName);
        return ApiResponse.<Void>builder()
                .message("File deleted successfully")
                .build();
    }

    @GetMapping("/all-files-of-user")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    public ApiResponse<Page<UploadedFile>> getAllFilesOfUser(
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String fileName,
            Pageable pageable) {
        Page<UploadedFile> files = fileStorageService.getAllFilesOfUser(contentType, fileName, pageable);
        return ApiResponse.<Page<UploadedFile>>builder()
                .message("Files fetched successfully")
                .result(files)
                .build();
    }

    @GetMapping("/check-usage/{fileName}")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    public ApiResponse<FileUsageResponse> checkFileUsage(@PathVariable String fileName) {
        FileUsageResponse usage = fileStorageService.checkFileUsage(fileName);
        return ApiResponse.<FileUsageResponse>builder()
                .message("File usage checked successfully")
                .result(usage)
                .build();
    }

    // API lấy tất cả file đã upload của người dùng
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<UploadedFile>> getAllUploadedFiles(
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String uploaderName,
            Pageable pageable) {
        Page<UploadedFile> files = fileStorageService.getAllUploadedFiles(contentType, fileName, uploaderName, pageable);
        return ApiResponse.<Page<UploadedFile>>builder()
                .message("All uploaded files fetched successfully")
                .result(files)
                .build();
    }

}
