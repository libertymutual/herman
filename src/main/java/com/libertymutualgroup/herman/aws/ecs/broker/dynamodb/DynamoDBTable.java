/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.libertymutualgroup.herman.aws.ecs.broker.dynamodb;

import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.LocalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.SSESpecification;
import com.amazonaws.services.dynamodbv2.model.StreamSpecification;
import java.util.List;
import java.util.Objects;

public class DynamoDBTable {

    private static final long defaultReadCapacity = 1L;
    private static final long defaultWriteCapacity = 1L;
    private List<AttributeDefinition> attributes;
    private List<GlobalSecondaryIndex> globalSecondaryIndexes;
    private List<KeySchemaElement> keySchema;
    private List<LocalSecondaryIndex> localSecondaryIndexes;
    private ProvisionedThroughput provisionedThroughput;
    private SSESpecification sseSpecification;
    private StreamSpecification streamSpecification;
    private String tableName;

    public static long getDefaultReadCapacity() {
        return defaultReadCapacity;
    }

    public static long getDefaultWriteCapacity() {
        return defaultWriteCapacity;
    }

    public List<AttributeDefinition> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<AttributeDefinition> attributes) {
        this.attributes = attributes;
    }

    public List<GlobalSecondaryIndex> getGlobalSecondaryIndexes() {
        return globalSecondaryIndexes;
    }

    public void setGlobalSecondaryIndexes(List<GlobalSecondaryIndex> globalSecondaryIndexes) {
        this.globalSecondaryIndexes = globalSecondaryIndexes;
    }

    public List<KeySchemaElement> getKeySchema() {
        return keySchema;
    }

    public void setKeySchema(List<KeySchemaElement> keySchema) {
        this.keySchema = keySchema;
    }

    public List<LocalSecondaryIndex> getLocalSecondaryIndexes() {
        return localSecondaryIndexes;
    }

    public void setLocalSecondaryIndexes(List<LocalSecondaryIndex> localSecondaryIndexes) {
        this.localSecondaryIndexes = localSecondaryIndexes;
    }

    public ProvisionedThroughput getProvisionedThroughput() {
        return provisionedThroughput;
    }

    public void setProvisionedThroughput(ProvisionedThroughput provisionedThroughput) {
        this.provisionedThroughput = provisionedThroughput;
    }

    public SSESpecification getSseSpecification() {
        return sseSpecification;
    }

    public void setSseSpecification(SSESpecification sseSpecification) {
        this.sseSpecification = sseSpecification;
    }

    public StreamSpecification getStreamSpecification() {
        return streamSpecification;
    }

    public void setStreamSpecification(StreamSpecification streamSpecification) {
        this.streamSpecification = streamSpecification;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public String toString() {
        return "DynamoDBTable{" +
            "attributes=" + attributes +
            ", globalSecondaryIndexes=" + globalSecondaryIndexes +
            ", keySchema=" + keySchema +
            ", localSecondaryIndexes=" + localSecondaryIndexes +
            ", provisionedThroughput=" + provisionedThroughput +
            ", sseSpecification=" + sseSpecification +
            ", streamSpecification=" + streamSpecification +
            ", tableName='" + tableName + '\'' +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DynamoDBTable that = (DynamoDBTable) o;
        return Objects.equals(attributes, that.attributes) &&
            Objects.equals(globalSecondaryIndexes, that.globalSecondaryIndexes) &&
            Objects.equals(keySchema, that.keySchema) &&
            Objects.equals(localSecondaryIndexes, that.localSecondaryIndexes) &&
            Objects.equals(provisionedThroughput, that.provisionedThroughput) &&
            Objects.equals(sseSpecification, that.sseSpecification) &&
            Objects.equals(streamSpecification, that.streamSpecification) &&
            Objects.equals(tableName, that.tableName);
    }

    @Override
    public int hashCode() {

        return Objects.hash(attributes, globalSecondaryIndexes, keySchema, localSecondaryIndexes, provisionedThroughput,
            sseSpecification, streamSpecification, tableName);
    }
}
