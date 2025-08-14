package com.ntabodoiqua.online_course_management.controller;

import com.ntabodoiqua.online_course_management.dto.request.ApiResponse;
import com.ntabodoiqua.online_course_management.dto.request.user.UserChangePasswordRequest;
import com.ntabodoiqua.online_course_management.dto.request.user.UserFilterRequest;
import com.ntabodoiqua.online_course_management.dto.request.user.UserSearchRequest;
import com.ntabodoiqua.online_course_management.dto.request.user.UserUpdateRequest;
import com.ntabodoiqua.online_course_management.dto.response.user.UserResponse;
import com.ntabodoiqua.online_course_management.exception.AppException;
import com.ntabodoiqua.online_course_management.exception.ErrorCode;
import com.ntabodoiqua.online_course_management.service.AdminService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/manage-users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AdminController {
    AdminService adminService;

    // Controller lấy danh sách người dùng
    @GetMapping
    public ApiResponse<Page<UserResponse>> getUsers(
            @ModelAttribute UserFilterRequest filter,
            Pageable pageable
    ) {
        return ApiResponse.<Page<UserResponse>>builder()
                .result(adminService.searchUsers(filter, pageable))
                .build();
    }

    // Controller lấy thông tin người dùng theo ID
    @GetMapping("/{userId}")
    ApiResponse<UserResponse> getUser(@PathVariable("userId") String userId){
        return ApiResponse.<UserResponse>builder()
                .result(adminService.getUser(userId))
                .build();
    }

    // Controller xóa người dùng theo ID
    @DeleteMapping("/{userId}")
    ApiResponse<String> deleteUser(@PathVariable String userId){
        adminService.deleteUser(userId);
        return ApiResponse.<String>builder()
                .result("User has been deleted")
                .build();
    }

    // Controller cập nhật thông tin người dùng theo ID
    @PutMapping("/{userId}")
    ApiResponse<UserResponse> updateUser(@PathVariable String userId, @RequestBody UserUpdateRequest request){
        return ApiResponse.<UserResponse>builder()
                .result(adminService.updateUser(userId, request))
                .build();
    }

    // Controller thay đổi mật khẩu người dùng
    @PutMapping("/{userId}/change-password")
    ApiResponse<String> changeUserPassword(@PathVariable String userId, @RequestBody @Valid UserChangePasswordRequest request) {
        var result = adminService.changeUserPassword(userId, request.getNewPassword());
        return ApiResponse.<String>builder()
                .result(result)
                .build();
    }

    // Controller enable người dùng
    @PutMapping("/{userId}/enable")
    ApiResponse<String> enableUser(@PathVariable String userId) {
        return ApiResponse.<String>builder()
                .result(adminService.enableUser(userId))
                .build();
    }

    // Controller disable người dùng
    // Lưu ý không thể disable người dùng admin
    @PutMapping("/{userId}/disable")
    ApiResponse<String> disableUser(@PathVariable String userId) {
        return ApiResponse.<String>builder()
                .result(adminService.disableUser(userId))
                .build();
    }
}
