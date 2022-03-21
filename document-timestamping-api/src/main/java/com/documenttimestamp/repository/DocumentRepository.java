package com.documenttimestamp.repository;

import com.documenttimestamp.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    Optional<Document> findFirstByDocumentChecksum(String documentChecksum);
}
