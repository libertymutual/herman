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

import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.AmazonCloudFormationException;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.StackResource;
import com.amazonaws.services.cloudformation.model.Tag;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.DescribeVpcsResult;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.Vpc;
import com.libertymutualgroup.herman.aws.AwsExecException;
import com.libertymutualgroup.herman.logging.HermanLogger;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class EcsClusterIntrospector {

    private AmazonCloudFormation cftClient;
    private AmazonEC2 ec2Client;
    private HermanLogger logger;

    public EcsClusterIntrospector(AmazonCloudFormation cftClient, AmazonEC2 ec2Client, HermanLogger logger) {
        this.cftClient = cftClient;
        this.ec2Client = ec2Client;
        this.logger = logger;
    }

    public EcsClusterMetadata introspect(String name, Regions region) {
        EcsClusterMetadata ecsClusterMetadata = new EcsClusterMetadata();

        String stackName = name;

        DescribeStackResourcesRequest req = new DescribeStackResourcesRequest()
            .withStackName(stackName);
        DescribeStackResourcesResult clusterStackResult;
        try {
            clusterStackResult = cftClient.describeStackResources(req);
        }
        catch (AmazonCloudFormationException ex) {
            stackName += "-shared";
            req.setStackName(stackName);
            clusterStackResult = cftClient.describeStackResources(req);
        }

        if (clusterStackResult == null) {
            throw new AwsExecException("Unable to find cluster to introspect from stack: " + name);
        }

        for (StackResource r: clusterStackResult.getStackResources()) {
            updateClusterMetadataWithStackResourceValue(ecsClusterMetadata, r);
        }

        DescribeStacksResult stackResult = cftClient.describeStacks(new DescribeStacksRequest().withStackName(stackName));
        List<Tag> clusterCftStackTags = stackResult.getStacks().get(0).getTags();
        ecsClusterMetadata.setClusterCftStackTags(clusterCftStackTags);

        Iterator<Parameter> stackParams = stackResult.getStacks().get(0).getParameters().iterator();
        while (stackParams.hasNext()) {
            updateClusterMetadataWithStackParamValue(ecsClusterMetadata, stackParams);
        }

        DescribeVpcsResult res = ec2Client.describeVpcs();

        Vpc vpc = null;
        for (Vpc v: res.getVpcs()) {
            if (isProperVpc(v)) {
                vpc = v;
            }
        }
        if (vpc == null) {
            throw new AwsExecException("Cannot find any VPC!");
        }
        ecsClusterMetadata.setVpcId(vpc.getVpcId());
        List<String> elbSubnets = ecsClusterMetadata.getElbSubnets();
        List<String> publicSubnets = ecsClusterMetadata.getPublicSubnets();

        DescribeSubnetsResult sub = ec2Client.describeSubnets();

        for (Subnet net: sub.getSubnets()) {
            if (subnetMatches(vpc, net)) {
                for (com.amazonaws.services.ec2.model.Tag t: net.getTags()) {
                    if ("Name".equals(t.getKey())) {
                        if (t.getValue().contains("private-elb")) {
                            elbSubnets.add(net.getSubnetId());
                        } else if (t.getValue().contains("public")) {
                            publicSubnets.add(net.getSubnetId());
                        }
                    }
                }
            }
        }

        logger.addLogEntry("Introspection complete:");
        logger.addLogEntry(ecsClusterMetadata.toString());
        return ecsClusterMetadata;
    }

    private void updateClusterMetadataWithStackParamValue(EcsClusterMetadata ecsClusterMetadata,
        Iterator<Parameter> stackParams) {
        Parameter p = stackParams.next();
        if ("NrOrgTag".equals(p.getParameterKey())) {
            ecsClusterMetadata.setNewrelicOrgTag(p.getParameterValue());
        } else if ("NrSbuTag".equals(p.getParameterKey())) {
            ecsClusterMetadata.setNewrelicSbuTag(p.getParameterValue());
        } else if ("NrLicenseKey".equals(p.getParameterKey())) {
            ecsClusterMetadata.setNewrelicLicenseKey(p.getParameterValue());
        } else if ("AccountRegionVpcAppsubnets".equals(p.getParameterKey())) {
            String nets = p.getParameterValue();
            String[] netList = nets.split(",");
            ecsClusterMetadata.setPrivateSubnets(Arrays.asList(netList));
        } else if ("MainClusterSecurityGroupId".equals(p.getParameterKey())) {
            ecsClusterMetadata.setAppSecurityGroup(p.getParameterValue());
        } else if ("SplunkUrl".equals(p.getParameterKey())) {
            ecsClusterMetadata.setSplunkUrl(p.getParameterValue());
        } else if ("ClusterName".equals(p.getParameterKey())) {
            ecsClusterMetadata.setClusterId(p.getParameterValue());
        }
    }

    private void updateClusterMetadataWithStackResourceValue(EcsClusterMetadata ecsClusterMetadata, StackResource r) {
        logger.addLogEntry("Resource: " + r.getLogicalResourceId() + " : " + r.getPhysicalResourceId());
        if (r.getLogicalResourceId().contains("ELBSecurity")) {
            ecsClusterMetadata.getElbSecurityGroups().add(r.getPhysicalResourceId());
        } else if (r.getLogicalResourceId().contains("AppSecurity")) {
            ecsClusterMetadata.setAppSecurityGroup(r.getPhysicalResourceId());
        } else if ("RDSSecurityGroup".equals(r.getLogicalResourceId())) {
            ecsClusterMetadata.setRdsSecurityGroup(r.getPhysicalResourceId());
        } else if ("ECSCluster".equals(r.getLogicalResourceId())) {
            ecsClusterMetadata.setClusterId(r.getPhysicalResourceId());
        } else if ("EncryptionKey".equals(r.getLogicalResourceId())) {
            ecsClusterMetadata.setEncryptionKey(r.getPhysicalResourceId());
        } else if ("DBSubnetGroup".equals(r.getLogicalResourceId())) {
            ecsClusterMetadata.setDbSubnetGroup(r.getPhysicalResourceId());
        } else if ("InstanceRole".equals(r.getLogicalResourceId())) {
            ecsClusterMetadata.setClusterEcsRole(r.getPhysicalResourceId());
        }
    }

    private boolean isProperVpc(Vpc vpc) {
        String[] defaultIds = {"sandbox", "dev", "nonprod", "prod"};

        for (com.amazonaws.services.ec2.model.Tag t: vpc.getTags()) {
            if ("Name".equals(t.getKey())
                    && Arrays.asList(defaultIds).stream()
                        .filter(defaultId -> t.getValue().contains(defaultId)).findAny().isPresent()) {
                return true;
            }
        }
        return false;
    }

    private boolean subnetMatches(Vpc vpc, Subnet subnet) {
        return vpc != null && subnet != null && subnet.getVpcId() != null && subnet.getVpcId().equals(vpc.getVpcId());
    }
}
