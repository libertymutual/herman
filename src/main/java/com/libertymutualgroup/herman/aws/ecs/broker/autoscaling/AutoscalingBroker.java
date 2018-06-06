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
package com.libertymutualgroup.herman.aws.ecs.broker.autoscaling;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.applicationautoscaling.AWSApplicationAutoScaling;
import com.amazonaws.services.applicationautoscaling.AWSApplicationAutoScalingClientBuilder;
import com.amazonaws.services.applicationautoscaling.model.CustomizedMetricSpecification;
import com.amazonaws.services.applicationautoscaling.model.MetricDimension;
import com.amazonaws.services.applicationautoscaling.model.PolicyType;
import com.amazonaws.services.applicationautoscaling.model.PutScalingPolicyRequest;
import com.amazonaws.services.applicationautoscaling.model.RegisterScalableTargetRequest;
import com.amazonaws.services.applicationautoscaling.model.ScalableDimension;
import com.amazonaws.services.applicationautoscaling.model.ServiceNamespace;
import com.amazonaws.services.applicationautoscaling.model.TargetTrackingScalingPolicyConfiguration;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.identitymanagement.model.GetRoleRequest;
import com.amazonaws.services.identitymanagement.model.Role;
import com.libertymutualgroup.herman.aws.ecs.EcsPushContext;
import com.libertymutualgroup.herman.aws.ecs.EcsPushDefinition;
import com.libertymutualgroup.herman.aws.ecs.cluster.EcsClusterMetadata;

public class AutoscalingBroker {

    private EcsPushContext context;

    public AutoscalingBroker(EcsPushContext context) {
        this.context = context;
    }

    public void broker(EcsClusterMetadata meta, EcsPushDefinition definition) {
        AWSApplicationAutoScaling autoScaling = AWSApplicationAutoScalingClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(context.getSessionCredentials()))
            .withClientConfiguration(context.getAwsClientConfig()).withRegion(context.getRegion()).build();

        AmazonIdentityManagement iamClient = AmazonIdentityManagementClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(context.getSessionCredentials()))
            .withClientConfiguration(context.getAwsClientConfig()).withRegion(context.getRegion()).build();

        Role ecsRole = iamClient.getRole(new GetRoleRequest().withRoleName(meta.getClusterEcsRole())).getRole();

        autoScaling.registerScalableTarget(

            new RegisterScalableTargetRequest()
                .withRoleARN(ecsRole.getArn())
                .withMaxCapacity(definition.getService().getInstanceCount() + 2)
                .withMinCapacity(definition.getService().getInstanceCount())
                .withResourceId("service/" + meta.getClusterId() + "/" + definition.getAppName())
                .withScalableDimension(ScalableDimension.EcsServiceDesiredCount)
                .withServiceNamespace(ServiceNamespace.Ecs)
        );

        TargetTrackingScalingPolicyConfiguration config = new TargetTrackingScalingPolicyConfiguration()
            .withCustomizedMetricSpecification(
                new CustomizedMetricSpecification()
                    .withDimensions(
                        new MetricDimension().withName("ServiceName").withValue(definition.getAppName()),
                        new MetricDimension().withName("ClusterName").withValue(meta.getClusterId())
                    )
                    .withMetricName("CPUUtilization")
                    .withNamespace("AWS/ECS")
                    .withStatistic("Average")
            )
            .withScaleInCooldown(60)
            .withScaleOutCooldown(60)
            .withTargetValue(50.0);

        autoScaling.putScalingPolicy(
            new PutScalingPolicyRequest()
                .withPolicyName(definition.getAppName())
                .withPolicyType(PolicyType.TargetTrackingScaling)
                .withResourceId("service/" + meta.getClusterId() + "/" + definition.getAppName())
                .withScalableDimension(ScalableDimension.EcsServiceDesiredCount)
                .withServiceNamespace("ecs")
                .withTargetTrackingScalingPolicyConfiguration(config)
        );

    }

}
