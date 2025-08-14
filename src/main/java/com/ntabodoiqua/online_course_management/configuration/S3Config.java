package com.ntabodoiqua.online_course_management.configuration;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.ntabodoiqua.online_course_management.configuration.properties.DigitalOceanSpacesProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class S3Config {

    private final DigitalOceanSpacesProperties spacesProperties;

    @Bean
    public AmazonS3 s3Client() {
        AWSCredentials credentials = new BasicAWSCredentials(spacesProperties.getAccessKey(), spacesProperties.getSecretKey());
        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(spacesProperties.getEndpointUrl(), spacesProperties.getRegion()))
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .build();
    }
} 