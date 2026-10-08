package com.docflow.service;

import com.docflow.entity.Batch;
import com.docflow.entity.Document;
import com.docflow.repository.BatchRepository;
import com.docflow.repository.DocumentRepository;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class BatchService {

    private final BatchRepository batchRepository;
    private final DocumentRepository documentRepository;
    private final DocumentProcessingService documentProcessingService;

    public BatchService(
            BatchRepository batchRepository,
            DocumentRepository documentRepository,
            DocumentProcessingService documentProcessingService) {

        this.batchRepository = batchRepository;
        this.documentRepository = documentRepository;
        this.documentProcessingService = documentProcessingService;
    }

    public Batch createBatch(MultipartFile[] files) {

        // Create batch
        Batch batch = new Batch();

        batch.setStatus("PROCESSING");
        batch.setTotalDocuments(files.length);
        batch.setCompletedDocuments(0);
        batch.setFailedDocuments(0);
        batch.setCreatedAt(LocalDateTime.now());

        Batch savedBatch = batchRepository.save(batch);

        // Upload directory
        String uploadDir = System.getProperty("user.dir")
                + File.separator + "uploads";

        File directory = new File(uploadDir);

        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Process every file
        for (MultipartFile file : files) {

            if (file == null || file.isEmpty()) {
                continue;
            }

            String fileName = file.getOriginalFilename();

            if (fileName == null || fileName.isBlank()) {
                continue;
            }

            if (!fileName.toLowerCase().endsWith(".pdf")) {
                continue;
            }

            try {

                // Create unique filename
                String uniqueFileName =
                        UUID.randomUUID() + "_" + fileName;

                String filePath = uploadDir
                        + File.separator
                        + uniqueFileName;

                // Save physical file
                file.transferTo(new File(filePath));

                // Create document
                Document document = new Document();

                document.setFileName(fileName);
                document.setFilePath(filePath);
                document.setStatus("PENDING");
                document.setBatchId(savedBatch.getId());

                // Save document
                Document savedDocument =
                        documentRepository.save(document);

                // Start asynchronous processing
                documentProcessingService
                        .processDocument(savedDocument.getId());

            } catch (Exception e) {

                System.out.println(
                        "Failed to upload file: " + fileName
                );

                e.printStackTrace();
            }
        }

        return savedBatch;
    }
    
    public Batch getBatch(Long id) {

        return batchRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Batch not found: " + id));
    }
}