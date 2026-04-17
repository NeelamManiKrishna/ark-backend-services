package com.app.ark_backend_services.repository;

import com.app.ark_backend_services.model.ExamSubject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ExamSubjectRepository extends MongoRepository<ExamSubject, String> {

    Optional<ExamSubject> findByIdAndExaminationId(String id, String examinationId);

    Page<ExamSubject> findByExaminationId(String examinationId, Pageable pageable);

    List<ExamSubject> findByExaminationId(String examinationId);

    List<ExamSubject> findByExaminationIdAndClassId(String examinationId, String classId);

    boolean existsByExaminationIdAndSubjectNameAndClassId(String examinationId, String subjectName, String classId);

    void deleteByExaminationId(String examinationId);

    void deleteByOrganizationId(String organizationId);

    void deleteByOrganizationIdAndBranchId(String organizationId, String branchId);
}
