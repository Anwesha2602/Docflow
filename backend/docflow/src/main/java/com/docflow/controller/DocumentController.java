package com.docflow.controller;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import com.docflow.entity.Document;
import com.docflow.entity.ProcessingHistory;
import com.docflow.service.DocumentService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.docflow.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.docflow.entity.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

 // Create document
    @PostMapping
    public Document createDocument(
            @RequestParam String fileName,
            @AuthenticationPrincipal User user) {

        return documentService.createDocument(fileName, user);
    }

    // Get all documents
    @GetMapping
    public List<Document> getAllDocuments(
            @AuthenticationPrincipal User user) {

        return documentService.getAllDocuments(user);
    }

    // Get document by ID
    @GetMapping("/{id}")
    public Document getDocumentById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        return documentService.getDocumentById(id, user);
    }

    // Upload document
    @PostMapping("/upload")
    public Document uploadDocument(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) {

        return documentService.uploadDocument(file, user);
    }

    // Download document
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        Document document = documentService.getDocumentById(id, user);

        Resource resource = documentService.downloadDocument(id, user);

        return ResponseEntity.ok()
                .header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + document.getFileName() + "\""
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    // Retry document
    @PostMapping("/{id}/retry")
    @PreAuthorize("hasRole('ADMIN')")
    public Document retryDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        return documentService.retryDocument(id, user);
    }

    // Get processing history
    @GetMapping("/{id}/history")
    public List<ProcessingHistory> getProcessingHistory(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        return documentService.getProcessingHistory(id, user);
    }
   
}