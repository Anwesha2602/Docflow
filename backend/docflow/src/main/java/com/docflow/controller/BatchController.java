package com.docflow.controller;

import com.docflow.entity.Batch;
import com.docflow.service.BatchService;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/batches")
public class BatchController {

    private final BatchService batchService;

    public BatchController(BatchService batchService) {
        this.batchService = batchService;
    }

    @PostMapping("/upload")
    public Batch uploadBatch(
            @RequestParam("file") MultipartFile[] files) {

        return batchService.createBatch(files);
    }
    
    @GetMapping("/{id}")
    public Batch getBatch(@PathVariable Long id) {
        return batchService.getBatch(id);
    }
}