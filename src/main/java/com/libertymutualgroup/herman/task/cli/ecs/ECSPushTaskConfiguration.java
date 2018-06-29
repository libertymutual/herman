/*
 * Copyright (C) 2018, Liberty Mutual Group
 *
 * Created on 6/29/18
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
}
