package com.converter.backend.configuration;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class EnvConfig {
    
    @PostConstruct
    public void configureEnv() {
        // Load .env file from project root
        Dotenv dotenv = Dotenv.configure()
                .directory("../")  // Go up from backend/ to project root
                .filename(".env")
                .load();
        
        // Set environment variables for Spring Boot
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });
    }
}
