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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewRelicConfiguration {

    private String conditions;
    private String channels;
    private String rdsPluginsConditions;
    private String nrqlConditions;
    private String infrastructureConditions;
    private String dbName;
    private String apdex;

    public String getConditions() {
        return conditions;
    }

    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    public String getChannels() {
        return channels;
    }

    public void setChannels(String channels) {
        this.channels = channels;
    }

    public String getRdsPluginsConditions() {
        return rdsPluginsConditions;
    }

    public void setRdsPluginsConditions(String rdsPluginsConditions) {
        this.rdsPluginsConditions = rdsPluginsConditions;
    }

    public String getNrqlConditions() {
        return nrqlConditions;
    }

    public void setNrqlConditions(String nrqlConditions) {
        this.nrqlConditions = nrqlConditions;
    }

    public String getInfrastructureConditions() {
        return infrastructureConditions;
    }

    public void setInfrastructureConditions(String infrastructureConditions) {
        this.infrastructureConditions = infrastructureConditions;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getApdex() {
        return apdex;
    }

    public void setApdex(String apdex) {
        this.apdex = apdex;
    }

    @Override
    public String toString() {
        return "NewRelicConfiguration{" +
            "conditions='" + conditions + '\'' +
            ", channels='" + channels + '\'' +
            ", rdsPluginsConditions='" + rdsPluginsConditions + '\'' +
            ", nrqlConditions='" + nrqlConditions + '\'' +
            ", infrastructureConditions='" + infrastructureConditions + '\'' +
            ", dbName='" + dbName + '\'' +
            ", apdex='" + apdex + '\'' +
            '}';
    }

    public NewRelicConfiguration withConditions(final String conditions) {
        this.conditions = conditions;
        return this;
    }

    public NewRelicConfiguration withChannels(final String channels) {
        this.channels = channels;
        return this;
    }

    public NewRelicConfiguration withRdsPluginsConditions(final String rdsPluginsConditions) {
        this.rdsPluginsConditions = rdsPluginsConditions;
        return this;
    }

    public NewRelicConfiguration withNrqlConditions(final String nrqlConditions) {
        this.nrqlConditions = nrqlConditions;
        return this;
    }

    public NewRelicConfiguration withInfrastructureConditions(final String infrastructureConditions) {
        this.infrastructureConditions = infrastructureConditions;
        return this;
    }

    public NewRelicConfiguration withDbName(final String dbName) {
        this.dbName = dbName;
        return this;
    }

    public NewRelicConfiguration withApdex(final String apdex) {
        this.apdex = apdex;
        return this;
    }
}