package com.libertymutualgroup.herman.aws.ecs.broker.custom;

import com.fasterxml.jackson.databind.JsonNode;
import com.libertymutualgroup.herman.aws.ecs.EcsPushDefinition;

public class CustomBrokerPayload {
    private EcsPushDefinition pushDefinition;
    private JsonNode brokerDefinition;
    private JsonNode environment;

    public CustomBrokerPayload(EcsPushDefinition pushDefinition, JsonNode brokerDefinition, JsonNode environment) {
        this.pushDefinition = pushDefinition;
        this.brokerDefinition = brokerDefinition;
        this.environment = environment;
    }

    public EcsPushDefinition getPushDefinition() {
        return pushDefinition;
    }

    public void setPushDefinition(EcsPushDefinition pushDefinition) {
        this.pushDefinition = pushDefinition;
    }

    public JsonNode getBrokerDefinition() {
        return brokerDefinition;
    }

    public void setBrokerDefinition(JsonNode brokerDefinition) {
        this.brokerDefinition = brokerDefinition;
    }

    public JsonNode getEnvironment() {
        return environment;
    }

    public void setEnvironment(JsonNode environment) {
        this.environment = environment;
    }
}
