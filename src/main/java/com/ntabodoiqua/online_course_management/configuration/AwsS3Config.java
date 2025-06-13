package com.ntabodoiqua.online_course_management.configuration;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AwsS3Config {

    @Value("${digitalocean.spaces.access-key}")
    private String accessKey;

    @Value("${digitalocean.spaces.secret-key}")
    private String secretKey;

    @Value("${digitalocean.spaces.endpoint-url}")
    private String endpointUrl;

    @Value("${digitalocean.spaces.region}")
    private String region;

    @Bean
    public AmazonS3 spacesS3Client() {
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        
        AwsClientBuilder.EndpointConfiguration endpointConfig = 
            new AwsClientBuilder.EndpointConfiguration(endpointUrl, region);

        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(endpointConfig)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .build();
    }
}