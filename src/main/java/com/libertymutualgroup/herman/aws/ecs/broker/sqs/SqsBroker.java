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
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.QueueAttributeName;
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
        Map<String, String> dlqAttributes = new HashMap<>();
        String dlqName = queue.getName() + "-dlq";

        if (!StringUtils.isEmpty(queue.getDelaySeconds())) {
            attributes.put("DelaySeconds", queue.getDelaySeconds());
        }
        if (!StringUtils.isEmpty(queue.getMaximumMessageSize())) {
            attributes.put("MaximumMessageSize", queue.getMaximumMessageSize());
            dlqAttributes.put("MaximumMessageSize", queue.getMaximumMessageSize());
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
            dlqAttributes.put("FifoQueue", "true");
            dlqAttributes.put("ContentBasedDeduplication", "true");
            dlqName = dlqName.replace(".fifo", "") + ".fifo" ;
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
                dlqAttributes.put("KmsMasterKeyId", queue.getKmsMasterKeyId());
            } else {
                attributes.put("KmsMasterKeyId", "alias/aws/sqs");
                dlqAttributes.put("KmsMasterKeyId", "alias/aws/sqs");
            }
        }

        if(queue.getRedrivePolicy() != null){
            try { // Check for existing DLQ and update if found
                updateQueueAttributes(client, dlqName, dlqAttributes);
            } catch (QueueDoesNotExistException ex) { // Create new
                LOGGER.debug("Error updating DLQ queue: " + queue.getName(), ex);
                createNewQueue(client, dlqName, dlqAttributes);
            }

            String dlqArn = getQueueArn(client, dlqName);
            String maximumReceiveCount = queue.getRedrivePolicy().getMaximumReceiveCount();

            attributes.put(QueueAttributeName.RedrivePolicy.toString(), "{\"maxReceiveCount\": \"" + maximumReceiveCount + "\", \"deadLetterTargetArn\": \"" + dlqArn + "\"}");
        }

        try { // Check for existing and update if found
            updateQueueAttributes(client, queue.getName(), attributes);
        } catch (QueueDoesNotExistException ex) { // Create new
            LOGGER.debug("Error updating queue: " + queue.getName(), ex);
            createNewQueue(client, queue.getName(), attributes);
        }
    }

    private String getQueueArn(AmazonSQS client, String queueName){
        String queueUrl = client.getQueueUrl(queueName).getQueueUrl();
        GetQueueAttributesResult queueAttributes = client.getQueueAttributes(
                new GetQueueAttributesRequest(queueUrl)
                        .withAttributeNames("QueueArn"));
        return queueAttributes.getAttributes().get("QueueArn");
    }

    private void createNewQueue(AmazonSQS client, String queueName, Map<String, String> attributes) {
        buildLogger
            .addLogEntry("Could not find queue with name: " + queueName + ". Attempting to create...");

        CreateQueueRequest request = new CreateQueueRequest().withQueueName(queueName).withAttributes(attributes);
        try {
            client.createQueue(request);
            buildLogger.addLogEntry("Created queue: " + queueName);
        } catch (Exception createQueueEx) {
            buildLogger.addErrorLogEntry(
                String.format("Error creating queue %s: %s", request.getQueueName(), request.getAttributes()));
            throw createQueueEx;
        }
    }

    private void updateQueueAttributes(AmazonSQS client, String queueName, Map<String, String> attributes) {
        String queueUrl;
        queueUrl = client.getQueueUrl(queueName).getQueueUrl();
        buildLogger.addLogEntry("Queue with name " + queueName + " already exists, attempting update.");

        try {
            client.setQueueAttributes(queueUrl, attributes);
            buildLogger.addLogEntry("Updated queue: " + queueName);
        } catch (Exception sqaEx) {
            buildLogger.addErrorLogEntry(
                String.format("Error updating attributes for queue %s: %s", queueName, attributes));
            throw sqaEx;
        }
    }
}
