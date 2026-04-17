package com.app.ark_backend_services.repository;

import com.app.ark_backend_services.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<User> findByOrganizationId(String organizationId, Pageable pageable);

    Page<User> findByOrganizationIdAndBranchId(String organizationId, String branchId, Pageable pageable);

    Optional<User> findByIdAndOrganizationId(String id, String organizationId);

    long countByOrganizationId(String organizationId);

    long countByOrganizationIdAndRole(String organizationId, User.Role role);

    long countByRole(User.Role role);

    void deleteByOrganizationId(String organizationId);

    void deleteByOrganizationIdAndBranchId(String organizationId, String branchId);
}
