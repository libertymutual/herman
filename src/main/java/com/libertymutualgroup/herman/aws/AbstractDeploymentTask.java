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
package com.libertymutualgroup.herman.aws;

import com.atlassian.bamboo.deployments.execution.DeploymentTaskContext;
import com.atlassian.bamboo.deployments.execution.DeploymentTaskType;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.util.concurrent.NotNull;

public abstract class AbstractDeploymentTask implements DeploymentTaskType {

    private final CustomVariableContext customVariableContext;

    /**
     * @param customVariableContext bamboo context to read variables.
     */
    public AbstractDeploymentTask(CustomVariableContext customVariableContext) {
        this.customVariableContext = customVariableContext;
    }

    @Override
    @NotNull
    public final TaskResult execute(@NotNull final DeploymentTaskContext taskContext) throws TaskException {
        return doExecute(taskContext);
    }

    protected abstract TaskResult doExecute(DeploymentTaskContext taskContext) throws TaskException;

    public CustomVariableContext getCustomVariableContext() {
        return this.customVariableContext;
    }
}
