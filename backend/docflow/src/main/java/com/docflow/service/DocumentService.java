
package com.docflow.service;

import com.docflow.entity.Document;
import com.docflow.entity.ProcessingHistory;
import com.docflow.exception.InvalidFileException;
import com.docflow.exception.DocumentNotFoundException;
import com.docflow.repository.DocumentRepository;
import com.docflow.repository.ProcessingHistoryRepository;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.InputStreamResource;

import java.io.InputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.List;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentProcessingService documentProcessingService;
    private final ProcessingHistoryRepository processingHistoryRepository;
    private final S3StorageService s3StorageService;

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentProcessingService documentProcessingService,
            ProcessingHistoryRepository processingHistoryRepository,
            S3StorageService s3StorageService) {

        this.documentRepository = documentRepository;
        this.documentProcessingService = documentProcessingService;
        this.processingHistoryRepository = processingHistoryRepository;
        this.s3StorageService = s3StorageService;
    }

    public Document createDocument(String fileName) {

        Document document = new Document(fileName, "PENDING");

        return documentRepository.save(document);
    }

    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    public Document getDocumentById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));
    }

    public Document uploadDocument(MultipartFile file) {

        // 1. Validate file
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File cannot be empty");
        }

        // 2. Get filename
        String fileName = file.getOriginalFilename();

        // 3. Validate filename
        if (fileName == null || fileName.isBlank()) {
            throw new InvalidFileException("File name cannot be empty");
        }

        // 4. Validate file type
        if (!fileName.toLowerCase().endsWith(".pdf")) {
            throw new InvalidFileException("Only PDF files are allowed");
        }

        try {

            // 5. Generate a unique S3 object key
            String safeFileName = fileName.replaceAll(
                    "[^a-zA-Z0-9._-]", "_"
            );

            String s3Key = "documents/"
                    + UUID.randomUUID()
                    + "_"
                    + safeFileName;

            // 6. Upload the PDF to S3
            try (InputStream inputStream = file.getInputStream()) {

                s3StorageService.uploadFile(
                        s3Key,
                        inputStream,
                        file.getSize()
                );
            }

            // 7. Save document metadata in PostgreSQL
            Document document = new Document();

            document.setFileName(fileName);

            // filePath now stores the S3 object key, not a local path
            document.setFilePath(s3Key);

            document.setStatus("PENDING");

            Document savedDocument = documentRepository.save(document);

            // 8. Start asynchronous processing
            documentProcessingService.processDocument(
                    savedDocument.getId()
            );

            return savedDocument;

        } catch (Exception e) {
            throw new RuntimeException("Could not upload document to S3", e);
        }
    }

    public Resource downloadDocument(Long id) {

        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));

        try {

            // Retrieve the PDF stream from S3
            InputStream inputStream =
                    s3StorageService.downloadFile(document.getFilePath());

            return new InputStreamResource(inputStream);

        } catch (Exception e) {
            throw new RuntimeException("Could not download document from S3", e);
        }
    }

    public Document retryDocument(Long id) {

        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));

        if (!"FAILED".equals(document.getStatus())) {
            throw new RuntimeException(
                    "Only FAILED documents can be retried"
            );
        }

        int attempts = processingHistoryRepository
                .findByDocumentIdOrderByAttemptAsc(id)
                .size();

        if (attempts >= 3) {
            throw new RuntimeException(
                    "Maximum processing attempts reached"
            );
        }

        document.setStatus("PENDING");
        document.setErrorMessage(null);
        document.setPageCount(null);
        document.setFileSize(null);

        Document savedDocument = documentRepository.save(document);

        documentProcessingService.processDocument(savedDocument.getId());

        return savedDocument;
    }

    public List<ProcessingHistory> getProcessingHistory(Long documentId) {

        documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        return processingHistoryRepository
                .findByDocumentIdOrderByAttemptAsc(documentId);
    }
}
