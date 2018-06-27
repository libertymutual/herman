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
package com.libertymutualgroup.herman.aws.ecs.broker.s3;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Regions;
import com.libertymutualgroup.herman.aws.ecs.EcsPushContext;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.task.common.CommonTaskProperties;

public class S3CreateContext {

    private HermanLogger logger;
    private PropertyHandler bambooPropertyHandler;
    private AWSCredentials sessionCredentials;
    private Regions region;
    private String rootPath;
    private CommonTaskProperties taskProperties;

    public HermanLogger getLogger() {
        return logger;
    }

    public void setLogger(HermanLogger logger) {
        this.logger = logger;
    }

    public PropertyHandler getBambooPropertyHandler() {
        return bambooPropertyHandler;
    }

    public void setBambooPropertyHandler(PropertyHandler bambooPropertyHandler) {
        this.bambooPropertyHandler = bambooPropertyHandler;
    }

    public AWSCredentials getSessionCredentials() {
        return sessionCredentials;
    }

    public void setSessionCredentials(AWSCredentials sessionCredentials) {
        this.sessionCredentials = sessionCredentials;
    }

    public Regions getRegion() {
        return region;
    }

    public void setRegion(Regions region) {
        this.region = region;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public CommonTaskProperties getTaskProperties() {
        return taskProperties;
    }

    public void setTaskProperties(CommonTaskProperties taskProperties) {
        this.taskProperties = taskProperties;
    }

    public S3CreateContext withLogger(final HermanLogger logger) {
        this.logger = logger;
        return this;
    }

    public S3CreateContext withBambooPropertyHandler(final PropertyHandler bambooPropertyHandler) {
        this.bambooPropertyHandler = bambooPropertyHandler;
        return this;
    }

    public S3CreateContext withSessionCredentials(final AWSCredentials sessionCredentials) {
        this.sessionCredentials = sessionCredentials;
        return this;
    }

    public S3CreateContext withRegion(final Regions region) {
        this.region = region;
        return this;
    }

    public S3CreateContext withRootPath(final String rootPath) {
        this.rootPath = rootPath;
        return this;
    }

    public S3CreateContext withTaskProperties(final CommonTaskProperties taskProperties) {
        this.taskProperties = taskProperties;
        return this;
    }

    @Override
    public String toString() {
        return "S3CreateContext{" +
            "logger=" + logger +
            ", bambooPropertyHandler=" + bambooPropertyHandler +
            ", sessionCredentials=" + sessionCredentials +
            ", region='" + region + '\'' +
            ", rootPath='" + rootPath + '\'' +
            ", taskProperties=" + taskProperties +
            '}';
    }

    public S3CreateContext fromECSPushContext(EcsPushContext pushContext) {
        this.logger = pushContext.getLogger();
        this.bambooPropertyHandler = pushContext.getPropertyHandler();
        this.sessionCredentials = pushContext.getSessionCredentials();
        this.region = pushContext.getRegion();
        this.rootPath = pushContext.getRootPath();
        this.taskProperties = pushContext.getTaskProperties();
        return this;
    }
}
