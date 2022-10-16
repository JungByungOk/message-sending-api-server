package com.msas.common.httplogfilter;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HttpLogRemoteInfoDTO {
    private String RemoteIp;
    private String RemoteHost;
    private int RemotePort;
}
