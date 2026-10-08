package com.docflow.service;

import com.docflow.entity.Document;
import com.docflow.entity.ProcessingHistory;
import com.docflow.repository.BatchRepository;
import com.docflow.repository.DocumentRepository;
import com.docflow.repository.ProcessingHistoryRepository;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import com.docflow.entity.Batch;
import java.util.List;
import com.docflow.repository.BatchRepository;

@Service
public class DocumentProcessingService {

	private static final int MAX_ATTEMPTS = 3;
    private final DocumentRepository documentRepository;
    private final ProcessingHistoryRepository processingHistoryRepository;
    private final BatchRepository batchRepository;
    
    public DocumentProcessingService(
            DocumentRepository documentRepository,
            ProcessingHistoryRepository processingHistoryRepository,
            BatchRepository batchRepository) {

        this.documentRepository = documentRepository;
        this.processingHistoryRepository = processingHistoryRepository;
        this.batchRepository = batchRepository;
    }

    @Async
    public void processDocument(Long documentId) {

        System.out.println("START processing: " + documentId);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() ->
                        new RuntimeException("Document not found: " + documentId));

        // Create a history record for this processing attempt
        ProcessingHistory history = new ProcessingHistory();

        history.setDocumentId(documentId);

        history.setAttempt(
                processingHistoryRepository
                        .findByDocumentIdOrderByAttemptAsc(documentId)
                        .size() + 1
        );

        history.setStatus("PROCESSING");
        history.setStartedAt(LocalDateTime.now());

        processingHistoryRepository.save(history);

        // Update document status
        document.setStatus("PROCESSING");
        document.setErrorMessage(null);

        documentRepository.save(document);

        try {

            // Get physical file
            File file = new File(document.getFilePath());

            // Get file size before PDF processing
            long fileSize = file.length();

            // Open PDF
            PDDocument pdf = Loader.loadPDF(file);

            // Extract number of pages
            int pageCount = pdf.getNumberOfPages();

            // Save extracted information
            document.setPageCount(pageCount);
            document.setFileSize(fileSize);

            // Processing completed
            document.setStatus("COMPLETED");

            documentRepository.save(document);
            updateBatchStatus(document.getBatchId());
            
            history.setStatus("COMPLETED");
            history.setCompletedAt(LocalDateTime.now());

            processingHistoryRepository.save(history);

            // Close PDF
            pdf.close();

            System.out.println(
                    "FINISHED processing: " + documentId
                    + " | Pages: " + pageCount
                    + " | Size: " + fileSize + " bytes"
            );

        } catch (Exception e) {

            document.setStatus("FAILED");

            document.setErrorMessage(
                    "Failed to process document: " + e.getMessage()
            );

            documentRepository.save(document);
            
            // Update processing history
            history.setStatus("FAILED");

            history.setErrorMessage(
                    "Failed to process document: " + e.getMessage()
            );

            history.setCompletedAt(LocalDateTime.now());

            processingHistoryRepository.save(history);

            System.out.println(
                    "FAILED processing: " + documentId
            );

            e.printStackTrace();

            // Check how many attempts have been made
            if (history.getAttempt() < MAX_ATTEMPTS) {

                System.out.println(
                        "Waiting 5 seconds before retrying document: "
                                + documentId
                );

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();

                    System.out.println(
                            "Retry interrupted for document: "
                                    + documentId
                    );

                    return;
                }

                System.out.println(
                        "Retrying document: " + documentId
                                + " | Attempt: " + (history.getAttempt() + 1)
                );

                document.setStatus("PENDING");
                documentRepository.save(document);

                processDocument(documentId);

            } else {

                System.out.println(
                        "Maximum attempts reached for document: "
                                + documentId
                );
                
                updateBatchStatus(document.getBatchId());
            }
            
        }
    }


    
    private void updateBatchStatus(Long batchId) {

        if (batchId == null) {
            return;
        }

        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() ->
                        new RuntimeException("Batch not found: " + batchId));

        List<Document> documents =
                documentRepository.findByBatchId(batchId);

        int completed = 0;
        int failed = 0;

        for (Document document : documents) {

            if ("COMPLETED".equals(document.getStatus())) {
                completed++;
            }

            if ("FAILED".equals(document.getStatus())) {
                failed++;
            }
        }

        batch.setCompletedDocuments(completed);
        batch.setFailedDocuments(failed);

        if (completed + failed == batch.getTotalDocuments()) {

            if (failed > 0) {
                batch.setStatus("COMPLETED_WITH_ERRORS");
            } else {
                batch.setStatus("COMPLETED");
            }

        } else {

            batch.setStatus("PROCESSING");
        }

        batchRepository.save(batch);
    }

}