package com.app.ark_backend_services.dto;

import lombok.Data;

import java.util.List;

@Data
public class PromotionExecuteResponse {

    private String sourceClassId;
    private String sourceClassName;
    private String targetClassId;
    private String targetClassName;
    private String sourceAcademicYear;
    private String targetAcademicYear;
    private PromotionSummary summary;
    private List<PromotionRecordDto> records;

    @Data
    public static class PromotionSummary {
        private int totalProcessed;
        private int promoted;
        private int graduated;
        private int heldBack;
    }

    @Data
    public static class PromotionRecordDto {
        private String studentId;
        private String studentArkId;
        private String studentName;
        private String promotionType;
        private String reason;
        private String targetClassId;
    }
}
