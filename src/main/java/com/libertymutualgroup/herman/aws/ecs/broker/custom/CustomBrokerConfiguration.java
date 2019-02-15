package com.libertymutualgroup.herman.aws.ecs.broker.custom;

import java.util.List;
import java.util.Map;

public class CustomBrokerConfiguration {
    private CustomBrokerPhase phase;
    private List<String> variablesToPass;
    private Map<String,Object> defaults;

    public CustomBrokerConfiguration() {}

    public CustomBrokerConfiguration(CustomBrokerPhase phase, List<String> variablesToPass, Map<String, Object> defaults) {
        this.phase = phase;
        this.variablesToPass = variablesToPass;
        this.defaults = defaults;
    }

    public CustomBrokerPhase getPhase() {
        return phase;
    }

    public void setPhase(CustomBrokerPhase phase) {
        this.phase = phase;
    }

    public List<String> getVariablesToPass() {
        return variablesToPass;
    }

    public void setVariablesToPass(List<String> variablesToPass) {
        this.variablesToPass = variablesToPass;
    }

    public Map<String, Object> getDefaults() {
        return defaults;
    }

    public void setDefaults(Map<String, Object> defaults) {
        this.defaults = defaults;
    }
}
