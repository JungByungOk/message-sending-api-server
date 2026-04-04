package com.msas.settings.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AwsTestResultDTO {
    private boolean connected;
    private String message;
    private int statusCode;
}
