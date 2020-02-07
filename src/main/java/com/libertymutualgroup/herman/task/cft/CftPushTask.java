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
package com.libertymutualgroup.herman.task.cft;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Regions;
import com.atlassian.bamboo.deployments.execution.DeploymentTaskContext;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.libertymutualgroup.herman.aws.AbstractDeploymentTask;
import com.libertymutualgroup.herman.aws.AwsExecException;
import com.libertymutualgroup.herman.aws.cft.CftPush;
import com.libertymutualgroup.herman.aws.cft.CftPushContext;
import com.libertymutualgroup.herman.aws.credentials.BambooCredentialsHandler;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.logging.AtlassianBuildLogger;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.util.FileUtil;
import com.libertymutualgroup.herman.util.PropertyHandlerUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;

public class CftPushTask extends AbstractDeploymentTask {

    private static final List<String> CFT_FILE_NAMES = Arrays.asList("cft.template", "cft.yml", "cft.json");

    @Autowired
    public CftPushTask(CustomVariableContext customVariableContext) {
        super(customVariableContext);
    }

    @Override
    public TaskResult doExecute(final DeploymentTaskContext taskContext) {
        final AtlassianBuildLogger buildLogger = new AtlassianBuildLogger(taskContext.getBuildLogger());
        final AWSCredentials sessionCredentials = BambooCredentialsHandler.getCredentials(taskContext);
        final Regions awsRegion = Regions.fromName(taskContext.getConfigurationMap().get("awsRegion"));
        buildLogger.addLogEntry("Starting Herman (version: " + getClass().getPackage().getImplementationVersion() + ") " + getClass().getName() + " in aws region: " + awsRegion.getName());
        final PropertyHandler handler = PropertyHandlerUtil.getTaskContextPropertyHandler(taskContext, sessionCredentials, getCustomVariableContext());
        final CftPushTaskProperties taskProperties = CftPushPropertyFactory.getTaskProperties(sessionCredentials, buildLogger, awsRegion, handler);

        CftPushContext context = new CftPushContext()
            .withLogger(buildLogger)
            .withEnvName(taskContext.getDeploymentContext().getEnvironmentName())
            .withRootPath(taskContext.getRootDirectory().getAbsolutePath())
            .withPropertyHandler(handler)
            .withSessionCredentials(sessionCredentials)
            .withAwsClientConfig(BambooCredentialsHandler.getConfiguration())
            .withRegion(awsRegion)
            .withTaskProperties(taskProperties);

        CftPush push = new CftPush(context);
        String name = deriveStackName(taskContext.getDeploymentContext().getDeploymentProjectName(), context.getEnvName());
        String template = getTemplate(taskContext, buildLogger);
        push.push(name, template);

        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }

    private String getTemplate(DeploymentTaskContext taskContext, HermanLogger buildLogger) {
        String root = taskContext.getRootDirectory().getAbsolutePath();
        FileUtil fileUtil = new FileUtil(root, buildLogger);

        String template = null;
        for (String fileName: CFT_FILE_NAMES) {
            boolean fileExists = fileUtil.fileExists(fileName);
            if (fileExists) {
                template = fileUtil.findFile(fileName, false);
                buildLogger.addLogEntry("Template used: " + fileName);
            }
        }
        if (template == null) {
            throw new AwsExecException("CloudFormation template not found. Valid file names: "
                + String.join(", ", CFT_FILE_NAMES));
        }
        return template;
    }

    private String deriveStackName(String deployProject, String deployEnvironment) {
        String concat = deployProject.replace(" ", "-") + "-" + deployEnvironment.replace(" ", "-");
        return concat.toLowerCase();
    }

}
