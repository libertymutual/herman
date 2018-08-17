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
package com.libertymutualgroup.herman.task.bamboo.ecs.cluster;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.util.concurrent.NotNull;
import com.atlassian.util.concurrent.Nullable;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

import static com.libertymutualgroup.herman.util.AwsRegionUtil.getRegionList;

public class ECSClusterPushTaskConfigurator extends AbstractTaskConfigurator {

    public static final int DEFAULT_TIMEOUT = 5;
    private static final String TIMEOUT_NAME = "timeout";
    private static final String AWS_REGION = "awsRegion";

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull final ActionParametersMap params,
        @Nullable final TaskDefinition previousTaskDefinition) {
        final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);

        config.put(AWS_REGION, params.getString(AWS_REGION));
        String timeout = params.getString(TIMEOUT_NAME);
        if (!StringUtils.isEmpty(timeout)) {
            config.put(TIMEOUT_NAME, params.getString(TIMEOUT_NAME));
        } else {
            config.put(TIMEOUT_NAME, Integer.toString(DEFAULT_TIMEOUT));
        }
        return config;
    }

    @Override
    public void validate(@NotNull final ActionParametersMap params, @NotNull final ErrorCollection errorCollection) {
        super.validate(params, errorCollection);

        final String timeoutValue = params.getString(TIMEOUT_NAME);
        if (StringUtils.isEmpty(timeoutValue) || !StringUtils.isNumeric(timeoutValue)) {
            errorCollection.addError(TIMEOUT_NAME, "Please enter a valid integer");
        }
    }

    @Override
    public void populateContextForCreate(@NotNull final Map<String, Object> context) {
        super.populateContextForCreate(context);
        context.put(AWS_REGION, "us-east-1");
        context.put("regionList", getRegionList());
        context.put(TIMEOUT_NAME, DEFAULT_TIMEOUT);
    }

    @Override
    public void populateContextForEdit(@NotNull final Map<String, Object> context,
        @NotNull final TaskDefinition taskDefinition) {
        super.populateContextForEdit(context, taskDefinition);

        context.put("regionList", getRegionList());
        context.put(AWS_REGION, taskDefinition.getConfiguration().get(AWS_REGION));

        //Prepop for existing tasks
        String timeout = taskDefinition.getConfiguration().get(TIMEOUT_NAME);
        if (!StringUtils.isEmpty(timeout)) {
            context.put(TIMEOUT_NAME, taskDefinition.getConfiguration().get(TIMEOUT_NAME));
        } else {
            context.put(TIMEOUT_NAME, DEFAULT_TIMEOUT);
        }

    }
}
