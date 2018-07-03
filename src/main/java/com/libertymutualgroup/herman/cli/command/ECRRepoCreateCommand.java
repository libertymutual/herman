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
import com.libertymutualgroup.herman.task.cli.ecr.ECRRepoCreateTask;
import com.libertymutualgroup.herman.task.cli.ecr.ECRRepoTaskConfiguration;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Help;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.ParentCommand;

@Command(name = "ecr-repo-create", mixinStandardHelpOptions = true)
public class ECRRepoCreateCommand implements Runnable {
    @ParentCommand
    private Cli cli;

    @Option(names = {"-repo", "--repoName"}, description = "Name of ECR repository to create", showDefaultValue = Help.Visibility.ALWAYS, arity = "1")
    private String repoName;

    @Override public void run() {
        cli.getLogger().addLogEntry("Starting ECR Create Repo...");
        ECRRepoTaskConfiguration config = new ECRRepoTaskConfiguration()
            .withRegion(cli.getRegion())
            .withRepoName(repoName);
        ECRRepoCreateTask createTask = new ECRRepoCreateTask(cli.getLogger());
        createTask.runTask(config);
    }
}
