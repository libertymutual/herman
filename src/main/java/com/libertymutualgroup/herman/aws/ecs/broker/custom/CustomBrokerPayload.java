package com.libertymutualgroup.herman.aws.ecs.broker.custom;

import com.fasterxml.jackson.databind.JsonNode;
import com.libertymutualgroup.herman.aws.ecs.EcsPushDefinition;
import com.libertymutualgroup.herman.aws.ecs.cluster.EcsClusterMetadata;

public class CustomBrokerPayload {
    private EcsPushDefinition pushDefinition;
    private EcsClusterMetadata clusterMetadata;
    private JsonNode brokerDefinition;

    public CustomBrokerPayload(EcsPushDefinition pushDefinition, EcsClusterMetadata clusterMetadata, JsonNode brokerDefinition) {
        this.pushDefinition = pushDefinition;
        this.clusterMetadata = clusterMetadata;
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

    public EcsClusterMetadata getClusterMetadata() {
        return clusterMetadata;
    }

    public void setClusterMetadata(EcsClusterMetadata clusterMetadata) {
        this.clusterMetadata = clusterMetadata;
    }
}
