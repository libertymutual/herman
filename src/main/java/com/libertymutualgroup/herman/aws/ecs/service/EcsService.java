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
package com.libertymutualgroup.herman.aws.ecs.service;

import com.amazonaws.services.ecs.model.DeploymentConfiguration;
import com.amazonaws.services.ecs.model.PlacementConstraint;
import com.amazonaws.services.ecs.model.PlacementStrategy;
import com.amazonaws.services.ecs.model.SchedulingStrategy;
import com.amazonaws.services.elasticloadbalancing.model.HealthCheck;

import java.util.List;

public class EcsService {

    private int instanceCount;
    private DeploymentConfiguration deploymentConfiguration;
    private String urlPrefixOverride;
    private String urlSuffix;
    private String elbSchemeOverride;
    private String urlSchemeOverride;
    private HealthCheck healthCheck;
    private String appStickinessCookie;
    private String protocol;
    private List<Integer> elbSourcePorts;
    private Integer healthCheckGracePeriodSeconds = 0;
    private List<PlacementConstraint> placementConstraints;
    private List<PlacementStrategy> placementStrategies;
    private SchedulingStrategy schedulingStrategy = SchedulingStrategy.REPLICA;

    public int getInstanceCount() {
        return instanceCount;
    }

    public void setInstanceCount(int instanceCount) {
        this.instanceCount = instanceCount;
    }

    public DeploymentConfiguration getDeploymentConfiguration() {
        return deploymentConfiguration;
    }

    public void setDeploymentConfiguration(DeploymentConfiguration deploymentConfiguration) {
        this.deploymentConfiguration = deploymentConfiguration;
    }

    public String getUrlPrefixOverride() {
        return urlPrefixOverride;
    }

    public void setUrlPrefixOverride(String urlPrefixOverride) {
        this.urlPrefixOverride = urlPrefixOverride;
    }

    public String getUrlSuffix() {
        return urlSuffix;
    }

    public void setUrlSuffix(String urlSuffix) {
        this.urlSuffix = urlSuffix;
    }

    public String getElbSchemeOverride() {
        return elbSchemeOverride;
    }

    public void setElbSchemeOverride(String elbSchemeOverride) {
        this.elbSchemeOverride = elbSchemeOverride;
    }

    public String getUrlSchemeOverride() {
        return urlSchemeOverride;
    }

    public void setUrlSchemeOverride(String urlSchemeOverride) {
        this.urlSchemeOverride = urlSchemeOverride;
    }

    public HealthCheck getHealthCheck() {
        return healthCheck;
    }

    public void setHealthCheck(HealthCheck healthCheck) {
        this.healthCheck = healthCheck;
    }

    public String getAppStickinessCookie() {
        return appStickinessCookie;
    }

    public void setAppStickinessCookie(String appStickinessCookie) {
        this.appStickinessCookie = appStickinessCookie;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public List<Integer> getElbSourcePorts() {
        return elbSourcePorts;
    }

    public void setElbSourcePorts(List<Integer> elbSourcePorts) {
        this.elbSourcePorts = elbSourcePorts;
    }

    public Integer getHealthCheckGracePeriodSeconds() {
        return healthCheckGracePeriodSeconds;
    }

    public void setHealthCheckGracePeriodSeconds(Integer healthCheckGracePeriodSeconds) {
        this.healthCheckGracePeriodSeconds = healthCheckGracePeriodSeconds;
    }

    public List<PlacementConstraint> getPlacementConstraints() {
        return placementConstraints;
    }

    public void setPlacementConstraints(
        List<PlacementConstraint> placementConstraints) {
        this.placementConstraints = placementConstraints;
    }

    public List<PlacementStrategy> getPlacementStrategies() {
        return placementStrategies;
    }

    public void setPlacementStrategies(List<PlacementStrategy> placementStrategies) {
        this.placementStrategies = placementStrategies;
    }

    public SchedulingStrategy getSchedulingStrategy() {
        return schedulingStrategy;
    }

    public void setSchedulingStrategy(SchedulingStrategy schedulingStrategy) {
        this.schedulingStrategy = schedulingStrategy;
    }

    @Override
    public String toString() {
        return "EcsService{" +
            "instanceCount=" + instanceCount +
            ", deploymentConfiguration=" + deploymentConfiguration +
            ", urlPrefixOverride='" + urlPrefixOverride + '\'' +
            ", urlSuffix='" + urlSuffix + '\'' +
            ", elbSchemeOverride='" + elbSchemeOverride + '\'' +
            ", urlSchemeOverride='" + urlSchemeOverride + '\'' +
            ", healthCheck=" + healthCheck +
            ", appStickinessCookie='" + appStickinessCookie + '\'' +
            ", protocol='" + protocol + '\'' +
            ", elbSourcePorts=" + elbSourcePorts +
            ", healthCheckGracePeriodSeconds=" + healthCheckGracePeriodSeconds +
            ", placementConstraints=" + placementConstraints +
            ", placementStrategies=" + placementStrategies +
            ", schedulingStrategy=" + schedulingStrategy +
            '}';
    }
}