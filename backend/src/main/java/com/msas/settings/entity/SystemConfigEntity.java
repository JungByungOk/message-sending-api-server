package com.msas.settings.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SystemConfigEntity {
    private String configKey;
    private String configValue;
    private String description;
    private Boolean encrypted;
    private LocalDateTime updatedAt;
}
