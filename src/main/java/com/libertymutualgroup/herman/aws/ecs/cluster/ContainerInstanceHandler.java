/*
 * Copyright (C) 2018, Liberty Mutual Group
 *
 * Created on 8/2/18
 */
package com.libertymutualgroup.herman.aws.ecs.cluster;

import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.ecs.AmazonECS;
import com.amazonaws.services.ecs.model.Attribute;
import com.amazonaws.services.ecs.model.ContainerInstance;
import com.amazonaws.services.ecs.model.DescribeContainerInstancesRequest;
import com.amazonaws.services.ecs.model.DescribeContainerInstancesResult;
import com.amazonaws.services.ecs.model.ListContainerInstancesRequest;
import com.amazonaws.services.ecs.model.ListContainerInstancesResult;
import com.amazonaws.services.ecs.model.PutAttributesRequest;
import com.amazonaws.services.ecs.model.TargetType;
import com.libertymutualgroup.herman.aws.asg.AutoscalingGroupHandler;
import com.libertymutualgroup.herman.logging.HermanLogger;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ContainerInstanceHandler {
    private AmazonECS ecsClient;
    private HermanLogger logger;

    public ContainerInstanceHandler(AmazonECS ecsClient, HermanLogger logger) {
        this.ecsClient = ecsClient;
        this.logger = logger;
    }

    public List<ContainerInstance> getContainerInstances(String cluster) {
        ListContainerInstancesRequest listRequest = new ListContainerInstancesRequest()
            .withCluster(cluster);
        ListContainerInstancesResult listResult = this.ecsClient.listContainerInstances(listRequest);
        if (listResult.getContainerInstanceArns().isEmpty()) {
            return null;
        }
        DescribeContainerInstancesRequest descrRequest = new DescribeContainerInstancesRequest()
            .withCluster(cluster)
            .withContainerInstances(listResult.getContainerInstanceArns());
        DescribeContainerInstancesResult descrResult = this.ecsClient.describeContainerInstances(descrRequest);
        return descrResult.getContainerInstances();
    }

    public void setAttributeOnCluster(String cluster, String key, String value) {
        this.logger.addLogEntry("Setting " + key + ": " + value + " on cluster " + cluster);
        List<ContainerInstance> containerInstances = getContainerInstances(cluster);
        if (containerInstances == null) {
            this.logger.addLogEntry("... No container instances found in cluster, skipping attribute set");
        }
        else {
            for (ContainerInstance containerInstance : containerInstances) {
                this.setAttribute(cluster, containerInstance.getContainerInstanceArn(), key, value);
            }
        }
    }

    public Set<String> getUnregisteredInstanceIds(String cluster, String asgName, AutoscalingGroupHandler asgHandler) {
        List<String> newAsgInstanceIds = asgHandler.getAsgInstancesInService(asgName).stream()
            .map(Instance::getInstanceId)
            .collect(Collectors.toList());
        List<ContainerInstance> clusterInstances = getContainerInstances(cluster);
        List<String> newContainerInstanceIds = clusterInstances.stream()
            .filter(instance -> (!"pre-drain".equals(getInstanceAttributeValue(instance, "state"))))
            .map(ContainerInstance::getEc2InstanceId)
            .collect(Collectors.toList());

        return newAsgInstanceIds.stream()
            .filter(newInstance -> !newContainerInstanceIds.contains(newInstance))
            .collect(Collectors.toSet());
    }

    private String getInstanceAttributeValue(ContainerInstance instance, String key) {
        for (Attribute attribute : instance.getAttributes()) {
            if (key.equals(attribute.getName())) {
                return attribute.getValue();
            }
        }
        return null;
    }

    private void setAttribute(String cluster, String containerInstanceId, String key, String value) {
        Attribute attribute = new Attribute()
            .withName(key)
            .withValue(value)
            .withTargetType(TargetType.ContainerInstance)
            .withTargetId(containerInstanceId);
        PutAttributesRequest putAttributesRequest = new PutAttributesRequest()
            .withAttributes(attribute)
            .withCluster(cluster);
        this.ecsClient.putAttributes(putAttributesRequest);
    }
}
