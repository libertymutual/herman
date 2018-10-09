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

import com.libertymutualgroup.herman.cli.Cli;
import com.libertymutualgroup.herman.task.cli.cft.CftPushTask;
import com.libertymutualgroup.herman.task.cli.cft.CftPushTaskConfiguration;
import picocli.CommandLine;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.ParentCommand;

@Command(name = "cft-push", mixinStandardHelpOptions = true)
public class CftPushCommand implements Runnable{
    @ParentCommand
    private Cli cli;

    @CommandLine.Option(names = {"-d", "--directory"}, description = "Path to configuration files", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    private String rootPath = System.getProperty("user.dir");

    @CommandLine.Option(names = {"-e", "-env", "--environment"}, description = "Environment to deploy")
    private String environmentName;

    @CommandLine.Option(names = {"-f", "--file"}, description = "Template File")
    private String templateFile;

    @CommandLine.Option(names = {"-n", "--name"}, description = "Stack Name")
    private String stackName;

    @CommandLine.Option(names = {"-v", "-vars", "--variables"}, description = "Custom build variables to be injected. <KEY>=<VALUE>")
    private Map<String, String> customVariables = new HashMap<>();

    @Override public void run() {
        String absPath = new File(this.rootPath).getAbsolutePath();
        cli.getLogger().addLogEntry("Starting CFT Push...");
        CftPushTask cftPushTask = new CftPushTask(cli.getLogger());
        CftPushTaskConfiguration config = new CftPushTaskConfiguration()
            .withRootPath(absPath)
            .withEnvironmentName(environmentName)
            .withRegion(cli.getRegion())
            .withCustomConfigurationBucket(cli.getCustomConfigurationBucket())
            .withCustomVariables(customVariables)
            .withTemplateFile(templateFile)
            .withStackName(stackName);

        cftPushTask.runTask(config);
    }
}