package com.msas.common.httplog;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HttpLogRemoteInfoDTO {
    private String RemoteIp;
    private String RemoteHost;
    private int RemotePort;
}
