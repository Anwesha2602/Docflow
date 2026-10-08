package com.docflow.repository;

import com.docflow.entity.ProcessingHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcessingHistoryRepository
        extends JpaRepository<ProcessingHistory, Long> {

    List<ProcessingHistory> findByDocumentIdOrderByAttemptAsc(Long documentId);
}