package com.meetback.dev.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final Path feedImageUploadDirectory;

    public WebConfig(
            @Value("${feed.image.upload-dir}")
            String feedImageUploadDirectory
    ) {

        this.feedImageUploadDirectory =
                Paths.get(
                                feedImageUploadDirectory
                        )
                        .toAbsolutePath()
                        .normalize();
    }

    @Override
    public void addResourceHandlers(
            ResourceHandlerRegistry registry
    ) {

        registry
                .addResourceHandler(
                        "/uploads/feed/**"
                )
                .addResourceLocations(
                        feedImageUploadDirectory
                                .toUri()
                                .toString()
                );
    }
}