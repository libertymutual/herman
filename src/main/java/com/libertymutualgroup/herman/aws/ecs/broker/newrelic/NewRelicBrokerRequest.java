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
public class NewRelicBrokerRequest {

    private String policyName;
    private String newRelicApplicationName;
    private String nrLicenseKey;
    private NewRelicApplicationDeploymentRequest deployment;
    private NewRelicConfiguration configuration;

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public String getNewRelicApplicationName() {
        return newRelicApplicationName;
    }

    public void setNewRelicApplicationName(String newRelicApplicationName) {
        this.newRelicApplicationName = newRelicApplicationName;
    }

    public String getNrLicenseKey() {
        return nrLicenseKey;
    }

    public void setNrLicenseKey(String nrLicenseKey) {
        this.nrLicenseKey = nrLicenseKey;
    }

    public NewRelicApplicationDeploymentRequest getDeployment() {
        return deployment;
    }

    public void setDeployment(NewRelicApplicationDeploymentRequest deployment) {
        this.deployment = deployment;
    }

    public NewRelicConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(NewRelicConfiguration configuration) {
        this.configuration = configuration;
    }

    public NewRelicBrokerRequest withPolicyName(final String policyName) {
        this.policyName = policyName;
        return this;
    }

    public NewRelicBrokerRequest withNewRelicApplicationName(final String newRelicApplicationName) {
        this.newRelicApplicationName = newRelicApplicationName;
        return this;
    }

    public NewRelicBrokerRequest withNrLicenseKey(final String nrLicenseKey) {
        this.nrLicenseKey = nrLicenseKey;
        return this;
    }

    public NewRelicBrokerRequest withDeployment(
        final NewRelicApplicationDeploymentRequest deployment) {
        this.deployment = deployment;
        return this;
    }

    public NewRelicBrokerRequest withConfiguration(
        final NewRelicConfiguration configuration) {
        this.configuration = configuration;
        return this;
    }

    @Override
    public String toString() {
        return "NewRelicBrokerRequest{" +
            "policyName='" + policyName + '\'' +
            ", newRelicApplicationName='" + newRelicApplicationName + '\'' +
            ", nrLicenseKey='" + nrLicenseKey + '\'' +
            ", deployment=" + deployment +
            ", configuration=" + configuration +
            '}';
    }
}
