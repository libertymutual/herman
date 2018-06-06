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

import com.amazonaws.regions.Regions;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.deployments.execution.DeploymentTaskContext;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.libertymutualgroup.herman.aws.AbstractDeploymentTask;
import com.libertymutualgroup.herman.aws.AwsExecException;
import com.libertymutualgroup.herman.aws.CredentialsHandler;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.aws.ecs.TaskContextPropertyHandler;
import com.libertymutualgroup.herman.aws.lambda.LambdaBroker;
import com.libertymutualgroup.herman.aws.lambda.LambdaPushContext;
import com.libertymutualgroup.herman.task.common.CommonTaskProperties;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class LambdaCreateTask extends AbstractDeploymentTask {

    private final static String TASK_CONFIG_FILE = "/config/plugin-tasks.yml";

    @Autowired
    public LambdaCreateTask(CustomVariableContext customVariableContext) {
        super(customVariableContext);
    }

    @Override
    public TaskResult doExecute(final DeploymentTaskContext taskContext) {
        final BuildLogger buildLogger = taskContext.getBuildLogger();
        PropertyHandler handler = new TaskContextPropertyHandler(taskContext, getCustomVariableContext());

        LambdaPushContext context = new LambdaPushContext()
            .withSessionCredentials(CredentialsHandler.getCredentials(taskContext))
            .withRootPath(taskContext.getRootDirectory().getAbsolutePath())
            .withBambooPropertyHandler(handler)
            .withLogger(buildLogger)
            .withTaskProperties(getTaskProperties());

        LambdaBroker lambdaBroker = new LambdaBroker(context,
            buildLogger,
            Regions.fromName(taskContext.getConfigurationMap().get("awsRegion")));
        try {
            lambdaBroker.brokerLambda();
        } catch (IOException e) {
            throw new AwsExecException(e);
        }

        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }

    CommonTaskProperties getTaskProperties() {
        try {
            InputStream lambdaCreateTaskPropertiesStream = getClass().getResourceAsStream(TASK_CONFIG_FILE);
            String lambdaCreateTaskPropertiesYml = IOUtils.toString(lambdaCreateTaskPropertiesStream);
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            return objectMapper.readValue(lambdaCreateTaskPropertiesYml, CommonTaskProperties.class);
        } catch (Exception ex) {
            throw new RuntimeException("Error getting Lambda Create Task Properties from " + TASK_CONFIG_FILE, ex);
        }
    }
}
