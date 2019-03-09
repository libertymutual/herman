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
package com.libertymutualgroup.herman.aws.ecs;

import com.libertymutualgroup.herman.logging.HermanLogger;

import java.util.Map;

public class CliPropertyHandler extends PropertyHandler {
    private Map<String,String> customVariables;

    public CliPropertyHandler(HermanLogger hermanLogger, String environmentName, String rootDirectory, Map<String, String> customVariables) {
        super(hermanLogger, environmentName, rootDirectory);
        this.customVariables = customVariables;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.libertymutualgroup.herman.aws.ecs.PropertyHandler#lookupVariable(java.lang.String)
     */
    @Override
    public String lookupVariable(String key) {
        if (this.props.containsKey(key)) {
            return this.props.getProperty(key);
        }
        return this.customVariables.getOrDefault(key, null);
    }
}
