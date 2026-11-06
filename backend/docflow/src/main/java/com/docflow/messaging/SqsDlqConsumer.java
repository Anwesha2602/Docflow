
package com.docflow.messaging;

import com.docflow.entity.Batch;
import com.docflow.entity.Document;
import com.docflow.repository.BatchRepository;
import com.docflow.repository.DocumentRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;

import java.util.List;

@Service
public class SqsDlqConsumer {

    private final SqsClient sqsClient;
    private final DocumentRepository documentRepository;
    private final BatchRepository batchRepository;

    @Value("${aws.sqs.dlq-queue-url}")
    private String dlqQueueUrl;

    public SqsDlqConsumer(
            SqsClient sqsClient,
            DocumentRepository documentRepository,
            BatchRepository batchRepository) {

        this.sqsClient = sqsClient;
        this.documentRepository = documentRepository;
        this.batchRepository = batchRepository;
    }

    @Scheduled(fixedDelay = 1000)
    public void receiveDlqMessages() {

        try {
            ReceiveMessageRequest request =
                    ReceiveMessageRequest.builder()
                            .queueUrl(dlqQueueUrl)
                            .maxNumberOfMessages(1)
                            .waitTimeSeconds(20)
                            .build();

            List<Message> messages =
                    sqsClient.receiveMessage(request).messages();

            for (Message message : messages) {

                try {
                    Long documentId = Long.valueOf(message.body());

                    Document document = documentRepository.findById(documentId)
                            .orElseThrow(() -> new RuntimeException(
                                    "Document not found: " + documentId));

                    document.setStatus("PERMANENTLY_FAILED");

                    document.setErrorMessage(
                            "Processing failed after SQS retries were exhausted."
                    );

                    documentRepository.save(document);

                    updateBatchStatus(document.getBatchId());

                    // Delete only after database updates succeed.
                    DeleteMessageRequest deleteRequest =
                            DeleteMessageRequest.builder()
                                    .queueUrl(dlqQueueUrl)
                                    .receiptHandle(message.receiptHandle())
                                    .build();

                    sqsClient.deleteMessage(deleteRequest);

                    System.out.println(
                            "Document permanently failed: " + documentId
                    );

                } catch (Exception e) {
                    System.out.println(
                            "DLQ message processing failed: " + e.getMessage()
                    );
                    // Do not delete the message.
                }
            }

        } catch (Exception e) {
            System.out.println(
                    "Error polling DLQ: " + e.getMessage()
            );
        }
    }

    private void updateBatchStatus(Long batchId) {

        if (batchId == null) {
            return;
        }

        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() ->
                        new RuntimeException("Batch not found: " + batchId));

        List<Document> documents =
                documentRepository.findByBatchId(batchId);

        int completed = 0;
        int permanentlyFailed = 0;

        for (Document document : documents) {

            if ("COMPLETED".equals(document.getStatus())) {
                completed++;
            }

            if ("PERMANENTLY_FAILED".equals(document.getStatus())) {
                permanentlyFailed++;
            }
        }

        batch.setCompletedDocuments(completed);
        batch.setFailedDocuments(permanentlyFailed);

        int total = batch.getTotalDocuments() == null
                ? 0 : batch.getTotalDocuments();

        if (completed + permanentlyFailed == total) {

            if (permanentlyFailed > 0) {
                batch.setStatus("COMPLETED_WITH_ERRORS");
            } else {
                batch.setStatus("COMPLETED");
            }

        } else {
            batch.setStatus("PROCESSING");
        }

        batchRepository.save(batch);
    }
}
