package com.libertymutualgroup.herman.task.cli.s3;

import com.amazonaws.regions.Regions;

import java.util.Map;

public class S3CreateTaskConfiguration {

    private String rootPath;
    private String environmentName;
    private Regions region;
    private int timeout;
    private Map<String, String> customVariables;

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

    public S3CreateTaskConfiguration withRootPath(final String rootPath) {
        this.rootPath = rootPath;
        return this;
    }

    public S3CreateTaskConfiguration withEnvironmentName(final String environmentName) {
        this.environmentName = environmentName;
        return this;
    }

    public S3CreateTaskConfiguration withTimeout(final int timeout) {
        this.timeout = timeout;
        return this;
    }

    public S3CreateTaskConfiguration withRegion(final Regions region) {
        this.region = region;
        return this;
    }

    public S3CreateTaskConfiguration withCustomVariables(final Map<String, String> customVariables) {
        this.customVariables = customVariables;
        return this;
    }



}
