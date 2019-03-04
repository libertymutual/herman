package com.libertymutualgroup.herman.aws.ecs.broker.custom;

import com.fasterxml.jackson.databind.JsonNode;
import com.libertymutualgroup.herman.aws.ecs.EcsPushDefinition;

public class CustomBrokerPayload {
    private EcsPushDefinition pushDefinition;
    private JsonNode brokerDefinition;

    public CustomBrokerPayload(EcsPushDefinition pushDefinition, JsonNode brokerDefinition) {
        this.pushDefinition = pushDefinition;
        this.brokerDefinition = brokerDefinition;
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
}
