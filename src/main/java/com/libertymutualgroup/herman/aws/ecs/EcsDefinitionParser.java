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
package com.libertymutualgroup.herman.aws.ecs;

import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.Projection;
import com.amazonaws.services.dynamodbv2.model.StreamSpecification;
import com.amazonaws.services.ecs.model.ContainerDefinition;
import com.amazonaws.services.ecs.model.LogConfiguration;
import com.amazonaws.services.ecs.model.LogDriver;
import com.amazonaws.services.ecs.model.PlacementConstraint;
import com.amazonaws.services.ecs.model.PlacementConstraintType;
import com.amazonaws.services.ecs.model.PlacementStrategy;
import com.amazonaws.services.ecs.model.PlacementStrategyType;
import com.amazonaws.services.ecs.model.PortMapping;
import com.amazonaws.services.ecs.model.TaskDefinitionPlacementConstraint;
import com.amazonaws.services.ecs.model.TaskDefinitionPlacementConstraintType;
import com.amazonaws.services.ecs.model.Ulimit;
import com.amazonaws.services.ecs.model.UlimitName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.libertymutualgroup.herman.aws.AwsExecException;
import com.libertymutualgroup.herman.aws.ecs.broker.dynamodb.DynamoDBMixIns;

import java.util.List;

public class EcsDefinitionParser {

    private PropertyHandler handler;

    public EcsDefinitionParser(PropertyHandler handler) {
        this.handler = handler;
    }

    public EcsPushDefinition parse(String template, boolean isJson) {

        ObjectMapper mapper = null;
        if (isJson) {
            mapper = new ObjectMapper();
        } else {
            mapper = new ObjectMapper(new YAMLFactory());
        }

        mapper.addMixIn(PortMapping.class, IgnoreTransportSetValueObjMixIn.class);
        mapper.addMixIn(Ulimit.class, IgnoreUlimitObjMixIn.class);
        mapper.addMixIn(PlacementConstraint.class, IgnorePlacementConstraint.class);
        mapper.addMixIn(LogConfiguration.class, IgnoreLogConfigLogDriver.class);
        mapper.addMixIn(PlacementStrategy.class, IgnorePlaceStrategyType.class);
        mapper.addMixIn(TaskDefinitionPlacementConstraint.class, IgnoreTaskPlacementConstraint.class);
        mapper.addMixIn(KeySchemaElement.class, DynamoDBMixIns.class);
        mapper.addMixIn(StreamSpecification.class, DynamoDBMixIns.class);
        mapper.addMixIn(AttributeDefinition.class, DynamoDBMixIns.class);
        mapper.addMixIn(Projection.class, DynamoDBMixIns.class);

        EcsPushDefinition ecsPushDefinition;
        try {
            // parse out any YAML comments
            template = mapper.writeValueAsString(mapper.readValue(template, Object.class));
            ecsPushDefinition = mapper.readValue(handler.mapInProperties(template), EcsPushDefinition.class);
        } catch (Exception e) {
            throw new AwsExecException(e);
        }
        patchJsonLogDefintionParseError(ecsPushDefinition.getContainerDefinitions());

        return ecsPushDefinition;
    }


    private void patchJsonLogDefintionParseError(List<ContainerDefinition> containers) {
        for (ContainerDefinition container : containers) {
            if (container.getLogConfiguration() != null) {
                LogConfiguration config = container.getLogConfiguration();
                if (config.getOptions() != null && config.getLogDriver() == null) {
                    if (config.getOptions().get("splunk-url") != null) {
                        config.setLogDriver("splunk");
                    } else if (config.getOptions().get("max-size") != null) {
                        config.setLogDriver("json-file");
                    }
                }
            }
        }
    }

    abstract class IgnoreTransportSetValueObjMixIn extends PortMapping {

        @Override
        @JsonProperty("protocol")
        public void setProtocol(String protocol) {
            super.setProtocol(protocol);
        }
    }

    interface IgnoreUlimitObjMixIn {

        @JsonIgnore
        void setName(UlimitName name);

        @JsonProperty("name")
        void setName(String name);
    }

    interface IgnorePlacementConstraint {

        @JsonIgnore
        void setType(PlacementConstraintType type);
    }

    interface IgnoreTaskPlacementConstraint {

        @JsonIgnore
        void setType(TaskDefinitionPlacementConstraintType type);
    }

    interface IgnoreLogConfigLogDriver {

        @JsonIgnore
        void setLogDriver(LogDriver logDriver);
    }

    interface IgnorePlaceStrategyType {

        @JsonIgnore
        void setType(PlacementStrategyType type);
    }
}
