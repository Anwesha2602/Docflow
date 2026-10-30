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

    //simply for creating the document
    @PostMapping  
    public Document createDocument(@RequestParam String fileName) {
        return documentService.createDocument(fileName);
    }
    
    //lists all the documents
    @GetMapping
    public List<Document> getAllDocuments() {
        return documentService.getAllDocuments();
    }
    
    //list only a particular document based on the unique id which is accepted as path variable
    @GetMapping("/{id}")
    public Document getDocumentById(@PathVariable Long id) {
        return documentService.getDocumentById(id);
    }
    
    //the file is uploaded
    @PostMapping("/upload")
    public Document uploadDocument(@RequestParam("file") MultipartFile file) {
        return documentService.uploadDocument(file);
    }
    
    //to download a particular file. prepare the HTTP response(header--tells the client how to interpret the response)
     //content type(tells explicitely it is pdf)
     // body(the actual file is passed as resource in the body)
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
    
    //for retrying a particular doc
    @PostMapping("/{id}/retry")
    public Document retryDocument(@PathVariable Long id) {
        return documentService.retryDocument(id);
    }
    
    //get list of history files
    @GetMapping("/{id}/history")
    public List<ProcessingHistory> getProcessingHistory(
            @PathVariable Long id) {

        return documentService.getProcessingHistory(id);
    }
   
}