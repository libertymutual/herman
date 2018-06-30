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
package com.libertymutualgroup.herman.task.cli.ecs;

import com.amazonaws.regions.Regions;

import java.util.Map;

public class ECSPushTaskConfiguration {
    private String rootPath;
    private int timeout;
    private String environmentName;
    private Regions region;
    private Map<String, String> customVariables;
    private String customConfigurationBucket;

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
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

    public String getCustomConfigurationBucket() {
        return customConfigurationBucket;
    }

    public void setCustomConfigurationBucket(String customConfigurationBucket) {
        this.customConfigurationBucket = customConfigurationBucket;
    }

    public ECSPushTaskConfiguration withRootPath(final String rootPath) {
        this.rootPath = rootPath;
        return this;
    }

    public ECSPushTaskConfiguration withTimeout(final int timeout) {
        this.timeout = timeout;
        return this;
    }

    public ECSPushTaskConfiguration withEnvironmentName(final String environmentName) {
        this.environmentName = environmentName;
        return this;
    }

    public ECSPushTaskConfiguration withRegion(final Regions region) {
        this.region = region;
        return this;
    }

    public ECSPushTaskConfiguration withCustomVariables(final Map<String, String> customVariables) {
        this.customVariables = customVariables;
        return this;
    }

    public ECSPushTaskConfiguration withCustomConfigurationBucket(final String customConfigurationBucket) {
        this.customConfigurationBucket = customConfigurationBucket;
        return this;
    }

    @Override
    public String toString() {
        return "ECSPushTaskConfiguration{" +
            "rootPath='" + rootPath + '\'' +
            ", timeout=" + timeout +
            ", environmentName='" + environmentName + '\'' +
            ", region=" + region +
            ", customVariables=" + customVariables +
            ", customConfigurationBucket='" + customConfigurationBucket + '\'' +
            '}';
    }
}