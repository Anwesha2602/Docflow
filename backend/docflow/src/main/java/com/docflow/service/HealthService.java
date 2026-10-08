package com.docflow.service;

import org.springframework.stereotype.Service;

@Service
public class HealthService {

    public String getHealthStatus() {
        return "DocFlow is running!";
    }
}