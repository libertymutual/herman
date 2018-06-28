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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;

public class ECSPushPropertyFactory {
    private final static String TASK_CONFIG_FILE = "/config/plugin-tasks.yml";

    public static ECSPushTaskProperties getTaskProperties() {
        try {
            InputStream ecsPushTaskPropertiesStream = ECSPushPropertyFactory.class.getResourceAsStream(TASK_CONFIG_FILE);
            String ecsPushTaskPropertiesYml = IOUtils.toString(ecsPushTaskPropertiesStream);
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            return objectMapper.readValue(ecsPushTaskPropertiesYml, ECSPushTaskProperties.class);
        } catch (Exception ex) {
            throw new RuntimeException("Error getting ECS Push Task Properties from " + TASK_CONFIG_FILE, ex);
        }
    }
}
