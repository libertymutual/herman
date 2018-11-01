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
import com.libertymutualgroup.herman.aws.credentials.CredentialsHandler;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.aws.ecs.broker.s3.BucketMeta;
import com.libertymutualgroup.herman.aws.ecs.broker.s3.S3Broker;
import com.libertymutualgroup.herman.aws.ecs.broker.s3.S3CreateContext;
import com.libertymutualgroup.herman.cli.Cli;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.task.s3.S3CreateTaskProperties;
import com.libertymutualgroup.herman.util.ConfigurationUtil;
import com.libertymutualgroup.herman.util.FileUtil;
import com.libertymutualgroup.herman.util.PropertyHandlerUtil;

import java.io.File;
import java.io.FileWriter;
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

    @Option(names = {"-v", "-vars", "--variables"}, description = "Custom build variables to be injected. <KEY>=<VALUE>")
    private Map<String, String> customVariables = new HashMap<>();

    @Override
    public void run() {
        executeS3Task(
                cli.getLogger(),
                cli.getRegion(),
                new CredentialsHandler(),
                new PropertyHandlerUtil(),
                new ConfigurationUtil());
    }

    public void executeS3Task(
            HermanLogger logger,
            Regions region,
            CredentialsHandler credentialsHandler,
            PropertyHandlerUtil propertyHandlerUtil,
            ConfigurationUtil configurationUtil) {
        String absPath = new File(this.rootPath).getAbsolutePath();
        final AWSCredentials sessionCredentials = credentialsHandler.getAWSCredentials();

        PropertyHandler propertyHandler = propertyHandlerUtil.getCliPropertyHandler(sessionCredentials, logger, environmentName, absPath, customVariables);

        S3CreateTaskProperties properties = configurationUtil.getConfigProperties(sessionCredentials, logger, region, S3CreateTaskProperties.class);

        S3CreateContext s3CreateContext = new S3CreateContext().withPropertyHandler(propertyHandler).withLogger(logger).withRegion(region).withRootPath(absPath).withSessionCredentials(sessionCredentials).withTaskProperties(properties).withFileUtil(new FileUtil(absPath, logger));

        S3Broker s3Broker = getBroker(s3CreateContext);
        BucketMeta meta = s3Broker.brokerFromConfigurationFile();

        logger.addLogEntry("S3 bucket name: " + meta.getName());
        logger.addLogEntry("S3 bucket region: " + meta.getRegion());

        try {
            FileWriter fileWriter = new FileWriter("s3.output.properties");
            fileWriter.write("bamboo.s3.brokered.name=" + meta.getName());
            fileWriter.write(System.lineSeparator());
            fileWriter.write("bamboo.s3.brokered.region=" + meta.getRegion());
            fileWriter.flush();
            fileWriter.close();
        } catch (Exception e) {
            logger.addErrorLogEntry("Could not create file for output properties");
        }
    }

    public S3Broker getBroker(S3CreateContext context){
        return new S3Broker(context);
    }

}
