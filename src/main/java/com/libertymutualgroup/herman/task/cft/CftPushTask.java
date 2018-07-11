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
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.libertymutualgroup.herman.aws.AbstractDeploymentTask;
import com.libertymutualgroup.herman.aws.cft.CftPush;
import com.libertymutualgroup.herman.aws.credentials.BambooCredentialsHandler;
import com.libertymutualgroup.herman.logging.AtlassianBuildLogger;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.util.ConfigurationUtil;
import org.springframework.beans.factory.annotation.Autowired;

public class CftPushTask extends AbstractDeploymentTask {

    @Autowired
    public CftPushTask(CustomVariableContext customVariableContext) {
        super(customVariableContext);
    }

    @Override
    public TaskResult doExecute(final DeploymentTaskContext taskContext) throws TaskException {
        final AtlassianBuildLogger buildLogger = new AtlassianBuildLogger(taskContext.getBuildLogger());
        final AWSCredentials sessionCredentials = BambooCredentialsHandler.getCredentials(taskContext);

        final Regions awsRegion = Regions.fromName(taskContext.getConfigurationMap().get("awsRegion"));

        CftPush push = new CftPush(buildLogger,
            taskContext,
            BambooCredentialsHandler.getCredentials(taskContext),
            BambooCredentialsHandler.getConfiguration(),
            awsRegion,
            getCustomVariableContext(),
            getTaskProperties(sessionCredentials, buildLogger, awsRegion));
        push.push();

        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }

    CFTPushTaskProperties getTaskProperties(AWSCredentials sessionCredentials, HermanLogger hermanLogger, Regions region) {
        try {
            String cftPushTaskPropertiesYml = ConfigurationUtil.getHermanConfigurationAsString(sessionCredentials, hermanLogger, region);
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            return objectMapper.readValue(cftPushTaskPropertiesYml, CFTPushTaskProperties.class);
        } catch (Exception ex) {
            throw new RuntimeException("Error getting Cft Push Task Properties", ex);
        }
    }

}
