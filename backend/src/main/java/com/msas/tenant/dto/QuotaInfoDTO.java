package com.msas.tenant.dto;

import lombok.Data;

@Data
public class QuotaInfoDTO {
    private String tenantId;
    private QuotaDetail daily;
    private QuotaDetail monthly;

    @Data
    public static class QuotaDetail {
        private int limit;
        private int used;
        private int remaining;

        public QuotaDetail(int limit, int used) {
            this.limit = limit;
            this.used = used;
            this.remaining = Math.max(0, limit - used);
        }
    }
}
