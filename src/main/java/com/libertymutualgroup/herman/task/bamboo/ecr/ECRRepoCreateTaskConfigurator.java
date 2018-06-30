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
package com.libertymutualgroup.herman.task.bamboo.ecr;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.util.concurrent.NotNull;
import com.atlassian.util.concurrent.Nullable;

import java.util.Map;

import static com.libertymutualgroup.herman.util.AwsRegionUtil.getRegionList;

public class ECRRepoCreateTaskConfigurator extends AbstractTaskConfigurator {


    private static final String AWS_REGION = "awsRegion";
    private static final String REPO_NAME = "repoName";

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull final ActionParametersMap params,
        @Nullable final TaskDefinition previousTaskDefinition) {
        final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);
        config.put(REPO_NAME, params.getString(REPO_NAME));
        config.put(AWS_REGION, params.getString(AWS_REGION));
        return config;
    }

    @Override
    public void populateContextForCreate(@NotNull final Map<String, Object> context) {
        super.populateContextForCreate(context);
        context.put(REPO_NAME, "${bamboo.maven.artifactId}");
        context.put(AWS_REGION, "us-east-1");
        context.put("regionList", getRegionList());
    }

    @Override
    public void populateContextForEdit(@NotNull final Map<String, Object> context,
        @NotNull final TaskDefinition taskDefinition) {
        super.populateContextForEdit(context, taskDefinition);

        context.put("regionList", getRegionList());
        context.put(REPO_NAME, taskDefinition.getConfiguration().get(REPO_NAME));
        context.put(AWS_REGION, taskDefinition.getConfiguration().get(AWS_REGION));

    }

}
