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
package com.libertymutualgroup.herman.task.ecr;

import com.amazonaws.regions.Regions;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.atlassian.bamboo.variable.VariableType;
import com.libertymutualgroup.herman.aws.CredentialsHandler;
import com.libertymutualgroup.herman.aws.ecr.EcrCreate;
import com.libertymutualgroup.herman.aws.ecr.EcrLogin;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

public class ECRRepoCreateTask implements TaskType {

    @Override
    public TaskResult execute(final TaskContext taskContext) {
        final BuildLogger buildLogger = taskContext.getBuildLogger();

        EcrCreate create = new EcrCreate(buildLogger, CredentialsHandler.getCredentials(taskContext),
            CredentialsHandler.getConfiguration(),
            Regions.fromName(taskContext.getConfigurationMap().get("awsRegion")));
        String repo = create.createRepo(taskContext.getConfigurationMap().get("repoName"));

        Properties p = new Properties();
        p.put("repository", repo);
        injectVariablesForBuild(p, taskContext);

        EcrLogin login = new EcrLogin(buildLogger, CredentialsHandler.getCredentials(taskContext),
            CredentialsHandler.getConfiguration(),
            Regions.fromName(taskContext.getConfigurationMap().get("awsRegion")));
        login.login();

        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }


    private void injectVariablesForBuild(final Properties properties, final TaskContext taskContext) {
        final Map<String, String> customBuildData = taskContext.getBuildContext().getBuildResult().getCustomBuildData();
        for (Entry<Object, Object> variable : properties.entrySet()) {
            String key = "ecs." + variable.getKey();
            String value = (String) variable.getValue();
            customBuildData.put(key, value);

            taskContext.getBuildLogger()
                .addBuildLogEntry("Injected variable bamboo." + key + "=" + value + " for " + VariableType.JOB);
        }
    }


}
