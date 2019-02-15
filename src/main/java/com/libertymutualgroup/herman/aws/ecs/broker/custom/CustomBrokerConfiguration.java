package com.libertymutualgroup.herman.aws.ecs.broker.custom;

import java.util.List;

public class CustomBrokerConfiguration {
    private CustomBrokerRuntime type;
    private String name;
    private List<String> variablesToPass;

    public CustomBrokerConfiguration(CustomBrokerRuntime type, String name, List<String> variablesToPass) {
        this.type = type;
        this.name = name;
        this.variablesToPass = variablesToPass;
    }

    public CustomBrokerRuntime getType() {
        return type;
    }

    public void setType(CustomBrokerRuntime type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getVariablesToPass() {
        return variablesToPass;
    }

    public void setVariablesToPass(List<String> variablesToPass) {
        this.variablesToPass = variablesToPass;
    }
}
