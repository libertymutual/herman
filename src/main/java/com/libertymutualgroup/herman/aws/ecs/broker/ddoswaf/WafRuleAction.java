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

import com.amazonaws.services.waf.model.WafActionType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WafRuleAction {

    private String id;
    private WafActionType action;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public WafActionType getAction() {
        return action;
    }

    public void setAction(WafActionType action) {
        this.action = action;
    }

    public WafRuleAction withId(final String id) {
        this.id = id;
        return this;
    }

    public WafRuleAction withAction(final WafActionType action) {
        this.action = action;
        return this;
    }

    @Override
    public String toString() {
        return "WafRuleAction{" +
            "id='" + id + '\'' +
            ", action=" + action +
            '}';
    }
}
