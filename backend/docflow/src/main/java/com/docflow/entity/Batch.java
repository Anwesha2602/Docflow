package com.docflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Batch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String status;

    private Integer totalDocuments;

    private Integer completedDocuments;

    private Integer failedDocuments;

    private LocalDateTime createdAt;

    public Batch() {
    }

    public Long getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTotalDocuments() {
        return totalDocuments;
    }

    public void setTotalDocuments(Integer totalDocuments) {
        this.totalDocuments = totalDocuments;
    }

    public Integer getCompletedDocuments() {
        return completedDocuments;
    }

    public void setCompletedDocuments(Integer completedDocuments) {
        this.completedDocuments = completedDocuments;
    }

    public Integer getFailedDocuments() {
        return failedDocuments;
    }

    public void setFailedDocuments(Integer failedDocuments) {
        this.failedDocuments = failedDocuments;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}