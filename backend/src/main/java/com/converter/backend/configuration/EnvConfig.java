package com.converter.backend.configuration;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class EnvConfig {

    @PostConstruct
    public void configureEnv() {
        // Charger le .env depuis le working directory courant (./)
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing() // ne plante pas si fichier manquant
                .load();

        // Set environment variables for Spring Boot
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });

        System.out.println("ENV variables loaded: " + dotenv.entries().size());
    }
}
