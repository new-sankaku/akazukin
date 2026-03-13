package com.akazukin.infrastructure.queue;

import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.port.PostPublisher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.CreateScheduleRequest;
import software.amazon.awssdk.services.scheduler.model.DeleteScheduleRequest;
import software.amazon.awssdk.services.scheduler.model.FlexibleTimeWindow;
import software.amazon.awssdk.services.scheduler.model.FlexibleTimeWindowMode;
import software.amazon.awssdk.services.scheduler.model.ScheduleState;
import software.amazon.awssdk.services.scheduler.model.SqsParameters;
import software.amazon.awssdk.services.scheduler.model.Target;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@ApplicationScoped
public class SqsPostPublisher implements PostPublisher {

    private static final Logger LOG = Logger.getLogger(SqsPostPublisher.class);
    private static final String SCHEDULE_NAME_PREFIX = "akazukin-post-";
    private static final String SCHEDULE_GROUP = "akazukin-scheduled-posts";

    private final SqsClient sqsClient;
    private final SchedulerClient schedulerClient;
    private final String queueUrl;
    private final String schedulerRoleArn;
    private final String schedulerTargetArn;

    @Inject
    public SqsPostPublisher(
            SqsClient sqsClient,
            SchedulerClient schedulerClient,
            @ConfigProperty(name = "akazukin.sqs.queue-url") String queueUrl,
            @ConfigProperty(name = "akazukin.scheduler.role-arn") String schedulerRoleArn,
            @ConfigProperty(name = "akazukin.scheduler.target-arn") String schedulerTargetArn) {
        this.sqsClient = sqsClient;
        this.schedulerClient = schedulerClient;
        this.queueUrl = queueUrl;
        this.schedulerRoleArn = schedulerRoleArn;
        this.schedulerTargetArn = schedulerTargetArn;
    }

    @Override
    public void publishForProcessing(UUID postId) {
        String messageBody = buildMessageBody(postId);

        LOG.infof("Sending SQS message for post %s to queue %s", postId, queueUrl);

        try {
            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .messageGroupId(postId.toString())
                    .build();

            sqsClient.sendMessage(request);

            LOG.infof("Successfully sent SQS message for post %s", postId);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to send SQS message for post %s", postId);
            throw new DomainException("SQS_SEND_FAILED",
                    "Failed to send post to processing queue: " + e.getMessage(), e);
        }
    }

    @Override
    public void schedulePost(UUID postId, Instant scheduledAt) {
        String scheduleName = SCHEDULE_NAME_PREFIX + postId;
        String messageBody = buildMessageBody(postId);
        String scheduleExpression = "at("
                + DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                        .withZone(ZoneOffset.UTC)
                        .format(scheduledAt)
                + ")";

        LOG.infof("Creating EventBridge schedule '%s' for post %s at %s",
                scheduleName, postId, scheduledAt);

        try {
            Target target = Target.builder()
                    .arn(schedulerTargetArn)
                    .roleArn(schedulerRoleArn)
                    .input(messageBody)
                    .sqsParameters(SqsParameters.builder()
                            .messageGroupId(postId.toString())
                            .build())
                    .build();

            CreateScheduleRequest request = CreateScheduleRequest.builder()
                    .name(scheduleName)
                    .groupName(SCHEDULE_GROUP)
                    .scheduleExpression(scheduleExpression)
                    .scheduleExpressionTimezone("UTC")
                    .flexibleTimeWindow(FlexibleTimeWindow.builder()
                            .mode(FlexibleTimeWindowMode.OFF)
                            .build())
                    .target(target)
                    .state(ScheduleState.ENABLED)
                    .description("Scheduled post: " + postId)
                    .build();

            schedulerClient.createSchedule(request);

            LOG.infof("Successfully created EventBridge schedule for post %s", postId);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to create EventBridge schedule for post %s", postId);
            throw new DomainException("SCHEDULE_CREATE_FAILED",
                    "Failed to schedule post: " + e.getMessage(), e);
        }
    }

    @Override
    public void cancelScheduledPost(UUID postId) {
        String scheduleName = SCHEDULE_NAME_PREFIX + postId;

        LOG.infof("Deleting EventBridge schedule '%s' for post %s", scheduleName, postId);

        try {
            DeleteScheduleRequest request = DeleteScheduleRequest.builder()
                    .name(scheduleName)
                    .groupName(SCHEDULE_GROUP)
                    .build();

            schedulerClient.deleteSchedule(request);

            LOG.infof("Successfully deleted EventBridge schedule for post %s", postId);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to delete EventBridge schedule for post %s", postId);
            throw new DomainException("SCHEDULE_DELETE_FAILED",
                    "Failed to cancel scheduled post: " + e.getMessage(), e);
        }
    }

    private String buildMessageBody(UUID postId) {
        return "{\"postId\":\"" + postId + "\",\"action\":\"PUBLISH\"}";
    }
}
