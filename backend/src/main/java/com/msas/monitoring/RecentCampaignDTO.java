package com.msas.monitoring;

import lombok.Data;

@Data
public class RecentCampaignDTO {
    private String batchId;
    private String templateName;
    private String startDateAt;
    private String status;
    private boolean isComplete;
    private long totalSent;
    private long delivered;
    private double deliveryRate;
    private long opens;
    private double openRate;
    private long clicks;
    private double clickRate;
    private long complaints;
    private double complaintRate;
    private double prevDeliveryRate;
    private double prevOpenRate;
    private double prevClickRate;
    private double prevComplaintRate;
}
