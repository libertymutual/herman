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
package com.libertymutualgroup.herman.aws.lambda;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LambdaPermission {

    @JsonProperty("Action")
    public String action;
    @JsonProperty("EventSourceToken")
    public String eventSourceToken;
    @JsonProperty("Effect")
    public String effect;
    @JsonProperty("Principal")
    public String principal;
    @JsonProperty("Qualifier")
    public String qualifier;
    @JsonProperty("SourceArn")
    public String sourceArn;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEventSourceToken() {
        return eventSourceToken;
    }

    public void setEventSourceToken(String eventSourceToken) {
        this.eventSourceToken = eventSourceToken;
    }

    public String getEffect() {
        return effect;
    }

    public void setEffect(String effect) {
        this.effect = effect;
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    public String getQualifier() {
        return qualifier;
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

    public String getSourceArn() {
        return sourceArn;
    }

    public void setSourceArn(String sourceArn) {
        this.sourceArn = sourceArn;
    }

    @Override
    public String toString() {
        return "LambdaPermission{" +
            "action=" + action +
            ", eventSourceToken='" + eventSourceToken + '\'' +
            ", effect='" + effect + '\'' +
            ", principal='" + principal + '\'' +
            ", qualifier='" + qualifier + '\'' +
            ", sourceArn='" + sourceArn + '\'' +
            '}';
    }
}
