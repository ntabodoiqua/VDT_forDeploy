package com.ntabodoiqua.online_course_management.service;

import com.ntabodoiqua.online_course_management.dto.request.user.UserFilterRequest;
import com.ntabodoiqua.online_course_management.dto.request.user.UserSearchRequest;
import com.ntabodoiqua.online_course_management.dto.request.user.UserUpdateRequest;
import com.ntabodoiqua.online_course_management.dto.response.user.UserResponse;
import com.ntabodoiqua.online_course_management.entity.User;
import com.ntabodoiqua.online_course_management.exception.AppException;
import com.ntabodoiqua.online_course_management.exception.ErrorCode;
import com.ntabodoiqua.online_course_management.mapper.UserMapper;
import com.ntabodoiqua.online_course_management.repository.RoleRepository;
import com.ntabodoiqua.online_course_management.repository.UserRepository;
import com.ntabodoiqua.online_course_management.specification.UserSpecification;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AdminService {
    UserRepository userRepository;
    RoleRepository roleRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

    // Service admin đổi mật khẩu người dùng
    @PreAuthorize("hasRole('ADMIN')")
    public String changeUserPassword(String userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Mã hóa mật khẩu mới
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
        // Ghi log
        log.info("Admin changed password for user {}", user.getUsername());
        return "Password changed successfully for user: " + user.getUsername();
    }

    // Service lấy danh sách người dùng
    // Kiểm tra role Admin mới được phép truy cập
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserResponse> getUsers(Pageable pageable) {
        log.info("Fetching all users with pagination");
        return userRepository.findAll(pageable).map(userMapper::toUserResponse);
    }

    // Service lấy thông tin người dùng theo ID
    // Kiểm tra role Admin mới được phép truy cập
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse getUser(String id){
        log.info("In method get user by Id");
        return userMapper.toUserResponse(userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)));
    }

    // Service xóa người dùng
    // Kiểm tra role Admin hoặc người dùng hiện tại mới được phép xóa
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(String userId){
        userRepository.deleteById(userId);
    }

    // Service cập nhật thông tin người dùng
    // Kiểm tra role Admin mới được phép cập nhật
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateUser(String userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Kiểm tra trùng email (nếu email thay đổi)
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            boolean emailExists = userRepository.existsByEmailAndIdNot(request.getEmail(), userId);
            if (emailExists) {
                throw new AppException(ErrorCode.EMAIL_EXISTED);
            }
        }

        // Kiểm tra trùng số điện thoại (nếu phone thay đổi)
        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            boolean phoneExists = userRepository.existsByPhoneAndIdNot(request.getPhone(), userId);
            if (phoneExists) {
                throw new AppException(ErrorCode.PHONE_EXISTED);
            }
        }

        userMapper.updateUser(user, request);
        var roles = roleRepository.findAllById(request.getRoles());
        user.setRoles(new HashSet<>(roles));

        user.setUpdatedAt(LocalDateTime.now());

        return userMapper.toUserResponse(userRepository.save(user));
    }


    // Service tìm kiếm người dùng theo tên, username
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserResponse> searchUsers(UserFilterRequest filter, Pageable pageable) {
        log.info("Filtering users with: {}", filter);
        return userRepository.findAll(UserSpecification.withFilter(filter), pageable)
                .map(userMapper::toUserResponse);
    }
    // Service enable tài khoản người dùng
    @PreAuthorize("hasRole('ADMIN')")
    public String enableUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Kiểm tra xem người dùng đã được kích hoạt chưa
        if (user.isEnabled()) {
            throw new AppException(ErrorCode.USER_ALREADY_ENABLED);
        }
        user.setEnabled(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Admin enabled user {}", user.getUsername());
        return "User enabled successfully: " + user.getUsername();
    }
    // Service disable tài khoản người dùng
    @PreAuthorize("hasRole('ADMIN')")
    public String disableUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Kiểm tra xem người dùng đã bị vô hiệu hóa chưa
        if (!user.isEnabled()) {
            throw new AppException(ErrorCode.USER_ALREADY_DISABLED);
        }
        // Kiểm tra xem người dùng hiện tại có phải là người dùng đang bị vô hiệu hóa không
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        if (user.getId().equals(currentUser.getId())) {
            log.warn("Admin attempted to disable their own account: {}", user.getUsername());
            throw new AppException(ErrorCode.ADMIN_CANNOT_DELETE_SELF);
        }

        user.setEnabled(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Admin disabled user {}", user.getUsername());
        return "User disabled successfully: " + user.getUsername();
    }
}
