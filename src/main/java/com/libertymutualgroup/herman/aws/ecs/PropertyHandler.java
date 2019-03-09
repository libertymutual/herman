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

import com.libertymutualgroup.herman.aws.AwsExecException;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.util.FileUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class PropertyHandler {
    private static final Pattern PROPERTY_PATTERN = Pattern.compile("\\$\\{([a-zA-Z0-9\\.\\_\\-]+)\\}");
    private HermanLogger hermanLogger;
    private String environmentName;
    private String rootDirectory;
    Properties props = new Properties();

    public PropertyHandler(HermanLogger hermanLogger, String environmentName, String rootDirectory) {
        this.hermanLogger = hermanLogger;
        this.environmentName = environmentName;
        this.rootDirectory = rootDirectory;
    }

    public void addProperty(String key, String value) {
        props.put(key, value);
    }

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

        if(getPropertiesToMatch(result).isEmpty()){
            return result;
        }

        return mapInProperties(result);

    }

    private Set<String> getPropertiesToMatch(String template) {
        Set<String> propertiesToMatch = new HashSet<>();
        Matcher propMatcher = PROPERTY_PATTERN.matcher(template);
        while (propMatcher.find()) {
            String propVal = propMatcher.group();
            propertiesToMatch.add(propVal);
        }
        return propertiesToMatch;
    }

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

    public abstract String lookupVariable(String inputKey);

    private void importPropFiles() {
        FileUtil util = new FileUtil(this.rootDirectory, this.hermanLogger);
        String envProps = util.findFile(this.environmentName + ".properties", true);

        if (props != null && envProps != null) {
            try {
                InputStream propStream = new ByteArrayInputStream(envProps.getBytes());
                props.load(propStream);
            } catch (IOException e) {
                hermanLogger.addErrorLogEntry("Error loading " + this.environmentName + ".properties: " + e.getMessage());
            }
        } else {
            hermanLogger.addErrorLogEntry("No property file was loaded - we looked for " + this.environmentName + ".properties");
        }
    }
}