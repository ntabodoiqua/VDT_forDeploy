package com.ntabodoiqua.online_course_management.configuration;

import com.ntabodoiqua.online_course_management.interceptor.UserStatusInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UrlPathHelper;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final UserStatusInterceptor userStatusInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Chỉ map URL /uploads/public/** tới thư mục uploads/public/
        registry.addResourceHandler("/uploads/public/**")
                .addResourceLocations("file:uploads/public/");
        
        // Không map /uploads/private/** để buộc phải qua controller với authentication
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        UrlPathHelper urlPathHelper = new UrlPathHelper();
        urlPathHelper.setAlwaysUseFullPath(true);
        urlPathHelper.setUrlDecode(true);
        configurer.setUrlPathHelper(urlPathHelper);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173/") // Địa chỉ frontend
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userStatusInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/token",
                        "/auth/introspect",
                        "/auth/logout",
                        "/auth/refresh",
                        "/users",
                        "/category/get-categories",
                        "/category",
                        "/uploads/public/**",
                        "/files/download/**",
                        "/courses/**",
                        "/lessons",
                        "/instructors/public/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/course-reviews/approved/**",
                        "/course-reviews/public/**",
                        "**/public/**"
                );
    }
}
