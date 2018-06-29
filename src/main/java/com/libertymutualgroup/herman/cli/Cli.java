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
package com.libertymutualgroup.herman.cli;

import com.amazonaws.regions.Regions;
import com.libertymutualgroup.herman.logging.SysoutLogger;
import com.libertymutualgroup.herman.task.cli.ecs.ECSPushTask;
import com.libertymutualgroup.herman.task.cli.ecs.ECSPushTaskConfiguration;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

@Command(description = "Runs Herman the AWS Task Helper", name = "herman", mixinStandardHelpOptions = true)
public class Cli implements Callable<Void> {

    private static final String CONFIG_BUCKET_TEMPLATE= "herman-configuration-<aws account #>-lts";

    @CommandLine.Parameters(paramLabel = "TASK", description = "Task to be executed", arity = "1")
    private Tasks task;

    @Option(names = {"-d", "--directory"}, description = "Path to configuration files", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    private String rootPath = System.getProperty("user.dir");

    @Option(names = {"-e", "-env", "--environment"}, description = "Environment to deploy")
    private String environmentName;

    @Option(names = {"-t", "--timeout"}, description = "Task timeout (in minutes)", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    private int timeout = 5;

    @Option(names = {"-r", "--region"}, description = "AWS Region to perform tasks", showDefaultValue = CommandLine.Help.Visibility.ALWAYS, arity = "1")
    private Regions region = Regions.US_EAST_1;

    @Option(names = {"-c", "--config"}, description = "Configuration S3 bucket name", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    private String configurationBucket = CONFIG_BUCKET_TEMPLATE;

    @Option(names = {"-v", "-vars", "--variables"}, description = "Custom build variables to be injected. <KEY>==<VALUE>")
    private Map<String, String> customVariables = new HashMap<>();

    public static void main(String[] args) {
        CommandLine.call(new Cli(), args);
    }

    @Override public Void call() throws Exception {
        SysoutLogger logger = new SysoutLogger();

        String absPath = new File(this.rootPath).getAbsolutePath();

        switch (task) {
            case ECS_PUSH:
                logger.addLogEntry("Starting ECS Push");
                ECSPushTaskConfiguration config = new ECSPushTaskConfiguration()
                    .withRootPath(absPath)
                    .withTimeout(timeout)
                    .withEnvironmentName(environmentName)
                    .withRegion(region)
                    .withCustomVariables(customVariables)
                    .withCustomConfigurationBucket(getCustomConfigurationBucket());

                ECSPushTask ecsPush = new ECSPushTask(logger);
                ecsPush.runTask(config);
                break;
            case CFT_PUSH:
                logger.addErrorLogEntry("Not yet implemented in CLI");
                System.exit(1);
                break;
            case ECR_REPO_CREATE:
                logger.addErrorLogEntry("Not yet implemented in CLI");
                System.exit(1);
                break;
            case LAMBDA_CREATE:
                logger.addErrorLogEntry("Not yet implemented in CLI");
                System.exit(1);
                break;
            case NEW_RELIC_DEPLOYMENT:
                logger.addErrorLogEntry("Not yet implemented in CLI");
                System.exit(1);
                break;
            case S3_CREATE:
                logger.addErrorLogEntry("Not yet implemented in CLI");
                System.exit(1);
                break;
            default:
                logger.addErrorLogEntry("Invalid task! Must be one of: " + String.join(", ", Tasks.getOptions()));
                System.exit(1);
                break;
        }
        return null;
    }

    private String getCustomConfigurationBucket() {
        String customConfigurationBucket = null;
        if (configurationBucket != null && !configurationBucket.equals(CONFIG_BUCKET_TEMPLATE)) {
            customConfigurationBucket = configurationBucket;
        }
        return customConfigurationBucket;
    }
}
