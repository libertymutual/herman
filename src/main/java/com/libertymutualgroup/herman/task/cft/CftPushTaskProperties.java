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
package com.libertymutualgroup.herman.task.cft;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.libertymutualgroup.herman.task.common.CommonTaskProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CftPushTaskProperties extends CommonTaskProperties {

    private String cftPushVariableBrokerLambda;

    public String getCftPushVariableBrokerLambda() {
        return cftPushVariableBrokerLambda;
    }

    public void setCftPushVariableBrokerLambda(String cftPushVariableBrokerLambda) {
        this.cftPushVariableBrokerLambda = cftPushVariableBrokerLambda;
    }


    public CftPushTaskProperties withCftPushVariableBrokerLambda(String cftPushVariableBrokerLambda) {
        this.cftPushVariableBrokerLambda = cftPushVariableBrokerLambda;
        return this;
    }

    @Override
    public String toString() {
        return "CftPushTaskProperties{" +
            "cftPushVariableBrokerLambda=" + cftPushVariableBrokerLambda +
            "} " + super.toString();
    }
}
