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
package com.libertymutualgroup.herman.task.bamboo.s3;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.atlassian.bamboo.deployments.execution.DeploymentTaskContext;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.libertymutualgroup.herman.aws.AbstractDeploymentTask;
import com.libertymutualgroup.herman.aws.credentials.BambooCredentialsHandler;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.aws.ecs.TaskContextPropertyHandler;
import com.libertymutualgroup.herman.aws.ecs.broker.s3.BucketMeta;
import com.libertymutualgroup.herman.aws.ecs.broker.s3.S3Broker;
import com.libertymutualgroup.herman.aws.ecs.broker.s3.S3CreateContext;
import com.libertymutualgroup.herman.logging.AtlassianBuildLogger;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.util.ConfigurationUtil;
import com.libertymutualgroup.herman.util.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;

import static com.libertymutualgroup.herman.task.s3.S3CreatePropertyFactory.getTaskProperties;

public class S3CreateTask extends AbstractDeploymentTask {

    @Autowired
    public S3CreateTask(CustomVariableContext customVariableContext) {
        super(customVariableContext);
    }

    @Override
    public TaskResult doExecute(final DeploymentTaskContext taskContext) {
        final AtlassianBuildLogger buildLogger = new AtlassianBuildLogger(taskContext.getBuildLogger());
        final AWSCredentials sessionCredentials = BambooCredentialsHandler.getCredentials(taskContext);
        final Regions awsRegion = Regions.fromName(taskContext.getConfigurationMap().getOrDefault("awsRegion",
            String.valueOf(S3CreateTaskConfigurator.DEFAULT_REGION)));

        PropertyHandler handler = new TaskContextPropertyHandler(taskContext, getCustomVariableContext());

        AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(sessionCredentials))
            .withClientConfiguration(BambooCredentialsHandler.getConfiguration()).build();
        String accountId = stsClient.getCallerIdentity(new GetCallerIdentityRequest()).getAccount();
        handler.addProperty("account.id", accountId);

        S3CreateContext s3CreateContext = new S3CreateContext()
            .withPropertyHandler(handler)
            .withLogger(buildLogger)
            .withRegion(awsRegion)
            .withRootPath(taskContext.getRootDirectory().getAbsolutePath())
            .withSessionCredentials(sessionCredentials)
            .withTaskProperties(getTaskProperties(sessionCredentials, buildLogger, awsRegion))
            .withFileUtil(new FileUtil(taskContext.getRootDirectory().getAbsolutePath(), buildLogger));

        S3Broker s3Broker = new S3Broker(s3CreateContext);
        BucketMeta meta = s3Broker.brokerFromConfigurationFile();

        taskContext.getDeploymentContext().getVariableContext().addLocalVariable("bamboo.s3.brokered.name",
            meta.getName());
        taskContext.getDeploymentContext().getVariableContext().addLocalVariable("bamboo.s3.brokered.region",
            meta.getRegion());

        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }

}
