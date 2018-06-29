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
package com.libertymutualgroup.herman.task.newrelic;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
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
import com.libertymutualgroup.herman.aws.ecs.TaskContextPropertyHandler;
import com.libertymutualgroup.herman.aws.ecs.broker.newrelic.NewRelicBroker;
import com.libertymutualgroup.herman.aws.ecs.broker.newrelic.NewRelicBrokerConfiguration;
import com.libertymutualgroup.herman.aws.ecs.broker.newrelic.NewRelicDefinition;
import com.libertymutualgroup.herman.logging.AtlassianBuildLogger;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.util.ConfigurationUtil;
import com.libertymutualgroup.herman.util.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;

public class NewRelicBrokerTask extends AbstractDeploymentTask {

    private static final String NEWRELIC_TEMPLATE = "newrelic-template";
    private static final String JSON = ".json";
    private static final String YML = ".yml";

    @Autowired
    public NewRelicBrokerTask(CustomVariableContext customVariableContext) {
        super(customVariableContext);
    }

    @Override
    public TaskResult doExecute(final DeploymentTaskContext taskContext) {
        final AtlassianBuildLogger buildLogger = new AtlassianBuildLogger(taskContext.getBuildLogger());
        final AWSCredentialsProvider awsCredentialsProvider = new AWSStaticCredentialsProvider(BambooCredentialsHandler.getCredentials(taskContext));

        final PropertyHandler bambooPropertyHandler = new TaskContextPropertyHandler(taskContext,
            getCustomVariableContext());
        final FileUtil fileUtil = new FileUtil(taskContext.getRootDirectory().getAbsolutePath(), buildLogger);

        NewRelicDefinition newRelicDefinition = getNewRelicDefinition(bambooPropertyHandler, buildLogger, fileUtil);
        buildLogger.addLogEntry(newRelicDefinition.toString());

        AWSLambda lambdaClient = AWSLambdaClientBuilder.standard()
            .withCredentials(awsCredentialsProvider)
            .withClientConfiguration(BambooCredentialsHandler.getConfiguration())
            .withRegion(Regions.fromName(taskContext.getConfigurationMap().get("awsRegion")))
            .build();

        NewRelicBrokerConfiguration newRelicBrokerConfiguration = getTaskProperties(
            awsCredentialsProvider.getCredentials(),
            buildLogger);
        NewRelicBroker newRelicBroker = new NewRelicBroker(
            bambooPropertyHandler,
            buildLogger,
            fileUtil,
            newRelicBrokerConfiguration,
            lambdaClient
        );
        newRelicBroker.brokerNewRelicApplicationDeployment(
            newRelicDefinition.getNewRelic(),
            newRelicDefinition.getPolicyName(),
            null,
            null);

        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }

    private NewRelicDefinition getNewRelicDefinition(PropertyHandler handler, HermanLogger buildLogger,
        FileUtil fileUtil) {
        String template;
        boolean isJson;
        if (fileUtil.fileExists(NEWRELIC_TEMPLATE + JSON)) {
            buildLogger.addLogEntry("Using " + NEWRELIC_TEMPLATE + JSON);
            template = fileUtil.findFile(NEWRELIC_TEMPLATE + JSON, false);
            isJson = true;
        } else if (fileUtil.fileExists(NEWRELIC_TEMPLATE + YML)) {
            buildLogger.addLogEntry("Using " + NEWRELIC_TEMPLATE + YML);
            template = fileUtil.findFile(NEWRELIC_TEMPLATE + YML, false);
            isJson = false;
        } else {
            throw new AwsExecException("No template provided!");
        }

        ObjectMapper mapper;
        if (isJson) {
            mapper = new ObjectMapper();
        } else {
            mapper = new ObjectMapper(new YAMLFactory());
        }
        NewRelicDefinition newRelicDefinition;
        try {
            newRelicDefinition = mapper.readValue(handler.mapInProperties(template), NewRelicDefinition.class);
        } catch (Exception e) {
            throw new AwsExecException("Error parsing template", e);
        }

        // Get the formatted policy name as some plans use bamboo prop values that need to be modified
        return newRelicDefinition.withFormattedPolicyName();
    }

    NewRelicBrokerConfiguration getTaskProperties(AWSCredentials sessionCredentials, HermanLogger hermanLogger) {
        try {
            String newRelicBrokerConfigurationYml = ConfigurationUtil.getHermanConfigurationAsString(sessionCredentials, hermanLogger);
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            return objectMapper.readValue(newRelicBrokerConfigurationYml, NewRelicBrokerConfiguration.class);
        } catch (Exception ex) {
            throw new RuntimeException("Error getting NewRelic Broker Configuration", ex);
        }
    }
}
