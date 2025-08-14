package com.ntabodoiqua.online_course_management.controller;

import com.ntabodoiqua.online_course_management.dto.request.ApiResponse;
import com.ntabodoiqua.online_course_management.dto.request.category.CategoryFilterRequest;
import com.ntabodoiqua.online_course_management.dto.request.course.CategoryRequest;
import com.ntabodoiqua.online_course_management.dto.response.course.CategoryResponse;
import com.ntabodoiqua.online_course_management.service.CategoryService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CategoryController {
    CategoryService categoryService;
    // Lấy danh sách danh mục với các tham số tìm kiếm và phân trang
    @GetMapping
    public ApiResponse<Page<CategoryResponse>> getCategories(
            @ModelAttribute CategoryFilterRequest filter,
            Pageable pageable) {

        Page<CategoryResponse> categories = categoryService.searchCategories(filter, pageable);
        return ApiResponse.<Page<CategoryResponse>>builder()
                .result(categories)
                .build();
    }

    // Lấy danh sách danh mục công khai
    @GetMapping("/public")
    public ApiResponse<Page<CategoryResponse>> getPublicCategories(
            @ModelAttribute CategoryFilterRequest filter,
            Pageable pageable) {

        Page<CategoryResponse> categories = categoryService.searchCategories(filter, pageable);
        return ApiResponse.<Page<CategoryResponse>>builder()
                .result(categories)
                .build();
    }

    // Tạo danh mục mới
    @PostMapping()
    public ApiResponse<CategoryResponse> createCategory(@RequestBody @Valid CategoryRequest categoryRequest) {
        log.info("Creating new category: {}", categoryRequest.getName());
        CategoryResponse createdCategory = categoryService.createCategory(categoryRequest);
        return ApiResponse.<CategoryResponse>builder()
                .message("Category created successfully")
                .result(createdCategory)
                .build();
    }

    // Cập nhật danh mục theo ID
    @PutMapping("/{categoryId}")
    public ApiResponse<CategoryResponse> updateCategory(@PathVariable String categoryId,
                                                        @RequestBody @Valid CategoryRequest categoryRequest) {
        log.info("Updating category with ID: {}", categoryId);
        CategoryResponse updatedCategory = categoryService.updateCategory(categoryId, categoryRequest);
        return ApiResponse.<CategoryResponse>builder()
                .message("Category updated successfully")
                .result(updatedCategory)
                .build();
    }

    // Xóa danh mục theo ID
    @DeleteMapping("/{categoryId}")
    public ApiResponse<String> deleteCategory(@PathVariable String categoryId) {
        log.info("Deleting category with ID: {}", categoryId);
        categoryService.deleteCategory(categoryId);
        return ApiResponse.<String>builder()
                .message("Category deleted successfully")
                .result("Category with ID " + categoryId + " has been deleted.")
                .build();
    }

    // Lấy thông tin danh mục theo ID
    @GetMapping("/{categoryId}")
    public ApiResponse<CategoryResponse> getCategoryById(@PathVariable String categoryId) {
        log.info("Fetching category with ID: {}", categoryId);
        CategoryResponse category = categoryService.getCategoryById(categoryId);
        return ApiResponse.<CategoryResponse>builder()
                .message("Category fetched successfully")
                .result(category)
                .build();
    }
}
