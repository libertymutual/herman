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
package com.libertymutualgroup.herman.aws.ecs.broker.ddoswaf;

import java.util.List;

public class DdosWafBrokerRequest {

    private String appName;
    private String elbResourceArn;
    private List<String> rules;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getElbResourceArn() {
        return elbResourceArn;
    }

    public void setElbResourceArn(String elbResourceArn) {
        this.elbResourceArn = elbResourceArn;
    }

    public List<String> getRules() {
        return rules;
    }

    public void setRules(List<String> rules) {
        this.rules = rules;
    }

    public DdosWafBrokerRequest withAppName(final String appName) {
        this.appName = appName;
        return this;
    }

    public DdosWafBrokerRequest withElbResourceArn(final String elbResourceArn) {
        this.elbResourceArn = elbResourceArn;
        return this;
    }

    public DdosWafBrokerRequest withRules(final List<String> rules) {
        this.rules = rules;
        return this;
    }

    @Override
    public String toString() {
        return "DdosWafBrokerRequest{" +
            "appName='" + appName + '\'' +
            ", elbResourceArn='" + elbResourceArn + '\'' +
            ", rules=" + rules +
            '}';
    }
}