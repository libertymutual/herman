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

public class S3Bucket {

    private String name;
    private String policyName;
    private S3EncryptionOption encryptionOption;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public S3EncryptionOption getEncryptionOption() {
        return encryptionOption;
    }

    public void setEncryptionOption(S3EncryptionOption encryptionOption) {
        this.encryptionOption = encryptionOption;
    }

    public S3Bucket withName(final String name) {
        this.name = name;
        return this;
    }

    public S3Bucket withPolicyName(final String policyName) {
        this.policyName = policyName;
        return this;
    }

    public S3Bucket withEncryptionOption(
        final S3EncryptionOption encryptionOption) {
        this.encryptionOption = encryptionOption;
        return this;
    }

    @Override
    public String toString() {
        return "S3Bucket{" +
            "name='" + name + '\'' +
            ", policyName='" + policyName + '\'' +
            ", encryptionOption=" + encryptionOption +
            '}';
    }
}
