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
import com.libertymutualgroup.herman.aws.ecs.broker.kms.KmsAppDefinition;
import com.libertymutualgroup.herman.aws.tags.HermanTag;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class S3InjectConfiguration implements KmsAppDefinition {

    private String appName;
    private String sbu;
    private String org;
    private String policyName;
    private Boolean website = false;
    private S3EncryptionOption encryptionOption;
    private List<S3EventConfiguration> lambdaNotifications;
    private List<S3EventConfiguration> snsNotifications;
    private Boolean createBucketKey;
    private String kmsKeyArn;
    private String kmsKeyName;
    private List<HermanTag> tags;

    // Index and Error files used for website buckets
    private String indexFile = "index.html";
    private String errorFile = "error.html";

    @Override
    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getSbu() {
        return sbu;
    }

    public void setSbu(String sbu) {
        this.sbu = sbu;
    }

    public String getOrg() {
        return org;
    }

    public void setOrg(String org) {
        this.org = org;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public Boolean getWebsite() {
        return website;
    }

    public void setWebsite(Boolean website) {
        this.website = website;
    }

    public S3EncryptionOption getEncryptionOption() {
        return encryptionOption;
    }

    public void setEncryptionOption(S3EncryptionOption encryptionOption) {
        this.encryptionOption = encryptionOption;
    }

    public List<S3EventConfiguration> getLambdaNotifications() {
        return lambdaNotifications;
    }

    public void setLambdaNotifications(
        List<S3EventConfiguration> lambdaNotifications) {
        this.lambdaNotifications = lambdaNotifications;
    }

    public Boolean getCreateBucketKey() {
        return createBucketKey;
    }

    public void setCreateBucketKey(Boolean createBucketKey) {
        this.createBucketKey = createBucketKey;
    }

    public String getKmsKeyArn() {
        return kmsKeyArn;
    }

    public void setKmsKeyArn(String kmsKeyArn) {
        this.kmsKeyArn = kmsKeyArn;
    }

    @Override
    public String getKmsKeyName() {
        return kmsKeyName;
    }

    public void setKmsKeyName(String kmsKeyName) {
        this.kmsKeyName = kmsKeyName;
    }

    public String getIndexFile() {
        return indexFile;
    }

    public void setIndexFile(String indexFile) {
        this.indexFile = indexFile;
    }

    public String getErrorFile() {
        return errorFile;
    }

    public void setErrorFile(String errorFile) {
        this.errorFile = errorFile;
    }

    public List<HermanTag> getTags() {
        return tags;
    }

    public void setTags(List<HermanTag> tags) {
        this.tags = tags;
    }

    public List<S3EventConfiguration> getSnsNotifications() {
        return snsNotifications;
    }

    public void setSnsNotifications(
        List<S3EventConfiguration> snsNotifications) {
        this.snsNotifications = snsNotifications;
    }

    public S3InjectConfiguration withAppName(final String appName) {
        this.appName = appName;
        return this;
    }

    public S3InjectConfiguration withSbu(final String sbu) {
        this.sbu = sbu;
        return this;
    }

    public S3InjectConfiguration withOrg(final String org) {
        this.org = org;
        return this;
    }

    public S3InjectConfiguration withPolicyName(final String policyName) {
        this.policyName = policyName;
        return this;
    }

    public S3InjectConfiguration withWebsite(final Boolean website) {
        this.website = website;
        return this;
    }

    public S3InjectConfiguration withEncryptionOption(
        final S3EncryptionOption encryptionOption) {
        this.encryptionOption = encryptionOption;
        return this;
    }

    public S3InjectConfiguration withLambdaNotifications(
        final List<S3EventConfiguration> lambdaNotifications) {
        this.lambdaNotifications = lambdaNotifications;
        return this;
    }

    public S3InjectConfiguration withCreateBucketKey(final Boolean createBucketKey) {
        this.createBucketKey = createBucketKey;
        return this;
    }

    public S3InjectConfiguration withKmsKeyArn(final String kmsKeyArn) {
        this.kmsKeyArn = kmsKeyArn;
        return this;
    }

    public S3InjectConfiguration withKmsKeyName(final String kmsKeyName) {
        this.kmsKeyName = kmsKeyName;
        return this;
    }

    public S3InjectConfiguration withIndexFile(final String indexFile) {
        this.indexFile = indexFile;
        return this;
    }

    public S3InjectConfiguration withErrorFile(final String errorFile) {
        this.errorFile = errorFile;
        return this;
    }

    public S3InjectConfiguration withSnsNotifications(final List<S3EventConfiguration> snsNotifications) {
        this.snsNotifications = snsNotifications;
        return this;
    }

    @Override
    public String toString() {
        return "S3InjectConfiguration{" +
            "appName='" + appName + '\'' +
            ", sbu='" + sbu + '\'' +
            ", org='" + org + '\'' +
            ", policyName='" + policyName + '\'' +
            ", website=" + website +
            ", encryptionOption=" + encryptionOption +
            ", lambdaNotifications=" + lambdaNotifications +
            ", snsNotifications=" + snsNotifications +
            ", createBucketKey=" + createBucketKey +
            ", kmsKeyArn='" + kmsKeyArn + '\'' +
            ", kmsKeyName='" + kmsKeyName + '\'' +
            ", tags=" + tags +
            ", indexFile='" + indexFile + '\'' +
            ", errorFile='" + errorFile + '\'' +
            '}';
    }
}