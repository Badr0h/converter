package com.converter.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Root API endpoint handler
 * Responds to GET / with API status information
 * Prevents Spring from searching for static files at root path
 */
@RestController
@RequestMapping("")
public class RootController {

    /**
     * Returns API status and version information
     */
    @GetMapping("/")
    public ResponseEntity<Map<String, String>> getApiStatus() {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("message", "ZenithConvert API is running");
        response.put("version", "1.0.7");
        return ResponseEntity.ok(response);
    }

    /**
     * Alternative endpoint without trailing slash
     */
    @GetMapping("")
    public ResponseEntity<Map<String, String>> getApiStatusRoot() {
        return getApiStatus();
    }
}
