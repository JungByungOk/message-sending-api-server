package com.msas.common.httplog;

import lombok.Data;

import java.util.Map;

@Data
//@ToString(exclude = {"request_body", "response_body"})
public class HttpLogDTO {
    private HttpLogRemoteInfoDTO remoteInfo;
    private String uri;
    private String method;
    private Map<String, String> headers;
    private Map<String, String> queryParameters;
    private String request_body;
    private String response_body;
}