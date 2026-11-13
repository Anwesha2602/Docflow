package com.docflow.service;

import com.docflow.entity.Document;
import com.docflow.exception.DocumentNotFoundException;
import com.docflow.messaging.SqsMessageProducer;
import com.docflow.repository.DocumentRepository;
import com.docflow.repository.ProcessingHistoryRepository;
import com.docflow.exception.InvalidFileException;
import static org.mockito.Mockito.verifyNoInteractions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ProcessingHistoryRepository processingHistoryRepository;

    @Mock
    private S3StorageService s3StorageService;

    @Mock
    private SqsMessageProducer sqsMessageProducer;

    @InjectMocks
    private DocumentService documentService;


    @Test
    void shouldReturnDocumentWhenIdExists() {

        // Arrange
        Document document = new Document();
        document.setFileName("sample.pdf");
        document.setStatus("COMPLETED");

        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));

        // Act
        Document result = documentService.getDocumentById(1L);

        // Assert
        assertNotNull(result);
        assertEquals("sample.pdf", result.getFileName());
        assertEquals("COMPLETED", result.getStatus());

        verify(documentRepository).findById(1L);
    }


    @Test
    void shouldThrowExceptionWhenDocumentDoesNotExist() {

        // Arrange
        when(documentRepository.findById(99L))
                .thenReturn(Optional.empty());

        // Act and Assert
        assertThrows(
                DocumentNotFoundException.class,
                () -> documentService.getDocumentById(99L)
        );

        verify(documentRepository).findById(99L);
    }
    
    @Test
    void shouldUploadPdfSuccessfully() throws Exception {

        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.pdf",
                "application/pdf",
                "dummy pdf content".getBytes()
        );

        Document savedDocument = new Document();

        when(documentRepository.save(any(Document.class)))
                .thenAnswer(invocation -> {

                    Document document = invocation.getArgument(0);

                    ReflectionTestUtils.setField(document, "id", 1L);

                    return document;
           });

        // Act
        Document result = documentService.uploadDocument(file);

        // Assert
        assertNotNull(result);
        assertEquals("sample.pdf", result.getFileName());
        assertEquals("PENDING", result.getStatus());

        verify(s3StorageService).uploadFile(
                anyString(),
                any(),
                anyLong()
        );

        verify(documentRepository).save(any(Document.class));

        verify(sqsMessageProducer).sendDocumentProcessingMessage(1L);
    }
    
    @Test
    void shouldRejectEmptyFile() {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.pdf",
                "application/pdf",
                new byte[0]
        );

        assertThrows(
                InvalidFileException.class,
                () -> documentService.uploadDocument(file)
        );

        verifyNoInteractions(
                s3StorageService,
                sqsMessageProducer
        );
    }
    @Test
    void shouldRejectNonPdfFile() {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.txt",
                "text/plain",
                "Hello World".getBytes()
        );

        assertThrows(
                InvalidFileException.class,
                () -> documentService.uploadDocument(file)
        );

        verifyNoInteractions(
                s3StorageService,
                sqsMessageProducer
        );
    }
    @Test
    void shouldRetryFailedDocument() {

        Document document = new Document();
        document.setStatus("FAILED");

        ReflectionTestUtils.setField(document, "id", 1L);

        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));

        when(documentRepository.save(any(Document.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Document result = documentService.retryDocument(1L);

        assertEquals("PENDING", result.getStatus());
        assertNull(result.getErrorMessage());

        verify(sqsMessageProducer)
                .sendDocumentProcessingMessage(1L);
    }
}