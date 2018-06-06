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
package com.libertymutualgroup.herman.aws.ecs.broker.newrelic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.libertymutualgroup.herman.task.newrelic.NewRelicBrokerProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NewRelicBrokerConfiguration {

    @JsonProperty("newRelic")
    private NewRelicBrokerProperties brokerProperties;

    public NewRelicBrokerProperties getBrokerProperties() {
        return brokerProperties;
    }

    public void setBrokerProperties(NewRelicBrokerProperties brokerProperties) {
        this.brokerProperties = brokerProperties;
    }

    public NewRelicBrokerConfiguration withBrokerProperties(
        final NewRelicBrokerProperties brokerProperties) {
        this.brokerProperties = brokerProperties;
        return this;
    }

    @Override
    public String toString() {
        return "NewRelicBrokerConfiguration{" +
            "brokerProperties=" + brokerProperties +
            '}';
    }
}