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
package com.libertymutualgroup.herman.aws.ecs;

import com.atlassian.bamboo.deployments.execution.DeploymentTaskContext;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.bamboo.variable.VariableContext;
import com.atlassian.bamboo.variable.VariableDefinitionContext;
import com.libertymutualgroup.herman.logging.AtlassianBuildLogger;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TaskContextPropertyHandler extends PropertyHandler {
    private DeploymentTaskContext deploymentTaskContext;
    private CustomVariableContext customVariableContext;
    private Set<String> propertyKeysUsed = new HashSet<>();

    public TaskContextPropertyHandler(DeploymentTaskContext deploymentTaskContext,
        CustomVariableContext customVariableContext) {
        super(
            new AtlassianBuildLogger(deploymentTaskContext.getBuildLogger()),
            deploymentTaskContext.getDeploymentContext().getEnvironmentName(),
            deploymentTaskContext.getRootDirectory().getAbsolutePath()
        );
        this.deploymentTaskContext = deploymentTaskContext;
        this.customVariableContext = customVariableContext;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.libertymutualgroup.herman.aws.ecs.PropertyHandler#lookupVariable(java.lang.String)
     */
    @Override
    public String lookupVariable(String inputKey) {
        String key = inputKey.replace("bamboo.", "");

        VariableContext commonVars = deploymentTaskContext.getCommonContext().getVariableContext();
        VariableContext vars = deploymentTaskContext.getDeploymentContext().getVariableContext();

        if ("deploy.environment".equals(key)) {
            propertyKeysUsed.add(key);
            return deploymentTaskContext.getDeploymentContext().getEnvironmentName();
        }
        if ("aws.region".equals(key)) {
            propertyKeysUsed.add(key);
            return deploymentTaskContext.getConfigurationMap().get("awsRegion");
        }
        if ("deploy.project".equals(key)) {
            propertyKeysUsed.add(key);
            return deploymentTaskContext.getDeploymentContext().getDeploymentProjectName();
        }
        if ("deploy.version".equals(key)) {
            propertyKeysUsed.add(key);
            return deploymentTaskContext.getDeploymentContext().getDeploymentVersion().getName();
        }

        String value = props.getProperty(key);
        if (value == null) {
            value = lookupVar(vars.getEffectiveVariables(), key);
        }
        if (value == null) {
            value = lookupVar(vars.getResultVariables(), key);
        }
        // check sys env with .
        if (value == null) {
            value = lookupVar(vars.getOriginalVariables(), key);
        }
        // //check sysenv with _
        if (value == null) {
            value = lookupVar(commonVars.getEffectiveVariables(), key);
        }
        if (value == null) {
            value = lookupVar(commonVars.getResultVariables(), key);
        }

        // Get value of a custom variable
        if (value == null) {
            for (Map.Entry<String, VariableDefinitionContext> entry : customVariableContext.getVariableContexts()
                .entrySet()) {
                if (key.equals(entry.getKey())) {
                    propertyKeysUsed.add(key);
                    value = entry.getValue().getValue();
                    break;
                }
            }
        }

        return value;
    }

    private String lookupVar(Map<String, VariableDefinitionContext> map, String key) {
        VariableDefinitionContext ctxt = map.get(key);
        if (ctxt != null) {
            propertyKeysUsed.add(key);
            return ctxt.getValue();
        } else {
            return null;
        }
    }

    Set<String> getPropertyKeysUsed() {
        return propertyKeysUsed;
    }
}
