package com.msas.scheduler.dto;

import lombok.Data;

import java.util.List;

@Data
public class ResponseJobStatus {
    private int numOfAllJobs;
    private int numOfGroups;
    private int numOfRunningJobs;
    private List<ResponseJob> jobs;
}
