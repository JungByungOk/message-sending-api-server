package com.msas.monitoring;

import lombok.Data;

@Data
public class MonitoringSummaryDTO {
    private long todaySentCount;
    private long todayDeliveredCount;
    private long todayBounceCount;
    private long todayComplaintCount;
    private double deliveryRate;
    private double bounceRate;
    private long runningBatchCount;
    private long pendingBatchCount;
}
