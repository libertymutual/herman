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
import com.libertymutualgroup.herman.aws.AwsExecException;
import com.libertymutualgroup.herman.logging.AtlassianBuildLogger;
import com.libertymutualgroup.herman.util.FileUtil;
import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TaskContextPropertyHandler implements PropertyHandler {

    static final Pattern PROPERTY_PATTERN = Pattern.compile("\\$\\{([a-zA-Z0-9\\.\\_\\-]+)\\}");
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskContextPropertyHandler.class);
    private Properties props = new Properties();
    private DeploymentTaskContext deploymentTaskContext;
    private CustomVariableContext customVariableContext;
    private Set<String> propertyKeysUsed = new HashSet<>();

    public TaskContextPropertyHandler(DeploymentTaskContext deploymentTaskContext,
        CustomVariableContext customVariableContext) {
        this.deploymentTaskContext = deploymentTaskContext;
        this.customVariableContext = customVariableContext;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.libertymutualgroup.herman.aws.ecs.PropertyHandler#addProperty(java.lang.String,
     * java.lang.String)
     */
    @Override
    public void addProperty(String key, String value) {
        props.put(key, value);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.libertymutualgroup.herman.aws.ecs.PropertyHandler#mapInProperties(java.lang.String)
     */
    @Override
    public String mapInProperties(String template) {
        importPropFiles();

        Set<String> propertiesToMatch = getPropertiesToMatch(template);
        String result = template;
        for (String prop : propertiesToMatch) {
            String token = prop.replace("${", "");
            token = token.replace("}", "");
            Pattern groupPattern = Pattern.compile("\\$\\{(" + token + ")\\}");
            Matcher matcher = groupPattern.matcher(result);
            String value = lookupVariable(token);
            if (value == null) {
                throw new AwsExecException("Missing property set for " + token);
            }
            result = matcher.replaceAll(value);
        }
        return result;

    }

    Set<String> getPropertiesToMatch(String template) {
        Set<String> propertiesToMatch = new HashSet<>();
        Matcher propMatcher = PROPERTY_PATTERN.matcher(template);
        while (propMatcher.find()) {
            String propVal = propMatcher.group();
            propertiesToMatch.add(propVal);
        }
        return propertiesToMatch;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.libertymutualgroup.herman.aws.ecs.PropertyHandler#lookupProperties(java.lang.String)
     */
    @Override
    public Properties lookupProperties(String... propList) {
        Properties newProps = new Properties();
        for (String prop : propList) {
            String val = lookupVariable(prop);
            if (val != null) {
                newProps.put(prop, val);
            }
        }
        return newProps;
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

    private void importPropFiles() {
        String env = deploymentTaskContext.getDeploymentContext().getEnvironmentName();
        FileUtil util = new FileUtil(deploymentTaskContext.getRootDirectory().getAbsolutePath(),
            new AtlassianBuildLogger(deploymentTaskContext.getBuildLogger()));
        String envProps = util.findFile(env + ".properties", true);

        if (props != null && envProps != null) {
            try {
                InputStream propStream = new ByteArrayInputStream(envProps.getBytes());
                props.load(propStream);
            } catch (IOException e) {
                LOGGER.debug("Error loading properties file: " + env, e);
                deploymentTaskContext.getBuildLogger()
                    .addBuildLogEntry("Error loading " + env + ".properties: " + e.getMessage());
            }
        }
    }

    public Set<String> getPropertyKeysUsed() {
        return propertyKeysUsed;
    }

}
