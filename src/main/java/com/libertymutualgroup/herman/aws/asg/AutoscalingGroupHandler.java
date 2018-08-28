/*
 * Copyright (C) 2018, Liberty Mutual Group
 *
 * Created on 8/2/18
 */
package com.libertymutualgroup.herman.aws.asg;

import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.autoscaling.model.LifecycleState;
import com.amazonaws.services.autoscaling.model.ResumeProcessesRequest;
import com.amazonaws.services.autoscaling.model.SetInstanceHealthRequest;
import com.amazonaws.services.autoscaling.model.SuspendProcessesRequest;
import com.libertymutualgroup.herman.logging.HermanLogger;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AutoscalingGroupHandler {
    private AmazonAutoScaling asgClient;
    private HermanLogger logger;

    private final List<String> SUSPEND_SCALING_PROCESSES = Arrays.asList("Launch", "HealthCheck", "ReplaceUnhealthy", "AZRebalance", "AlarmNotification", "ScheduledActions", "AddToLoadBalancer");

    public AutoscalingGroupHandler(AmazonAutoScaling asgClient, HermanLogger logger) {
        this.asgClient = asgClient;
        this.logger = logger;
    }

    public void pauseScalingOperations(String asgName) {
        this.logger.addLogEntry("...Suspending Auto Scaling operations on: " + asgName);
        SuspendProcessesRequest suspendRequest = new SuspendProcessesRequest()
            .withAutoScalingGroupName(asgName)
            .withScalingProcesses(SUSPEND_SCALING_PROCESSES);
        this.asgClient.suspendProcesses(suspendRequest);
    }

    public void resumeScalingOperations(String asgName) {
        this.logger.addLogEntry("...Resuming Auto Scaling operations on: " + asgName);
        ResumeProcessesRequest resumeRequest = new ResumeProcessesRequest()
            .withAutoScalingGroupName(asgName);
        this.asgClient.resumeProcesses(resumeRequest);
    }

    public AutoScalingGroup getAsg(String asgName) {
        DescribeAutoScalingGroupsRequest request = new DescribeAutoScalingGroupsRequest()
            .withAutoScalingGroupNames(asgName);
        return this.asgClient.describeAutoScalingGroups(request).getAutoScalingGroups().get(0);
    }

    public List<Instance> getAsgInstancesInService(String asgName) {
        AutoScalingGroup group = getAsg(asgName);
        return group.getInstances().stream()
            .filter(instance -> LifecycleState.InService.toString().equals(instance.getLifecycleState()) && "Healthy".equals(instance.getHealthStatus()))
            .collect(Collectors.toList());
    }

    public void setEc2Unhealthy(String instanceId) {
        SetInstanceHealthRequest healthRequest = new SetInstanceHealthRequest()
            .withInstanceId(instanceId)
            .withHealthStatus("Unhealthy");
        this.asgClient.setInstanceHealth(healthRequest);
    }
}
