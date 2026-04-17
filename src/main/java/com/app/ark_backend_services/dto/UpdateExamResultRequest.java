package com.app.ark_backend_services.dto;

import com.app.ark_backend_services.model.ExamResult.ResultStatus;
import lombok.Data;

@Data
public class UpdateExamResultRequest {

    private Double marksObtained;

    private String remarks;

    private ResultStatus status;
}
