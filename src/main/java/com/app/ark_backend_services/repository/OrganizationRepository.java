package com.app.ark_backend_services.repository;

import com.app.ark_backend_services.model.Organization;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OrganizationRepository extends MongoRepository<Organization, String> {

    Optional<Organization> findByName(String name);

    boolean existsByName(String name);

    long countByStatus(Organization.OrganizationStatus status);
}
