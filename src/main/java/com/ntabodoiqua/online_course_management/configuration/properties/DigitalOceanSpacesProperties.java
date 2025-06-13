package com.ntabodoiqua.online_course_management.configuration.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "digitalocean.spaces")
@Data
public class DigitalOceanSpacesProperties {
    private String accessKey;
    private String secretKey;
    private String endpointUrl;
    private String region;
    private String bucketName;
    private String baseUrl;
} 