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
package com.libertymutualgroup.herman.task.ecs;

import com.amazonaws.regions.Regions;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.deployments.execution.DeploymentTaskContext;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.libertymutualgroup.herman.aws.AbstractDeploymentTask;
import com.libertymutualgroup.herman.aws.CredentialsHandler;
import com.libertymutualgroup.herman.aws.ecs.EcsPush;
import com.libertymutualgroup.herman.aws.ecs.EcsPushContext;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.aws.ecs.TaskContextPropertyHandler;
import com.libertymutualgroup.herman.util.FileUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class ECSPushTask extends AbstractDeploymentTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(ECSPushTask.class);
    private final static String TASK_CONFIG_FILE = "/config/plugin-tasks.yml";

    @Autowired
    public ECSPushTask(CustomVariableContext customVariableContext) {
        super(customVariableContext);
    }

    @Override
    public TaskResult doExecute(final DeploymentTaskContext taskContext) {
        final BuildLogger buildLogger = taskContext.getBuildLogger();
        int timeout = Integer.parseInt(taskContext.getConfigurationMap().getOrDefault("timeout",
            String.valueOf(ECSPushTaskConfigurator.DEFAULT_TIMEOUT)));

        ECSPushTaskProperties taskProperties = getTaskProperties();

        PropertyHandler handler = new TaskContextPropertyHandler(taskContext, getCustomVariableContext());
        handler.addProperty("herman.rdsCredentialBrokerImage", taskProperties.getRdsCredentialBrokerImage());

        EcsPushContext context = new EcsPushContext()
            .withLogger(buildLogger)
            .withBambooPropertyHandler(handler)
            .withEnvName(taskContext.getDeploymentContext().getEnvironmentName())
            .withSessionCredentials(CredentialsHandler.getCredentials(taskContext))
            .withAwsClientConfig(CredentialsHandler.getConfiguration())
            .withRegion(Regions.fromName(taskContext.getConfigurationMap().get("awsRegion")))
            .withTimeout(timeout)
            .withRootPath(taskContext.getRootDirectory().getAbsolutePath())
            .withTaskProperties(taskProperties);
        EcsPush push = new EcsPush(context);
        push.push();

        spitAscii(taskContext.getBuildLogger(), taskContext.getRootDirectory().getAbsolutePath());

        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }

    ECSPushTaskProperties getTaskProperties() {
        try {
            InputStream ecsPushTaskPropertiesStream = getClass().getResourceAsStream(TASK_CONFIG_FILE);
            String ecsPushTaskPropertiesYml = IOUtils.toString(ecsPushTaskPropertiesStream);
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            return objectMapper.readValue(ecsPushTaskPropertiesYml, ECSPushTaskProperties.class);
        } catch (Exception ex) {
            throw new RuntimeException("Error getting ECS Push Task Properties from " + TASK_CONFIG_FILE, ex);
        }
    }

    private void spitAscii(BuildLogger logger, String rootPath) {
        FileUtil fileUtil = new FileUtil(rootPath, logger);

        BufferedReader rdr;
        if (fileUtil.fileExists("ascii.txt")) {
            String ascii = fileUtil.findFile("ascii.txt", true);
            rdr = new BufferedReader(new StringReader(ascii));
        } else {
            InputStream file = getClass().getResourceAsStream("/asciiwhale.txt");
            rdr = new BufferedReader(new InputStreamReader(file));
        }

        try {
            String line;
            while ((line = rdr.readLine()) != null) {
                logger.addBuildLogEntry(line);
            }
        } catch (IOException e1) {
            LOGGER.debug("Error parsing ascii", e1);
            logger.addBuildLogEntry("...");
        }
    }
}
