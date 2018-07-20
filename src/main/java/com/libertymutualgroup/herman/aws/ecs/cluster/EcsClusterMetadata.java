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

import com.amazonaws.services.cloudformation.model.Tag;
import java.util.ArrayList;
import java.util.List;

public class EcsClusterMetadata {

    private String rdsSecurityGroup;
    private String appSecurityGroup;
    private List<String> elbSecurityGroups = new ArrayList<>();
    private String clusterId;
    private List<String> elbSubnets = new ArrayList<>();
    private List<String> publicSubnets = new ArrayList<>();
    private List<String> privateSubnets = new ArrayList<>();
    private List<Tag> clusterCftStackTags;
    private String encryptionKey;
    private String dbSubnetGroup;
    private String newrelicOrgTag;
    private String newrelicSbuTag;
    private String newrelicLicenseKey;
    private String clusterEcsRole;
    private String vpcId;
    private String splunkUrl;

    public String getRdsSecurityGroup() {
        return rdsSecurityGroup;
    }

    public void setRdsSecurityGroup(String rdsSecurityGroup) {
        this.rdsSecurityGroup = rdsSecurityGroup;
    }

    public String getAppSecurityGroup() {
        return appSecurityGroup;
    }

    public void setAppSecurityGroup(String appSecurityGroup) {
        this.appSecurityGroup = appSecurityGroup;
    }

    public List<String> getElbSecurityGroups() {
        return elbSecurityGroups;
    }

    public void setElbSecurityGroups(List<String> elbSecurityGroups) {
        this.elbSecurityGroups = elbSecurityGroups;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public List<String> getElbSubnets() {
        return elbSubnets;
    }

    public void setElbSubnets(List<String> elbSubnets) {
        this.elbSubnets = elbSubnets;
    }

    public List<String> getPublicSubnets() {
        return publicSubnets;
    }

    public void setPublicSubnets(List<String> publicSubnets) {
        this.publicSubnets = publicSubnets;
    }

    public List<String> getPrivateSubnets() {
        return privateSubnets;
    }

    public void setPrivateSubnets(List<String> privateSubnets) {
        this.privateSubnets = privateSubnets;
    }

    public List<Tag> getClusterCftStackTags() {
        return clusterCftStackTags;
    }

    public void setClusterCftStackTags(List<Tag> clusterCftStackTags) {
        this.clusterCftStackTags = clusterCftStackTags;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public String getDbSubnetGroup() {
        return dbSubnetGroup;
    }

    public void setDbSubnetGroup(String dbSubnetGroup) {
        this.dbSubnetGroup = dbSubnetGroup;
    }

    public String getNewrelicOrgTag() {
        return newrelicOrgTag;
    }

    public void setNewrelicOrgTag(String newrelicOrgTag) {
        this.newrelicOrgTag = newrelicOrgTag;
    }

    public String getNewrelicSbuTag() {
        return newrelicSbuTag;
    }

    public void setNewrelicSbuTag(String newrelicSbuTag) {
        this.newrelicSbuTag = newrelicSbuTag;
    }

    public String getNewrelicLicenseKey() {
        return newrelicLicenseKey;
    }

    public void setNewrelicLicenseKey(String newrelicLicenseKey) {
        this.newrelicLicenseKey = newrelicLicenseKey;
    }

    public String getClusterEcsRole() {
        return clusterEcsRole;
    }

    public void setClusterEcsRole(String clusterEcsRole) {
        this.clusterEcsRole = clusterEcsRole;
    }

    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    public String getSplunkUrl() {
        return splunkUrl;
    }

    public void setSplunkUrl(String splunkUrl) {
        this.splunkUrl = splunkUrl;
    }

    @Override
    public String toString() {
        return "EcsClusterMetadata{" +
            "rdsSecurityGroup='" + rdsSecurityGroup + '\'' +
            ", appSecurityGroup='" + appSecurityGroup + '\'' +
            ", elbSecurityGroups=" + elbSecurityGroups +
            ", clusterId='" + clusterId + '\'' +
            ", elbSubnets=" + elbSubnets +
            ", publicSubnets=" + publicSubnets +
            ", privateSubnets=" + privateSubnets +
            ", clusterCftStackTags=" + clusterCftStackTags +
            ", encryptionKey='" + encryptionKey + '\'' +
            ", dbSubnetGroup='" + dbSubnetGroup + '\'' +
            ", newrelicOrgTag='" + newrelicOrgTag + '\'' +
            ", newrelicSbuTag='" + newrelicSbuTag + '\'' +
            ", newrelicLicenseKey='" + newrelicLicenseKey + '\'' +
            ", clusterEcsRole='" + clusterEcsRole + '\'' +
            ", vpcId='" + vpcId + '\'' +
            ", splunkUrl='" + splunkUrl + '\'' +
            '}';
    }
}
