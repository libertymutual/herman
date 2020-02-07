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
package com.libertymutualgroup.herman.task.bamboo.ecs.cluster;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Regions;
import com.atlassian.bamboo.deployments.execution.DeploymentTaskContext;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.libertymutualgroup.herman.aws.AbstractDeploymentTask;
import com.libertymutualgroup.herman.aws.credentials.BambooCredentialsHandler;
import com.libertymutualgroup.herman.aws.ecs.EcsPushContext;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.aws.ecs.cluster.EcsClusterPush;
import com.libertymutualgroup.herman.logging.AtlassianBuildLogger;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.task.ecs.ECSPushPropertyFactory;
import com.libertymutualgroup.herman.task.ecs.ECSPushTaskProperties;
import com.libertymutualgroup.herman.util.FileUtil;
import com.libertymutualgroup.herman.util.PropertyHandlerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

public class ECSClusterPushTask extends AbstractDeploymentTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(ECSClusterPushTask.class);

    @Autowired
    public ECSClusterPushTask(CustomVariableContext customVariableContext) {
        super(customVariableContext);
    }

    @Override
    public TaskResult doExecute(final DeploymentTaskContext taskContext) {
        final AtlassianBuildLogger buildLogger = new AtlassianBuildLogger(taskContext.getBuildLogger());
        final AWSCredentials sessionCredentials = BambooCredentialsHandler.getCredentials(taskContext);
        final Regions awsRegion = Regions.fromName(taskContext.getConfigurationMap().get("awsRegion"));
        buildLogger.addLogEntry("Starting Herman (version: " + getClass().getPackage().getImplementationVersion() + ") " + getClass().getName() + " in aws region: " + awsRegion.getName());
        final int timeout = Integer.parseInt(taskContext.getConfigurationMap().getOrDefault("timeout",
            String.valueOf(ECSClusterPushTaskConfigurator.DEFAULT_TIMEOUT)));
        final PropertyHandler handler = PropertyHandlerUtil.getTaskContextPropertyHandler(
            taskContext, sessionCredentials, getCustomVariableContext());
        final ECSPushTaskProperties taskProperties = ECSPushPropertyFactory.getTaskProperties(sessionCredentials, buildLogger, awsRegion, handler);

        EcsPushContext context = new EcsPushContext()
            .withLogger(buildLogger)
            .withPropertyHandler(handler)
            .withEnvName(taskContext.getDeploymentContext().getEnvironmentName())
            .withSessionCredentials(sessionCredentials)
            .withAwsClientConfig(BambooCredentialsHandler.getConfiguration())
            .withRegion(awsRegion)
            .withTimeout(timeout)
            .withRootPath(taskContext.getRootDirectory().getAbsolutePath())
            .withTaskProperties(taskProperties);
        EcsClusterPush ecsClusterPush = new EcsClusterPush(context);
        ecsClusterPush.pushCluster();

        spitAscii(buildLogger, taskContext.getRootDirectory().getAbsolutePath());

        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }

    private void spitAscii(HermanLogger logger, String rootPath) {
        FileUtil fileUtil = new FileUtil(rootPath, logger);

        BufferedReader rdr;
        if (fileUtil.fileExists("ascii.txt")) {
            String ascii = fileUtil.findFile("ascii.txt", true);
            rdr = new BufferedReader(new StringReader(ascii));
        } else {
            InputStream file = getClass().getResourceAsStream("/asciiecs.txt");
            rdr = new BufferedReader(new InputStreamReader(file));
        }

        try {
            String line;
            while ((line = rdr.readLine()) != null) {
                logger.addLogEntry(line);
            }
        } catch (IOException e1) {
            LOGGER.debug("Error parsing ascii", e1);
            logger.addLogEntry("...");
        }
    }
}
