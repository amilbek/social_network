package com.project.social_network.logger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggingConfiguration {

    @Bean
    public RequestAndResponseLoggingFilter requestResponseLoggingFilter() {
        return new RequestAndResponseLoggingFilter();
    }
}