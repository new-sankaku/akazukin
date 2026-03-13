package com.akazukin.web.queue;

import com.akazukin.application.usecase.PostPublishUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class SqsPostConsumer {

    private static final Logger LOG = Logger.getLogger(SqsPostConsumer.class);
    private static final String ACTION_PUBLISH = "PUBLISH";

    private final PostPublishUseCase postPublishUseCase;
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Inject
    public SqsPostConsumer(PostPublishUseCase postPublishUseCase,
                           SqsClient sqsClient,
                           ObjectMapper objectMapper) {
        this.postPublishUseCase = postPublishUseCase;
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Processes SQS messages from a Lambda SQS event.
     * Each message contains a JSON body with postId and action.
     *
     * @param messages the list of SQS messages from the Lambda event
     */
    public void processMessages(List<SqsMessage> messages) {
        for (SqsMessage message : messages) {
            processMessage(message);
        }
    }

    /**
     * Processes a single SQS message containing a post publish request.
     * Parses the message body, validates the action, and delegates to PostPublishUseCase.
     *
     * @param message the SQS message to process
     */
    public void processMessage(SqsMessage message) {
        String messageId = message.messageId();
        String body = message.body();

        LOG.infof("Processing SQS message %s: %s", messageId, body);

        try {
            JsonNode json = objectMapper.readTree(body);

            String postIdStr = json.path("postId").asText(null);
            if (postIdStr == null || postIdStr.isEmpty()) {
                throw new IllegalArgumentException("Missing required field: postId");
            }

            String action = json.path("action").asText(null);
            if (action == null || action.isEmpty()) {
                throw new IllegalArgumentException("Missing required field: action");
            }

            if (!ACTION_PUBLISH.equals(action)) {
                throw new IllegalArgumentException("Unknown action: " + action);
            }

            UUID postId = UUID.fromString(postIdStr);

            LOG.infof("Processing %s action for post %s", action, postId);

            postPublishUseCase.processPost(postId);

            LOG.infof("Successfully processed SQS message %s for post %s", messageId, postId);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to process SQS message %s: %s", messageId, body);
            throw new RuntimeException(
                    "Failed to process SQS message " + messageId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Record representing an SQS message from a Lambda event.
     * This is a simplified model of the AWS Lambda SQS event record.
     */
    public record SqsMessage(
        String messageId,
        String body,
        String receiptHandle
    ) {}
}
