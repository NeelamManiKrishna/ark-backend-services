package com.app.ark_backend_services.repository;

import com.app.ark_backend_services.model.ClassProgression;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ClassProgressionRepository extends MongoRepository<ClassProgression, String> {

    Optional<ClassProgression> findByOrganizationIdAndBranchId(String organizationId, String branchId);

    boolean existsByOrganizationIdAndBranchId(String organizationId, String branchId);

    void deleteByOrganizationId(String organizationId);

    void deleteByOrganizationIdAndBranchId(String organizationId, String branchId);
}
