package com.msas.scheduler.dto;

import lombok.Data;

import java.util.List;

@Data
public class ResponseAllJobStatusDTO {
    private int numOfAllJobs;
    private int numOfGroups;
    private int numOfRunningJobs;
    private List<JobInfoDTO> jobs;
}
