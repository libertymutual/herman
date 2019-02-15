package com.libertymutualgroup.herman.aws.ecs;

public class CustomBrokerDefinition {
    private String name;
    private Object properties;

    public CustomBrokerDefinition() {
    }

    public CustomBrokerDefinition(String name, Object properties) {
        this.name = name;
        this.properties = properties;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getProperties() {
        return properties;
    }

    public void setProperties(Object properties) {
        this.properties = properties;
    }
}
