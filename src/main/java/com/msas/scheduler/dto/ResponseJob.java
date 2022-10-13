package com.msas.scheduler.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseJob {
    private String jobName;
    private String groupName;
    private String jobStatus;
    private String scheduleTime;
    private String lastFiredTime;
    private String nextFireTime;
}
