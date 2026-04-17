package com.app.ark_backend_services.dto;

import com.app.ark_backend_services.model.ExamSubject;
import com.app.ark_backend_services.model.ExamSubject.ExamSubjectStatus;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
public class ExamSubjectResponse {

    private String id;
    private String examinationId;
    private String organizationId;
    private String branchId;
    private String classId;
    private String subjectName;
    private String subjectCode;
    private Double maxMarks;
    private Double passingMarks;
    private LocalDate examDate;
    private ExamSubjectStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public static ExamSubjectResponse from(ExamSubject es) {
        ExamSubjectResponse response = new ExamSubjectResponse();
        response.setId(es.getId());
        response.setExaminationId(es.getExaminationId());
        response.setOrganizationId(es.getOrganizationId());
        response.setBranchId(es.getBranchId());
        response.setClassId(es.getClassId());
        response.setSubjectName(es.getSubjectName());
        response.setSubjectCode(es.getSubjectCode());
        response.setMaxMarks(es.getMaxMarks());
        response.setPassingMarks(es.getPassingMarks());
        response.setExamDate(es.getExamDate());
        response.setStatus(es.getStatus());
        response.setCreatedAt(es.getCreatedAt());
        response.setUpdatedAt(es.getUpdatedAt());
        return response;
    }
}
