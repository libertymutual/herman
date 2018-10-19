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
import com.libertymutualgroup.herman.task.s3.S3CreateTaskProperties;
import com.libertymutualgroup.herman.util.FileUtil;

public class S3CreateContext {

    private HermanLogger logger;
    private PropertyHandler propertyHandler;
    private AWSCredentials sessionCredentials;
    private Regions region;
    private String rootPath;
    private S3CreateTaskProperties taskProperties;
    private FileUtil fileUtil;

    public S3CreateContext fromECSPushContext(EcsPushContext pushContext) {
        this.logger = pushContext.getLogger();
        this.propertyHandler = pushContext.getPropertyHandler();
        this.sessionCredentials = pushContext.getSessionCredentials();
        this.region = pushContext.getRegion();
        this.rootPath = pushContext.getRootPath();
        this.taskProperties = new S3CreateTaskProperties()
            .fromECSPushTaskProperties(pushContext.getTaskProperties());
        this.fileUtil = new FileUtil(pushContext.getRootPath(), pushContext.getLogger());
        return this;
    }

    public HermanLogger getLogger() {
        return logger;
    }

    public void setLogger(HermanLogger logger) {
        this.logger = logger;
    }

    public PropertyHandler getPropertyHandler() {
        return propertyHandler;
    }

    public void setPropertyHandler(PropertyHandler propertyHandler) {
        this.propertyHandler = propertyHandler;
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

    public S3CreateTaskProperties getTaskProperties() {
        return taskProperties;
    }

    public void setTaskProperties(S3CreateTaskProperties taskProperties) {
        this.taskProperties = taskProperties;
    }

    public FileUtil getFileUtil() {
        return fileUtil;
    }

    public void setFileUtil(FileUtil fileUtil) {
        this.fileUtil = fileUtil;
    }

    public S3CreateContext withLogger(final HermanLogger logger) {
        this.logger = logger;
        return this;
    }

    public S3CreateContext withPropertyHandler(
        final PropertyHandler propertyHandler) {
        this.propertyHandler = propertyHandler;
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

    public S3CreateContext withTaskProperties(
        final S3CreateTaskProperties taskProperties) {
        this.taskProperties = taskProperties;
        return this;
    }

    public S3CreateContext withFileUtil(final FileUtil fileUtil) {
        this.fileUtil = fileUtil;
        return this;
    }

    @Override
    public String toString() {
        return "S3CreateContext{" +
            "logger=" + logger +
            ", propertyHandler=" + propertyHandler +
            ", sessionCredentials=" + sessionCredentials +
            ", region=" + region +
            ", rootPath='" + rootPath + '\'' +
            ", taskProperties=" + taskProperties +
            ", fileUtil=" + fileUtil +
            '}';
    }
}
