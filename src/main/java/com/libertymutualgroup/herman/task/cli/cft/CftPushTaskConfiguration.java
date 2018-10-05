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
package com.libertymutualgroup.herman.task.cli.cft;

import com.amazonaws.regions.Regions;

import java.util.Map;

public class CftPushTaskConfiguration {
    private String rootPath;
    private String environmentName;
    private Regions region;
    private String stackName;
    private String templateFile;
    private Map<String, String> customVariables;
    private String customConfigurationLambda;

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

    public Regions getRegion() {
        return region;
    }

    public void setRegion(Regions region) {
        this.region = region;
    }

    public Map<String, String> getCustomVariables() {
        return customVariables;
    }

    public void setCustomVariables(Map<String, String> customVariables) {
        this.customVariables = customVariables;
    }

    public String getCustomConfigurationLambda() {
        return customConfigurationLambda;
    }

    public void setCustomConfigurationLambda(String customConfigurationLambda) {
        this.customConfigurationLambda = customConfigurationLambda;
    }

    public CftPushTaskConfiguration withCustomConfigurationLambda(String customConfigurationLambda) {
        this.customConfigurationLambda = customConfigurationLambda;
        return this;
    }

    public CftPushTaskConfiguration withRootPath(final String rootPath) {
        this.rootPath = rootPath;
        return this;
    }

    public CftPushTaskConfiguration withEnvironmentName(final String environmentName) {
        this.environmentName = environmentName;
        return this;
    }

    public CftPushTaskConfiguration withRegion(final Regions region) {
        this.region = region;
        return this;
    }

    public String getStackName() {
        return stackName;
    }

    public void setStackName(String stackName) {
        this.stackName = stackName;
    }

    public CftPushTaskConfiguration withStackName(String stackName) {
        this.stackName = stackName;
        return this;
    }

    public String getTemplateFile() {
        return templateFile;
    }

    public void setTemplateFile(String templateFile) {
        this.templateFile = templateFile;
    }

    public CftPushTaskConfiguration withTemplateFile(String templateName) {
        this.templateFile = templateName;
        return this;
    }

    public CftPushTaskConfiguration withCustomVariables(final Map<String, String> customVariables) {
        this.customVariables = customVariables;
        return this;
    }

    public CftPushTaskConfiguration withCustomConfigurationBucket(final String customConfigurationBucket) {
        this.customConfigurationLambda = customConfigurationBucket;
        return this;
    }

    @Override
    public String toString() {
        return "ECSPushTaskConfiguration{" +
            "rootPath='" + rootPath + '\'' +
            ", environmentName='" + environmentName + '\'' +
            ", region=" + region +
            ", stackName=" + stackName +
            ", templateFile=" + templateFile +
            ", customVariables=" + customVariables +
            ", customConfigurationLambda='" + customConfigurationLambda + '\'' +
            '}';
    }
}