package com.campus.parking.common.controller;

import com.campus.parking.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> healthCheck() {
        return ApiResponse.ok("Smart Campus Parking Backend is UP", Map.of(
                "status", "HEALTHY",
                "system", "Modular Monolith",
                "version", "1.0.0"
        ));
    }
}