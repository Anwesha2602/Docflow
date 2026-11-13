package com.docflow.service;

import com.docflow.entity.Document;
import com.docflow.entity.ProcessingHistory;
import com.docflow.repository.DocumentRepository;
import com.docflow.repository.ProcessingHistoryRepository;
import com.docflow.repository.BatchRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentProcessingServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ProcessingHistoryRepository processingHistoryRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private S3StorageService s3StorageService;

    @InjectMocks
    private DocumentProcessingService processingService;

    @Test
    void shouldMarkDocumentFailedWhenS3DownloadFails() {

        Document document = new Document();
        document.setFileName("sample.pdf");
        document.setFilePath("documents/sample.pdf");
        document.setStatus("PENDING");

        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));

        when(processingHistoryRepository
                .findByDocumentIdOrderByAttemptAsc(1L))
                .thenReturn(List.of());

        when(s3StorageService.downloadFile("documents/sample.pdf"))
                .thenThrow(new RuntimeException("S3 unavailable"));

        assertThrows(
                RuntimeException.class,
                () -> processingService.processDocument(1L)
        );

        assertEquals("FAILED", document.getStatus());

        ArgumentCaptor<ProcessingHistory> captor =
                ArgumentCaptor.forClass(ProcessingHistory.class);

        verify(processingHistoryRepository, times(2))
                .save(captor.capture());

        ProcessingHistory history = captor.getValue();

        assertEquals("FAILED", history.getStatus());

        verify(documentRepository, times(2)).save(document);
    }
}