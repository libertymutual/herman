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
package com.libertymutualgroup.herman.task.cli.ecs;

import com.libertymutualgroup.herman.aws.ecs.CliPropertyHandler;
import com.libertymutualgroup.herman.aws.ecs.PropertyHandler;
import com.libertymutualgroup.herman.logging.HermanLogger;
import com.libertymutualgroup.herman.task.ecs.ECSPushPropertyFactory;
import com.libertymutualgroup.herman.task.ecs.ECSPushTaskProperties;

import java.util.Map;

public class ECSPushTask {
    private HermanLogger logger;

    public ECSPushTask(HermanLogger logger) {
        this.logger = logger;
    }

    public void doExecute(String rootPath, int timeout, String environmentName, Map<String, String> customVariables) {
        ECSPushTaskProperties taskProperties = ECSPushPropertyFactory.getTaskProperties();

        PropertyHandler propertyHandler = new CliPropertyHandler(logger, environmentName, rootPath, customVariables);
    }


}
