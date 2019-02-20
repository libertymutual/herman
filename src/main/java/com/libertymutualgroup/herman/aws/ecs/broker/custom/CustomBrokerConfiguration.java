package com.libertymutualgroup.herman.aws.ecs.broker.custom;

import java.util.Map;

public class CustomBrokerConfiguration {
    private String description;
    private CustomBrokerPhase phase;
    private Map<String, String> variablesToPass;
    private Map<String,Object> defaults;

    public CustomBrokerConfiguration() {}

    public CustomBrokerConfiguration(String description, CustomBrokerPhase phase, Map<String, String> variablesToPass, Map<String, Object> defaults) {
        this.description = description;
        this.phase = phase;
        this.variablesToPass = variablesToPass;
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

    public void setPhase(CustomBrokerPhase phase) {
        this.phase = phase;
    }

    public Map<String, String> getVariablesToPass() {
        return variablesToPass;
    }

    public void setVariablesToPass(Map<String, String> variablesToPass) {
        this.variablesToPass = variablesToPass;
    }

    public Map<String, Object> getDefaults() {
        return defaults;
    }

    public void setDefaults(Map<String, Object> defaults) {
        this.defaults = defaults;
    }
}
