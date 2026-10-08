package com.docflow.repository;

import com.docflow.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
	List<Document> findByBatchId(Long batchId);
}