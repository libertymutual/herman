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
package com.libertymutualgroup.herman.task.s3;

import com.amazonaws.regions.Regions;
import com.atlassian.bamboo.deployments.execution.DeploymentTaskContext;
import com.atlassian.bamboo.task.TaskException;
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
import com.libertymutualgroup.herman.task.common.CommonTaskProperties;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;

public class S3CreateTask extends AbstractDeploymentTask {

    private final static String TASK_CONFIG_FILE = "/config/plugin-tasks.yml";

    @Autowired
    public S3CreateTask(CustomVariableContext customVariableContext) {
        super(customVariableContext);
    }

    @Override
    public TaskResult doExecute(final DeploymentTaskContext taskContext) throws TaskException {
        final AtlassianBuildLogger buildLogger = new AtlassianBuildLogger(taskContext.getBuildLogger());
        Regions awsRegion = Regions.fromName(taskContext.getConfigurationMap().getOrDefault("awsRegion",
            String.valueOf(S3CreateTaskConfigurator.DEFAULT_REGION)));
        PropertyHandler handler = new TaskContextPropertyHandler(taskContext, getCustomVariableContext());

        S3CreateContext s3CreateContext = new S3CreateContext()
            .withBambooPropertyHandler(handler)
            .withLogger(buildLogger)
            .withRegion(awsRegion)
            .withRootPath(taskContext.getRootDirectory().getAbsolutePath())
            .withSessionCredentials(BambooCredentialsHandler.getCredentials(taskContext))
            .withTaskProperties(getTaskProperties());

        S3Broker s3Broker = new S3Broker(s3CreateContext);
        BucketMeta meta = s3Broker.brokerFromConfigurationFile();

        taskContext.getDeploymentContext().getVariableContext().addLocalVariable("bamboo.s3.brokered.name",
            meta.getName());
        taskContext.getDeploymentContext().getVariableContext().addLocalVariable("bamboo.s3.brokered.region",
            meta.getRegion());

        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }

    CommonTaskProperties getTaskProperties() {
        try {
            InputStream lambdaCreateTaskPropertiesStream = getClass().getResourceAsStream(TASK_CONFIG_FILE);
            String lambdaCreateTaskPropertiesYml = IOUtils.toString(lambdaCreateTaskPropertiesStream);
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            return objectMapper.readValue(lambdaCreateTaskPropertiesYml, CommonTaskProperties.class);
        } catch (Exception ex) {
            throw new RuntimeException("Error getting S3 Create Task Task Properties from " + TASK_CONFIG_FILE, ex);
        }
    }
}
