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
package com.libertymutualgroup.herman.task.lambda;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Regions;
import com.atlassian.bamboo.deployments.execution.DeploymentTaskContext;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.libertymutualgroup.herman.aws.AbstractDeploymentTask;
import com.libertymutualgroup.herman.aws.AwsExecException;
import com.libertymutualgroup.herman.aws.credentials.BambooCredentialsHandler;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.aws.lambda.LambdaBroker;
import com.libertymutualgroup.herman.aws.lambda.LambdaPushContext;
import com.libertymutualgroup.herman.logging.AtlassianBuildLogger;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.task.common.CommonTaskProperties;
import com.libertymutualgroup.herman.util.ConfigurationUtil;
import com.libertymutualgroup.herman.util.PropertyHandlerUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

public class LambdaCreateTask extends AbstractDeploymentTask {

    @Autowired
    public LambdaCreateTask(CustomVariableContext customVariableContext) {
        super(customVariableContext);
    }

    @Override
    public TaskResult doExecute(final DeploymentTaskContext taskContext) {
        final AtlassianBuildLogger buildLogger = new AtlassianBuildLogger(taskContext.getBuildLogger());
        final AWSCredentials sessionCredentials = BambooCredentialsHandler.getCredentials(taskContext);
        final Regions region = Regions.fromName(taskContext.getConfigurationMap().get("awsRegion"));
        buildLogger.addLogEntry("Starting Herman (version: " + getClass().getPackage().getImplementationVersion() + ") " + getClass().getName() + " in aws region: " + region.getName());
        final PropertyHandler handler = PropertyHandlerUtil
            .getTaskContextPropertyHandler(taskContext, sessionCredentials, getCustomVariableContext());

        LambdaPushContext context = new LambdaPushContext()
            .withSessionCredentials(sessionCredentials)
            .withRootPath(taskContext.getRootDirectory().getAbsolutePath())
            .withBambooPropertyHandler(handler)
            .withLogger(buildLogger)
            .withTaskProperties(getTaskProperties(sessionCredentials, buildLogger, region, handler));

        LambdaBroker lambdaBroker = new LambdaBroker(context,
            buildLogger,
            region);
        try {
            lambdaBroker.brokerLambda();
        } catch (IOException e) {
            throw new AwsExecException(e);
        }

        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }

    CommonTaskProperties getTaskProperties(AWSCredentials sessionCredentials, HermanLogger hermanLogger, Regions region, PropertyHandler handler) {
        try {
            String lambdaCreateTaskPropertiesYml = ConfigurationUtil.getHermanConfigurationAsString(sessionCredentials, hermanLogger, region);
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            return objectMapper.readValue(handler.mapInProperties(lambdaCreateTaskPropertiesYml), CommonTaskProperties.class);
        } catch (Exception ex) {
            throw new RuntimeException("Error getting Lambda Create Task Properties", ex);
        }
    }
}
