package com.msas.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorDetail error;

    private ApiResponse(boolean success, T data, ErrorDetail error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(true, null, null);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorDetail(code, message));
    }

    public static <T> ApiResponse<T> error(String code, String message, Object details) {
        return new ApiResponse<>(false, null, new ErrorDetail(code, message, details));
    }

    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetail {
        private final String code;
        private final String message;
        private final Object details;

        public ErrorDetail(String code, String message) {
            this(code, message, null);
        }

        public ErrorDetail(String code, String message, Object details) {
            this.code = code;
            this.message = message;
            this.details = details;
        }
    }
}
