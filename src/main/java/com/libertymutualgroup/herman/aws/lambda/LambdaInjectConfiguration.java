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
package com.libertymutualgroup.herman.aws.lambda;

import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ecs.model.KeyValuePair;
import com.libertymutualgroup.herman.aws.ecs.broker.dynamodb.DynamoAppDefinition;
import com.libertymutualgroup.herman.aws.ecs.broker.dynamodb.DynamoDBTable;
import com.libertymutualgroup.herman.aws.ecs.broker.iam.IamAppDefinition;
import com.libertymutualgroup.herman.aws.ecs.broker.kinesis.KinesisAppDefinition;
import com.libertymutualgroup.herman.aws.ecs.broker.kinesis.KinesisStream;
import com.libertymutualgroup.herman.aws.ecs.broker.kms.KmsAppDefinition;
import com.libertymutualgroup.herman.aws.ecs.broker.sns.SnsTopic;
import com.libertymutualgroup.herman.aws.ecs.broker.sqs.SqsQueue;
import com.libertymutualgroup.herman.aws.tags.HermanTag;

import java.util.List;

public class LambdaInjectConfiguration implements IamAppDefinition, KmsAppDefinition, DynamoAppDefinition, KinesisAppDefinition {

    private String functionName;
    private String zipFileName;
    private List<KeyValuePair> environment;
    private String handler;
    private Integer memorySize = 128;
    private String runtime;
    private Integer timeout = 5;
    private String iamPolicy;
    private String assumeRolePolicy;
    private String useKms = "false";
    private String kmsKeyName;
    private SecurityGroup customSecurityGroup;
    private List<String> subnetIds;
    private List<String> securityGroupIds;
    private List<KinesisStream> streams;
    private List<SqsQueue> queues;
    private List<SnsTopic> topics;
    private List<DynamoDBTable> dynamoDBTables;
    private List<HermanTag> tags;
    private String scheduleExpression;

    public SecurityGroup getCustomSecurityGroup() {
        return customSecurityGroup;
    }

