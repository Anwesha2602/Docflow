
package com.docflow.config;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
@EnableScheduling
public class SqsConfig {

    @Bean
    public SqsClient sqsClient() {

        return SqsClient.builder()
                .region(Region.AP_SOUTH_1)
                .credentialsProvider(
                    ProfileCredentialsProvider.create("docflow")
                )
                .build();
    }
}
