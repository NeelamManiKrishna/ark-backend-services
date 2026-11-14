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

    Page<ExamResult> findByExaminationIdAndStudentId(String examinationId, String studentId, Pageable pageable);

    List<ExamResult> findByExamSubjectId(String examSubjectId);

    List<ExamResult> findByExamSubjectIdIn(java.util.Collection<String> examSubjectIds);

    List<ExamResult> findByExaminationIdIn(java.util.Collection<String> examinationIds);

    Page<ExamResult> findByExamSubjectId(String examSubjectId, Pageable pageable);

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