    public void setCustomSecurityGroup(SecurityGroup customSecurityGroup) {
        this.customSecurityGroup = customSecurityGroup;
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public String getZipFileName() {
        return zipFileName;
    }

    public void setZipFileName(String zipFileName) {
        this.zipFileName = zipFileName;
    }

    public List<KeyValuePair> getEnvironment() {
        return environment;
    }

    public void setEnvironment(List<KeyValuePair> environment) {
        this.environment = environment;
    }

    public String getHandler() {
        return handler;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }

    public Integer getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(Integer memorySize) {
        this.memorySize = memorySize;
    }

    public String getRuntime() {
        return runtime;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public List<String> getSubnetIds() {
        return subnetIds;
    }

    public void setSubnetIds(List<String> subnetIds) {
        this.subnetIds = subnetIds;
    }

    public List<String> getSecurityGroupIds() {
        return securityGroupIds;
    }

    public void setSecurityGroupIds(List<String> securityGroupIds) {
        this.securityGroupIds = securityGroupIds;
    }

    public String getIamPolicy() {
        return iamPolicy;
    }

    public void setIamPolicy(String iamPolicy) {
        this.iamPolicy = iamPolicy;
    }

    public Boolean getUseKms() {
        return "true".equalsIgnoreCase(useKms);
    }

    public void setUseKms(String useKms) {
        this.useKms = useKms;
    }

    @Override
    public String getKmsKeyName() {
        return kmsKeyName;
    }

    public void setKmsKeyName(String kmsKeyName) {
        this.kmsKeyName = kmsKeyName;
    }

    public String getAssumeRolePolicy() {
        return assumeRolePolicy;
    }

    public void setAssumeRolePolicy(String assumeRolePolicy) {
        this.assumeRolePolicy = assumeRolePolicy;
    }

    public List<KinesisStream> getStreams() {
        return streams;
    }

    public void setStreams(List<KinesisStream> streams) {
        this.streams = streams;
    }

    public List<SqsQueue> getQueues() {
        return queues;
    }

    public void setQueues(List<SqsQueue> queues) {
        this.queues = queues;
    }

    public List<SnsTopic> getTopics() {
        return topics;
    }

    public void setTopics(List<SnsTopic> topics) {
        this.topics = topics;
    }

    @Override
    public List<DynamoDBTable> getDynamoDBTables() {
        return dynamoDBTables;
    }

    public void setDynamoDBTables(List<DynamoDBTable> dynamoDBTables) {
        this.dynamoDBTables = dynamoDBTables;
    }

    @Override public List<HermanTag> getTags() {
        return tags;
    }

    public void setTags(List<HermanTag> tags) {
        this.tags = tags;
    }

    public String getScheduleExpression() {
        return scheduleExpression;
    }

    public void setScheduleExpression(String scheduleExpression) {
        this.scheduleExpression = scheduleExpression;
    }

    public LambdaInjectConfiguration withFunctionName(final String functionName) {
        this.functionName = functionName;
        return this;
    }

    public LambdaInjectConfiguration withZipFileName(final String zipFileName) {
        this.zipFileName = zipFileName;
        return this;
    }

    public LambdaInjectConfiguration withEnvironment(
        final List<KeyValuePair> environment) {
        this.environment = environment;
        return this;
    }

    public LambdaInjectConfiguration withHandler(final String handler) {
        this.handler = handler;
        return this;
    }

    public LambdaInjectConfiguration withMemorySize(final Integer memorySize) {
        this.memorySize = memorySize;
        return this;
    }

    public LambdaInjectConfiguration withRuntime(final String runtime) {
        this.runtime = runtime;
        return this;
    }

    public LambdaInjectConfiguration withTimeout(final Integer timeout) {
        this.timeout = timeout;
        return this;
    }

    public LambdaInjectConfiguration withSubnetIds(final List<String> subnetIds) {
        this.subnetIds = subnetIds;
        return this;
    }

    public LambdaInjectConfiguration withSecurityGroupIds(final List<String> securityGroupIds) {
        this.securityGroupIds = securityGroupIds;
        return this;
    }

    public LambdaInjectConfiguration withIamPolicy(final String iamPolicy) {
        this.iamPolicy = iamPolicy;
        return this;
    }

    public LambdaInjectConfiguration withUseKms(final String useKms) {
        this.useKms = useKms;
        return this;
    }

    public LambdaInjectConfiguration withKmsKeyName(final String kmsKeyName) {
        this.kmsKeyName = kmsKeyName;
        return this;
    }

    public LambdaInjectConfiguration withCustomSecurityGroup(final SecurityGroup customSecurityGroup) {
        this.customSecurityGroup = customSecurityGroup;
        return this;
    }

    public LambdaInjectConfiguration withScheduleExpression(final String scheduleExpression) {
        this.scheduleExpression = scheduleExpression;
        return this;
    }

    @Override
    public String getAppName() {
        return functionName;
    }

    @Override
    public String toString() {
        return "LambdaInjectConfiguration{" +
            "functionName='" + functionName + '\'' +
            ", zipFileName='" + zipFileName + '\'' +
            ", environment=" + environment +
            ", handler='" + handler + '\'' +
            ", memorySize=" + memorySize +
            ", runtime='" + runtime + '\'' +
            ", timeout=" + timeout +
            ", subnetIds=" + subnetIds +
            ", securityGroupIds=" + securityGroupIds +
            ", iamPolicy='" + iamPolicy + '\'' +
            ", assumeRolePolicy='" + assumeRolePolicy + '\'' +
            ", useKms='" + useKms + '\'' +
            ", kmsKeyName='" + kmsKeyName + '\'' +
            ", customSecurityGroup=" + customSecurityGroup +
            ", streams=" + streams +
            ", queues=" + queues +
            ", topics=" + topics +
            ", dynamoDbTables=" + dynamoDBTables +
            ", tags=" + tags +
            ", scheduleExpression=" + scheduleExpression +
            '}';
    }
}
