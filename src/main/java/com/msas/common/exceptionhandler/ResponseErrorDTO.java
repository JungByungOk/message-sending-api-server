package com.msas.common.exceptionhandler;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

//@Data
@Data
@Builder
public class ResponseErrorDTO {

    private String serviceName;
    private String errorCode;
    private String errorType;
    private String errorMessage;

}
