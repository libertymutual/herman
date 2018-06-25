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
package com.libertymutualgroup.herman.aws.lambda;

import com.amazonaws.auth.AWSCredentials;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.task.common.CommonTaskProperties;
import com.libertymutualgroup.herman.logging.HermanLogger;

public class LambdaPushContext {

    private AWSCredentials sessionCredentials;
    private String rootPath;
    private PropertyHandler bambooPropertyHandler;
    private HermanLogger logger;
    private CommonTaskProperties taskProperties;

    public AWSCredentials getSessionCredentials() {
        return sessionCredentials;
    }

    public void setSessionCredentials(AWSCredentials sessionCredentials) {
        this.sessionCredentials = sessionCredentials;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public PropertyHandler getBambooPropertyHandler() {
        return bambooPropertyHandler;
    }

    public void setBambooPropertyHandler(PropertyHandler bambooPropertyHandler) {
        this.bambooPropertyHandler = bambooPropertyHandler;
    }

    public HermanLogger getLogger() {
        return logger;
    }

    public void setLogger(HermanLogger logger) {
        this.logger = logger;
    }

    public CommonTaskProperties getTaskProperties() {
        return taskProperties;
    }

    public void setTaskProperties(CommonTaskProperties taskProperties) {
        this.taskProperties = taskProperties;
    }

    public LambdaPushContext withSessionCredentials(final AWSCredentials sessionCredentials) {
        this.sessionCredentials = sessionCredentials;
        return this;
    }

    public LambdaPushContext withRootPath(final String rootPath) {
        this.rootPath = rootPath;
        return this;
    }

    public LambdaPushContext withBambooPropertyHandler(
        final PropertyHandler bambooPropertyHandler) {
        this.bambooPropertyHandler = bambooPropertyHandler;
        return this;
    }

    public LambdaPushContext withLogger(final HermanLogger logger) {
        this.logger = logger;
        return this;
    }

    public LambdaPushContext withTaskProperties(
        final CommonTaskProperties taskProperties) {
        this.taskProperties = taskProperties;
        return this;
    }

    @Override
    public String toString() {
        return "LambdaPushContext{" +
            "sessionCredentials=" + sessionCredentials +
            ", rootPath='" + rootPath + '\'' +
            ", bambooPropertyHandler=" + bambooPropertyHandler +
            ", logger=" + logger +
            ", taskProperties=" + taskProperties +
            '}';
    }
}
