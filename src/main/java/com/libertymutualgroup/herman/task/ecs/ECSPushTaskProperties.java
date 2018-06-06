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
package com.libertymutualgroup.herman.task.ecs;

import com.libertymutualgroup.herman.aws.ecs.loadbalancing.SSLCertificate;
import com.libertymutualgroup.herman.aws.ecs.logging.SplunkInstance;
import com.libertymutualgroup.herman.task.common.CommonTaskProperties;
import com.libertymutualgroup.herman.task.newrelic.NewRelicBrokerProperties;
import java.util.List;

public class ECSPushTaskProperties extends CommonTaskProperties {

    private NewRelicBrokerProperties newRelic;
    private List<SSLCertificate> sslCertificates;
    private String ecsConsoleLinkPattern;
    private List<SplunkInstance> splunkInstances;
    private String rdsCredentialBrokerImage;
    private String dnsBrokerLambda;

    public NewRelicBrokerProperties getNewRelic() {
        return newRelic;
    }

    public void setNewRelic(NewRelicBrokerProperties newRelic) {
        this.newRelic = newRelic;
    }

    public List<SSLCertificate> getSslCertificates() {
        return sslCertificates;
    }

    public void setSslCertificates(
        List<SSLCertificate> sslCertificates) {
        this.sslCertificates = sslCertificates;
    }

    public String getEcsConsoleLinkPattern() {
        return ecsConsoleLinkPattern;
    }

    public void setEcsConsoleLinkPattern(String ecsConsoleLinkPattern) {
        this.ecsConsoleLinkPattern = ecsConsoleLinkPattern;
    }

    public List<SplunkInstance> getSplunkInstances() {
        return splunkInstances;
    }

    public void setSplunkInstances(List<SplunkInstance> splunkInstances) {
        this.splunkInstances = splunkInstances;
    }

    public String getRdsCredentialBrokerImage() {
        return rdsCredentialBrokerImage;
    }

    public void setRdsCredentialBrokerImage(String rdsCredentialBrokerImage) {
        this.rdsCredentialBrokerImage = rdsCredentialBrokerImage;
    }

    public String getDnsBrokerLambda() {
        return dnsBrokerLambda;
    }

    public void setDnsBrokerLambda(String dnsBrokerLambda) {
        this.dnsBrokerLambda = dnsBrokerLambda;
    }

    public ECSPushTaskProperties withNewRelic(
        final NewRelicBrokerProperties newRelic) {
        this.newRelic = newRelic;
        return this;
    }

    public ECSPushTaskProperties withSslCertificates(
        final List<SSLCertificate> sslCertificates) {
        this.sslCertificates = sslCertificates;
        return this;
    }

    public ECSPushTaskProperties withEcsConsoleLinkPattern(final String ecsConsoleLinkPattern) {
        this.ecsConsoleLinkPattern = ecsConsoleLinkPattern;
        return this;
    }

    public ECSPushTaskProperties withSplunkInstances(
        final List<SplunkInstance> splunkInstances) {
        this.splunkInstances = splunkInstances;
        return this;
    }

    public ECSPushTaskProperties withRdsCredentialBrokerImage(final String rdsCredentialBrokerImage) {
        this.rdsCredentialBrokerImage = rdsCredentialBrokerImage;
        return this;
    }

    public ECSPushTaskProperties withDnsBrokerLambda(final String dnsBrokerLambda) {
        this.dnsBrokerLambda = dnsBrokerLambda;
        return this;
    }

    @Override
    public String toString() {
        return "ECSPushTaskProperties{" +
            "newRelic=" + newRelic +
            ", sslCertificates=" + sslCertificates +
            ", ecsConsoleLinkPattern='" + ecsConsoleLinkPattern + '\'' +
            ", splunkInstances=" + splunkInstances +
            ", rdsCredentialBrokerImage='" + rdsCredentialBrokerImage + '\'' +
            ", dnsBrokerLambda='" + dnsBrokerLambda + '\'' +
            "} " + super.toString();
    }

    @Override
    public ECSPushTaskProperties withCompany(final String company) {
        setCompany(company);
        return this;
    }

    @Override
    public ECSPushTaskProperties withSbu(final String sbu) {
        setSbu(sbu);
        return this;
    }

    @Override
    public ECSPushTaskProperties withOrg(final String org) {
        setOrg(org);
        return this;
    }

    @Override
    public ECSPushTaskProperties withEngine(final String engine) {
        setEngine(engine);
        return this;
    }
}