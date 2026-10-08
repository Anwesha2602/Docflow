package com.docflow.controller;
import org.springframework.web.multipart.MultipartFile;
import com.docflow.entity.Document;
import com.docflow.entity.ProcessingHistory;
import com.docflow.service.DocumentService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    public Document createDocument(@RequestParam String fileName) {
        return documentService.createDocument(fileName);
    }
    
    @GetMapping
    public List<Document> getAllDocuments() {
        return documentService.getAllDocuments();
    }
    
    @GetMapping("/{id}")
    public Document getDocumentById(@PathVariable Long id) {
        return documentService.getDocumentById(id);
    }
    
    @PostMapping("/upload")
    public Document uploadDocument(@RequestParam("file") MultipartFile file) {
        return documentService.uploadDocument(file);
    }
    
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) {

        Document document = documentService.getDocumentById(id);

        Resource resource = documentService.downloadDocument(id);

        return ResponseEntity.ok()
                .header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + document.getFileName() + "\""
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
    
    @PostMapping("/{id}/retry")
    public Document retryDocument(@PathVariable Long id) {
        return documentService.retryDocument(id);
    }
    
    @GetMapping("/{id}/history")
    public List<ProcessingHistory> getProcessingHistory(
            @PathVariable Long id) {

        return documentService.getProcessingHistory(id);
    }
   
}