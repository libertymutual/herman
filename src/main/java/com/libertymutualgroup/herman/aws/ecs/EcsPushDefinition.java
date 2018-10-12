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
package com.libertymutualgroup.herman.aws.ecs;

import com.amazonaws.services.ecs.model.ContainerDefinition;
import com.amazonaws.services.ecs.model.KeyValuePair;
import com.amazonaws.services.ecs.model.PlacementStrategy;
import com.amazonaws.services.ecs.model.TaskDefinitionPlacementConstraint;
import com.amazonaws.services.ecs.model.Ulimit;
import com.amazonaws.services.ecs.model.Volume;
import com.libertymutualgroup.herman.aws.ecs.broker.dynamodb.DynamoAppDefinition;
import com.libertymutualgroup.herman.aws.ecs.broker.dynamodb.DynamoDBTable;
import com.libertymutualgroup.herman.aws.ecs.broker.iam.IamAppDefinition;
import com.libertymutualgroup.herman.aws.ecs.broker.kinesis.KinesisAppDefinition;
import com.libertymutualgroup.herman.aws.ecs.broker.kinesis.KinesisStream;
import com.libertymutualgroup.herman.aws.ecs.broker.kms.KmsAppDefinition;
import com.libertymutualgroup.herman.aws.ecs.broker.newrelic.NewRelicConfiguration;
import com.libertymutualgroup.herman.aws.ecs.broker.rds.RdsInstance;
import com.libertymutualgroup.herman.aws.ecs.broker.s3.S3Bucket;
import com.libertymutualgroup.herman.aws.ecs.broker.sns.SnsTopic;
import com.libertymutualgroup.herman.aws.ecs.broker.sqs.SqsQueue;
import com.libertymutualgroup.herman.aws.ecs.service.EcsService;

import java.util.List;

public class EcsPushDefinition implements IamAppDefinition, KmsAppDefinition, DynamoAppDefinition, KinesisAppDefinition {

    private List<ContainerDefinition> containerDefinitions;
    private String cluster;
    private EcsService service;
    private String appName;
    private List<TaskDefinitionPlacementConstraint> placementConstraints;
    private List<PlacementStrategy> placementStrategies;
    private List<Volume> volumes;
    private String networkMode;
    private Integer taskMemory;

    private List<S3Bucket> buckets;
    private List<KinesisStream> streams;
    private List<SqsQueue> queues;
    private List<SnsTopic> topics;
    private List<DynamoDBTable> dynamoDBTables;
    private RdsInstance database;
    private String taskRoleArn;
    private NewRelicConfiguration newRelic;
    private String notificationWebhook;
    //oddball flags - deprecate elb/iam soon
    private String iamOptOut;
    private String useElb;
    private String betaAutoscale;
    private String iamPolicy;
    private String useKms;
    private String kmsKeyName;
    private String iamRole;
    private String albTimeout;
    private List<Ulimit> ulimits;

    public List<ContainerDefinition> getContainerDefinitions() {
        return containerDefinitions;
    }

