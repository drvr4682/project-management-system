package com.pms.projectservice.service;

import org.springframework.stereotype.Service;

@Service
public class ProjectService {

    public String healthCheck() {
        return "Project Service is running";
    }
}