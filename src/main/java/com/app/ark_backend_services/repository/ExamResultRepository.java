package com.app.ark_backend_services.repository;

import com.app.ark_backend_services.model.ExamResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ExamResultRepository extends MongoRepository<ExamResult, String> {

    Optional<ExamResult> findByIdAndOrganizationId(String id, String organizationId);

    Page<ExamResult> findByExaminationIdAndClassId(String examinationId, String classId, Pageable pageable);

    List<ExamResult> findByExaminationIdAndStudentId(String examinationId, String studentId);

    List<ExamResult> findByExamSubjectId(String examSubjectId);

    Page<ExamResult> findByExaminationId(String examinationId, Pageable pageable);

    boolean existsByExamSubjectIdAndStudentId(String examSubjectId, String studentId);

    void deleteByExaminationId(String examinationId);

    void deleteByExamSubjectId(String examSubjectId);

    long countByExaminationId(String examinationId);

    long countByExaminationIdAndStatus(String examinationId, ExamResult.ResultStatus status);

    void deleteByStudentId(String studentId);

    void deleteByOrganizationId(String organizationId);

    void deleteByOrganizationIdAndBranchId(String organizationId, String branchId);
}
