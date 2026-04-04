package com.msas.onboarding;

import lombok.Data;

import java.util.List;

@Data
public class OnboardingStatusDTO {
    private String tenantId;
    private String domain;
    private List<OnboardingStepDTO> steps;
    private String verificationStatus;
    private String tenantStatus;
}
