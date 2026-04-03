package com.msas.tenant.dto;

import lombok.Data;

import java.util.List;

@Data
public class ResponseTenantListDTO {

    private int totalCount;
    private List<ResponseTenantDTO> tenants;
}