    public void setContainerDefinitions(
        List<ContainerDefinition> containerDefinitions) {
        this.containerDefinitions = containerDefinitions;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public EcsService getService() {
        return service;
    }

    public void setService(EcsService service) {
        this.service = service;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public List<TaskDefinitionPlacementConstraint> getPlacementConstraints() {
        return placementConstraints;
    }

    public void setPlacementConstraints(
        List<TaskDefinitionPlacementConstraint> placementConstraints) {
        this.placementConstraints = placementConstraints;
    }

    public List<PlacementStrategy> getPlacementStrategies() {
        return placementStrategies;
    }

    public void setPlacementStrategies(List<PlacementStrategy> placementStrategies) {
        this.placementStrategies = placementStrategies;
    }

    public List<Volume> getVolumes() {
        return volumes;
    }

    public void setVolumes(List<Volume> volumes) {
        this.volumes = volumes;
    }

    public List<S3Bucket> getBuckets() {
        return buckets;
    }

    public void setBuckets(List<S3Bucket> buckets) {
        this.buckets = buckets;
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

    public List<DynamoDBTable> getDynamoDBTables() {
        return dynamoDBTables;
    }

    public void setDynamoDBTables(List<DynamoDBTable> dynamoDBTables) {
        this.dynamoDBTables = dynamoDBTables;
    }

    public RdsInstance getDatabase() {
        return database;
    }

    public void setDatabase(RdsInstance database) {
        this.database = database;
    }

    public String getTaskRoleArn() {
        return taskRoleArn;
    }

    public void setTaskRoleArn(String taskRoleArn) {
        this.taskRoleArn = taskRoleArn;
    }

    public NewRelicConfiguration getNewRelic() {
        return newRelic;
    }

    public void setNewRelic(NewRelicConfiguration newRelic) {
        this.newRelic = newRelic;
    }

    public String getNotificationWebhook() {
        return notificationWebhook;
    }

    public void setNotificationWebhook(String notificationWebhook) {
        this.notificationWebhook = notificationWebhook;
    }

    public String getIamOptOut() {
        return iamOptOut;
    }

    public void setIamOptOut(String iamOptOut) {
        this.iamOptOut = iamOptOut;
    }

    public String getUseElb() {
        return useElb;
    }

    public void setUseElb(String useElb) {
        this.useElb = useElb;
    }

    public String getBetaAutoscale() {
        return betaAutoscale;
    }

    public void setBetaAutoscale(String betaAutoscale) {
        this.betaAutoscale = betaAutoscale;
    }

    public String getIamPolicy() {
        return iamPolicy;
    }

    public void setIamPolicy(String iamPolicy) {
        this.iamPolicy = iamPolicy;
    }

    public String getUseKms() {
        return useKms;
    }

    public void setUseKms(String useKms) {
        this.useKms = useKms;
    }

    public String getKmsKeyName() {
        return kmsKeyName;
    }

    public void setKmsKeyName(String kmsKeyName) {
        this.kmsKeyName = kmsKeyName;
    }

    public String getIamRole() {
        return iamRole;
    }

    public void setIamRole(String iamRole) {
        this.iamRole = iamRole;
    }

    public String getAlbTimeout() {
        return albTimeout;
    }

    public void setAlbTimeout(String albTimeout) {
        this.albTimeout = albTimeout;
    }

    public String getNetworkMode() {
        return networkMode;
    }

    public void setNetworkMode(String networkMode) {
        this.networkMode = networkMode;
    }

    public String getTaskMemory() {
        return taskMemory == null ? null : taskMemory.toString();
    }

    public void setTaskMemory(Integer taskMemory) {
        this.taskMemory = taskMemory;
    }

    public List<Ulimit> getUlimits() {
        return ulimits;
    }

    public void setUlimits(List<Ulimit> ulimits) {
        this.ulimits = ulimits;
    }

    @Override
    public String toString() {
        return "EcsPushDefinition{" +
            "containerDefinitions=" + containerDefinitions +
            ", cluster='" + cluster + '\'' +
            ", service=" + service +
            ", appName='" + appName + '\'' +
            ", placementConstraints=" + placementConstraints +
            ", placementStrategies=" + placementStrategies +
            ", volumes=" + volumes +
            ", buckets=" + buckets +
            ", streams=" + streams +
            ", queues=" + queues +
            ", topics=" + topics +
            ", dynamoDbTables=" + dynamoDBTables +
            ", database=" + database +
            ", taskRoleArn='" + taskRoleArn + '\'' +
            ", newRelic=" + newRelic +
            ", notificationWebhook='" + notificationWebhook + '\'' +
            ", iamOptOut='" + iamOptOut + '\'' +
            ", useElb='" + useElb + '\'' +
            ", betaAutoscale='" + betaAutoscale + '\'' +
            ", iamPolicy='" + iamPolicy + '\'' +
            ", useKms='" + useKms + '\'' +
            ", kmsKeyName='" + kmsKeyName + '\'' +
            ", iamRole='" + iamRole + '\'' +
            ", albTimeout='" + albTimeout + '\'' +
            ", ulimits='" + ulimits + '\'' +
            '}';
    }

    public String getNewRelicApplicationName() {
        String newRelicApplicationName = null;
        if (this.getContainerDefinitions() != null) {
            newRelicApplicationName = this.getContainerDefinitions().iterator().next().getEnvironment().stream()
                .filter(environmentVar -> "NEW_RELIC_APP_NAME".equals(environmentVar.getName()))
                .findAny()
                .map(KeyValuePair::getValue)
                .orElse(null);
        }
        return newRelicApplicationName;
    }
}
