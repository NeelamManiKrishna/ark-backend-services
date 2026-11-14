package com.app.ark_backend_services.repository;

import com.app.ark_backend_services.model.RefreshSession;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface RefreshSessionRepository extends MongoRepository<RefreshSession, String> {

    Optional<RefreshSession> findByTokenHash(String tokenHash);

    void deleteByUserId(String userId);
}
