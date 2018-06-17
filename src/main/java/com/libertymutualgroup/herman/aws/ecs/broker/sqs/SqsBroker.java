/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.libertymutualgroup.herman.aws.ecs.broker.sqs;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import com.libertymutualgroup.herman.aws.AwsExecException;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.logging.HermanLogger;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SqsBroker {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqsBroker.class);

    private HermanLogger buildLogger;
    private PropertyHandler handler;

    public SqsBroker(HermanLogger buildLogger, PropertyHandler handler) {
        this.buildLogger = buildLogger;
        this.handler = handler;
    }

    /**
     * http://docs.aws.amazon.com/AWSSimpleQueueService/latest/APIReference/API_SetQueueAttributes.html
     * <p>
     * Valid Map Keys: All | Policy | VisibilityTimeout | MaximumMessageSize |
     * MessageRetentionPeriod | ApproximateNumberOfMessages |
     * ApproximateNumberOfMessagesNotVisible | CreatedTimestamp |
     * LastModifiedTimestamp | QueueArn | ApproximateNumberOfMessagesDelayed |
     * DelaySeconds | ReceiveMessageWaitTimeSeconds | RedrivePolicy | FifoQueue
     * | ContentBasedDeduplication | KmsMasterKeyId |
     * KmsDataKeyReusePeriodSeconds
     */

    public void brokerQueue(AmazonSQS client, SqsQueue queue, String queuePolicy) {
        buildLogger.addLogEntry("Starting SQS Broker with queue: " + queue.getName());
        Map<String, String> attributes = new HashMap<>();
        if (!StringUtils.isEmpty(queue.getDelaySeconds())) {
            attributes.put("DelaySeconds", queue.getDelaySeconds());
        }
        if (!StringUtils.isEmpty(queue.getMaximumMessageSize())) {
            attributes.put("MaximumMessageSize", queue.getMaximumMessageSize());
        }
        if (!StringUtils.isEmpty(queue.getMessageRetentionPeriod())) {
            attributes.put("MessageRetentionPeriod", queue.getMessageRetentionPeriod());
        }

        if (!StringUtils.isEmpty(queue.getFifoQueue()) && Objects.equals(queue.getFifoQueue().toLowerCase(), "true")) {
            if (!queue.getName().endsWith(".fifo")) {
                throw new AwsExecException("Unable to create fifo queue because queueName does not end with .fifo");
            }

            attributes.put("FifoQueue", "true");
            attributes.put("ContentBasedDeduplication", "true");
        }

        if (queuePolicy != null) {
            String fullPolicy = handler.mapInProperties(queuePolicy);
            attributes.put("Policy", fullPolicy);
        }

        if (!StringUtils.isEmpty(queue.getReceiveMessageWaitTimeSeconds())) {
            attributes.put("ReceiveMessageWaitTimeSeconds", queue.getReceiveMessageWaitTimeSeconds());
        }
        if (!StringUtils.isEmpty(queue.getVisibilityTimeout())) {
            attributes.put("VisibilityTimeout", queue.getVisibilityTimeout());
        }

        if (queue.getServerSideEncryption()) {
            if (!StringUtils.isEmpty(queue.getKmsMasterKeyId())) {
                attributes.put("KmsMasterKeyId", queue.getKmsMasterKeyId());
            } else {
                attributes.put("KmsMasterKeyId", "alias/aws/sqs");
            }
        }

        try { // Check for existing and update if found
            updateQueueAttributes(client, queue, attributes);
        } catch (QueueDoesNotExistException ex) { // Create new
            LOGGER.debug("Error updating queue: " + queue.getName(), ex);
            createNewQueue(client, queue, attributes);
        }
    }

    private void createNewQueue(AmazonSQS client, SqsQueue queue, Map<String, String> attributes) {
        buildLogger
            .addLogEntry("Could not find queue with name: " + queue.getName() + ". Attempting to create...");

        CreateQueueRequest request = new CreateQueueRequest().withQueueName(queue.getName()).withAttributes(attributes);
        try {
            client.createQueue(request);
            buildLogger.addLogEntry("Created queue: " + queue.getName());
        } catch (Exception createQueueEx) {
            buildLogger.addErrorLogEntry(
                String.format("Error creating queue %s: %s", request.getQueueName(), request.getAttributes()));
            throw createQueueEx;
        }
    }

    private void updateQueueAttributes(AmazonSQS client, SqsQueue queue, Map<String, String> attributes) {
        String queueUrl;
        queueUrl = client.getQueueUrl(queue.getName()).getQueueUrl();
        buildLogger.addLogEntry("Queue with name " + queue.getName() + " already exists, attempting update.");

        try {
            client.setQueueAttributes(queueUrl, attributes);
        } catch (Exception sqaEx) {
            buildLogger.addErrorLogEntry(
                String.format("Error updating attributes for queue %s: %s", queue.getName(), attributes));
            throw sqaEx;
        }

        buildLogger.addLogEntry("Updated queue: " + queue.getName());
    }
}
