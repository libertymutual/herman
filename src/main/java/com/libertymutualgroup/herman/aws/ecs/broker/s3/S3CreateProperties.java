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
package com.libertymutualgroup.herman.aws.ecs.broker.s3;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.libertymutualgroup.herman.aws.CommonTaskProperties;
import com.libertymutualgroup.herman.task.ecs.ECSPushTaskProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class S3CreateProperties extends CommonTaskProperties {

    private S3BrokerProperties s3;
    private String logsBucket;

    public S3CreateProperties fromECSPushTaskProperties(ECSPushTaskProperties taskProperties) {
        return this
            .withCompany(taskProperties.getCompany())
            .withEngine(taskProperties.getEngine())
            .withOrg(taskProperties.getOrg())
            .withSbu(taskProperties.getSbu())
            .withS3(taskProperties.getS3());
    }

    public S3BrokerProperties getS3() {
        return s3;
    }

    public void setS3(S3BrokerProperties s3) {
        this.s3 = s3;
    }

    public String getLogsBucket() {
        return logsBucket;
    }

    public void setLogsBucket(String logsBucket) {
        this.logsBucket = logsBucket;
    }

    public S3CreateProperties withS3(final S3BrokerProperties s3) {
        this.s3 = s3;
        return this;
    }

    public S3CreateProperties withLogsBucket(final String logsBucket) {
        this.logsBucket = logsBucket;
        return this;
    }

    @Override
    public String toString() {
        return "S3CreateProperties{" +
            "s3=" + s3 + '\'' +
            ", logsBucket=" + logsBucket + '\'' +
            "} " + super.toString();
    }

    @Override
    public S3CreateProperties withCompany(final String company) {
        this.setCompany(company);
        return this;
    }

    @Override
    public S3CreateProperties withSbu(final String sbu) {
        this.setSbu(sbu);
        return this;
    }

    @Override
    public S3CreateProperties withOrg(final String org) {
        this.setOrg(org);
        return this;
    }

    @Override
    public S3CreateProperties withEngine(final String engine) {
        this.setEngine(engine);
        return this;
    }
}