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

import com.amazonaws.services.ecs.model.KeyValuePair;
import com.libertymutualgroup.herman.aws.ecs.broker.iam.IamAppDefinition;
import com.libertymutualgroup.herman.aws.ecs.broker.kms.KmsAppDefinition;
import java.util.List;

public class LambdaInjectConfiguration implements IamAppDefinition, KmsAppDefinition {

    private String functionName;
    private String zipFileName;
    private List<KeyValuePair> environment;
    private String handler;
    private Integer memorySize = 128;
    private String runtime;
    private Integer timeout = 5;
    private String vpcId;
    private String iamPolicy;
    private String useKms = "false";
    private String kmsKeyName;

    @Override
    public String getAppName() {
        return functionName;
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public String getZipFileName() {
        return zipFileName;
    }

    public void setZipFileName(String zipFileName) {
        this.zipFileName = zipFileName;
    }

    public List<KeyValuePair> getEnvironment() {
        return environment;
    }

    public void setEnvironment(List<KeyValuePair> environment) {
        this.environment = environment;
    }

    public String getHandler() {
        return handler;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }

    public Integer getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(Integer memorySize) {
        this.memorySize = memorySize;
    }

    public String getRuntime() {
        return runtime;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    public String getIamPolicy() {
        return iamPolicy;
    }

    public void setIamPolicy(String iamPolicy) {
        this.iamPolicy = iamPolicy;
    }

    public Boolean getUseKms() {
        return "true".equalsIgnoreCase(useKms);
    }

    public void setUseKms(String useKms) {
        this.useKms = useKms;
    }

    @Override
    public String getKmsKeyName() {
        return kmsKeyName;
    }

    public void setKmsKeyName(String kmsKeyName) {
        this.kmsKeyName = kmsKeyName;
    }
}
