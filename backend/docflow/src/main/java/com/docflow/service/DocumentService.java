
package com.docflow.service;
import com.docflow.entity.User;
import com.docflow.entity.Document;
import com.docflow.entity.ProcessingHistory;
import com.docflow.exception.InvalidFileException;
import com.docflow.exception.DocumentNotFoundException;
import com.docflow.repository.DocumentRepository;
import com.docflow.repository.ProcessingHistoryRepository;
import com.docflow.messaging.SqsMessageProducer;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.InputStreamResource;

import java.io.InputStream;
import java.util.UUID;
import java.util.List;
import com.docflow.entity.User;
import com.docflow.entity.Role;
import org.springframework.security.access.AccessDeniedException;
@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ProcessingHistoryRepository processingHistoryRepository;
    private final S3StorageService s3StorageService;
    private final SqsMessageProducer sqsMessageProducer;

    public DocumentService(
            DocumentRepository documentRepository,
            ProcessingHistoryRepository processingHistoryRepository,
            S3StorageService s3StorageService,
            SqsMessageProducer sqsMessageProducer) {

        this.documentRepository = documentRepository;
        this.processingHistoryRepository = processingHistoryRepository;
        this.s3StorageService = s3StorageService;
        this.sqsMessageProducer = sqsMessageProducer;
    }

    public Document createDocument(String fileName, User user) {

        Document document = new Document(fileName, "PENDING");
        document.setOwner(user);

        return documentRepository.save(document);
    }

    public List<Document> getAllDocuments(User user) {

        if (user.getRole() == Role.ADMIN) {
            return documentRepository.findAll();
        }

        return documentRepository.findByOwner_Id(user.getId());
    }

    public Document getDocumentById(Long id, User user) {

        return getAuthorizedDocument(id, user);
    }

    public Document uploadDocument(MultipartFile file, User user) {

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
            document.setFilePath(s3Key);
            document.setStatus("PENDING");
            document.setOwner(user);

            Document savedDocument = documentRepository.save(document);

            // 8. Send document ID to SQS
            sqsMessageProducer.sendDocumentProcessingMessage(
                    savedDocument.getId()
            );

            return savedDocument;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Could not upload document or send processing message", e
            );
        }
    }

    public Resource downloadDocument(Long id, User user) {

    	Document document = getAuthorizedDocument(id, user);

        try {

            // Retrieve the PDF stream from S3
            InputStream inputStream =
                    s3StorageService.downloadFile(document.getFilePath());

            return new InputStreamResource(inputStream);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Could not download document from S3", e
            );
        }
    }

    public Document retryDocument(Long id, User user) {

    	if (user.getRole() != Role.ADMIN) {
    	    throw new AccessDeniedException(
    	            "Only administrators can retry documents"
    	    );
    	}

    	Document document = getAuthorizedDocument(id, user);

        if (!"FAILED".equals(document.getStatus())) {
            throw new RuntimeException(
                    "Only FAILED documents can be retried"
            );
        }

        document.setStatus("PENDING");
        document.setErrorMessage(null);
        document.setPageCount(null);
        document.setFileSize(null);

        Document savedDocument = documentRepository.save(document);

        // Send a fresh processing message to SQS
        sqsMessageProducer.sendDocumentProcessingMessage(
                savedDocument.getId()
        );

        return savedDocument;
    }

    public List<ProcessingHistory> getProcessingHistory(Long documentId, User user) {

    	getAuthorizedDocument(documentId, user);

        return processingHistoryRepository
                .findByDocumentIdOrderByAttemptAsc(documentId);
    }
    
    private Document getAuthorizedDocument(Long id, User user) {

        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));

        boolean isAdmin = user.getRole() == Role.ADMIN;

        boolean isOwner = document.getOwner() != null
                && document.getOwner().getId().equals(user.getId());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException(
                    "You are not authorized to access this document"
            );
        }

        return document;
    }
}
