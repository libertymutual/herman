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

public class SqsQueue {

    private String name;
    private String policyName;
    private String delaySeconds;
    private String maximumMessageSize;
    private String messageRetentionPeriod;
    private String receiveMessageWaitTimeSeconds;
    private String visibilityTimeout;
    private String kmsMasterKeyId;
    private String fifoQueue;
    private Boolean serverSideEncryption = true;

    public String getKmsMasterKeyId() {
        return kmsMasterKeyId;
    }

    public void setKmsMasterKeyId(String kmsMasterKeyId) {
        this.kmsMasterKeyId = kmsMasterKeyId;
    }

    public String getDelaySeconds() {
        return delaySeconds;
    }

    public void setDelaySeconds(String delaySeconds) {
        this.delaySeconds = delaySeconds;
    }

    public String getMaximumMessageSize() {
        return maximumMessageSize;
    }

    public void setMaximumMessageSize(String maximumMessageSize) {
        this.maximumMessageSize = maximumMessageSize;
    }

    public String getMessageRetentionPeriod() {
        return messageRetentionPeriod;
    }

    public void setMessageRetentionPeriod(String messageRetentionPeriod) {
        this.messageRetentionPeriod = messageRetentionPeriod;
    }

    public String getReceiveMessageWaitTimeSeconds() {
        return receiveMessageWaitTimeSeconds;
    }

    public void setReceiveMessageWaitTimeSeconds(String receiveMessageWaitTimeSeconds) {
        this.receiveMessageWaitTimeSeconds = receiveMessageWaitTimeSeconds;
    }

    public String getVisibilityTimeout() {
        return visibilityTimeout;
    }

    public void setVisibilityTimeout(String visibilityTimeout) {
        this.visibilityTimeout = visibilityTimeout;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFifoQueue() {
        return fifoQueue;
    }

    public void setFifoQueue(String fifoQueue) {
        this.fifoQueue = fifoQueue;
    }

    public Boolean getServerSideEncryption() {
        return serverSideEncryption;
    }

    public void setServerSideEncryption(Boolean serverSideEncryption) {
        this.serverSideEncryption = serverSideEncryption;
    }

    @Override
    public String toString() {
        return "SqsQueue{" +
            "name='" + name + '\'' +
            ", policyName='" + policyName + '\'' +
            ", delaySeconds='" + delaySeconds + '\'' +
            ", maximumMessageSize='" + maximumMessageSize + '\'' +
            ", messageRetentionPeriod='" + messageRetentionPeriod + '\'' +
            ", receiveMessageWaitTimeSeconds='" + receiveMessageWaitTimeSeconds + '\'' +
            ", visibilityTimeout='" + visibilityTimeout + '\'' +
            ", kmsMasterKeyId='" + kmsMasterKeyId + '\'' +
            ", fifoQueue='" + fifoQueue + '\'' +
            ", serverSideEncryption=" + serverSideEncryption +
            '}';
    }
}
