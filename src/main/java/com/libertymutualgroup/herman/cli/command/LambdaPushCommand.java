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
package com.libertymutualgroup.herman.cli.command;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.libertymutualgroup.herman.aws.AwsExecException;
import com.libertymutualgroup.herman.aws.credentials.CredentialsHandler;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.aws.lambda.LambdaBroker;
import com.libertymutualgroup.herman.aws.lambda.LambdaPushContext;
import com.libertymutualgroup.herman.cli.Cli;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.task.common.CommonTaskProperties;
import com.libertymutualgroup.herman.util.ConfigurationUtil;
import com.libertymutualgroup.herman.util.PropertyHandlerUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Help;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.ParentCommand;

@Command(name = "lambda-push", mixinStandardHelpOptions = true)
public class LambdaPushCommand implements Runnable {
    @ParentCommand
    private Cli cli;

    @Option(names = {"-d", "--directory"}, description = "Path to configuration files", showDefaultValue = Help.Visibility.ALWAYS)
    private String rootPath = System.getProperty("user.dir");

    @Option(names = {"-e", "-env", "--environment"}, description = "Environment to deploy")
    private String environmentName;

    @Option(names = {"-v", "-vars", "--variables"}, description = "Custom build variables to be injected. <KEY>=<VALUE>")
    private Map<String, String> customVariables = new HashMap<>();

    @Override
    public void run() {
        String absPath = new File(this.rootPath).getAbsolutePath();
        cli.getLogger().addLogEntry("Starting Lambda Push...");

        final AWSCredentials sessionCredentials = CredentialsHandler.getCredentials();
        final Regions region = cli.getRegion();
        final PropertyHandler handler = new PropertyHandlerUtil().getCliPropertyHandler(
                sessionCredentials, cli.getLogger(), environmentName, absPath, customVariables
        );
        LambdaPushContext context = new LambdaPushContext()
                .withSessionCredentials(sessionCredentials)
                .withRootPath(absPath)
                .withBambooPropertyHandler(handler)
                .withLogger(cli.getLogger())
                .withTaskProperties(getTaskProperties(sessionCredentials, cli.getLogger(), region, handler));

        LambdaBroker lambdaBroker = new LambdaBroker(context,
                cli.getLogger(),
                region);

        try {
            lambdaBroker.brokerLambda();
        } catch (IOException e) {
            throw new AwsExecException(e);
        }
    }

    private CommonTaskProperties getTaskProperties(AWSCredentials sessionCredentials, HermanLogger hermanLogger, Regions region, PropertyHandler handler) {
        try {
            String lambdaCreateTaskPropertiesYml = ConfigurationUtil.getHermanConfigurationAsString(sessionCredentials, hermanLogger, region);
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            return objectMapper.readValue(handler.mapInProperties(lambdaCreateTaskPropertiesYml), CommonTaskProperties.class);
        } catch (Exception ex) {
            throw new RuntimeException("Error getting Lambda Create Task Properties", ex);
        }
    }
}
