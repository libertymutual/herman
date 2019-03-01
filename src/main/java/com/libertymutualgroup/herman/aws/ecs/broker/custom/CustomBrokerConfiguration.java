package com.libertymutualgroup.herman.aws.ecs.broker.custom;

import java.util.Map;

public class CustomBrokerConfiguration {
    private String description;
    private String readme;
    private CustomBrokerPhase phase;
    private Map<String,Object> defaults;

    public CustomBrokerConfiguration() {}

    public CustomBrokerConfiguration(String description, CustomBrokerPhase phase, Map<String, Object> defaults) {
        this.description = description;
        this.phase = phase;
        this.defaults = defaults;
    }

    public CustomBrokerPhase getPhase() {
        return phase;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReadme() {
        return readme;
    }

    public void setReadme(String readme) {
        this.readme = readme;
    }

    public void setPhase(CustomBrokerPhase phase) {
        this.phase = phase;
    }

    public Map<String, Object> getDefaults() {
        return defaults;
    }

    public void setDefaults(Map<String, Object> defaults) {
        this.defaults = defaults;
    }
}
