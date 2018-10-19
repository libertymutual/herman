package com.libertymutualgroup.herman.aws.ecs.broker.dynamodb;

import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProjectionType;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.StreamViewType;
import com.fasterxml.jackson.annotation.JsonSetter;

public interface DynamoDBMixIns {

    @JsonSetter
    void setKeyType(KeyType keyType);

    @JsonSetter
    void setStreamViewType(StreamViewType streamViewType);

    @JsonSetter
    void setAttributeType(ScalarAttributeType attributeType);

    @JsonSetter
    void setProjectionType(ProjectionType projectionType);
}
