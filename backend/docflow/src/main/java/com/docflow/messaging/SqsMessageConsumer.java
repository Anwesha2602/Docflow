
package com.docflow.messaging;

import com.docflow.service.DocumentProcessingService;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;

import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Service
public class SqsMessageConsumer {

    private final SqsClient sqsClient;
    private final DocumentProcessingService documentProcessingService;

    @Value("${aws.sqs.queue-url}")
    private String queueUrl;

    public SqsMessageConsumer(
            SqsClient sqsClient,
            DocumentProcessingService documentProcessingService) {

        this.sqsClient = sqsClient;
        this.documentProcessingService = documentProcessingService;
    }

    // Poll SQS for messages
    @Scheduled(fixedDelay = 1000)
    public void receiveMessages() {

        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(1)
                .waitTimeSeconds(20)
                .build();

        List<Message> messages =
                sqsClient.receiveMessage(request).messages();

        for (Message message : messages) {

            String documentId = message.body();

            System.out.println(
                    "Received document ID from SQS: " + documentId
            );

            try {

                // Convert message body into document ID
                Long id = Long.valueOf(documentId);

                // Process the document synchronously
                documentProcessingService.processDocument(id);

                // Delete message only after successful processing
                DeleteMessageRequest deleteRequest =
                        DeleteMessageRequest.builder()
                                .queueUrl(queueUrl)
                                .receiptHandle(message.receiptHandle())
                                .build();

                sqsClient.deleteMessage(deleteRequest);

                System.out.println(
                        "SQS message deleted for document: " + id
                );

            } catch (Exception e) {

                // Do NOT delete the message.
                // SQS will make it available again after visibility timeout.

                System.out.println(
                        "Processing failed for message: " + documentId
                );

                System.out.println(
                        "SQS will retry this message."
                );
            }
        }
    }
}
