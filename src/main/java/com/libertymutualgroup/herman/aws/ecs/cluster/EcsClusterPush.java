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
package com.libertymutualgroup.herman.aws.ecs.cluster;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.cloudformation.model.AmazonCloudFormationException;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.GetTemplateRequest;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackResource;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.TagDescription;
import com.amazonaws.services.ecs.AmazonECS;
import com.amazonaws.services.ecs.AmazonECSClientBuilder;
import com.amazonaws.services.ecs.model.Cluster;
import com.amazonaws.services.ecs.model.ContainerInstance;
import com.amazonaws.services.ecs.model.ContainerInstanceStatus;
import com.amazonaws.services.ecs.model.CreateClusterRequest;
import com.amazonaws.services.ecs.model.DescribeClustersRequest;
import com.amazonaws.services.ecs.model.DescribeClustersResult;
import com.amazonaws.services.ecs.model.DescribeContainerInstancesRequest;
import com.amazonaws.services.ecs.model.DescribeContainerInstancesResult;
import com.amazonaws.services.ecs.model.ListContainerInstancesRequest;
import com.amazonaws.services.ecs.model.ListContainerInstancesResult;
import com.amazonaws.services.ecs.model.Tag;
import com.amazonaws.services.ecs.model.TagResourceRequest;
import com.amazonaws.services.ecs.model.UntagResourceRequest;
import com.amazonaws.services.ecs.model.UpdateContainerInstancesStateRequest;
import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.Lists;
import com.libertymutualgroup.herman.aws.AwsExecException;
import com.libertymutualgroup.herman.aws.asg.AutoscalingGroupHandler;
import com.libertymutualgroup.herman.aws.cft.CftPush;
import com.libertymutualgroup.herman.aws.cft.CftPushContext;
import com.libertymutualgroup.herman.aws.cft.StackUtils;
import com.libertymutualgroup.herman.aws.ecs.EcsPushContext;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.aws.tags.TagUtil;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.task.cft.CftPushPropertyFactory;
import com.libertymutualgroup.herman.task.cft.CftPushTaskProperties;
import com.libertymutualgroup.herman.util.FileUtil;
import com.libertymutualgroup.herman.util.TemplateFormat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class EcsClusterPush {
    private EcsPushContext context;
    private AmazonCloudFormation cfnClient;
    private AmazonECS ecsClient;
    private AmazonEC2 ec2Client;
    private PropertyHandler propertyHandler;
    private HermanLogger logger;
    private FileUtil fileUtil;
    private EcsClusterPushDefinition definition;
    private AutoscalingGroupHandler asgHandler;
    private ContainerInstanceHandler containerInstanceHandler;
    private StackUtils stackUtils;
    private HashMap<String, String> cftParams = new HashMap<>();

    private final String TEMPLATE_NAME = "template";
    private final int POLLING_INTERVAL_MS = 10000;

    public EcsClusterPush(EcsPushContext context) {
        this.context = context;
        this.logger = context.getLogger();

        this.cfnClient = AmazonCloudFormationClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(context.getSessionCredentials()))
            .withClientConfiguration(context.getAwsClientConfig()).withRegion(context.getRegion()).build();

        this.ecsClient = AmazonECSClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(context.getSessionCredentials()))
            .withClientConfiguration(context.getAwsClientConfig()).withRegion(context.getRegion()).build();

        this.ec2Client = AmazonEC2ClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(context.getSessionCredentials()))
            .withClientConfiguration(context.getAwsClientConfig()).withRegion(context.getRegion()).build();

        this.propertyHandler = context.getPropertyHandler();

        this.fileUtil = new FileUtil(context.getRootPath(), this.logger);
        this.definition = loadConfiguration();

        AmazonAutoScaling asgClient = AmazonAutoScalingClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(context.getSessionCredentials()))
            .withClientConfiguration(context.getAwsClientConfig()).withRegion(context.getRegion()).build();

        this.asgHandler = new AutoscalingGroupHandler(asgClient, this.logger);
        this.containerInstanceHandler = new ContainerInstanceHandler(this.ecsClient, this.ec2Client, this.logger);
        this.stackUtils = new StackUtils(this.cfnClient, this.logger);
    }

    public void pushCluster() {
        this.definition.setClusterName(this.definition.getClusterName() + "-" + this.context.getRegion().getName());
        EcsClusterDeployState existingStackState = findExistingStack(this.definition.getClusterName());
        String sharedStackTemplate = loadStackFile(this.definition.getSharedCftName());
        String instanceStackTemplate = loadStackFile(this.definition.getInstanceCftName());

        String sharedStackName = this.definition.getClusterName() + "-shared";
        int instanceStackNumber;
        setCftParams();

        if (existingStackState == null) { // New Cluster
            this.logger.addLogEntry("Creating new cluster: " + this.definition.getClusterName());
            // ECS Cluster
            CreateClusterRequest newClusterRequest = new CreateClusterRequest()
                .withClusterName(this.definition.getClusterName())
                .withTags(TagUtil.hermanToEcsTags(this.definition.getTags()));
            Cluster newCluster = this.ecsClient.createCluster(newClusterRequest).getCluster();

            addClusterProperties(newCluster);

            // Shared stack
            pushStack(sharedStackName, sharedStackTemplate);
            this.cftParams.put("SharedStackName", sharedStackName);

            // Instance Stack
            instanceStackNumber = 1;

            String instanceStackName = this.definition.getClusterName() + "-instances-" + instanceStackNumber;
            this.logger.addLogEntry("... Creating new instance stack with name: " + instanceStackName);
            Stack newInstanceStack = pushStack(instanceStackName, instanceStackTemplate);
            String newAsgName = this.findAsgInStack(newInstanceStack.getStackName());
            waitForInstanceStartup(newAsgName, Optional.empty());
            waitForInstanceEcsRegistration(this.definition.getClusterName(), newAsgName);

            this.containerInstanceHandler.setAttributeOnCluster(this.definition.getClusterName(), "state", "running");

        }
        else { // Update Cluster
            this.logger.addLogEntry("Updating cluster: " + this.definition.getClusterName());
            DescribeClustersResult clusters = this.ecsClient.describeClusters(new DescribeClustersRequest().withClusters(this.definition.getClusterName()));
            if (clusters == null || clusters.getClusters().size() == 0) {
                throw new AwsExecException("Unable to find existing cluster with name " + this.definition.getClusterName());
            }
            Cluster existingCluster = clusters.getClusters().get(0);

            this.logger.addLogEntry("... Updating cluster tags");

            if (!existingCluster.getTags().isEmpty()) {
                UntagResourceRequest clearTagsRequest = new UntagResourceRequest()
                    .withTagKeys(existingCluster.getTags().stream().map(Tag::getKey).collect(Collectors.toList()))
                    .withResourceArn(existingCluster.getClusterArn());
                this.ecsClient.untagResource(clearTagsRequest);
            }

            if (this.definition.getTags() != null) {
                TagResourceRequest addTagsRequest = new TagResourceRequest()
                    .withTags(TagUtil.hermanToEcsTags(this.definition.getTags()))
                    .withResourceArn(existingCluster.getClusterArn());
                this.ecsClient.tagResource(addTagsRequest);
            }

            this.logger.addLogEntry("... Cluster tagging update complete!");

            addClusterProperties(existingCluster);
            // Shared Stack
            pushStack(sharedStackName, sharedStackTemplate);
            this.cftParams.put("SharedStackName", sharedStackName);

            // Instance Stack
            GetTemplateRequest existingTemplateRequest = new GetTemplateRequest()
                .withStackName(existingStackState.getInstanceStack().getStackName());
            String existingInstanceTemplate = this.cfnClient.getTemplate(existingTemplateRequest).getTemplateBody();

            if (!existingInstanceTemplate.equals(instanceStackTemplate) || parametersUpdated(existingStackState.getInstanceStack().getParameters())) {
                String instanceStackName = this.findNextStackName(this.definition.getClusterName());

                // Find old container instances
                List<String> oldContainerInstanceArns = null;
                ListContainerInstancesResult listContainerInstancesResult = this.ecsClient.listContainerInstances(new ListContainerInstancesRequest().withCluster(this.definition.getClusterName()));
                if (listContainerInstancesResult.getContainerInstanceArns() != null) {
                    oldContainerInstanceArns = listContainerInstancesResult.getContainerInstanceArns();
                }


                ArrayList<String> newContainerInstanceArns = new ArrayList<>();
                String oldAsgName = this.findAsgInStack(existingStackState.getInstanceStack().getStackName());
                try {
                    // Set pre-drain to avoid placing containers on old boxes
                    this.containerInstanceHandler.setAttributeOnCluster(this.definition.getClusterName(), "state", "pre-drain");

                    this.logger.addLogEntry("... Creating new instance stack with name: " + instanceStackName);
                    Stack newInstanceStack = pushStack(instanceStackName, instanceStackTemplate);
                    String newAsgName = this.findAsgInStack(newInstanceStack.getStackName());
                    waitForInstanceStartup(newAsgName, Optional.empty());
                    waitForInstanceEcsRegistration(this.definition.getClusterName(), newAsgName);

                    // Pause scaling operations to prevent bringing anymore old boxes online
                    this.asgHandler.pauseScalingOperations(oldAsgName);

                    List<String> midUpdateInstanceArns = this.ecsClient.listContainerInstances(new ListContainerInstancesRequest().withCluster(this.definition.getClusterName())).getContainerInstanceArns();

                    if (oldContainerInstanceArns != null) {
                        for (String arn : midUpdateInstanceArns) {
                            if (!oldContainerInstanceArns.contains(arn)) {
                                newContainerInstanceArns.add(arn);
                            }
                        }
                    }
                    else {
                        newContainerInstanceArns.addAll(midUpdateInstanceArns);
                    }

                    // Set instances to draining
                    if (this.definition.isDrainingEnabled() && oldContainerInstanceArns != null && !oldContainerInstanceArns.isEmpty()) {
                        this.logger.addLogEntry("Moving " + existingCluster.getRunningTasksCount() + " tasks across " + existingCluster.getActiveServicesCount() + " services to new container instances.");
                        List<List<String>> partitionedInstanceArns = Lists.partition(oldContainerInstanceArns, this.definition.getMaxConcurrentDraining());
                        this.logger.addLogEntry("... Draining instances in sets of " + this.definition.getMaxConcurrentDraining());
                        for (List<String> instancePartition : partitionedInstanceArns) {
                            setContainerInstanceStatus(this.definition.getClusterName(), instancePartition, ContainerInstanceStatus.DRAINING);
                            waitForDrainingComplete(this.definition.getClusterName(), instancePartition);
                        }
                        this.logger.addLogEntry("... All old instances drained.");
                    }
                }
                catch (Exception ex) { // Don't leave cluster in pre-drain state if anything goes wrong
                    this.logger.addErrorLogEntry("Error occurred while updating cluster. Recovering old instances...");
                    this.logger.addErrorLogEntry(ex.toString());
                    if (oldContainerInstanceArns != null && !oldContainerInstanceArns.isEmpty()) {
                        setContainerInstanceStatus(this.definition.getClusterName(), oldContainerInstanceArns, ContainerInstanceStatus.ACTIVE);
                        try {
                            Thread.sleep(POLLING_INTERVAL_MS); // Wait 1 cycle to allow ACTIVE status to set before killing
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (this.definition.isDrainingEnabled() && !newContainerInstanceArns.isEmpty()) {
                        setContainerInstanceStatus(this.definition.getClusterName(), newContainerInstanceArns, ContainerInstanceStatus.DRAINING);
                        waitForDrainingComplete(this.definition.getClusterName(), newContainerInstanceArns);
                    }
                    this.containerInstanceHandler.setAttributeOnCluster(this.definition.getClusterName(), "state", "active");
                    this.asgHandler.resumeScalingOperations(oldAsgName);
                    throw new AwsExecException(ex);
                }

                // Instances empty, continue to delete
                deleteStack(existingStackState.getInstanceStack().getStackName());
                this.logger.addLogEntry("Cluster Update Complete!");
            }
            else {
                this.logger.addLogEntry("... No instance updates to apply, skipping...");
            }
        }
    }

    private void setCftParams() {
        this.definition.getCftParameters().forEach(param -> {
            this.cftParams.put(param.getKey(), param.getValue());
        });
    }

    private EcsClusterDeployState findExistingStack(String clusterName) {
        logger.addLogEntry("Looking for existing stacks");
        logger.addLogEntry("... Searching for shared stack with name: " + clusterName + "-shared");
        DescribeStacksRequest sharedStackSearch = new DescribeStacksRequest()
            .withStackName(clusterName + "-shared");
        List<Stack> stacksResponse;
        try {
            stacksResponse = this.cfnClient.describeStacks(sharedStackSearch).getStacks();
        }
        catch (AmazonCloudFormationException ex) {
            logger.addLogEntry("... Unable to find shared stack, creating new cluster");
            return null;
        }
        if (stacksResponse == null || stacksResponse.size() == 0) {
            logger.addLogEntry("... Unable to find shared stack, creating new cluster");
            return null;
        }

        Stack sharedStack = stacksResponse.get(0);

        logger.addLogEntry("... Found shared stack: " + sharedStack.getStackName());
        logger.addLogEntry("... Searching for instance stack");
        List<ContainerInstance> existingContainerInstances = containerInstanceHandler.getContainerInstances(clusterName);
        List<Stack> instanceFilteredStacks;
        if (existingContainerInstances == null || existingContainerInstances.size() == 0) {
            List<Stack> allAccountStacks = getAllStacks();
            instanceFilteredStacks = allAccountStacks.stream()
                .filter(stack -> stack.getStackName().contains(this.definition.getClusterName() + "-instances"))
                .sorted(Comparator.comparing(Stack::getStackName).reversed())
                .collect(Collectors.toList());
        }
        else {
            List<TagDescription> existingContainerInstanceTags = containerInstanceHandler.getContainerInstanceTags(existingContainerInstances.get(0));
            String existingStackName = existingContainerInstanceTags.stream().filter(tag -> "aws:cloudformation:stack-name".equals(tag.getKey())).collect(Collectors.toList()).get(0).getValue();
            instanceFilteredStacks = Collections.singletonList(getStackWithName(existingStackName));
        }

        if (instanceFilteredStacks.size() == 0) {
            logger.addLogEntry("... Unable to find instance stack");
            return new EcsClusterDeployState()
                .withClusterName(this.definition.getClusterName())
                .withSharedStack(sharedStack);
        }
        Stack latestInstanceStack = instanceFilteredStacks.get(0);
        logger.addLogEntry("... Found latest instance stack: " + latestInstanceStack.getStackName());

        return new EcsClusterDeployState()
            .withClusterName(this.definition.getClusterName())
            .withSharedStack(sharedStack)
            .withInstanceStack(latestInstanceStack);
    }

    private String findAsgInStack(String stackName) {
        DescribeStackResourcesRequest describeStackResourceRequest = new DescribeStackResourcesRequest().withStackName(stackName);
        List<StackResource> stackResources = this.cfnClient.describeStackResources(describeStackResourceRequest).getStackResources();
        for (StackResource resource : stackResources) {
            if ("AWS::AutoScaling::AutoScalingGroup".equals(resource.getResourceType())) {
                return resource.getPhysicalResourceId();
            }
        }

        logger.addErrorLogEntry("Unable to find Autoscaling Group in stack: " + stackName);
        return null;
    }

    private Stack pushStack(String stackName, String stackTemplate) {
        this.logger.addLogEntry("Pushing stack: " + stackName + "...");

        CftPushTaskProperties cftPushTaskProperties = CftPushPropertyFactory.getTaskProperties(context.getSessionCredentials(), this.logger, context.getRegion(), this.propertyHandler);
        CftPushContext cftPushContext = new CftPushContext()
            .withLogger(this.logger)
            .withEnvName(context.getEnvName())
            .withRootPath(context.getRootPath())
            .withPropertyHandler(context.getPropertyHandler())
            .withSessionCredentials(context.getSessionCredentials())
            .withAwsClientConfig(context.getAwsClientConfig())
            .withRegion(context.getRegion())
            .withTemplateParameters(this.cftParams)
            .withTaskProperties(cftPushTaskProperties);
        CftPush cftPush = new CftPush(cftPushContext);

        cftPush.push(stackName, stackTemplate, this.definition.getTags());

        return this.cfnClient.describeStacks(new DescribeStacksRequest().withStackName(stackName)).getStacks().get(0);
    }

    private void deleteStack(String stackName) {
        this.logger.addLogEntry("Deleting stack: " + stackName);
        DeleteStackRequest deleteStackRequest = new DeleteStackRequest()
            .withStackName(stackName);
        this.cfnClient.deleteStack(deleteStackRequest);
        try {
            this.stackUtils.waitForCompletion(stackName);
        }
        catch (AmazonCloudFormationException ex) {
            this.logger.addLogEntry("DELETE_COMPLETE");
        }
    }

    private EcsClusterPushDefinition loadConfiguration() {
        ObjectMapper mapper;

        for (TemplateFormat format : TemplateFormat.values()) {
            if (fileUtil.fileExists(TEMPLATE_NAME + "." + format.toSuffix())) {
                String template = fileUtil.findFile(TEMPLATE_NAME + "." + format.toSuffix(), false);

                if (TemplateFormat.YML.equals(format)) {
                    mapper = new ObjectMapper(new YAMLFactory());
                }
                else {
                    mapper = new ObjectMapper();
                }

                try {
                    String deployEnvironment = this.context.getEnvName();
                    if (deployEnvironment != null) {
                        this.propertyHandler.addProperty("DeployEnvironment", deployEnvironment);
                    }
                    return mapper.readValue(this.propertyHandler.mapInProperties(template), EcsClusterPushDefinition.class);
                } catch (IOException e) {
                    throw new AwsExecException(e);
                }
            }
        }
        throw new AwsExecException("No template provided!");
    }

    private List<Stack> getAllStacks() {
        DescribeStacksResult stacksResult = this.cfnClient.describeStacks();
        ArrayList<Stack> stacks = new ArrayList<>(stacksResult.getStacks());
        String nextToken = stacksResult.getNextToken();
        while (nextToken != null) {
            DescribeStacksRequest pagedRequest = new DescribeStacksRequest().withNextToken(nextToken);
            DescribeStacksResult pagedResult = this.cfnClient.describeStacks(pagedRequest);
            stacks.addAll(pagedResult.getStacks());
            nextToken = pagedResult.getNextToken();
        }

        return stacks;
    }

    private Stack getStackWithName(String stackName) {
        DescribeStacksResult stacksResult = this.cfnClient.describeStacks(new DescribeStacksRequest().withStackName(stackName));
        return stacksResult.getStacks().get(0);
    }

    private String findNextStackName(String clusterName) {
        List<Stack> allAccountStacks = getAllStacks();
        List<Stack> instanceFilteredStacks = allAccountStacks.stream()
            .filter(stack -> stack.getStackName().contains(clusterName + "-instances"))
            .sorted(Comparator.comparing(Stack::getStackName).reversed())
            .collect(Collectors.toList());
        Stack latestStack = instanceFilteredStacks.get(0);
        String[] stackNameSplit = latestStack.getStackName().split("-");
        int stackNumber = Integer.parseInt(stackNameSplit[stackNameSplit.length - 1]) + 1;
        return clusterName + "-instances-" + stackNumber;
    }

    private long waitForInstanceStartup(String asgName, Optional<Long> timeout) {
        long timeoutMillis = timeout.orElse(TimeUnit.MINUTES.toMillis(this.context.getTimeout()));
        boolean instancesStarted = false;
        this.logger.addLogEntry("Waiting for instances to start");
        // Wait for ASG Instances to Start
        while (!instancesStarted && timeoutMillis > 0) {
            List<String> newAsgInstanceIds = this.asgHandler.getAsgInstancesInService(asgName).stream()
                .map(Instance::getInstanceId)
                .collect(Collectors.toList());

            int desiredAsgSize = this.asgHandler.getAsg(asgName).getDesiredCapacity();
            this.logger.addLogEntry("... Current: " + newAsgInstanceIds.size() + " Desired: " + desiredAsgSize);
            instancesStarted = desiredAsgSize == newAsgInstanceIds.size();

            try {
                Thread.sleep(POLLING_INTERVAL_MS);
            }
            catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new AwsExecException("Interrupted while waiting for instances to start");
            }
            timeoutMillis -= POLLING_INTERVAL_MS;
        }

        if (!instancesStarted) {
            this.logger.addErrorLogEntry("Instance startup timed out! Rolling back...");
            throw new AwsExecException("Instance startup timed out");
        }
        return timeoutMillis;
    }

    private void waitForInstanceEcsRegistration(String cluster, String asgName) {
        long timeoutMillis = TimeUnit.MINUTES.toMillis(this.context.getTimeout());
        long registrationTimeoutMillis = TimeUnit.MINUTES.toMillis(this.definition.getInstanceRegistrationTimeout());
        HashMap<String, Long> pendingInstanceTimeouts = new HashMap<>();
        Set<String> pendingInstanceIds;

        do {
            pendingInstanceIds = this.containerInstanceHandler.getUnregisteredInstanceIds(cluster, asgName, this.asgHandler);
            this.logger.addLogEntry("... " + pendingInstanceIds.size() + " instances pending cluster registration");
            if (!pendingInstanceIds.isEmpty()) {
                this.logger.addLogEntry("... Waiting for " + String.join(", ", pendingInstanceIds) + " to register with cluster.");
            }

            HashSet<String> unhealthyInstances = new HashSet<>();
            for (String instanceId : pendingInstanceIds) {
                if (pendingInstanceTimeouts.containsKey(instanceId)) {
                    if (pendingInstanceTimeouts.get(instanceId) <= 0) { // Timed out
                        unhealthyInstances.add(instanceId);
                    }
                    else { // Count down further
                        long currentTime = pendingInstanceTimeouts.get(instanceId);
                        pendingInstanceTimeouts.put(instanceId, currentTime - POLLING_INTERVAL_MS);
                    }
                }
                else {
                    pendingInstanceTimeouts.put(instanceId, registrationTimeoutMillis);
                }
            }

            if (!unhealthyInstances.isEmpty()) { // Instances timed out in this pass, wait for replacements
                for (String unhealthyInstance : unhealthyInstances) {
                    this.logger.addLogEntry(unhealthyInstance + " never registered to cluster. Marking unhealthy.");
                    this.asgHandler.setEc2Unhealthy(unhealthyInstance);
                }
                unhealthyInstances.clear();
                timeoutMillis = waitForInstanceStartup(asgName, Optional.of(timeoutMillis));
            }
            else {
                try {
                    Thread.sleep(POLLING_INTERVAL_MS);
                }
                catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new AwsExecException("Interrupted while waiting for instances to register");
                }
                timeoutMillis -= POLLING_INTERVAL_MS;
            }

        } while (!pendingInstanceIds.isEmpty() && timeoutMillis > 0);

    }

    private void waitForDrainingComplete(String cluster, List<String> containerInstanceArns) {
        long timeoutMillis = TimeUnit.MINUTES.toMillis(this.context.getTimeout());

        boolean drained = false;
        while (!drained && timeoutMillis > 0) {
            DescribeContainerInstancesRequest describeInstancesRequest = new DescribeContainerInstancesRequest()
                .withCluster(cluster)
                .withContainerInstances(containerInstanceArns);
            DescribeContainerInstancesResult describeInstancesResult = this.ecsClient.describeContainerInstances(describeInstancesRequest);
            for (ContainerInstance instance : describeInstancesResult.getContainerInstances()) {
                this.logger.addLogEntry("..." + instance.getContainerInstanceArn() + " - " + instance.getEc2InstanceId() + " - Tasks: " + instance.getRunningTasksCount() + " - DRAINING");
            }

            drained = describeInstancesResult.getContainerInstances()
                .stream()
                .allMatch(instance -> instance.getRunningTasksCount() == 0);

            try {
                Thread.sleep(POLLING_INTERVAL_MS);
            }
            catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new AwsExecException("Interrupted while waiting for instances to drain");
            }
            timeoutMillis -= POLLING_INTERVAL_MS;
        }

        if (!drained) {
            this.logger.addErrorLogEntry("Unable to drain all instances! Manual cleanup of old stack may be required.");
        }
        this.logger.addLogEntry("... Draining complete for: " + String.join(", ", containerInstanceArns));
    }

    private void setContainerInstanceStatus(String cluster, List<String> containerInstanceArns, ContainerInstanceStatus status) {
        for (String arn : containerInstanceArns) {
            this.logger.addLogEntry("... Setting instance " + arn + " status to " + status.toString());
        }
        UpdateContainerInstancesStateRequest updateStateRequest = new UpdateContainerInstancesStateRequest()
            .withCluster(cluster)
            .withContainerInstances(containerInstanceArns)
            .withStatus(status);
        this.ecsClient.updateContainerInstancesState(updateStateRequest);
    }

    private void addClusterProperties(Cluster cluster) {
        this.cftParams.put("ClusterName", cluster.getClusterName());
        this.cftParams.put("ClusterArn", cluster.getClusterArn());
    }

    private String loadStackFile(String stackFileName) {
        return fileUtil.findFile(stackFileName, false);
    }

    private boolean parametersUpdated(List<Parameter> existingStackParameters) {
        List<String> ignoreList = Arrays.asList("ClusterName", "ClusterArn");
        this.logger.addLogEntry("Checking parameter updates...");
        List<Parameter> filteredParameters = existingStackParameters.stream().filter(parameter -> !ignoreList.contains(parameter.getParameterKey())).collect(Collectors.toList());
        for (Parameter existingStackParam : filteredParameters) {
            String deployProperty = this.propertyHandler.lookupVariable(existingStackParam.getParameterKey());
            this.logger.addLogEntry("... " + existingStackParam.getParameterKey() + " ---  Stack: " + existingStackParam.getParameterValue() + " Deploy: " + deployProperty);
            if (!StringUtils.isNullOrEmpty(deployProperty) && !existingStackParam.getParameterValue().equals(deployProperty)) {
                return true;
            }
        }
        return false;
    }

}
