package com.app.ark_backend_services.dto;

import lombok.Data;

import java.util.List;

@Data
public class PromotionPreviewResponse {

    private SourceClassInfo sourceClass;
    private String targetClassName;
    private String targetAcademicYear;
    private boolean isTerminalClass;
    private int totalEligible;
    private int totalRecommendedPromote;
    private int totalRecommendedHoldBack;
    private int totalRecommendedGraduate;
    private int totalNoExamData;
    private List<PromotionCandidate> candidates;

    @Data
    public static class SourceClassInfo {
        private String id;
        private String name;
        private String section;
        private String academicYear;
        private String branchId;
        private String status;
    }

    @Data
    public static class PromotionCandidate {
        private String studentId;
        private String studentArkId;
        private String firstName;
        private String lastName;
        private String rollNumber;
        private String recommendation;
        private boolean hasFailingResults;
        private List<String> failedSubjects;
        private String examSummary;
        private boolean hasExamData;
    }
}
