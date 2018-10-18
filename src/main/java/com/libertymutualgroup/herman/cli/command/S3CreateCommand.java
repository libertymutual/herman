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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.libertymutualgroup.herman.aws.credentials.CredentialsHandler;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.aws.ecs.broker.s3.S3Broker;
import com.libertymutualgroup.herman.aws.ecs.broker.s3.S3CreateContext;
import com.libertymutualgroup.herman.aws.ecs.broker.s3.S3CreateProperties;
import com.libertymutualgroup.herman.cli.Cli;
import com.libertymutualgroup.herman.task.cli.s3.S3CreateTaskConfiguration;
import com.libertymutualgroup.herman.util.ConfigurationUtil;
import com.libertymutualgroup.herman.util.FileUtil;
import com.libertymutualgroup.herman.util.PropertyHandlerUtil;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Help;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.ParentCommand;

@Command(name = "s3-create", mixinStandardHelpOptions = true)
public class S3CreateCommand implements Runnable {
    @ParentCommand
    private Cli cli;

    @Option(names = {"-d", "--directory"}, description = "Path to configuration files", showDefaultValue = Help.Visibility.ALWAYS)
    private String rootPath = System.getProperty("user.dir");

    @Option(names = {"-e", "-env", "--environment"}, description = "Environment to deploy")
    private String environmentName;

    @Option(names = {"-t", "--timeout"}, description = "Task timeout (in minutes)", showDefaultValue = Help.Visibility.ALWAYS)
    private int timeout = 5;

    @Option(names = {"-v", "-vars", "--variables"}, description = "Custom build variables to be injected. <KEY>=<VALUE>")
    private Map<String, String> customVariables = new HashMap<>();

    @Override
    public void run() {
        String absPath = new File(this.rootPath).getAbsolutePath();
        cli.getLogger().addLogEntry("Starting S3 Create...");
        S3CreateTaskConfiguration config = new S3CreateTaskConfiguration()
            .withRootPath(absPath)
            .withTimeout(timeout)
            .withEnvironmentName(environmentName)
            .withRegion(cli.getRegion())
            .withCustomVariables(customVariables);


        final AWSCredentials sessionCredentials = CredentialsHandler.getCredentials();

        PropertyHandler propertyHandler = PropertyHandlerUtil.getCliPropertyHandler(sessionCredentials,cli.getLogger(),
                config.getEnvironmentName(),
                config.getRootPath(),
                config.getCustomVariables());

        String s3CreateTaskPropertiesYml = ConfigurationUtil.getHermanConfigurationAsString(sessionCredentials, cli.getLogger(), config.getRegion());
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        S3CreateProperties properties = new S3CreateProperties();
        try {
            properties = objectMapper.readValue(s3CreateTaskPropertiesYml, S3CreateProperties.class);
        } catch(Exception e){
            cli.getLogger().addErrorLogEntry("Error getting S3 Create Task Properties from config bucket. Continuing...", e);
        }

        S3CreateContext s3CreateContext = new S3CreateContext()
                .withPropertyHandler(propertyHandler)
                .withLogger(cli.getLogger())
                .withRegion(config.getRegion())
                .withRootPath(config.getRootPath())
                .withSessionCredentials(sessionCredentials)
                .withTaskProperties(properties)
                .withFileUtil(new FileUtil(config.getRootPath(), cli.getLogger()));

        S3Broker s3Broker = new S3Broker(s3CreateContext);
        s3Broker.brokerFromConfigurationFile();

        cli.getLogger().addLogEntry("Done!");
    }

}
