package com.msas.onboarding;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingStepDTO {
    private int step;
    private String name;
    private String status;  // COMPLETED, WAITING, PENDING
}
