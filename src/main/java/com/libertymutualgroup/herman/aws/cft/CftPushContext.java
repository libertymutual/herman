/*
 * Copyright (C) 2018, Liberty Mutual Group
 *
 * Created on 8/8/18
 */
package com.libertymutualgroup.herman.aws.cft;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Regions;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.task.cft.CFTPushTaskProperties;

import java.util.Map;

public class CftPushContext {
    private HermanLogger logger;
    private String envName;
    private String rootPath;
    private PropertyHandler propertyHandler;
    private AWSCredentials sessionCredentials;
    private ClientConfiguration awsClientConfig;
    private Regions region;
    private Map<String, String> templateParameters;
    private CFTPushTaskProperties taskProperties;

    public HermanLogger getLogger() {
        return logger;
    }

    public void setLogger(HermanLogger logger) {
        this.logger = logger;
    }

    public String getEnvName() {
        return envName;
    }

    public void setEnvName(String envName) {
        this.envName = envName;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
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

    public ClientConfiguration getAwsClientConfig() {
        return awsClientConfig;
    }

    public void setAwsClientConfig(ClientConfiguration awsClientConfig) {
        this.awsClientConfig = awsClientConfig;
    }

    public Regions getRegion() {
        return region;
    }

    public void setRegion(Regions region) {
        this.region = region;
    }

    public Map<String, String> getTemplateParameters() {
        return templateParameters;
    }

    public void setTemplateParameters(Map<String, String> templateParameters) {
        this.templateParameters = templateParameters;
    }

    public CFTPushTaskProperties getTaskProperties() {
        return taskProperties;
    }

    public void setTaskProperties(CFTPushTaskProperties taskProperties) {
        this.taskProperties = taskProperties;
    }

    public CftPushContext withLogger(final HermanLogger logger) {
        this.logger = logger;
        return this;
    }

    public CftPushContext withEnvName(final String envName) {
        this.envName = envName;
        return this;
    }

    public CftPushContext withRootPath(final String rootPath) {
        this.rootPath = rootPath;
        return this;
    }

    public CftPushContext withPropertyHandler(final PropertyHandler propertyHandler) {
        this.propertyHandler = propertyHandler;
        return this;
    }

    public CftPushContext withSessionCredentials(final AWSCredentials sessionCredentials) {
        this.sessionCredentials = sessionCredentials;
        return this;
    }

    public CftPushContext withAwsClientConfig(final ClientConfiguration awsClientConfig) {
        this.awsClientConfig = awsClientConfig;
        return this;
    }

    public CftPushContext withRegion(final Regions region) {
        this.region = region;
        return this;
    }

    public CftPushContext withTemplateParameters(final Map<String, String> parameters) {
        this.templateParameters = parameters;
        return this;
    }

    public CftPushContext withTaskProperties(final CFTPushTaskProperties taskProperties) {
        this.taskProperties = taskProperties;
        return this;
    }

    @Override
    public String toString() {
        return "CftPushContext{" +
            "logger=" + logger +
            ", propertyHandler=" + propertyHandler +
            ", envName='" + envName + '\'' +
            ", sessionCredentials=" + sessionCredentials +
            ", awsClientConfig=" + awsClientConfig +
            ", region=" + region +
            ", rootPath='" + rootPath + '\'' +
            ", templateParameters='" + templateParameters + '\'' +
            ", taskProperties=" + taskProperties +
            '}';
    }
}
