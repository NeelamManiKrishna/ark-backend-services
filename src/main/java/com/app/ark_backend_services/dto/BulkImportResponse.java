package com.app.ark_backend_services.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BulkImportResponse {

    private String entityType;
    private int totalRows;
    private int successCount;
    private int failureCount;
    private List<RowError> errors;

    @Data
    @AllArgsConstructor
    public static class RowError {
        private int row;
        private String message;
    }
}
