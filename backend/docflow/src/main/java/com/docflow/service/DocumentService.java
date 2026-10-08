package com.docflow.service;

import com.docflow.entity.Document;
import com.docflow.entity.ProcessingHistory;
import com.docflow.exception.InvalidFileException;
import com.docflow.exception.DocumentNotFoundException;
import com.docflow.repository.DocumentRepository;
import com.docflow.repository.ProcessingHistoryRepository;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;
import java.io.File;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentProcessingService documentProcessingService;
    private final ProcessingHistoryRepository processingHistoryRepository;
    
    public DocumentService(
            DocumentRepository documentRepository,
            DocumentProcessingService documentProcessingService,
            ProcessingHistoryRepository processingHistoryRepository) {

        this.documentRepository = documentRepository;
        this.documentProcessingService = documentProcessingService;
        this.processingHistoryRepository = processingHistoryRepository;
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

            // 5. Create uploads directory
            String uploadDir = System.getProperty("user.dir")
                    + File.separator + "uploads";

            File directory = new File(uploadDir);

            if (!directory.exists()) {
                directory.mkdirs();
            }

            // 6. Create file path
            String uniqueFileName = UUID.randomUUID() + "_" + fileName;
            String filePath = uploadDir
                    + File.separator
                    + uniqueFileName;

            // 7. Save actual file to disk
            file.transferTo(new File(filePath));

            // 8. Create database record
            Document document = new Document();

            document.setFileName(fileName);
            document.setFilePath(filePath);
            document.setStatus("PENDING");

//            // 9. Save metadata to PostgreSQL
//            return documentRepository.save(document);
            
            Document savedDocument = documentRepository.save(document);

            documentProcessingService.processDocument(savedDocument.getId());

            return savedDocument;

        } catch (Exception e) {
            throw new RuntimeException("Could not save file", e);
        }
    }
    
    public Resource downloadDocument(Long id) {

        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));

        File file = new File(document.getFilePath());

        if (!file.exists()) {
            throw new RuntimeException("File not found on disk");
        }

        return new FileSystemResource(file);
    }
    
    public Document retryDocument(Long id) {

        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));

        // Only FAILED documents can be retried
        if (!"FAILED".equals(document.getStatus())) {
            throw new RuntimeException(
                    "Only FAILED documents can be retried"
            );
        }

        // Check how many attempts have already happened
        int attempts = processingHistoryRepository
                .findByDocumentIdOrderByAttemptAsc(id)
                .size();

        // Do not allow more than 3 attempts
        if (attempts >= 3) {
            throw new RuntimeException(
                    "Maximum processing attempts reached"
            );
        }

        // Reset processing information
        document.setStatus("PENDING");
        document.setErrorMessage(null);
        document.setPageCount(null);
        document.setFileSize(null);

        Document savedDocument = documentRepository.save(document);

        // Start processing asynchronously
        documentProcessingService.processDocument(savedDocument.getId());

        return savedDocument;
    }
    
    public List<ProcessingHistory> getProcessingHistory(Long documentId) {

        // Make sure the document exists
        documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        return processingHistoryRepository
                .findByDocumentIdOrderByAttemptAsc(documentId);
    }
}




