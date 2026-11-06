
package com.docflow.messaging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
public class SqsMessageProducer {

    private final SqsClient sqsClient;

    @Value("${aws.sqs.queue-url}")
    private String queueUrl;

    public SqsMessageProducer(SqsClient sqsClient) {
        this.sqsClient = sqsClient;
    }

    public void sendDocumentProcessingMessage(Long documentId) {

        SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(String.valueOf(documentId))
                .build();

        sqsClient.sendMessage(request);

        System.out.println(
                "Document processing message sent for ID: " + documentId
        );
    }
}
