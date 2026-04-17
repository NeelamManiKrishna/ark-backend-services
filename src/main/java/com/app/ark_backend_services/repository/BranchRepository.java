package com.app.ark_backend_services.repository;

import com.app.ark_backend_services.model.Branch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface BranchRepository extends MongoRepository<Branch, String> {

    Page<Branch> findByOrganizationId(String organizationId, Pageable pageable);

    Optional<Branch> findByIdAndOrganizationId(String id, String organizationId);

    boolean existsByOrganizationIdAndName(String organizationId, String name);

    long countByOrganizationId(String organizationId);

    java.util.List<Branch> findByOrganizationId(String organizationId);

    void deleteByOrganizationId(String organizationId);
}
